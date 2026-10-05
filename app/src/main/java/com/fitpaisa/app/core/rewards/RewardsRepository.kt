package com.fitpaisa.app.core.rewards

import com.fitpaisa.app.BuildConfig
import com.fitpaisa.app.core.auth.SupabaseClientProvider
import com.fitpaisa.app.core.network.EdgeFunctions
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Server balance. `ok=false` means it could not be read (treat as 0). */
data class BalanceResponse(
    val ok: Boolean = false,
    val availableCoins: Int = 0,
    val maturingCoins: Int = 0
)

/** Outcome of a Convert tap, in plain language for the UI. */
sealed interface ConvertOutcome {
    /** Server credited new coins. */
    data class Converted(val coins: Int, val newSteps: Int) : ConvertOutcome
    /** Nothing new to convert (already converted, or below 100 steps). */
    data object NothingNew : ConvertOutcome
    /** Something went wrong; the message is safe to show. */
    data class Failed(val message: String) : ConvertOutcome
}

/**
 * Talks to the reward edge functions. The server is the ONLY authority that
 * creates coins — this repository only sends step observations and reads back
 * what the server decided. It never computes a balance locally.
 *
 * Responses are parsed defensively (field by field, unknown fields ignored)
 * so a server-side schema change can never crash the app — worst case the
 * user sees an honest error message.
 */
@Singleton
class RewardsRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Sends today's observed step total to the server. The server caps it at
     * the user's level daily cap, credits only NEW steps since the last
     * convert, and posts held coins to the ledger. Safe to call repeatedly:
     * replays credit zero.
     */
    suspend fun convertTodaySteps(stepsToday: Int): ConvertOutcome =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val response = client.functions.invoke(
                    function = EdgeFunctions.SYNC_STEPS,
                    body = buildJsonObject {
                        put("local_date", LocalDate.now().toString())
                        put("observed_total", stepsToday.coerceAtLeast(0))
                        put("timezone", "Asia/Karachi")
                        put("app_version", BuildConfig.VERSION_NAME)
                        put("idempotency_key", "android-convert-${UUID.randomUUID()}")
                        put("source_summary", buildJsonObject {
                            put("source", "hardware_step_counter")
                        })
                    }
                )
                val status = response.status.value
                if (status == 401) {
                    return@withContext ConvertOutcome.Failed(
                        "Session expired. Please sign in again."
                    )
                }
                if (status !in 200..299) {
                    return@withContext ConvertOutcome.Failed(
                        "Server busy (code $status). Try again in a bit."
                    )
                }
                val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
                if (body["ok"]?.jsonPrimitive?.booleanOrNull != true) {
                    val err = body["error"]?.jsonPrimitive?.contentOrNull
                    return@withContext ConvertOutcome.Failed(
                        if (err == "unauthorized") "Session expired. Please sign in again."
                        else "Convert failed (${err ?: "unknown error"}). Try again."
                    )
                }
                if (body["deduped"]?.jsonPrimitive?.booleanOrNull == true) {
                    return@withContext ConvertOutcome.NothingNew
                }
                val coins = body["coins_held"]?.jsonPrimitive?.intOrNull ?: 0
                val newSteps = body["new_steps"]?.jsonPrimitive?.intOrNull ?: 0
                if (coins <= 0) ConvertOutcome.NothingNew
                else ConvertOutcome.Converted(coins, newSteps)
            } catch (e: Exception) {
                ConvertOutcome.Failed(
                    "Couldn't reach the server (${e.message ?: "network error"}). " +
                        "Check your internet and try again."
                )
            }
        }

    /** Reads the server balance (releases matured holds first). Never computed locally. */
    suspend fun getBalance(): BalanceResponse =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val response = client.functions.invoke(
                    function = EdgeFunctions.GET_BALANCE,
                    body = buildJsonObject { }
                )
                if (response.status.value !in 200..299) return@withContext BalanceResponse()
                val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
                BalanceResponse(
                    ok = body["ok"]?.jsonPrimitive?.booleanOrNull == true,
                    availableCoins = body["available_coins"]?.jsonPrimitive?.intOrNull ?: 0,
                    maturingCoins = body["maturing_coins"]?.jsonPrimitive?.intOrNull ?: 0
                )
            } catch (e: Exception) {
                BalanceResponse()
            }
        }
}
