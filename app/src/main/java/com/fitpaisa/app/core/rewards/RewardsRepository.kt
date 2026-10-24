package com.fitpaisa.app.core.rewards

import com.fitpaisa.app.BuildConfig
import com.fitpaisa.app.core.auth.SupabaseClientProvider
import com.fitpaisa.app.core.network.EdgeFunctions
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
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
    val maturingCoins: Int = 0,
    val level: Int = 1,
    val levelName: String = "Starter",
    val lifetimeSteps: Long = 0
)

/** Outcome of a Convert tap, in plain language for the UI. */
sealed interface ConvertOutcome {
    /** Server credited new coins. */
    data class Converted(
        val coins: Int,
        val newSteps: Int,
        val streakDays: Int = 0,
        val streakBonus: Int = 0,
        val challengeBonus: Int = 0
    ) : ConvertOutcome
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
    val stock: Int,
    val isActive: Boolean = true
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
    val stock: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true
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
                val streakDays = body["streak_days"]?.jsonPrimitive?.intOrNull ?: 0
                val streakBonus = body["streak_bonus"]?.jsonPrimitive?.intOrNull ?: 0
                val challengeBonus = body["challenge_bonus"]?.jsonPrimitive?.intOrNull ?: 0
                if (coins <= 0) ConvertOutcome.NothingNew
                else ConvertOutcome.Converted(coins, newSteps, streakDays, streakBonus, challengeBonus)
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
                    maturingCoins = body["maturing_coins"]?.jsonPrimitive?.intOrNull ?: 0,
                    level = body["level"]?.jsonPrimitive?.intOrNull ?: 1,
                    levelName = body["level_name"]?.jsonPrimitive?.contentOrNull ?: "Starter",
                    lifetimeSteps = body["lifetime_steps"]?.jsonPrimitive?.longOrNull ?: 0
                )
            } catch (e: Exception) {
                BalanceResponse()
            }
        }

    /** Fetches the active product catalog. */
    /** Last products error (for diagnostics). Null if the last load worked. */
    var lastProductsError: String? = null
        private set

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
                lastProductsError = null
                rows.map { dto ->
                    Product(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        imageUrl = dto.imageUrl,
                        coinPrice = dto.coinPrice,
                        stock = dto.stock,
                        isActive = dto.isActive
                    )
                }
            } catch (e: Exception) {
                lastProductsError = "${e::class.simpleName}: ${e.message}"
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
        isActive: Boolean,
        imageUrl: String? = null
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
                    if (imageUrl != null) put("image_url", imageUrl)
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

    /** Deletes a product permanently. */
    suspend fun deleteProduct(id: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                client.from("products").delete {
                    filter { eq("id", id) }
                }
                true
            } catch (e: Exception) { false }
        }

    /** All products including inactive (for admin management). */
    suspend fun getAllProductsAdmin(): List<Product> =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val rows = client.from("products")
                    .select { order("sort_order", Order.ASCENDING) }
                    .decodeList<ProductDto>()
                rows.map { dto ->
                    Product(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        imageUrl = dto.imageUrl,
                        coinPrice = dto.coinPrice,
                        stock = dto.stock,
                        isActive = dto.isActive
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * Uploads a product image to the public 'product-images' bucket.
     * Returns the public URL, or null on failure.
     */
    suspend fun uploadProductImage(bytes: ByteArray, extension: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val fileName = "product_${System.currentTimeMillis()}.$extension"
                client.storage.from("product-images").upload(fileName, bytes)
                client.storage.from("product-images").publicUrl(fileName)
            } catch (e: Exception) { null }
        }

    /** Fetches the signed-in user's referral code (null if unavailable). */
    suspend fun myReferralCode(): String? =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val userId = client.auth.currentUserOrNull()?.id ?: return@withContext null
                val row = client.from("profiles")
                    .select {
                        filter { eq("id", userId) }
                    }
                    .decodeSingleOrNull<ReferralCodeDto>()
                row?.referralCode
            } catch (e: Exception) { null }
        }

    /**
     * Claims a referral code. Returns (success, message).
     * Server enforces: valid code, not self, one-time only.
     */
    suspend fun claimReferral(code: String): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val response = client.functions.invoke(
                    function = EdgeFunctions.CLAIM_REFERRAL,
                    body = buildJsonObject {
                        put("code", code.trim().uppercase())
                    }
                )
                val text = response.bodyAsText()
                val parsed = Json.parseToJsonElement(text).jsonObject
                if (response.status.value == 200 && parsed["ok"]?.jsonPrimitive?.booleanOrNull == true) {
                    val bonus = parsed["bonus_coins"]?.jsonPrimitive?.intOrNull ?: 500
                    true to "+$bonus FitCoins referral bonus!"
                } else {
                    val error = parsed["error"]?.jsonPrimitive?.contentOrNull
                    false to when (error) {
                        "invalid_code" -> "That code doesn't exist — check and try again."
                        "cannot_refer_self" -> "You can't use your own code."
                        "already_claimed" -> "You've already claimed a referral bonus."
                        else -> "Couldn't apply that code. Try again."
                    }
                }
            } catch (e: Exception) {
                false to "Couldn't apply that code. Try again."
            }
        }

    /** Result of a lucky spin. */
    sealed interface SpinOutcome {
        data class Won(val prizeCoins: Int, val spinType: String) : SpinOutcome
        data object FreeSpinUsed : SpinOutcome
        data class Failed(val message: String) : SpinOutcome
    }

    /**
     * Calls the spin-wheel Edge Function. The server picks the prize.
     * @param spinType "free" (1/day) or "ad" (after watching a rewarded ad).
     */
    suspend fun spin(spinType: String): SpinOutcome =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val today = java.time.LocalDate.now().toString()
                val response = client.functions.invoke(
                    function = EdgeFunctions.SPIN_WHEEL,
                    body = buildJsonObject {
                        put("local_date", today)
                        put("spin_type", spinType)
                    }
                )
                val text = response.bodyAsText()
                val parsed = Json.parseToJsonElement(text).jsonObject
                if (response.status.value !in 200..299) {
                    return@withContext SpinOutcome.Failed("Spin failed — try again.")
                }
                if (parsed["already_used"]?.jsonPrimitive?.booleanOrNull == true) {
                    return@withContext SpinOutcome.FreeSpinUsed
                }
                val prize = parsed["prize_coins"]?.jsonPrimitive?.intOrNull ?: 0
                if (prize <= 0) SpinOutcome.Failed("Spin failed — try again.")
                else SpinOutcome.Won(prize, spinType)
            } catch (e: Exception) {
                SpinOutcome.Failed("Couldn't reach the server. Check your internet.")
            }
        }

    /** Fetches the weekly leaderboard (top walkers this week). */
    var lastLeaderboardError: String? = null
        private set

    suspend fun getLeaderboard(): List<LeaderboardEntry> =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val rows = client.from("weekly_leaderboard")
                    .select()
                    .decodeList<LeaderboardEntry>()
                lastLeaderboardError = null
                rows
            } catch (e: Exception) {
                lastLeaderboardError = "${e::class.simpleName}: ${e.message}"
                emptyList()
            }
        }

    /** Fetches the user's unlocked achievements. */
    suspend fun getAchievements(): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val userId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
                client.from("user_achievements")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<Map<String, String>>()
                    .mapNotNull { it["achievement_id"] }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** Checks if today's free spin is still available. */
    suspend fun isFreeSpinAvailable(): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val client = clientProvider.client
                val today = java.time.LocalDate.now().toString()
                val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false
                val rows = client.from("spin_history")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("local_date", today)
                            eq("spin_type", "free")
                        }
                    }
                    .decodeList<Map<String, String>>()
                rows.isEmpty()
            } catch (e: Exception) {
                true // Assume available if we can't check; server enforces the limit.
            }
        }
}

@Serializable
private data class ReferralCodeDto(
    @SerialName("referral_code") val referralCode: String? = null
)

@Serializable
data class LeaderboardEntry(
    @SerialName("user_id") val userId: String = "",
    @SerialName("display_name") val displayName: String = "Walker",
    @SerialName("week_steps") val weekSteps: Long = 0
)
