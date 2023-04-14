package com.andyha.coreutils.imageToken

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import timber.log.Timber
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class LoadingImageTokenDataFetcher(val model: LoadingImageToken) : DataFetcher<InputStream> {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        try {
            val url = URL(model.url)
            val urlConnection = (url.openConnection() as? HttpURLConnection)
                ?.apply {
                    addRequestProperty("Authorization", model.accessToken)
                    useCaches = false
                    doInput = true
                }
                ?.also {
                    it.connect()
                }

            val inputStream = urlConnection?.inputStream
            callback.onDataReady(inputStream)
        } catch (e: Exception) {
            Timber.e(e)
            callback.onDataReady(null)
        }
    }

    override fun cleanup() {
        // do nothing
    }

    override fun cancel() {
        //do nothing
    }

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.REMOTE
    }
}