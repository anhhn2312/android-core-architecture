package com.andyha.coreutils.imageToken

import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.io.InputStream


class LoadingImageTokenLoaderFactory : ModelLoaderFactory<LoadingImageToken, InputStream> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<LoadingImageToken, InputStream> {
        return LoadingImageTokenModelLoader()
    }

    override fun teardown() {
        //do nothing
    }
}