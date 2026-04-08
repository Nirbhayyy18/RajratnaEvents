package com.rajratna.manager.models

data class Payment(
    val id: String = "",
    val orderId: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val mode: String = "Cash" // "Cash" or "UPI"
)
