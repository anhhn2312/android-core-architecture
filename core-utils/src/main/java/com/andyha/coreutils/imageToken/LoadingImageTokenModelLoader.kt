package com.andyha.coreutils.imageToken

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream


class LoadingImageTokenModelLoader: ModelLoader<LoadingImageToken, InputStream> {

    override fun buildLoadData(model: LoadingImageToken, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        val url = model.url
        val diskCacheKey = ObjectKey(url!!)
        return ModelLoader.LoadData(diskCacheKey, LoadingImageTokenDataFetcher(model))
    }

    override fun handles(model: LoadingImageToken): Boolean {
        return !model.url.isNullOrBlank()
    }
}