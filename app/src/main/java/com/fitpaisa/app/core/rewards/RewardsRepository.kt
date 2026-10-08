package com.fitpaisa.app.core.rewards

import com.fitpaisa.app.BuildConfig
import com.fitpaisa.app.core.auth.SupabaseClientProvider
import com.fitpaisa.app.core.network.EdgeFunctions
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

@Serializable
private data class ProductDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("coin_price") val coinPrice: Int = 0,
    val stock: Int = 0
)

@Serializable
private data class ProfileAdminDto(
    @SerialName("is_admin") val isAdmin: Boolean = false
)

@Serializable
private data class RedemptionDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("coins_spent") val coinsSpent: Int = 0,
    val status: String = "",
    @SerialName("recipient_name") val recipientName: String? = null,
    @SerialName("recipient_phone") val recipientPhone: String? = null,
    @SerialName("recipient_address") val recipientAddress: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
private data class CashoutRequestDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val method: String = "",
    val coins: Int = 0,
    val status: String = "",
    @SerialName("account_title") val accountTitle: String? = null,
    @SerialName("account_number") val accountNumber: String? = null,
    @SerialName("provider_reference") val providerReference: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * Talks to the reward edge functions and database tables.
 */
@Singleton
class RewardsRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider
) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Sends today's observed step total to the server.
     */
    suspend fun convertTodaySteps(stepsToday: Int): ConvertOutcome =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                client.auth.awaitInitialization()
                if (client.auth.currentSessionOrNull() == null) {
                    return@withContext ConvertOutcome.Failed(
                        "You don't look signed in (no session). Please sign in again."
                    )
                }
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

    /** Reads the server balance. */
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

    /** Fetches the active product catalog. */
    suspend fun getProducts(): List<Product> =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val rows = client.from("products")
                    .select {
                        filter {
                            eq("is_active", true)
                        }
                        order("sort_order", Order.ASCENDING)
                    }
                    .decodeList<ProductDto>()
                rows.map { dto ->
                    Product(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        imageUrl = dto.imageUrl,
                        coinPrice = dto.coinPrice,
                        stock = dto.stock
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** Redeems a product. */
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

    /** True when the signed-in user is an admin. */
    suspend fun isAdmin(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false
                val profile = client.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingleOrNull<ProfileAdminDto>()
                profile?.isAdmin == true
            } catch (e: Exception) {
                false
            }
        }

    /** Admin: list redemptions. */
    suspend fun adminRedemptions(): List<AdminOrder> =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val rows = client.from("redemptions")
                    .select {
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<RedemptionDto>()
                rows.map { dto ->
                    AdminOrder(
                        id = dto.id,
                        userId = dto.userId,
                        title = dto.productName,
                        coins = dto.coinsSpent,
                        status = dto.status,
                        detail = listOfNotNull(
                            dto.recipientName,
                            dto.recipientPhone,
                            dto.recipientAddress
                        ).filter { it.isNotBlank() }.joinToString(" | "),
                        createdAt = dto.createdAt.orEmpty()
                    )
                }
            } catch (e: Exception) { emptyList() }
        }

    /** Admin: list cashout requests. */
    suspend fun adminCashouts(): List<AdminOrder> =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val rows = client.from("cashout_requests")
                    .select {
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<CashoutRequestDto>()
                rows.map { dto ->
                    AdminOrder(
                        id = dto.id,
                        userId = dto.userId,
                        title = "${dto.method} — Rs ${dto.coins / 100}",
                        coins = dto.coins,
                        status = dto.status,
                        detail = listOfNotNull(
                            dto.accountTitle,
                            dto.accountNumber,
                            dto.providerReference
                        ).filter { it.isNotBlank() }.joinToString(" | "),
                        createdAt = dto.createdAt.orEmpty()
                    )
                }
            } catch (e: Exception) { emptyList() }
        }

    /** Admin: fulfill/cancel a redemption. */
    suspend fun resolveRedemption(id: String, fulfilled: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
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

    /** Admin: mark a cashout paid or rejected. */
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
                    client.from("products").update(payload) {
                        filter {
                            eq("id", id)
                        }
                    }
                }
                true
            } catch (e: Exception) { false }
        }
}
