package com.andyha.coredata.manager.biometric

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager.*
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import com.andyha.coredata.storage.preference.AppSharedPreference
import com.andyha.coredata.storage.preference.accountBiometric
import com.andyha.coredata.storage.preference.refreshToken
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject

/**
 * Cryptography manager: Handles encryption and decryption
 */

class BiometricsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val prefs: AppSharedPreference,
) : IBiometricsManager {

    private var keyStore: KeyStore? = null

    override fun canAuthenticate(callback: ((result: BiometricResult) -> Unit)?): Boolean {
        val canAuthenticateWeak = from(context).canAuthenticate(BIOMETRIC_WEAK)
        val canAuthenticate = from(context).canAuthenticate(BIOMETRIC_STRONG)

        when {
            canAuthenticate == BIOMETRIC_SUCCESS || (canAuthenticateWeak == BIOMETRIC_SUCCESS && canAuthenticate == BIOMETRIC_STATUS_UNKNOWN) -> {
                callback?.invoke(BiometricResult.Success)
                return true
            }
            canAuthenticate == BIOMETRIC_ERROR_NONE_ENROLLED -> {
                callback?.invoke(BiometricResult.NotEnrolled)
            }
            else -> {
                callback?.invoke(BiometricResult.Unavailable)
            }
        }
        return false
    }

    override fun requestAuthentication(
        fragment: Fragment,
        emailAddress: String,
        checkDisable: Boolean,
        callback: BiometricCallback,
        messageResID: Int,
        negativeButtonResID: Int?,
        removeOldKey: Boolean
    ) {
        if (!canAuthenticate {
                if (it != BiometricResult.Success) {
                    callback.invoke(it, null)
                }
            }) return
        // in case of biometric not enable in app setting - disable, will show scan fingerprint then handle it after fingerprint success
        // check disable o day
        if (!checkBiometricEnabled(emailAddress) && checkDisable) {
            callback.invoke(BiometricResult.Disabled, null)
            return
        }
        showBiometricAuthenticationPrompt(
            fragment,
            emailAddress,
            callback,
            messageResID,
            negativeButtonResID,
            removeOldKey
        )
    }

    override fun showBiometricAuthenticationPrompt(
        fragment: Fragment,
        emailAddress: String,
        callback: BiometricCallback,
        messageResID: Int,
        negativeButtonResID: Int?,
        removeOldKey: Boolean
    ) {
        val secretKeyName = ""

        val biometricPrompt =
            BiometricPromptUtils.createBiometricPrompt(
                fragment,
                callback,
                checkBiometricEnabled(emailAddress)
            ) ?: return

        val promptInfo = BiometricPromptUtils.createPromptInfo(
            context = context,
            titleResId = messageResID,
            negativeButtonTextResId = negativeButtonResID
        )
        try {
            biometricPrompt.cancelAuthentication()
        } catch (ex: java.lang.Exception) {
            Timber.d("error to show biometric ${ex.message}")
        }

        showBiometricPrompt(
            emailAddress,
            secretKeyName,
            biometricPrompt,
            promptInfo,
            callback,
            removeOldKey
        )
    }

    private fun showBiometricPrompt(
        emailAddress: String,
        secretKeyName: String,
        biometricPrompt: BiometricPrompt,
        promptInfo: BiometricPrompt.PromptInfo,
        callback: BiometricCallback,
        removeOldKey: Boolean
    ) {
        if (removeOldKey) {
            //Khi active lại vân tay sẽ expire key cũ
            updateStatusBiometricExpired(emailAddress)
        }

        try {
            val cipher = getCipher()
            val secretKey = getOrCreateSecretKey(secretKeyName)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            biometricPrompt.authenticate(promptInfo)
        } catch (e: KeyPermanentlyInvalidatedException) {
            if (removeOldKey) {
                keyStore?.deleteEntry(secretKeyName)
            }
            callback.invoke(BiometricResult.BiometricChanged, null)
        }
    }

    override fun saveEnabledBiometricUser(
        email: String,
        callback: (result: AuthenticationResult) -> Unit
    ) {
        val result = if (email.isNotEmpty()) {
            val userBiometric = UserBiometric().apply {
                accountEmailAddress = email
                refreshToken = prefs.refreshToken
                isBiometricExpired = false
            }

            // Save user biometric
            prefs.accountBiometric = gson.toJson(userBiometric)

            AuthenticationResult(
                success = true, message = "Success", accountName = email
            )
        } else {
            AuthenticationResult(success = false, message = "accountName is empty")
        }
        callback.invoke(result)
    }

    override fun checkBiometricEnabled(accountEmailAddress: String): Boolean {
        val userBiometricText = prefs.accountBiometric
        if (userBiometricText.isNotEmpty()) {
            return try {
                val userBiometric = gson.fromJson(userBiometricText, UserBiometric::class.java)
                userBiometric != null
                        && accountEmailAddress.equals(userBiometric.accountEmailAddress, true)
                        && !userBiometric.isBiometricExpired
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    override fun updateStatusBiometricExpired(accountEmailAddress: String) {
        Timber.d("updateStatusBiometricExpired: accountEmailAddress = $accountEmailAddress")
        try {
            val userBiometricText = prefs.accountBiometric
            if (userBiometricText.isNotEmpty()) {
                val userBiometric = gson.fromJson(userBiometricText, UserBiometric::class.java)
                if (userBiometric.accountEmailAddress.equals(accountEmailAddress, true)) {
                    userBiometric.isBiometricExpired = true
                    prefs.accountBiometric = gson.toJson(userBiometric)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            prefs.accountBiometric = ""
        }
    }

    override fun checkAndRelease(accountEmailAddress: String?) {
        try {
            val userBiometricText = prefs.accountBiometric
            if (userBiometricText.isNotEmpty()) {
                val userBiometric = gson.fromJson(userBiometricText, UserBiometric::class.java)
                if (!userBiometric.accountEmailAddress.equals(accountEmailAddress, true)) {
                    prefs.accountBiometric = ""
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            prefs.accountBiometric = ""
        }
    }

    private fun getCipher(): Cipher {
        val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE /$ENCRYPTION_PADDING"
        return Cipher.getInstance(transformation)
    }

    private fun getOrCreateSecretKey(keyName: String): SecretKey {
        // If SecretKey was previously created for that keyName, then grab and return it.
        keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore?.load(null) // Keystore must be loaded before it can be accessed
        keyStore?.getKey(keyName, null)?.let { return it as SecretKey }

        // if you reach here, then a new SecretKey must be generated for that keyName
        val paramsBuilder = KeyGenParameterSpec.Builder(
            keyName,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )

        paramsBuilder.apply {
            setBlockModes(ENCRYPTION_BLOCK_MODE)
            setEncryptionPaddings(ENCRYPTION_PADDING)
            setKeySize(KEY_SIZE)
            setUserAuthenticationRequired(true)
        }

        val keyGenParams = paramsBuilder.build()
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(keyGenParams)
        return keyGenerator.generateKey()
    }


    override fun getTokenFromBiometrics(accountEmailAddress: String): String? {
        val userBiometricText = prefs.accountBiometric
        var ret: String? = null
        if (userBiometricText.isNotEmpty()) {
            try {
                val userBiometric = gson.fromJson(userBiometricText, UserBiometric::class.java)
                if (userBiometric != null &&
                    accountEmailAddress.equals(userBiometric.accountEmailAddress, true) &&
                    !userBiometric.isBiometricExpired
                ) {
                    ret = userBiometric.refreshToken
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return ret
    }

    companion object {
        private const val KEY_SIZE = 256
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    }
}

interface IBiometricsManager {

    /**
     * Check for biometrics authentication availability
     */
    fun canAuthenticate(callback: ((result: BiometricResult) -> Unit)? = null): Boolean

    /**
     * Request a biometric authentication
     */
    fun requestAuthentication(
        fragment: Fragment,
        emailAddress: String,
        checkDisable: Boolean,
        callback: BiometricCallback,
        messageResID: Int,
        negativeButtonResID: Int? = null,
        removeOldKey: Boolean = false
    )

    /**
     * Show biometric authentication dialog
     */
    fun showBiometricAuthenticationPrompt(
        fragment: Fragment,
        emailAddress: String,
        callback: BiometricCallback,
        messageResID: Int,
        negativeButtonResID: Int? = null,
        removeOldKey: Boolean = false
    )

    /**
     * Check if biometrics login is enabled for a specific account
     */
    fun checkBiometricEnabled(accountEmailAddress: String): Boolean

    /**
     * Update expired status for a specific email address
     */
    fun updateStatusBiometricExpired(accountEmailAddress: String)

    fun checkAndRelease(accountEmailAddress: String?)

    fun saveEnabledBiometricUser(
        email: String,
        callback: (result: AuthenticationResult) -> Unit
    )


    fun getTokenFromBiometrics(accountEmailAddress: String): String?
}

enum class BiometricResult {
    Unavailable, // no hardware or not supported hardware
    NotEnrolled, // no biometric enrolled in the device
    Disabled, //biometric is disabled in Settings
    Success,
    ConfirmPwToEnable,
    Failed,
    UserCanceled,
    CLickNegative,
    ManyAttempts,
    BiometricChanged
}