package com.rajratna.manager.services

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rajratna.manager.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirestoreService {
    private val db by lazy { FirebaseFirestore.getInstance() }

    // ── Orders ─────────────────────────────────────────────
    private val ordersRef by lazy { db.collection("orders") }

    fun getOrdersFlow(): Flow<List<Order>> = callbackFlow {
        val listener = ordersRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val orders = snapshot?.documents?.map { doc ->
                    Order(
                        id = doc.id,
                        customerName = doc.getString("customerName") ?: "",
                        mobile = doc.getString("mobile") ?: "",
                        location = doc.getString("location") ?: "",
                        eventDate = doc.getString("eventDate") ?: "",
                        chairs = (doc.getLong("chairs") ?: 0).toInt(),
                        tables = (doc.getLong("tables") ?: 0).toInt(),
                        jars = (doc.getLong("jars") ?: 0).toInt(),
                        transportCharge = doc.getDouble("transportCharge") ?: 0.0,
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        advancePaid = doc.getDouble("advancePaid") ?: 0.0,
                        amountPaid = doc.getDouble("amountPaid") ?: 0.0,
                        isDelivered = doc.getBoolean("isDelivered") ?: false,
                        isReturned = doc.getBoolean("isReturned") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                } ?: emptyList()
                trySend(orders)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addOrder(order: Order): String {
        val data = hashMapOf(
            "customerName" to order.customerName,
            "mobile" to order.mobile,
            "location" to order.location,
            "eventDate" to order.eventDate,
            "chairs" to order.chairs,
            "tables" to order.tables,
            "jars" to order.jars,
            "transportCharge" to order.transportCharge,
            "totalAmount" to order.totalAmount,
            "advancePaid" to order.advancePaid,
            "amountPaid" to order.amountPaid,
            "isDelivered" to order.isDelivered,
            "isReturned" to order.isReturned,
            "createdAt" to order.createdAt
        )
        val doc = ordersRef.add(data).await()
        return doc.id
    }

    suspend fun updateOrder(orderId: String, updates: Map<String, Any>) {
        ordersRef.document(orderId).update(updates).await()
    }

    suspend fun deleteOrder(orderId: String) {
        ordersRef.document(orderId).delete().await()
    }

    /** Check if order exists with same customer name (case-insensitive) and date */
    suspend fun isDuplicateOrder(customerName: String, eventDate: String): Boolean {
        val result = ordersRef
            .whereEqualTo("customerName", customerName)
            .whereEqualTo("eventDate", eventDate)
            .get().await()
        return result.documents.isNotEmpty()
    }

    // ── Payments ───────────────────────────────────────────
    private val paymentsRef by lazy { db.collection("payments") }

    fun getPaymentsFlow(): Flow<List<Payment>> = callbackFlow {
        val listener = paymentsRef
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val payments = snapshot?.documents?.map { doc ->
                    Payment(
                        id = doc.id,
                        orderId = doc.getString("orderId") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        mode = doc.getString("mode") ?: "Cash"
                    )
                } ?: emptyList()
                trySend(payments)
            }
        awaitClose { listener.remove() }
    }

    fun getPaymentsForOrderFlow(orderId: String): Flow<List<Payment>> = callbackFlow {
        val listener = paymentsRef
            .whereEqualTo("orderId", orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val payments = snapshot?.documents?.map { doc ->
                    Payment(
                        id = doc.id,
                        orderId = doc.getString("orderId") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        mode = doc.getString("mode") ?: "Cash"
                    )
                } ?: emptyList()
                trySend(payments)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addPayment(payment: Payment): String {
        val data = hashMapOf(
            "orderId" to payment.orderId,
            "amount" to payment.amount,
            "date" to payment.date,
            "mode" to payment.mode
        )
        val doc = paymentsRef.add(data).await()
        return doc.id
    }

    // ── Customers ──────────────────────────────────────────
    private val customersRef by lazy { db.collection("customers") }

    fun getCustomersFlow(): Flow<List<Customer>> = callbackFlow {
        val listener = customersRef.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val customers = snapshot?.documents?.map { doc ->
                Customer(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    mobile = doc.getString("mobile") ?: "",
                    location = doc.getString("location") ?: "",
                    type = doc.getString("type") ?: "occasional"
                )
            } ?: emptyList()
            trySend(customers)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addOrUpdateCustomer(customer: Customer) {
        if (customer.mobile.isNotBlank()) {
            val existing = customersRef.whereEqualTo("mobile", customer.mobile).get().await()
            if (existing.documents.isNotEmpty()) {
                existing.documents.first().reference.update(
                    mapOf("name" to customer.name, "location" to customer.location, "type" to customer.type)
                ).await()
                return
            }
        }
        customersRef.add(
            hashMapOf("name" to customer.name, "mobile" to customer.mobile, "location" to customer.location, "type" to customer.type)
        ).await()
    }

    // ── Stock ──────────────────────────────────────────────
    private val stockDoc by lazy { db.collection("config").document("stock") }

    fun getStockFlow(): Flow<Stock> = callbackFlow {
        val listener = stockDoc.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val stock = Stock(
                totalChairs = (snapshot?.getLong("totalChairs") ?: 0).toInt(),
                totalTables = (snapshot?.getLong("totalTables") ?: 0).toInt(),
                totalJars = (snapshot?.getLong("totalJars") ?: 0).toInt()
            )
            trySend(stock)
        }
        awaitClose { listener.remove() }
    }

    suspend fun updateStock(stock: Stock) {
        stockDoc.set(
            hashMapOf("totalChairs" to stock.totalChairs, "totalTables" to stock.totalTables, "totalJars" to stock.totalJars)
        ).await()
    }

    // ── Water Customers ────────────────────────────────────
    private val waterCustomersRef by lazy { db.collection("waterCustomers") }

    fun getWaterCustomersFlow(): Flow<List<WaterCustomer>> = callbackFlow {
        val listener = waterCustomersRef.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val customers = snapshot?.documents?.map { doc ->
                WaterCustomer(id = doc.id, name = doc.getString("name") ?: "", mobile = doc.getString("mobile") ?: "", address = doc.getString("address") ?: "")
            } ?: emptyList()
            trySend(customers)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addWaterCustomer(customer: WaterCustomer): String {
        val doc = waterCustomersRef.add(hashMapOf("name" to customer.name, "mobile" to customer.mobile, "address" to customer.address)).await()
        return doc.id
    }

    // ── Water Deliveries ───────────────────────────────────
    private val waterDeliveriesRef by lazy { db.collection("waterDeliveries") }

    fun getWaterDeliveriesFlow(date: String): Flow<List<WaterDelivery>> = callbackFlow {
        val listener = waterDeliveriesRef
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val deliveries = snapshot?.documents?.map { doc ->
                    WaterDelivery(id = doc.id, customerId = doc.getString("customerId") ?: "", date = doc.getString("date") ?: "", isDelivered = doc.getBoolean("isDelivered") ?: false)
                } ?: emptyList()
                trySend(deliveries)
            }
        awaitClose { listener.remove() }
    }

    suspend fun markWaterDelivery(customerId: String, date: String, delivered: Boolean) {
        val existing = waterDeliveriesRef.whereEqualTo("customerId", customerId).whereEqualTo("date", date).get().await()
        if (existing.documents.isNotEmpty()) {
            existing.documents.first().reference.update("isDelivered", delivered).await()
        } else {
            waterDeliveriesRef.add(hashMapOf("customerId" to customerId, "date" to date, "isDelivered" to delivered)).await()
        }
    }
}
