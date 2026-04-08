package com.rajratna.manager.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rajratna.manager.models.Order
import com.rajratna.manager.models.OrderStatus
import com.rajratna.manager.models.PaymentStatus
import com.rajratna.manager.ui.theme.*
import com.rajratna.manager.viewmodels.OrdersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel,
    onAddOrder: () -> Unit,
    onOrderClick: (String) -> Unit
) {
    val orders by viewModel.filteredOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val filters = listOf("All", "Today", "Upcoming", "Completed")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orders", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddOrder,
                icon = { Icon(Icons.Filled.Add, "Add Order") },
                text = { Text("Add Order") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Filter tabs
            ScrollableTabRow(
                selectedTabIndex = filters.indexOf(selectedFilter),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                filters.forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        text = { Text(filter) }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (orders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.EventNote,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No orders found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        OrderCard(order = order, onClick = { onOrderClick(order.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(order.status)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "📅 ${order.eventDate}   📍 ${order.location}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            val items = buildList {
                if (order.chairs > 0) add("${order.chairs} Chairs")
                if (order.tables > 0) add("${order.tables} Tables")
                if (order.jars > 0) add("${order.jars} Jars")
            }.joinToString(" • ")

            Text(
                text = items.ifEmpty { "No items" },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "₹${order.totalAmount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                PaymentChip(order.paymentStatus)
            }
        }
    }
}

@Composable
fun StatusChip(status: OrderStatus) {
    val (color, textColor) = when (status) {
        OrderStatus.BOOKED -> Pair(Blue600.copy(alpha = 0.15f), Blue700)
        OrderStatus.ACTIVE -> Pair(Amber600.copy(alpha = 0.15f), Amber600)
        OrderStatus.DELIVERED -> Pair(Green600.copy(alpha = 0.15f), Green600)
        OrderStatus.COMPLETED -> Pair(Green600.copy(alpha = 0.15f), Green600)
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun PaymentChip(status: PaymentStatus) {
    val (color, textColor) = when (status) {
        PaymentStatus.PAID -> Pair(Green600.copy(alpha = 0.15f), Green600)
        PaymentStatus.PARTIAL -> Pair(Amber600.copy(alpha = 0.15f), Amber600)
        PaymentStatus.PENDING -> Pair(Red600.copy(alpha = 0.15f), Red600)
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color) {
        Text(
            text = status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}
