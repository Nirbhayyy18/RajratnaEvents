package com.rajratna.manager.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rajratna.manager.models.Order
import com.rajratna.manager.viewmodels.OrdersViewModel
import com.rajratna.manager.viewmodels.UiEvent
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrderScreen(
    viewModel: OrdersViewModel,
    onBack: () -> Unit
) {
    var customerName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var chairs by remember { mutableIntStateOf(0) }
    var tables by remember { mutableIntStateOf(0) }
    var jars by remember { mutableIntStateOf(0) }
    var transportCharge by remember { mutableStateOf("") }
    var advancePaid by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-fill customer when mobile changes
    LaunchedEffect(mobile) {
        if (mobile.length >= 10) {
            val existing = viewModel.findCustomerByMobile(mobile.trim())
            if (existing != null) {
                if (customerName.isBlank()) customerName = existing.name
                if (location.isBlank()) location = existing.location
            }
        }
    }

    // Listen for UI events (snackbar)
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val transport = transportCharge.toDoubleOrNull() ?: 0.0
    val advance = advancePaid.toDoubleOrNull() ?: 0.0
    val total = Order.calculateTotal(chairs, tables, jars, transport)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Order") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Customer Details
            Text("Customer Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = customerName,
                onValueChange = { customerName = it },
                label = { Text("Customer Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = { Text("Mobile Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date Picker
            OutlinedTextField(
                value = eventDate,
                onValueChange = {},
                label = { Text("Event Date *") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Filled.CalendarMonth, "Pick date")
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Items
            Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            QuantitySelector(label = "Chairs (₹5/day)", value = chairs, onValueChange = { chairs = maxOf(0, it) })
            QuantitySelector(label = "Tables (₹30/day)", value = tables, onValueChange = { tables = maxOf(0, it) })
            QuantitySelector(label = "Water Jars (₹30/day)", value = jars, onValueChange = { jars = maxOf(0, it) })

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Charges
            Text("Charges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = transportCharge,
                onValueChange = { transportCharge = it },
                label = { Text("Transport Charge (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = advancePaid,
                onValueChange = { advancePaid = it },
                label = { Text("Advance Payment (₹)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Total Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Amount", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "₹${total.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (advance > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Remaining", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "₹${(total - advance).toInt()}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            // Save Button
            Button(
                onClick = {
                    // Validation
                    if (customerName.isBlank()) { errorMsg = "Customer name is required"; return@Button }
                    if (eventDate.isBlank()) { errorMsg = "Event date is required"; return@Button }
                    if (chairs == 0 && tables == 0 && jars == 0) { errorMsg = "Add at least one item"; return@Button }
                    if (advance < 0 || transport < 0) { errorMsg = "Values cannot be negative"; return@Button }

                    // Stock validation
                    val stockError = viewModel.validateStock(chairs, tables, jars)
                    if (stockError != null) { errorMsg = "⚠️ Not enough stock:\n$stockError"; return@Button }

                    errorMsg = null
                    isSaving = true

                    val order = Order(
                        customerName = customerName.trim(),
                        mobile = mobile.trim(),
                        location = location.trim(),
                        eventDate = eventDate,
                        chairs = chairs,
                        tables = tables,
                        jars = jars,
                        transportCharge = transport,
                        totalAmount = total,
                        advancePaid = advance
                    )
                    viewModel.addOrder(
                        order,
                        onSuccess = { isSaving = false; onBack() },
                        onError = { isSaving = false; errorMsg = it }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Order", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        eventDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun QuantitySelector(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = { if (value > 0) onValueChange(value - 1) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Filled.Remove, "Decrease", modifier = Modifier.size(20.dp))
            }

            // Editable numeric input field
            OutlinedTextField(
                value = if (value == 0) "" else value.toString(),
                onValueChange = { text ->
                    val parsed = text.filter { it.isDigit() }.toIntOrNull() ?: 0
                    onValueChange(maxOf(0, parsed))
                },
                modifier = Modifier.width(64.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            )

            FilledIconButton(
                onClick = { onValueChange(value + 1) },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Add, "Increase", modifier = Modifier.size(20.dp))
            }
        }
    }
}
