package com.rajratna.manager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.manager.models.WaterCustomer
import com.rajratna.manager.models.WaterDelivery
import com.rajratna.manager.services.FirestoreService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate

class WaterViewModel : ViewModel() {

    private val _customers = MutableStateFlow<List<WaterCustomer>>(emptyList())
    val customers: StateFlow<List<WaterCustomer>> = _customers

    private val _deliveries = MutableStateFlow<List<WaterDelivery>>(emptyList())
    val deliveries: StateFlow<List<WaterDelivery>> = _deliveries

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadCustomers()
        loadDeliveries()
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            FirestoreService.getWaterCustomersFlow()
                .catch { _isLoading.value = false }
                .collect { _customers.value = it; _isLoading.value = false }
        }
    }

    private fun loadDeliveries() {
        viewModelScope.launch {
            FirestoreService.getWaterDeliveriesFlow(_selectedDate.value)
                .catch { /* ignore */ }
                .collect { _deliveries.value = it }
        }
    }

    fun setDate(date: String) {
        _selectedDate.value = date
        loadDeliveries()
    }

    fun toggleDelivery(customerId: String, delivered: Boolean) {
        viewModelScope.launch {
            try {
                FirestoreService.markWaterDelivery(customerId, _selectedDate.value, delivered)
            } catch (_: Exception) { }
        }
    }

    fun addCustomer(name: String, mobile: String, address: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                FirestoreService.addWaterCustomer(
                    WaterCustomer(name = name, mobile = mobile, address = address)
                )
                onDone()
            } catch (_: Exception) { }
        }
    }
}
