package eu.dhryciuk.uploader.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

data class ProcessedImage(
    val full: ByteArray,
    val thumbnail: ByteArray,
    val width: Int,
    val height: Int
)

class ImageProcessor(private val resolver: ContentResolver) {
    fun process(uri: Uri): ProcessedImage {
        val source = ImageDecoder.createSource(resolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val target = scaledSize(info.size.width, info.size.height, FULL_MAX)
            decoder.setTargetSize(target.first, target.second)
        }
        val thumbSize = scaledSize(bitmap.width, bitmap.height, THUMB_MAX)
        val thumbnail = Bitmap.createScaledBitmap(bitmap, thumbSize.first, thumbSize.second, true)
        val fullBytes = compress(bitmap, FULL_QUALITY)
        val thumbBytes = compress(thumbnail, THUMB_QUALITY)
        val result = ProcessedImage(fullBytes, thumbBytes, bitmap.width, bitmap.height)
        if (thumbnail !== bitmap) thumbnail.recycle()
        bitmap.recycle()
        return result
    }

    private fun scaledSize(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        if (width <= maxEdge && height <= maxEdge) return width to height
        val scale = maxEdge.toFloat() / maxOf(width, height)
        return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
    }

    @Suppress("DEPRECATION")
    private fun compress(bitmap: Bitmap, quality: Int): ByteArray {
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }
        return ByteArrayOutputStream().use { output ->
            check(bitmap.compress(format, quality, output)) { "Nie udało się przetworzyć zdjęcia." }
            output.toByteArray()
        }
    }

    private companion object {
        const val FULL_MAX = 2200
        const val THUMB_MAX = 520
        const val FULL_QUALITY = 86
        const val THUMB_QUALITY = 72
    }
}
