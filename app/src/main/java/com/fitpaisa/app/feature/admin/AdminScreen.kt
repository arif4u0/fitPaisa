package com.fitpaisa.app.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitpaisa.app.core.rewards.AdminOrder
import com.fitpaisa.app.core.rewards.RewardsRepository
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.OnGold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private val SuccessGreen = Color(0xFF66BB6A)
private val NeutralGray = Color(0xFF9E9E9E)

private fun statusColor(status: String): Color = when (status.lowercase()) {
    "pending" -> Gold
    "fulfilled", "paid" -> SuccessGreen
    "cancelled", "rejected" -> NeutralGray
    else -> MutedSage
}

private fun statusLabel(status: String): String =
    status.replaceFirstChar { it.uppercase() }

private fun shortDate(iso: String): String =
    if (iso.length >= 10) iso.take(10) else iso

/**
 * Admin management screen: product redemptions, cashout requests, and
 * adding products to the catalog. Only visible to admin users —
 * everyone else sees a "Not authorized" message.
 */
@Composable
fun AdminScreen(viewModel: AdminViewModel = hiltViewModel()) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Orders", "Cashouts", "Products")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Text(
            text = "Admin",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        Spacer(Modifier.height(12.dp))

        if (!viewModel.authChecked) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = Gold)
            }
            return
        }

        if (!viewModel.isAdmin) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Not authorized",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CreamWhite
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "This area is only for fitPaisa admins.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSage
                )
            }
            return
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Gold
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Gold else MutedSage,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        viewModel.message?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Gold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LaunchedEffect(msg) {
                delay(4000)
                viewModel.clearMessage()
            }
        }

        when (selectedTab) {
            0 -> OrdersTab(viewModel)
            1 -> CashoutsTab(viewModel)
            2 -> ProductsTab(viewModel)
        }
    }
}

@Composable
private fun OrdersTab(viewModel: AdminViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (viewModel.loading) {
            CircularProgressIndicator(color = Gold, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        if (!viewModel.loading && viewModel.redemptions.isEmpty()) {
            Text(
                text = "No orders yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSage,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        viewModel.redemptions.forEach { order ->
            OrderCard(
                order = order,
                onFulfill = { viewModel.fulfillRedemption(order.id) },
                onCancel = { viewModel.cancelRedemption(order.id) }
            )
        }
    }
}

@Composable
private fun CashoutsTab(viewModel: AdminViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (viewModel.loading) {
            CircularProgressIndicator(color = Gold, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        if (!viewModel.loading && viewModel.cashouts.isEmpty()) {
            Text(
                text = "No cashout requests yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSage,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        viewModel.cashouts.forEach { order ->
            CashoutCard(
                order = order,
                onPay = { ref -> viewModel.payCashout(order.id, ref) },
                onReject = { viewModel.rejectCashout(order.id) }
            )
        }
    }
}

@Composable
private fun ProductsTab(viewModel: AdminViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Add a product to the catalog",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        ProductForm(onSave = { name, desc, price, stock, active ->
            viewModel.addProduct(name, desc, price, stock, active)
        })
        Text(
            text = "New products appear in the shop right away. Editing or removing existing products is coming soon.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSage
        )
    }
}

@Composable
private fun OrderCard(
    order: AdminOrder,
    onFulfill: () -> Unit,
    onCancel: () -> Unit
) {
    val pending = order.status.equals("pending", ignoreCase = true)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.title.ifBlank { "Order" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CreamWhite,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusLabel(order.status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor(order.status)
                )
            }
            Text(
                text = "${order.coins} coins",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Gold
            )
            if (order.detail.isNotBlank()) {
                Text(
                    text = order.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )
            }
            if (order.createdAt.isNotBlank()) {
                Text(
                    text = shortDate(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )
            }
            if (pending) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onFulfill,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = OnGold
                        )
                    ) {
                        Text("Mark Fulfilled", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedSage)
                    ) {
                        Text("Cancel & Refund")
                    }
                }
            }
        }
    }
}

@Composable
private fun CashoutCard(
    order: AdminOrder,
    onPay: (String) -> Unit,
    onReject: () -> Unit
) {
    val pending = order.status.equals("pending", ignoreCase = true)
    var reference by remember(order.id) { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.title.ifBlank { "Cashout" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CreamWhite,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusLabel(order.status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor(order.status)
                )
            }
            Text(
                text = "${order.coins} coins",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Gold
            )
            if (order.detail.isNotBlank()) {
                Text(
                    text = order.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )
            }
            if (order.createdAt.isNotBlank()) {
                Text(
                    text = shortDate(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )
            }
            if (pending) {
                Spacer(Modifier.height(4.dp))
                AdminTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = "Payment reference (Easypaisa / bank ref)"
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPay(reference.trim()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Gold,
                            contentColor = OnGold
                        )
                    ) {
                        Text("Mark Paid", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedSage)
                    ) {
                        Text("Reject & Refund")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductForm(
    onSave: (name: String, description: String, coinPrice: Int, stock: Int, isActive: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var stockText by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminTextField(value = name, onValueChange = { name = it }, label = "Product name")
            AdminTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AdminTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                    label = "Price (coins)",
                    modifier = Modifier.weight(1f)
                )
                AdminTextField(
                    value = stockText,
                    onValueChange = { stockText = it.filter { c -> c.isDigit() } },
                    label = "Stock",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = OnGold)
                )
                Text(
                    text = "Show in shop",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CreamWhite
                )
            }
            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE57373)
                )
            }
            Button(
                onClick = {
                    val price = priceText.toIntOrNull() ?: 0
                    val stock = stockText.toIntOrNull() ?: 0
                    if (name.isBlank()) {
                        error = "Give the product a name."
                    } else if (price <= 0) {
                        error = "Price must be more than 0 coins."
                    } else {
                        error = null
                        onSave(name.trim(), description.trim(), price, stock, isActive)
                        name = ""
                        description = ""
                        priceText = ""
                        stockText = ""
                        isActive = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = OnGold
                )
            ) {
                Text("Add Product", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MutedSage) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = CreamWhite,
            unfocusedTextColor = CreamWhite,
            focusedBorderColor = Gold,
            unfocusedBorderColor = MutedSage.copy(alpha = 0.5f),
            cursorColor = Gold
        ),
        singleLine = true
    )
}

/**
 * Loads admin data on init (after verifying the user is an admin) and
 * exposes actions for resolving orders, cashouts, and adding products.
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val rewardsRepository: RewardsRepository
) : ViewModel() {

    var authChecked by mutableStateOf(false)
        private set
    var isAdmin by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var redemptions by mutableStateOf<List<AdminOrder>>(emptyList())
        private set
    var cashouts by mutableStateOf<List<AdminOrder>>(emptyList())
        private set
    var message by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            isAdmin = rewardsRepository.isAdmin()
            authChecked = true
            if (isAdmin) reload()
        }
    }

    fun clearMessage() {
        message = null
    }

    fun reload() {
        viewModelScope.launch {
            loading = true
            redemptions = rewardsRepository.adminRedemptions()
            cashouts = rewardsRepository.adminCashouts()
            loading = false
        }
    }

    fun fulfillRedemption(id: String) {
        viewModelScope.launch {
            val ok = rewardsRepository.resolveRedemption(id, fulfilled = true)
            message = if (ok) "Order marked as fulfilled." else "Something went wrong. Try again."
            reload()
        }
    }

    fun cancelRedemption(id: String) {
        viewModelScope.launch {
            val ok = rewardsRepository.resolveRedemption(id, fulfilled = false)
            message = if (ok) "Order cancelled and coins refunded." else "Something went wrong. Try again."
            reload()
        }
    }

    fun payCashout(id: String, reference: String) {
        viewModelScope.launch {
            val ok = rewardsRepository.resolveCashout(id, paid = true, reference = reference)
            message = if (ok) "Cashout marked as paid." else "Something went wrong. Try again."
            reload()
        }
    }

    fun rejectCashout(id: String) {
        viewModelScope.launch {
            val ok = rewardsRepository.resolveCashout(id, paid = false, reference = "")
            message = if (ok) "Cashout rejected and coins refunded." else "Something went wrong. Try again."
            reload()
        }
    }

    fun addProduct(
        name: String,
        description: String,
        coinPrice: Int,
        stock: Int,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            val ok = rewardsRepository.saveProduct(
                id = null,
                name = name,
                description = description,
                coinPrice = coinPrice,
                stock = stock,
                isActive = isActive
            )
            message = if (ok) "Product added to the catalog." else "Something went wrong. Try again."
        }
    }
}
