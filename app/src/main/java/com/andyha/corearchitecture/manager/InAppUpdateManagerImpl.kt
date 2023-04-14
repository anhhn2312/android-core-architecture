package com.andyha.corearchitecture.manager

import android.content.Context
import com.andyha.coredata.manager.InAppUpdateManager
import com.andyha.coreextension.targetSdkVersion
import com.andyha.coreextension.versionCode
import com.andyha.corenetwork.remoteConfig.RemoteConfigManager
import com.andyha.corenetwork.remoteConfig.hotfixVersions
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject


class InAppUpdateManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigManager: RemoteConfigManager,
) : InAppUpdateManager {
    private val _newAvailableUpdate = MutableSharedFlow<Pair<Int, AppUpdateInfo>>(replay = 1)
    override val newAvailableUpdate = _newAvailableUpdate.asSharedFlow()

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(context) }

    override fun checkForUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        val hotfixVersions = remoteConfigManager.hotfixVersions
        Timber.d("InAppUpdate: Hotfix versions: $hotfixVersions")
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val newVersionCode = appUpdateInfo.availableVersionCode()
            Timber.d("InAppUpdate: info: $newVersionCode")
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (isMajorUpdate(newVersionCode)) {
                    _newAvailableUpdate.tryEmit(Pair(IMMEDIATE, appUpdateInfo))
                } else if (isHotfixUpdate()) {
                    _newAvailableUpdate.tryEmit(Pair(IMMEDIATE, appUpdateInfo))
                }
            } else if (appUpdateInfo.updateAvailability() == DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                Timber.d("InAppUpdate: type: DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS")
                _newAvailableUpdate.tryEmit(Pair(IMMEDIATE, appUpdateInfo))
            }
        }
    }

    private fun isMajorUpdate(newVersionCode: Int): Boolean {
        val currentMajor = getMajorNumber(context.versionCode())
        val newMajor = getMajorNumber(newVersionCode)
        Timber.d("InAppUpdate: currentMajor($currentMajor) - newMajor($newMajor)")
        return (currentMajor ?: 0) < (newMajor ?: 0)
    }

    private fun isHotfixUpdate(): Boolean {
        val hotfixVersions = remoteConfigManager.hotfixVersions
        Timber.d("InAppUpdate: Hotfix versions: $hotfixVersions")
        return hotfixVersions.contains(context.versionCode().toString())
    }

    private fun getMajorNumber(versionCode: Int): Int? {
        val targetSdk = context.targetSdkVersion().toString()
        return versionCode.toString().replaceFirst(targetSdk, "")
            .let { it.substring(0, it.length - 4).toIntOrNull() }
    }
}