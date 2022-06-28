package com.andyha.coredata.manager.biometric

import android.content.Context
import androidx.annotation.StringRes
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.andyha.coredata.R
import timber.log.Timber
import java.lang.ref.SoftReference

/**
 * Since we are using the same methods in more than one Activity, better give them their own file.
 */
object BiometricPromptUtils {

    // Use weakReference to avoid memory leak
    fun createBiometricPrompt(
        fragment: Fragment,
        callback: BiometricCallback,
        isBiometricEnable: Boolean = false
    ): BiometricPrompt? = createBiometricPrompt(
        SoftReference(fragment),
        SoftReference(callback),
        isBiometricEnable
    )

    private fun createBiometricPrompt(
        fragmentReference: SoftReference<Fragment>,
        callbackReference: SoftReference<BiometricCallback>,
        isBiometricEnable: Boolean = false
    ): BiometricPrompt? {
        val fragment = fragmentReference.get() ?: return null
        val executor = ContextCompat.getMainExecutor(fragment.context)

        val authenCallback = object : AuthenticationCallback() {

            override fun onAuthenticationError(errCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errCode, errString)
                Timber.e("errCode is $errCode and errString is: $errString - fragment: ${fragmentReference.get()}")
                val callback = callbackReference.get() ?: return

                val result = when (errCode) {
                    ERROR_NO_BIOMETRICS -> BiometricResult.NotEnrolled
                    ERROR_LOCKOUT, ERROR_LOCKOUT_PERMANENT -> BiometricResult.ManyAttempts
                    ERROR_USER_CANCELED -> BiometricResult.UserCanceled
                    ERROR_NEGATIVE_BUTTON -> BiometricResult.CLickNegative
                    else -> BiometricResult.Failed
                }
                callback.invoke(result, null)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Timber.e("onAuthenticationFailed")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Timber.d("Authentication was successful. callback = ${callbackReference.get()}")
                if (isBiometricEnable) {
                    callbackReference.get()?.invoke(BiometricResult.Success, result)
                } else {
                    callbackReference.get()?.invoke(BiometricResult.ConfirmPwToEnable, result)
                }
            }
        }
        return BiometricPrompt(fragment, executor, authenCallback)
    }


    fun createPromptInfo(
        context: Context,
        @StringRes titleResId: Int? = null,
        @StringRes subtitleResId: Int? = null,
        @StringRes descriptionResId: Int? = null,
        @StringRes negativeButtonTextResId: Int? = null
    ): PromptInfo {
        val negativeButtonText = negativeButtonTextResId ?: R.string.common_btn_cancel
        return PromptInfo.Builder().apply {
            titleResId?.let { setTitle(context.getString(titleResId)) }
            subtitleResId?.let { setSubtitle(context.getString(subtitleResId)) }
            descriptionResId?.let { setDescription(context.getString(descriptionResId)) }
            setConfirmationRequired(false)
            setNegativeButtonText(context.getString(negativeButtonText))
        }.build()
    }
}

abstract class BiometricCallback {
    abstract fun invoke(
        result: BiometricResult,
        authenResult: BiometricPrompt.AuthenticationResult?
    )
}