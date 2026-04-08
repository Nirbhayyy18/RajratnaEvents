package com.rajratna.manager.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rajratna.manager.models.Order
import com.rajratna.manager.models.Stock
import com.rajratna.manager.ui.theme.*
import com.rajratna.manager.viewmodels.OrdersViewModel
import com.rajratna.manager.viewmodels.StockViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: OrdersViewModel, stockViewModel: StockViewModel) {
    // Get all orders
    val allOrders by viewModel.allOrders.collectAsState()
    val stock by stockViewModel.stock.collectAsState()

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Group orders by date
    val ordersByDate = allOrders.groupBy { it.eventDate }
    val selectedDateStr = selectedDate.toString()
    val ordersForDate = ordersByDate[selectedDateStr] ?: emptyList()

    // Aggregated totals for selected date
    val totalChairs = ordersForDate.sumOf { it.chairs }
    val totalTables = ordersForDate.sumOf { it.tables }
    val totalJars = ordersForDate.sumOf { it.jars }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month navigation
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Filled.ChevronLeft, "Previous")
                    }
                    Text(
                        "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Filled.ChevronRight, "Next")
                    }
                }
            }

            // Day headers
            item {
                Row(Modifier.fillMaxWidth()) {
                    listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                        Text(
                            day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Calendar grid
            item {
                val firstDay = currentMonth.atDay(1)
                val dayOfWeekOffset = (firstDay.dayOfWeek.value - 1) // Monday = 0
                val daysInMonth = currentMonth.lengthOfMonth()

                Column {
                    var dayCounter = 1 - dayOfWeekOffset
                    for (week in 0..5) {
                        if (dayCounter > daysInMonth) break
                        Row(Modifier.fillMaxWidth()) {
                            for (dayOfWeek in 0..6) {
                                if (dayCounter < 1 || dayCounter > daysInMonth) {
                                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val date = currentMonth.atDay(dayCounter)
                                    val dateStr = date.toString()
                                    val hasOrders = ordersByDate.containsKey(dateStr)
                                    val isSelected = date == selectedDate
                                    val isToday = date == LocalDate.now()

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> MaterialTheme.colorScheme.surface
                                                }
                                            )
                                            .clickable { selectedDate = date },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                "$dayCounter",
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (hasOrders) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (hasOrders) {
                                                Box(
                                                    Modifier.size(4.dp).clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                            else MaterialTheme.colorScheme.primary
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                                dayCounter++
                            }
                        }
                    }
                }
            }

            // Aggregated totals for selected date
            item {
                Divider(Modifier.padding(vertical = 4.dp))
                Text(
                    "📅 ${selectedDate}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (ordersForDate.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Items Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                AggStat("🪑 Chairs", totalChairs, stock.totalChairs)
                                AggStat("🪑 Tables", totalTables, stock.totalTables)
                                AggStat("💧 Jars", totalJars, stock.totalJars)
                            }
                        }
                    }
                }

                // Overbooking warnings
                item {
                    if (totalChairs > stock.totalChairs || totalTables > stock.totalTables || totalJars > stock.totalJars) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Red600.copy(alpha = 0.1f))
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("⚠️ Overbooking Warning!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Red600)
                                if (totalChairs > stock.totalChairs) Text("• Chairs: Need $totalChairs, have ${stock.totalChairs}", color = Red600)
                                if (totalTables > stock.totalTables) Text("• Tables: Need $totalTables, have ${stock.totalTables}", color = Red600)
                                if (totalJars > stock.totalJars) Text("• Jars: Need $totalJars, have ${stock.totalJars}", color = Red600)
                            }
                        }
                    }
                }

                // Orders for this date
                items(ordersForDate) { order ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(order.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                            Text("₹${order.totalAmount.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                item {
                    Text("No orders for this date", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AggStat(label: String, required: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$required",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (required > total) Red600 else Green600
        )
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text("of $total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
