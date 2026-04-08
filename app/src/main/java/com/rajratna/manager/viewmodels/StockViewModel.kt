package com.rajratna.manager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.manager.models.Order
import com.rajratna.manager.models.Stock
import com.rajratna.manager.services.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class StockViewModel : ViewModel() {

    private val _stock = MutableStateFlow(Stock())
    val stock: StateFlow<Stock> = _stock

    private val _activeOrders = MutableStateFlow<List<Order>>(emptyList())

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Computed: items currently out (in non-returned orders)
    val outChairs: Int get() = _activeOrders.value.filter { !it.isReturned }.sumOf { it.chairs }
    val outTables: Int get() = _activeOrders.value.filter { !it.isReturned }.sumOf { it.tables }
    val outJars: Int get() = _activeOrders.value.filter { !it.isReturned }.sumOf { it.jars }

    val availableChairs: Int get() = _stock.value.totalChairs - outChairs
    val availableTables: Int get() = _stock.value.totalTables - outTables
    val availableJars: Int get() = _stock.value.totalJars - outJars

    init {
        loadStock()
        loadOrders()
    }

    private fun loadStock() {
        viewModelScope.launch {
            FirestoreService.getStockFlow()
                .catch { /* ignore errors, use defaults */ }
                .collect { _stock.value = it; _isLoading.value = false }
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            FirestoreService.getOrdersFlow()
                .catch { /* ignore */ }
                .collect { _activeOrders.value = it }
        }
    }

    fun updateStock(totalChairs: Int, totalTables: Int, totalJars: Int) {
        viewModelScope.launch {
            val newStock = Stock(totalChairs, totalTables, totalJars)
            try {
                FirestoreService.updateStock(newStock)
            } catch (_: Exception) { }
        }
    }
}
