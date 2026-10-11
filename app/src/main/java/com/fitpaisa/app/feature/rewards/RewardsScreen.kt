package com.fitpaisa.app.feature.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitpaisa.app.core.rewards.Product
import com.fitpaisa.app.core.rewards.RequestOutcome
import com.fitpaisa.app.core.rewards.RewardsRepository
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.ErrorRed
import com.fitpaisa.app.ui.theme.ForestGreen
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.OnGold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rewards catalog: products the user can buy with FitCoins.
 *
 * The catalog and the redeem flow are server-driven via [RewardsRepository]:
 * the server checks stock and the coin balance atomically, so two users can
 * never overspend the same stock.
 */
@Composable
fun RewardsScreen(viewModel: RewardsViewModel = hiltViewModel()) {
    val products = viewModel.products
    val loading = viewModel.loading
    val selected = viewModel.selectedProduct

    // Refresh the catalog every time the tab becomes visible,
    // so newly added products appear without restarting the app.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Text(
            text = "Rewards",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Spend your FitCoins on real products.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSage
        )
        Spacer(Modifier.height(16.dp))

        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Gold)
                }
            }
            products.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No rewards right now — check back later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSage
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onTap = { viewModel.selectProduct(product) }
                        )
                    }
                }
            }
        }
    }

    if (selected != null) {
        RedeemDialog(
            product = selected,
            name = viewModel.name,
            onNameChange = { viewModel.name = it },
            phone = viewModel.phone,
            onPhoneChange = { viewModel.phone = it },
            address = viewModel.address,
            onAddressChange = { viewModel.address = it },
            redeeming = viewModel.redeeming,
            resultMessage = viewModel.resultMessage,
            resultSuccess = viewModel.resultSuccess,
            onRedeem = { viewModel.redeem() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
}

@Composable
private fun ProductCard(product: Product, onTap: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Product image (if uploaded).
            if (!product.imageUrl.isNullOrBlank()) {
                coil.compose.AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CreamWhite
            )
            if (product.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage,
                    maxLines = 2
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinPrice(coins = product.coinPrice)
                Spacer(Modifier.weight(1f))
                StockLabel(stock = product.stock)
            }
        }
    }
}

@Composable
private fun CoinPrice(coins: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(Gold, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$coins FitCoins",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Gold
        )
    }
}

@Composable
private fun StockLabel(stock: Int) {
    when {
        stock <= 0 -> Text(
            text = "Out of stock",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = ErrorRed
        )
        stock < 5 -> Text(
            text = "Only $stock left",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Gold
        )
        else -> Text(
            text = "In stock",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSage
        )
    }
}

@Composable
private fun RedeemDialog(
    product: Product,
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    redeeming: Boolean,
    resultMessage: String?,
    resultSuccess: Boolean,
    onRedeem: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!redeeming) onDismiss() },
        containerColor = ForestGreenCard,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CreamWhite
            )
        },
        text = {
            Column {
                if (product.description.isNotBlank()) {
                    Text(
                        text = product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSage
                    )
                    Spacer(Modifier.height(8.dp))
                }
                CoinPrice(coins = product.coinPrice)
                Spacer(Modifier.height(16.dp))

                RewardField(
                    value = name,
                    onValueChange = onNameChange,
                    label = "Your name",
                    enabled = !redeeming && resultMessage == null
                )
                Spacer(Modifier.height(8.dp))
                RewardField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = "Phone number",
                    keyboardType = KeyboardType.Phone,
                    enabled = !redeeming && resultMessage == null
                )
                Spacer(Modifier.height(8.dp))
                RewardField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = "Delivery address",
                    enabled = !redeeming && resultMessage == null
                )

                if (resultMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = resultMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (resultSuccess) Gold else ErrorRed
                    )
                }
            }
        },
        confirmButton = {
            if (resultMessage != null) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Close", color = Gold, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onRedeem,
                    enabled = !redeeming && product.stock > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = OnGold,
                        disabledContainerColor = Gold.copy(alpha = 0.25f),
                        disabledContentColor = CreamWhite.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (redeeming) {
                        CircularProgressIndicator(
                            color = OnGold,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Redeem for ${product.coinPrice} coins",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        dismissButton = {
            if (resultMessage == null) {
                TextButton(
                    onClick = { if (!redeeming) onDismiss() },
                    enabled = !redeeming
                ) {
                    Text(text = "Cancel", color = MutedSage)
                }
            }
        }
    )
}

@Composable
private fun RewardField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedTextColor = CreamWhite,
            unfocusedTextColor = CreamWhite,
            disabledTextColor = CreamWhite.copy(alpha = 0.6f),
            focusedContainerColor = ForestGreen,
            unfocusedContainerColor = ForestGreen,
            disabledContainerColor = ForestGreen,
            cursorColor = Gold,
            focusedLabelColor = Gold,
            unfocusedLabelColor = MutedSage,
            disabledLabelColor = MutedSage,
            focusedIndicatorColor = Gold,
            unfocusedIndicatorColor = MutedSage.copy(alpha = 0.5f),
            disabledIndicatorColor = MutedSage.copy(alpha = 0.25f),
            focusedPlaceholderColor = MutedSage,
            unfocusedPlaceholderColor = MutedSage
        )
    )
}

/**
 * Holds the catalog list, the selected product, the recipient form,
 * and the redeem result. All server work goes through [RewardsRepository].
 */
@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val rewardsRepository: RewardsRepository
) : ViewModel() {

    var products by mutableStateOf<List<Product>>(emptyList())
        private set
    var productsError by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(true)
        private set

    var selectedProduct by mutableStateOf<Product?>(null)
        private set

    var name by mutableStateOf("")
    var phone by mutableStateOf("")
    var address by mutableStateOf("")

    var redeeming by mutableStateOf(false)
        private set
    var resultMessage by mutableStateOf<String?>(null)
        private set
    var resultSuccess by mutableStateOf(false)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading = true
            products = rewardsRepository.getProducts()
            productsError = rewardsRepository.lastProductsError
            loading = false
        }
    }

    fun selectProduct(product: Product) {
        selectedProduct = product
        name = ""
        phone = ""
        address = ""
        redeeming = false
        resultMessage = null
        resultSuccess = false
    }

    fun dismissDialog() {
        selectedProduct = null
    }

    fun redeem() {
        val product = selectedProduct ?: return
        if (name.isBlank() || phone.isBlank() || address.isBlank()) {
            resultMessage = "Please fill in your name, phone number and delivery address."
            resultSuccess = false
            return
        }
        viewModelScope.launch {
            redeeming = true
            resultMessage = null
            when (val outcome = rewardsRepository.redeemProduct(
                productId = product.id,
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim()
            )) {
                is RequestOutcome.Success -> {
                    resultMessage = outcome.message
                    resultSuccess = true
                    refresh()
                }
                is RequestOutcome.Failed -> {
                    resultMessage = outcome.message
                    resultSuccess = false
                }
            }
            redeeming = false
        }
    }
}
