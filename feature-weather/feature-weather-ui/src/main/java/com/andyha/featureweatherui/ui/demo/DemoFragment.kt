package com.andyha.featureweatherui.ui.demo

import android.os.Bundle
import android.view.View
import com.andyha.coreconfig.buildConfig.BuildConfig
import com.andyha.coreui.base.fragment.BaseFragment
import com.andyha.featureweatherui.databinding.FragmentDemoBinding
import javax.inject.Inject

class DemoFragment :
    BaseFragment<FragmentDemoBinding, DemoViewModel>({ FragmentDemoBinding.inflate(it) }) {

    @Inject
    lateinit var buildConfig: BuildConfig

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding().textView.text = "Storage Encrypted: ${buildConfig.isStorageEncrypted}\n" +
                "Logging Enabled: ${buildConfig.isLoggingEnabled} \n" +
                "Crashlytics Enabled: ${buildConfig.isCrashlyticsEnabled} \n" +
                "Analytics Enabled: ${buildConfig.isAnalyticsEnabled} \n" +
                "Production Release: ${buildConfig.isProductionRelease} \n"
    }
}