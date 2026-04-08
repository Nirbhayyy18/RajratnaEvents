package com.rajratna.manager.models

import java.time.LocalDate

data class Order(
    val id: String = "",
    val customerName: String = "",
    val mobile: String = "",
    val location: String = "",
    val eventDate: String = "",
    val chairs: Int = 0,
    val tables: Int = 0,
    val jars: Int = 0,
    val transportCharge: Double = 0.0,
    val totalAmount: Double = 0.0,
    val advancePaid: Double = 0.0,
    val amountPaid: Double = 0.0,
    val isDelivered: Boolean = false,
    val isReturned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val remaining: Double get() = totalAmount - advancePaid - amountPaid

    val status: OrderStatus
        get() {
            if (isReturned) return OrderStatus.COMPLETED
            if (isDelivered) return OrderStatus.DELIVERED
            val today = LocalDate.now().toString()
            return when {
                eventDate > today -> OrderStatus.BOOKED
                else -> OrderStatus.ACTIVE
            }
        }

    val paymentStatus: PaymentStatus
        get() = when {
            remaining <= 0.0 -> PaymentStatus.PAID
            advancePaid + amountPaid > 0 -> PaymentStatus.PARTIAL
            else -> PaymentStatus.PENDING
        }

    companion object {
        const val CHAIR_RATE = 5.0
        const val TABLE_RATE = 30.0
        const val JAR_RATE = 30.0

        fun calculateTotal(chairs: Int, tables: Int, jars: Int, transportCharge: Double): Double {
            return (chairs * CHAIR_RATE) + (tables * TABLE_RATE) + (jars * JAR_RATE) + transportCharge
        }
    }
}

enum class OrderStatus(val label: String) {
    BOOKED("Booked"),
    ACTIVE("Active"),
    DELIVERED("Delivered"),
    COMPLETED("Completed")
}

enum class PaymentStatus(val label: String) {
    PAID("Paid"),
    PARTIAL("Partial"),
    PENDING("Pending")
}
