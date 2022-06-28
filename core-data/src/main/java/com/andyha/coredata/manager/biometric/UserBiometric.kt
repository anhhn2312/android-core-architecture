package com.andyha.coredata.manager.biometric

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*

data class UserBiometric(
    var accountEmailAddress: String? = null,
    var isBiometricExpired: Boolean = false,
    var refreshToken: String? = null,
)

fun String?.fromJsonToListUserBiometric(): ArrayList<UserBiometric> {
    return try {
        val listType: Type = object : TypeToken<List<UserBiometric>>() {}.type
        Gson().fromJson(this, listType)
    } catch (exception: Exception) {
        arrayListOf()
    }
}

fun ArrayList<UserBiometric>?.fromListToJson(): String {
    return try {
        val listType: Type = object : TypeToken<ArrayList<UserBiometric>>() {}.type
        Gson().toJson(this, listType)
    } catch (e: Exception) {
        ""
    }
}
