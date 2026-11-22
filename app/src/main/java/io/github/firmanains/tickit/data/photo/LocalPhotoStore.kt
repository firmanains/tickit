package io.github.firmanains.tickit.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import io.github.firmanains.tickit.domain.repository.PhotoStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Proof photos live in app-private storage: free, offline, gone on uninstall. */
class LocalPhotoStore(
    context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PhotoStore {
    private val dir = File(context.filesDir, DIR)

    override fun newPhotoPath(): String = File(dir.apply { mkdirs() }, "proof-${System.currentTimeMillis()}.jpg").path

    override suspend fun discard(path: String) {
        withContext(io) { File(path).delete() }
    }

    /** Camera JPEGs are 3–5 MB; a daily proof at that size would eat gigabytes a year. */
    suspend fun shrink(path: String) = withContext(io) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return@withContext // missing or not an image
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE_PX) sample *= 2
        val orientation = ExifInterface(path).getAttribute(ExifInterface.TAG_ORIENTATION)
        val bitmap = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return@withContext
        File(path).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
        bitmap.recycle()
        // Re-encoding drops EXIF; put the rotation back so the photo isn't sideways.
        if (orientation != null) {
            ExifInterface(path).apply { setAttribute(ExifInterface.TAG_ORIENTATION, orientation) }.saveAttributes()
        }
    }

    companion object {
        const val DIR = "proofs" // also referenced by res/xml/proof_paths.xml and the backup rules
        private const val MAX_EDGE_PX = 1600
    }
}
