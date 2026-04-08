package com.rajratna.manager.ui.stock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rajratna.manager.ui.theme.*
import com.rajratna.manager.viewmodels.StockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: StockViewModel,
    onBack: () -> Unit
) {
    val stock by viewModel.stock.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stock / Inventory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, "Edit Stock")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Inventory Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                StockItemCard(
                    name = "🪑 Chairs",
                    total = stock.totalChairs,
                    out = viewModel.outChairs,
                    available = viewModel.availableChairs
                )

                StockItemCard(
                    name = "🪑 Tables",
                    total = stock.totalTables,
                    out = viewModel.outTables,
                    available = viewModel.availableTables
                )

                StockItemCard(
                    name = "💧 Water Jars",
                    total = stock.totalJars,
                    out = viewModel.outJars,
                    available = viewModel.availableJars
                )
            }
        }
    }

    // Edit Stock Dialog
    if (showEditDialog) {
        var editChairs by remember { mutableStateOf(stock.totalChairs.toString()) }
        var editTables by remember { mutableStateOf(stock.totalTables.toString()) }
        var editJars by remember { mutableStateOf(stock.totalJars.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Total Stock") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editChairs,
                        onValueChange = { editChairs = it },
                        label = { Text("Total Chairs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTables,
                        onValueChange = { editTables = it },
                        label = { Text("Total Tables") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editJars,
                        onValueChange = { editJars = it },
                        label = { Text("Total Jars") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateStock(
                        editChairs.toIntOrNull() ?: 0,
                        editTables.toIntOrNull() ?: 0,
                        editJars.toIntOrNull() ?: 0
                    )
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StockItemCard(name: String, total: Int, out: Int, available: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StockStat(label = "Total", value = total, color = Blue700)
                StockStat(label = "Out", value = out, color = Amber600)
                StockStat(
                    label = "Available",
                    value = available,
                    color = if (available < 0) Red600 else Green600
                )
            }
        }
    }
}

@Composable
fun StockStat(label: String, value: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$value",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
    }
}
