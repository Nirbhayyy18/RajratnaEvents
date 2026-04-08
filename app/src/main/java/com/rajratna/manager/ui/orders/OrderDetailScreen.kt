package com.rajratna.manager.ui.orders

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rajratna.manager.models.Order
import com.rajratna.manager.models.OrderStatus
import com.rajratna.manager.models.Payment
import com.rajratna.manager.services.FirestoreService
import com.rajratna.manager.ui.theme.*
import com.rajratna.manager.viewmodels.OrdersViewModel
import com.rajratna.manager.viewmodels.UiEvent
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: OrdersViewModel,
    onBack: () -> Unit
) {
    val order = viewModel.getOrderById(orderId)

    if (order == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Order not found") }
        return
    }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showConfirmDelivered by remember { mutableStateOf(false) }
    var showConfirmReturned by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Payment history for this order
    var payments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    LaunchedEffect(orderId) {
        FirestoreService.getPaymentsForOrderFlow(orderId)
            .catch { /* ignore */ }
            .collect { payments = it }
    }

    // Snackbar events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Detail") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (order.status) {
                        OrderStatus.BOOKED -> Blue600.copy(alpha = 0.1f)
                        OrderStatus.ACTIVE -> Amber600.copy(alpha = 0.1f)
                        OrderStatus.DELIVERED -> Green600.copy(alpha = 0.15f)
                        OrderStatus.COMPLETED -> Green600.copy(alpha = 0.1f)
                    }
                )
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Status", style = MaterialTheme.typography.labelMedium)
                        Text(order.status.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (order.isDelivered) Text("✅ Delivered", style = MaterialTheme.typography.labelLarge, color = Green600)
                        if (order.isReturned) Text("✅ Returned", style = MaterialTheme.typography.labelLarge, color = Green600)
                    }
                }
            }

            // Customer Info
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    InfoRow("👤", "Name", order.customerName)
                    InfoRow("📱", "Mobile", order.mobile.ifEmpty { "Not provided" })
                    InfoRow("📍", "Location", order.location.ifEmpty { "Not provided" })
                    InfoRow("📅", "Event Date", order.eventDate)
                }
            }

            // Items
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (order.chairs > 0) ItemRow("🪑", "Chairs", order.chairs, Order.CHAIR_RATE)
                    if (order.tables > 0) ItemRow("🪑", "Tables", order.tables, Order.TABLE_RATE)
                    if (order.jars > 0) ItemRow("💧", "Water Jars", order.jars, Order.JAR_RATE)
                    if (order.transportCharge > 0) {
                        Divider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🚛 Transport", style = MaterialTheme.typography.bodyMedium)
                            Text("₹${order.transportCharge.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Payment Summary
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Payment Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    PaymentRow("Total Amount", "₹${order.totalAmount.toInt()}")
                    PaymentRow("Advance Paid", "- ₹${order.advancePaid.toInt()}")
                    if (order.amountPaid > 0) PaymentRow("Additional Paid", "- ₹${order.amountPaid.toInt()}")
                    Divider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Remaining", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("₹${order.remaining.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (order.remaining > 0) Red600 else Green600)
                    }
                }
            }

            // Payment History
            if (payments.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Payment History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        payments.forEach { p ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${p.date} • ${p.mode}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${p.amount.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Green600)
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Text("Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (!order.isDelivered) {
                Button(
                    onClick = { showConfirmDelivered = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue700),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Filled.LocalShipping, "Deliver", Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text("Mark Delivered", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (order.isDelivered && !order.isReturned) {
                Button(
                    onClick = { showConfirmReturned = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green600),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Filled.AssignmentReturn, "Return", Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text("Mark Returned", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (order.remaining > 0) {
                OutlinedButton(onClick = { showPaymentDialog = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), enabled = !isProcessing) {
                    Icon(Icons.Filled.Payment, "Payment", Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text("Add Payment", style = MaterialTheme.typography.titleMedium)
                }
            }

            // WhatsApp Share
            OutlinedButton(
                onClick = {
                    val msg = buildWhatsAppMessage(order)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://wa.me/${order.mobile.replace("+", "").replace(" ", "")}?text=${Uri.encode(msg)}")
                    }
                    try { context.startActivity(intent) } catch (_: Exception) { }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Share, "Share", Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text("Share on WhatsApp", style = MaterialTheme.typography.titleMedium)
            }

            if (order.mobile.isNotEmpty()) {
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.mobile}"))) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Phone, "Call", Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                    Text("Call Customer", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Confirm Delivered Dialog ───────────────────────
    if (showConfirmDelivered) {
        AlertDialog(
            onDismissRequest = { showConfirmDelivered = false },
            title = { Text("Mark Delivered?") },
            text = { Text("Confirm items delivered to ${order.customerName}.") },
            confirmButton = {
                TextButton(onClick = {
                    isProcessing = true
                    viewModel.updateOrder(order.id, mapOf("isDelivered" to true)) { isProcessing = false }
                    showConfirmDelivered = false
                }) { Text("Yes, Delivered") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDelivered = false }) { Text("Cancel") } }
        )
    }

    // ── Confirm Returned Dialog ───────────────────────
    if (showConfirmReturned) {
        AlertDialog(
            onDismissRequest = { showConfirmReturned = false },
            title = { Text("Mark Returned?") },
            text = { Text("Confirm all items returned from ${order.customerName}.") },
            confirmButton = {
                TextButton(onClick = {
                    isProcessing = true
                    viewModel.updateOrder(order.id, mapOf("isReturned" to true)) { isProcessing = false }
                    showConfirmReturned = false
                }) { Text("Yes, Returned") }
            },
            dismissButton = { TextButton(onClick = { showConfirmReturned = false }) { Text("Cancel") } }
        )
    }

    // ── Add Payment Dialog (with mode selector) ───────
    if (showPaymentDialog) {
        var paymentAmount by remember { mutableStateOf("") }
        var paymentMode by remember { mutableStateOf("Cash") }
        var paymentError by remember { mutableStateOf<String?>(null) }
        var isAdding by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isAdding) showPaymentDialog = false },
            title = { Text("Add Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Remaining: ₹${order.remaining.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = paymentAmount, onValueChange = { paymentAmount = it; paymentError = null },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    // Payment mode selector
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = paymentMode == "Cash", onClick = { paymentMode = "Cash" }, label = { Text("💵 Cash") })
                        FilterChip(selected = paymentMode == "UPI", onClick = { paymentMode = "UPI" }, label = { Text("📱 UPI") })
                    }
                    paymentError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = paymentAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0) { paymentError = "Enter a valid amount"; return@TextButton }
                        if (amount > order.remaining) { paymentError = "Exceeds remaining (₹${order.remaining.toInt()})"; return@TextButton }
                        isAdding = true
                        viewModel.addPayment(order.id, amount, paymentMode, order.amountPaid)
                        showPaymentDialog = false
                    },
                    enabled = !isAdding
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showPaymentDialog = false }, enabled = !isAdding) { Text("Cancel") } }
        )
    }
}

private fun buildWhatsAppMessage(order: Order): String {
    return buildString {
        appendLine("*RAJRATNA MANAGER — BILL*")
        appendLine("─────────────────")
        appendLine("*Customer:* ${order.customerName}")
        appendLine("*Date:* ${order.eventDate}")
        appendLine("*Location:* ${order.location}")
        appendLine("─────────────────")
        appendLine("*Items:*")
        if (order.chairs > 0) appendLine("• Chairs × ${order.chairs} = ₹${(order.chairs * Order.CHAIR_RATE).toInt()}")
        if (order.tables > 0) appendLine("• Tables × ${order.tables} = ₹${(order.tables * Order.TABLE_RATE).toInt()}")
        if (order.jars > 0) appendLine("• Jars × ${order.jars} = ₹${(order.jars * Order.JAR_RATE).toInt()}")
        if (order.transportCharge > 0) appendLine("• Transport = ₹${order.transportCharge.toInt()}")
        appendLine("─────────────────")
        appendLine("*Total:* ₹${order.totalAmount.toInt()}")
        appendLine("*Paid:* ₹${(order.advancePaid + order.amountPaid).toInt()}")
        appendLine("*Remaining:* ₹${order.remaining.toInt()}")
        appendLine("─────────────────")
        appendLine("_Thank you! — Rajratna_")
    }
}

@Composable
private fun InfoRow(emoji: String, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$emoji $label", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ItemRow(emoji: String, name: String, qty: Int, rate: Double) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$emoji $name × $qty", style = MaterialTheme.typography.bodyMedium)
        Text("₹${(qty * rate).toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PaymentRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
