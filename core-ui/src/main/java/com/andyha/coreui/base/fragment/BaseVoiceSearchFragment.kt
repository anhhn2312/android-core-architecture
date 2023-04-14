package com.andyha.coreui.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.ActivityResultLauncher
import androidx.viewbinding.ViewBinding
import com.andyha.coreui.base.ui.widget.BaseSearchView
import com.andyha.coreui.base.viewModel.BaseViewModel


abstract class BaseVoiceSearchFragment<VB : ViewBinding>(val factory: (LayoutInflater) -> VB) :
    BaseFragment<VB, BaseViewModel>(factory) {

    protected lateinit var voiceSearchResultLauncher: ActivityResultLauncher<Unit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerVoiceSearchResultContract()
    }

    private fun registerVoiceSearchResultContract() {
        voiceSearchResultLauncher =
            registerForActivityResult(BaseSearchView.VoiceSearchActivityResultContract()) {
                onVoiceSearchResult(it)
            }
    }

    protected open fun onVoiceSearchResult(result: String) {}
}