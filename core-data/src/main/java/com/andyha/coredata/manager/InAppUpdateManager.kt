package com.andyha.coredata.manager

import com.google.android.play.core.appupdate.AppUpdateInfo
import kotlinx.coroutines.flow.SharedFlow


interface InAppUpdateManager {
    val newAvailableUpdate: SharedFlow<Pair<Int, AppUpdateInfo>>
    fun checkForUpdate()
}