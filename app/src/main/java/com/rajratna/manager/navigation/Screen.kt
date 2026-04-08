package com.rajratna.manager.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Orders : Screen("orders", "Orders", Icons.Filled.ListAlt)
    object Calendar : Screen("calendar", "Calendar", Icons.Filled.CalendarMonth)
    object Water : Screen("water", "Water", Icons.Filled.WaterDrop)
    object More : Screen("more", "More", Icons.Filled.MoreHoriz)

    // Detail screens (no bottom nav icon)
    object AddOrder : Screen("add_order", "Add Order")
    object OrderDetail : Screen("order_detail/{orderId}", "Order Detail") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }
    object Stock : Screen("stock", "Stock")
    object Payments : Screen("payments", "Payments")
}
