package com.andyha.coredata.manager.biometric

data class AuthenticationResult(
    val success: Boolean = false,
    val accountName: String? = null,
    val message: String? = null
)