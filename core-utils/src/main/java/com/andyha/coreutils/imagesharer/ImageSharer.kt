package com.andyha.coreutils.imagesharer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import com.andyha.coredata.storage.preference.AppSharedPreference
import com.andyha.coredata.storage.preference.token
import com.andyha.corenetwork.config.NetworkConfigConstants
import com.andyha.coreutils.imageToken.LoadingImageToken
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


/**
 * Download images, store in internal storage and share images. Sample:
 *
 * ImageSharer.Builder(requireContext())
 *  .text("airplane images")
 *  .image("https://homepages.cae.wisc.edu/~ece533/images/airplane.png")
 *  .progressListener { progress ->
 *      when (progress) {
 *          is ImageSharer.Progress.Downloaded -> TODO()
 *          is ImageSharer.Progress.Error -> TODO()
 *          is ImageSharer.Progress.Saved -> TODO()
 *          is ImageSharer.Progress.Shared -> TODO()
 *          is ImageSharer.Progress.Started -> TODO()
 *      }
 *  }
 *  .build()
 *  .launch()
 *  .disposedBy(compositeDisposable)
 */
class ImageSharer private constructor(
    private val context: Context,
    private val titleShare: String,
    private val images: ArrayList<ImageShareMetadata>,
    private val text: String?,
    private val progressListener: ((Progress) -> Unit)?
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val token: String by lazy {
        val preference = AppSharedPreference.getInstance()
        return@lazy "${NetworkConfigConstants.BEARER} ${preference.token}"
    }

    /**
     * Download images
     */
    private fun downloadImage(context: Context, metadata: ImageShareMetadata): Bitmap {
        return if (metadata.tokenRequired) {
            val loadingImageToken = LoadingImageToken(metadata.url, token)
            Glide
                .with(context)
                .asBitmap()
                .load(loadingImageToken)
                .submit()
                .get()
        } else {
            Glide
                .with(context)
                .asBitmap()
                .load(metadata.url)
                .submit()
                .get()
        }
    }

    /**
     * Save image in internal storage
     */
    private fun storeImage(context: Context, image: Bitmap?): File? {
        if (image == null) {
            return null
        }

        val imageFile = getOutputMediaFile(context)

        try {
            val fos = FileOutputStream(imageFile)
            image.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.close()
            Timber.d("Image directory: $imageFile")
            return imageFile
        } catch (e: FileNotFoundException) {
            Timber.d("File not found: %s", e.message)
        } catch (e: IOException) {
            Timber.d("Error accessing file: %s", e.message)
        }
        return null
    }

    /**
     * Get cache file
     */
    private fun getOutputMediaFile(context: Context): File {
        val fileName = "Image-${System.currentTimeMillis()}.png"
        val directory = context.getExternalFilesDir("share")

        return File(directory?.path + File.separator + fileName)
    }

    /**
     * Share
     */
    private fun share(context: Context, files: List<File>, text: String?, titleShare: String) {
        val uris = files
            .filter { file -> file.exists() }
            .map { file ->
                FileProvider.getUriForFile(
                    context,
                    context.applicationContext.packageName.toString() + ".fileprovider",
                    file
                )
            }
        val isShareMultiples = uris.size > 1
        val action = if (isShareMultiples) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
        val sharingIntent = Intent(action).apply {
            type = when {
                uris.isNotEmpty() && text != null -> "*/*"
                uris.isNotEmpty() && text == null -> "image/*"
                uris.isEmpty() && text != null -> "text/plain"
                else -> throw IllegalArgumentException("Nothing to share, c'mon")
            }

            if (text != null) {
                if (isShareMultiples) {
                    putExtra(Intent.EXTRA_TEXT, arrayListOf(text))
                } else {
                    putExtra(Intent.EXTRA_TEXT, text)
                }
            }
            if (uris.isNotEmpty()) {
                if (isShareMultiples) {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                } else {
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                }
            }

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(sharingIntent, titleShare))
    }

    fun launch() {
        coroutineScope.launch {
            flow {
                val savedFiles = arrayListOf<File>()
                images.forEachIndexed { index, file ->
                    val bitmap = downloadImage(context, file)
                    emit(Progress.Downloaded(index))

                    val savedFile = storeImage(context, bitmap) ?: return@forEachIndexed
                    savedFiles.add(savedFile)
                    emit(Progress.Saved(index))
                }

                if (savedFiles.isNotEmpty()) {
                    share(context, savedFiles, text, titleShare)
                }
            }
                .flowOn(Dispatchers.IO)
                .onStart { progressListener?.invoke(Progress.Started) }
                .catch { progressListener?.invoke(Progress.Error(it)) }
                .onCompletion { progressListener?.invoke(Progress.Shared) }
                .collect { progressListener?.invoke(it) }
        }
    }

    class Builder(private val context: Context) {

        private var titleShare: String = "Share via"
        private var images: ArrayList<ImageShareMetadata> = arrayListOf()
        private var text: String? = null
        private var progressListener: ((Progress) -> Unit)? = null

        fun text(text: String): Builder {
            this.text = text
            return this
        }

        fun text(@StringRes text: Int): Builder {
            this.text = context.getString(text)
            return this
        }

        fun image(url: ImageShareMetadata): Builder {
            images.add(url)
            return this
        }

        fun images(urls: List<ImageShareMetadata>): Builder {
            images.addAll(urls)
            return this
        }

        fun titleShare(@StringRes titleShare: Int): Builder {
            return titleShare(context.getString(titleShare))
        }

        fun titleShare(titleShare: String): Builder {
            this.titleShare = titleShare
            return this
        }

        fun progressListener(progressListener: ((Progress) -> Unit)?): Builder {
            this.progressListener = progressListener
            return this
        }

        fun build(): ImageSharer {
            if (images.isEmpty()) {
                throw IllegalArgumentException("Images must not be empty")
            }
            return ImageSharer(context, titleShare, images, text, progressListener)
        }
    }

    sealed class Progress {
        object Started : Progress()
        class Downloaded(val index: Int) : Progress()
        class Saved(val index: Int) : Progress()
        object Shared : Progress()
        class Error(val error: Throwable) : Progress()
    }
}