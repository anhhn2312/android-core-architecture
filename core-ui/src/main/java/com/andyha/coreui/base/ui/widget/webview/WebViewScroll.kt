package com.andyha.coreui.base.ui.widget.webview

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import com.andyha.coreextension.dimen
import com.andyha.coreui.R
import timber.log.Timber
import kotlin.math.floor


class WebViewScroll @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    private var mOnScrollChangedCallback: OnScrollChangedCallback? = null

    fun setOnScrollChangedCallback(onScrollChangedCallback: OnScrollChangedCallback) {
        mOnScrollChangedCallback = onScrollChangedCallback
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (mOnScrollChangedCallback != null) {
            val webViewHeight = measuredHeight
            Timber.i("---> webViewHeight: $webViewHeight")
            Timber.i("---> scrollY: $scrollY")
            val paddingScrollWebView = context.dimen(R.dimen.dimen4dp)

            val totalHeightWebViewScroll = scrollY + webViewHeight + paddingScrollWebView
            Timber.d("---> totalHeightWebViewScroll: $totalHeightWebViewScroll")

            val scale = resources.displayMetrics.density
            val heightContent: Int = floor(((contentHeight * scale).toDouble())).toInt()
            Timber.d("---> heightContent: $heightContent")

            var scrollEnd = false
            if (totalHeightWebViewScroll >= heightContent) {
                scrollEnd = true
            }

            mOnScrollChangedCallback!!.onScroll(l, t, oldl, oldt, scrollEnd)
        }
    }

    interface OnScrollChangedCallback {
        fun onScroll(l: Int, t: Int, oldL: Int, oldT: Int, scrollEnd: Boolean)
    }
}
