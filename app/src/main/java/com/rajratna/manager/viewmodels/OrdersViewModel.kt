package com.rajratna.manager.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajratna.manager.models.*
import com.rajratna.manager.services.FirestoreService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/** One-shot UI event for snackbars */
sealed class UiEvent {
    data class Success(val message: String) : UiEvent()
    data class Error(val message: String) : UiEvent()
}

class OrdersViewModel : ViewModel() {

    // ── Orders ──────────────────────────────────────────
    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    val allOrders: StateFlow<List<Order>> = _allOrders

    private val _filteredOrders = MutableStateFlow<List<Order>>(emptyList())
    val filteredOrders: StateFlow<List<Order>> = _filteredOrders

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // ── UI Events (snackbar) ────────────────────────────
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    // ── Payments ────────────────────────────────────────
    private val _allPayments = MutableStateFlow<List<Payment>>(emptyList())
    val allPayments: StateFlow<List<Payment>> = _allPayments

    // ── Customers ───────────────────────────────────────
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers

    // ── Stock (for validation) ──────────────────────────
    private val _stock = MutableStateFlow(Stock())
    val stock: StateFlow<Stock> = _stock

    init {
        loadOrders()
        loadPayments()
        loadCustomers()
        loadStock()
    }

    private fun loadOrders() {
        viewModelScope.launch {
            FirestoreService.getOrdersFlow()
                .catch { e -> _error.value = e.message; _isLoading.value = false }
                .collect { orders -> _allOrders.value = orders; applyFilter(); _isLoading.value = false }
        }
    }

    private fun loadPayments() {
        viewModelScope.launch {
            FirestoreService.getPaymentsFlow()
                .catch { /* ignore */ }
                .collect { _allPayments.value = it }
        }
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            FirestoreService.getCustomersFlow()
                .catch { /* ignore */ }
                .collect { _customers.value = it }
        }
    }

    private fun loadStock() {
        viewModelScope.launch {
            FirestoreService.getStockFlow()
                .catch { /* ignore */ }
                .collect { _stock.value = it }
        }
    }

    // ── Filtering ───────────────────────────────────────
    fun setFilter(filter: String) { _selectedFilter.value = filter; applyFilter() }

    private fun applyFilter() {
        val orders = _allOrders.value
        _filteredOrders.value = when (_selectedFilter.value) {
            "Today" -> orders.filter { it.eventDate == LocalDate.now().toString() && !it.isReturned }
            "Upcoming" -> orders.filter { it.status == OrderStatus.BOOKED }
            "Completed" -> orders.filter { it.status == OrderStatus.COMPLETED }
            else -> orders
        }
    }

    // ── Stock Validation ────────────────────────────────
    fun validateStock(chairs: Int, tables: Int, jars: Int): String? {
        val s = _stock.value
        val activeOrders = _allOrders.value.filter { !it.isReturned }
        val usedChairs = activeOrders.sumOf { it.chairs }
        val usedTables = activeOrders.sumOf { it.tables }
        val usedJars = activeOrders.sumOf { it.jars }

        val errors = mutableListOf<String>()
        if (chairs > s.totalChairs - usedChairs) errors.add("Chairs: need $chairs, available ${s.totalChairs - usedChairs}")
        if (tables > s.totalTables - usedTables) errors.add("Tables: need $tables, available ${s.totalTables - usedTables}")
        if (jars > s.totalJars - usedJars) errors.add("Jars: need $jars, available ${s.totalJars - usedJars}")
        return if (errors.isEmpty()) null else errors.joinToString("\n")
    }

    // ── Add Order ───────────────────────────────────────
    fun addOrder(order: Order, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                // Duplicate check
                val isDupe = FirestoreService.isDuplicateOrder(order.customerName, order.eventDate)
                if (isDupe) { onError("Duplicate: ${order.customerName} already has an order on ${order.eventDate}"); return@launch }

                FirestoreService.addOrder(order)

                // Save/update customer
                if (order.mobile.isNotBlank()) {
                    FirestoreService.addOrUpdateCustomer(Customer(name = order.customerName, mobile = order.mobile, location = order.location))
                }

                // Add advance as first payment
                if (order.advancePaid > 0) {
                    FirestoreService.addPayment(Payment(orderId = "", amount = order.advancePaid, date = LocalDate.now().toString(), mode = "Cash"))
                }

                _uiEvent.emit(UiEvent.Success("Order saved!"))
                onSuccess()
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.Error(e.message ?: "Failed"))
                onError(e.message ?: "Failed to add order")
            }
        }
    }

    // ── Update Order ────────────────────────────────────
    fun updateOrder(orderId: String, updates: Map<String, Any>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                FirestoreService.updateOrder(orderId, updates)
                _uiEvent.emit(UiEvent.Success("Updated!"))
                onDone()
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.Error(e.message ?: "Update failed"))
                _error.value = e.message
            }
        }
    }

    // ── Add Payment (with history) ──────────────────────
    fun addPayment(orderId: String, amount: Double, mode: String, currentAmountPaid: Double) {
        viewModelScope.launch {
            try {
                // Update order total paid
                FirestoreService.updateOrder(orderId, mapOf("amountPaid" to (currentAmountPaid + amount)))
                // Store payment record
                FirestoreService.addPayment(Payment(orderId = orderId, amount = amount, date = LocalDate.now().toString(), mode = mode))
                _uiEvent.emit(UiEvent.Success("Payment of ₹${amount.toInt()} added!"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.Error(e.message ?: "Payment failed"))
            }
        }
    }

    fun getOrderById(orderId: String): Order? = _allOrders.value.find { it.id == orderId }

    // ── Customer lookup ─────────────────────────────────
    fun findCustomerByMobile(mobile: String): Customer? = _customers.value.find { it.mobile == mobile }

    // ── Income Calculation ──────────────────────────────
    fun getTodayIncome(): Double {
        val today = LocalDate.now().toString()
        return _allPayments.value.filter { it.date == today }.sumOf { it.amount }
    }

    fun getWeeklyIncome(): Double {
        val now = LocalDate.now()
        val weekStart = now.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
        return _allPayments.value.filter {
            try { val d = LocalDate.parse(it.date); !d.isBefore(weekStart) && !d.isAfter(now) } catch (_: Exception) { false }
        }.sumOf { it.amount }
    }

    fun getMonthlyIncome(): Double {
        val now = LocalDate.now()
        val monthStart = now.withDayOfMonth(1)
        return _allPayments.value.filter {
            try { val d = LocalDate.parse(it.date); !d.isBefore(monthStart) && !d.isAfter(now) } catch (_: Exception) { false }
        }.sumOf { it.amount }
    }
}
