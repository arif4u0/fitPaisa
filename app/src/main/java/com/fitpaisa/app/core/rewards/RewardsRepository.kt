package com.fitpaisa.app.core.rewards

import com.fitpaisa.app.BuildConfig
import com.fitpaisa.app.core.auth.SupabaseClientProvider
import com.fitpaisa.app.core.network.EdgeFunctions
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Response from the sync-steps edge function. */
@Serializable
data class ConvertResponse(
    val ok: Boolean = false,
    val deduped: Boolean = false,
    @SerialName("eligible_total") val eligibleTotal: Int = 0,
    @SerialName("new_steps") val newSteps: Int = 0,
    @SerialName("coins_held") val coinsHeld: Int = 0,
    val level: Int = 1,
    @SerialName("daily_cap") val dailyCap: Int = 0
)

/** Response from the get-balance edge function. */
@Serializable
data class BalanceResponse(
    val ok: Boolean = false,
    @SerialName("available_coins") val availableCoins: Int = 0,
    @SerialName("maturing_coins") val maturingCoins: Int = 0
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
 */
@Singleton
class RewardsRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider
) {

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
                if (response.status.value !in 200..299) {
                    return@withContext ConvertOutcome.Failed(
                        "Server busy (${response.status.value}). Try again."
                    )
                }
                val data = response.body<ConvertResponse>()
                if (!data.ok) return@withContext ConvertOutcome.Failed("Convert failed. Try again.")
                if (data.deduped || data.coinsHeld <= 0) {
                    ConvertOutcome.NothingNew
                } else {
                    ConvertOutcome.Converted(data.coinsHeld, data.newSteps)
                }
            } catch (e: Exception) {
                ConvertOutcome.Failed("No connection. Check internet and try again.")
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
                if (response.status.value !in 200..299) {
                    return@withContext BalanceResponse()
                }
                response.body<BalanceResponse>()
            } catch (e: Exception) {
                BalanceResponse()
            }
        }
}
