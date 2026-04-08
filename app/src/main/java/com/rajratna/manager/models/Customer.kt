package com.rajratna.manager.models

data class Customer(
    val id: String = "",
    val name: String = "",
    val mobile: String = "",
    val location: String = "",
    val type: String = "occasional" // "daily" or "occasional"
)
