package com.rajratna.manager.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rajratna.manager.models.OrderStatus
import com.rajratna.manager.ui.theme.*
import com.rajratna.manager.viewmodels.OrdersViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: OrdersViewModel) {
    LaunchedEffect(Unit) { viewModel.setFilter("All") }
    val allOrders by viewModel.allOrders.collectAsState()

    val today = LocalDate.now().toString()
    val todayOrders = allOrders.filter { it.eventDate == today && !it.isReturned }
    val activeOrders = allOrders.filter { it.status == OrderStatus.ACTIVE || it.status == OrderStatus.DELIVERED }
    val pendingPayments = allOrders.filter { it.remaining > 0 }
    val totalPending = pendingPayments.sumOf { it.remaining }

    val todayIncome = viewModel.getTodayIncome()
    val weeklyIncome = viewModel.getWeeklyIncome()
    val monthlyIncome = viewModel.getMonthlyIncome()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rajratna Manager", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium)

            // Quick Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Today", "${todayOrders.size}", "deliveries", Blue700, Modifier.weight(1f))
                StatCard("Active", "${activeOrders.size}", "orders", Amber600, Modifier.weight(1f))
                StatCard("Pending", "₹${totalPending.toInt()}", "${pendingPayments.size} orders", Red600, Modifier.weight(1f))
            }

            // Income Stats
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Green600.copy(alpha = 0.08f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💰 Income", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IncStat("Today", todayIncome)
                        IncStat("Week", weeklyIncome)
                        IncStat("Month", monthlyIncome)
                    }
                }
            }

            // Today's Deliveries
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📦 Today's Deliveries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (todayOrders.isEmpty()) {
                        Text("No deliveries today", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        todayOrders.forEach { order ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(order.customerName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    buildList {
                                        if (order.chairs > 0) add("${order.chairs}C")
                                        if (order.tables > 0) add("${order.tables}T")
                                        if (order.jars > 0) add("${order.jars}J")
                                    }.joinToString(" "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Overdue Alerts
            val overdueOrders = allOrders.filter { it.eventDate < today && !it.isReturned }
            if (overdueOrders.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Red600.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⚠️ Not Returned (${overdueOrders.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Red600)
                        Spacer(Modifier.height(8.dp))
                        overdueOrders.take(5).forEach { order ->
                            Text("• ${order.customerName} — ${order.eventDate}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (overdueOrders.size > 5) {
                            Text("+ ${overdueOrders.size - 5} more...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, subtitle: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun IncStat(label: String, amount: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("₹${amount.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Green600)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}