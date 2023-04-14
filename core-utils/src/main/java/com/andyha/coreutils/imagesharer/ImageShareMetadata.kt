package com.andyha.coreutils.imagesharer


data class ImageShareMetadata(
    val url: String,
    val tokenRequired: Boolean = false,
)