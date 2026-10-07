package com.fitpaisa.app.core.rewards

import com.fitpaisa.app.BuildConfig
import com.fitpaisa.app.core.auth.SupabaseClientProvider
import com.fitpaisa.app.core.network.EdgeFunctions
import io.github.jan.supabase.auth.auth
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

/** A product in the FitCoin catalog. */
data class Product(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val coinPrice: Int,
    val stock: Int
)

/** Outcome of a redeem or cashout request, in plain language. */
sealed interface RequestOutcome {
    data class Success(val message: String) : RequestOutcome
    data class Failed(val message: String) : RequestOutcome
}

/** An admin-visible redemption or cashout row. */
data class AdminOrder(
    val id: String,
    val userId: String,
    val title: String,
    val coins: Int,
    val status: String,
    val detail: String,
    val createdAt: String
)

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
                // The SDK silently falls back to the anon key when there is no session,
                // which the server rejects with 401. Fail fast with a clear message instead.
                client.auth.awaitInitialization()
                val session = client.auth.currentSessionOrNull()
                    ?: return@withContext ConvertOutcome.Failed(
                        "You don't look signed in (no session). Please sign in again."
                    )
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
                // Note: functions.invoke() throws on HTTP errors (it does not return a 401
                // response), so auth failures arrive here as an exception whose message
                // mentions 401/Unauthorized. The server now returns a specific reason
                // (no_auth_header, jwt_rejected: ..., etc.) — surface it for diagnosis.
                val msg = e.message.orEmpty()
                if (msg.contains("401") || msg.contains("unauthorized", ignoreCase = true)) {
                    val reason = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"")
                        .find(msg)?.groupValues?.getOrNull(1)
                    ConvertOutcome.Failed(
                        "Server said: ${reason ?: "unauthorized"}. Tell me this exact message."
                    )
                } else {
                    ConvertOutcome.Failed(
                        "Couldn't reach the server (${e.message ?: "network error"}). " +
                            "Check your internet and try again."
                    )
                }
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

    /** Fetches the active product catalog (public read via RLS). */
    suspend fun getProducts(): List<Product> =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val rows = client.from("products")
                    .select()
                    .eq("is_active", true)
                    .order("sort_order", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()
                rows.mapNotNull { r ->
                    try {
                        Product(
                            id = r["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            name = r["name"]?.jsonPrimitive?.content ?: "",
                            description = r["description"]?.jsonPrimitive?.content ?: "",
                            imageUrl = r["image_url"]?.jsonPrimitive?.contentOrNull,
                            coinPrice = r["coin_price"]?.jsonPrimitive?.intOrNull ?: 0,
                            stock = r["stock"]?.jsonPrimitive?.intOrNull ?: 0
                        )
                    } catch (e: Exception) { null }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** Redeems a product: server checks stock + balance atomically. */
    suspend fun redeemProduct(
        productId: String,
        name: String,
        phone: String,
        address: String
    ): RequestOutcome =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                client.auth.awaitInitialization()
                if (client.auth.currentSessionOrNull() == null) {
                    return@withContext RequestOutcome.Failed("Please sign in again.")
                }
                val response = client.functions.invoke(
                    function = EdgeFunctions.REDEEM_PRODUCT,
                    body = buildJsonObject {
                        put("product_id", productId)
                        put("recipient_name", name)
                        put("recipient_phone", phone)
                        put("recipient_address", address)
                    }
                )
                val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
                if (body["ok"]?.jsonPrimitive?.booleanOrNull == true) {
                    RequestOutcome.Success("Order placed! We'll contact you for delivery.")
                } else {
                    val err = body["error"]?.jsonPrimitive?.contentOrNull
                    RequestOutcome.Failed(
                        when {
                            err?.contains("insufficient_coins") == true -> "Not enough coins for this product."
                            err?.contains("out_of_stock") == true -> "Sorry, this product is out of stock."
                            else -> "Couldn't place the order. Try again."
                        }
                    )
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                RequestOutcome.Failed(
                    if (msg.contains("insufficient_coins")) "Not enough coins for this product."
                    else if (msg.contains("out_of_stock")) "Sorry, this product is out of stock."
                    else "Couldn't place the order. Check your internet and try again."
                )
            }
        }

    /** Files a manual cashout request. */
    suspend fun requestCashout(
        coins: Int,
        method: String,
        accountTitle: String,
        accountNumber: String
    ): RequestOutcome =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                client.auth.awaitInitialization()
                if (client.auth.currentSessionOrNull() == null) {
                    return@withContext RequestOutcome.Failed("Please sign in again.")
                }
                val response = client.functions.invoke(
                    function = EdgeFunctions.REQUEST_CASHOUT,
                    body = buildJsonObject {
                        put("coins", coins)
                        put("method", method)
                        put("account_title", accountTitle)
                        put("account_number", accountNumber)
                    }
                )
                val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
                if (body["ok"]?.jsonPrimitive?.booleanOrNull == true) {
                    val rupees = body["rupees"]?.jsonPrimitive?.contentOrNull ?: ""
                    RequestOutcome.Success("Request sent! Rs $rupees will be sent to your account after review.")
                } else {
                    val err = body["error"]?.jsonPrimitive?.contentOrNull
                    RequestOutcome.Failed(
                        when {
                            err?.contains("below_minimum") == true -> "Minimum cashout is 10,000 coins (Rs 100)."
                            err?.contains("insufficient_coins") == true -> "Not enough available coins."
                            else -> "Couldn't send the request. Try again."
                        }
                    )
                }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                RequestOutcome.Failed(
                    if (msg.contains("below_minimum")) "Minimum cashout is 10,000 coins (Rs 100)."
                    else if (msg.contains("insufficient_coins")) "Not enough available coins."
                    else "Couldn't send the request. Check your internet and try again."
                )
            }
        }

    /** True when the signed-in user is an admin (for the admin screen). */
    suspend fun isAdmin(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false
                val rows = client.from("profiles")
                    .select("is_admin") { filter { eq("id", userId) } }
                    .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()
                rows.firstOrNull()?.get("is_admin")?.jsonPrimitive?.booleanOrNull == true
            } catch (e: Exception) {
                false
            }
        }

    /** Admin: list redemptions (pending first). */
    suspend fun adminRedemptions(): List<AdminOrder> =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val rows = client.from("redemptions")
                    .select()
                    .order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()
                rows.mapNotNull { r ->
                    try {
                        AdminOrder(
                            id = r["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            userId = r["user_id"]?.jsonPrimitive?.content ?: "",
                            title = r["product_name"]?.jsonPrimitive?.content ?: "",
                            coins = r["coins_spent"]?.jsonPrimitive?.intOrNull ?: 0,
                            status = r["status"]?.jsonPrimitive?.content ?: "",
                            detail = listOfNotNull(
                                r["recipient_name"]?.jsonPrimitive?.contentOrNull,
                                r["recipient_phone"]?.jsonPrimitive?.contentOrNull,
                                r["recipient_address"]?.jsonPrimitive?.contentOrNull
                            ).joinToString(" | "),
                            createdAt = r["created_at"]?.jsonPrimitive?.contentOrNull ?: ""
                        )
                    } catch (e: Exception) { null }
                }
            } catch (e: Exception) { emptyList() }
        }

    /** Admin: list cashout requests (pending first). */
    suspend fun adminCashouts(): List<AdminOrder> =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val rows = client.from("cashout_requests")
                    .select()
                    .order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()
                rows.mapNotNull { r ->
                    try {
                        val coins = r["coins"]?.jsonPrimitive?.intOrNull ?: 0
                        AdminOrder(
                            id = r["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            userId = r["user_id"]?.jsonPrimitive?.content ?: "",
                            title = "${r["method"]?.jsonPrimitive?.content ?: ""} — Rs ${coins / 100}",
                            coins = coins,
                            status = r["status"]?.jsonPrimitive?.content ?: "",
                            detail = listOfNotNull(
                                r["account_title"]?.jsonPrimitive?.contentOrNull,
                                r["account_number"]?.jsonPrimitive?.contentOrNull,
                                r["provider_reference"]?.jsonPrimitive?.contentOrNull
                            ).joinToString(" | "),
                            createdAt = r["created_at"]?.jsonPrimitive?.contentOrNull ?: ""
                        )
                    } catch (e: Exception) { null }
                }
            } catch (e: Exception) { emptyList() }
        }

    /** Admin: fulfill/cancel a redemption (cancel refunds). */
    suspend fun resolveRedemption(id: String, fulfilled: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Use the RPC so the refund is atomic.
                clientProvider.client.postgrest.rpc(
                    "resolve_redemption",
                    buildJsonObject {
                        put("p_redemption_id", id)
                        put("p_fulfilled", fulfilled)
                    }
                )
                true
            } catch (e: Exception) { false }
        }

    /** Admin: mark a cashout paid (with reference) or rejected (refunds). */
    suspend fun resolveCashout(id: String, paid: Boolean, reference: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                clientProvider.client.postgrest.rpc(
                    "resolve_cashout",
                    buildJsonObject {
                        put("p_request_id", id)
                        put("p_paid", paid)
                        put("p_reference", reference)
                    }
                )
                true
            } catch (e: Exception) { false }
        }

    /** Admin: add or update a product. Null id = create. */
    suspend fun saveProduct(
        id: String?,
        name: String,
        description: String,
        coinPrice: Int,
        stock: Int,
        isActive: Boolean
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val payload = buildJsonObject {
                    put("name", name)
                    put("description", description)
                    put("coin_price", coinPrice)
                    put("stock", stock)
                    put("is_active", isActive)
                }
                if (id == null) {
                    client.from("products").insert(payload)
                } else {
                    client.from("products").update(payload) { filter { eq("id", id) } }
                }
                true
            } catch (e: Exception) { false }
        }
}
