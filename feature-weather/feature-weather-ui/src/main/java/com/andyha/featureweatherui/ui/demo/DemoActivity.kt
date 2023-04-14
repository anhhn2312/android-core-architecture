package com.andyha.featureweatherui.ui.demo

import android.os.Bundle
import com.andyha.coreui.base.activity.BaseActivity
import com.andyha.featureweatherui.databinding.ActivityDemoBinding

class DemoActivity :
    BaseActivity<ActivityDemoBinding>({ ActivityDemoBinding.inflate(it) }) {
    override fun onActivityCreated(savedInstanceState: Bundle?) {
    }
}