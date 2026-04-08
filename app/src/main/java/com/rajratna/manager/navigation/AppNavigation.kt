package com.rajratna.manager.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.rajratna.manager.ui.calendar.CalendarScreen
import com.rajratna.manager.ui.home.HomeScreen
import com.rajratna.manager.ui.more.MoreScreen
import com.rajratna.manager.ui.orders.AddOrderScreen
import com.rajratna.manager.ui.orders.OrderDetailScreen
import com.rajratna.manager.ui.orders.OrdersScreen
import com.rajratna.manager.ui.payments.PaymentsScreen
import com.rajratna.manager.ui.splash.SplashScreen
import com.rajratna.manager.ui.stock.StockScreen
import com.rajratna.manager.ui.water.WaterScreen
import com.rajratna.manager.viewmodels.OrdersViewModel
import com.rajratna.manager.viewmodels.StockViewModel
import com.rajratna.manager.viewmodels.WaterViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    ordersViewModel: OrdersViewModel = viewModel(),
    stockViewModel: StockViewModel = viewModel(),
    waterViewModel: WaterViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(onFinish = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(viewModel = ordersViewModel)
        }

        composable(Screen.Orders.route) {
            OrdersScreen(
                viewModel = ordersViewModel,
                onAddOrder = { navController.navigate(Screen.AddOrder.route) },
                onOrderClick = { orderId -> navController.navigate(Screen.OrderDetail.createRoute(orderId)) }
            )
        }

        composable(Screen.AddOrder.route) {
            AddOrderScreen(viewModel = ordersViewModel, onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(orderId = orderId, viewModel = ordersViewModel, onBack = { navController.popBackStack() })
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(viewModel = ordersViewModel, stockViewModel = stockViewModel)
        }

        composable(Screen.Water.route) {
            WaterScreen(viewModel = waterViewModel)
        }

        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToStock = { navController.navigate(Screen.Stock.route) },
                onNavigateToPayments = { navController.navigate(Screen.Payments.route) }
            )
        }

        composable(Screen.Stock.route) {
            StockScreen(viewModel = stockViewModel, onBack = { navController.popBackStack() })
        }

        composable(Screen.Payments.route) {
            PaymentsScreen(
                viewModel = ordersViewModel,
                onBack = { navController.popBackStack() },
                onOrderClick = { orderId -> navController.navigate(Screen.OrderDetail.createRoute(orderId)) }
            )
        }
    }
}
