package com.galla.app.hardware

import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Making and reading QR codes, entirely on the phone (the ZXing library).
 * Nothing is sent anywhere to create or to recognise a code.
 */
object QrCodes {

    /**
     * The grid of dark and light squares for some text. Asking for size 0 gives the smallest
     * grid (one cell per QR "module"), which callers then draw as large as they like.
     */
    fun matrix(text: String): BitMatrix = QRCodeWriter().encode(
        text,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
    )

    /** A black-on-white picture of the QR code, `sizePx` wide, with a white border around it. */
    fun bitmap(text: String, sizePx: Int): Bitmap {
        val grid = matrix(text)
        val quiet = 2
        val cells = grid.width + quiet * 2
        val cell = (sizePx / cells).coerceAtLeast(1)
        val size = cell * cells
        val pixels = IntArray(size * size) { Color.WHITE }
        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                if (!grid.get(x, y)) continue
                val left = (x + quiet) * cell
                val top = (y + quiet) * cell
                for (dy in 0 until cell) {
                    val row = (top + dy) * size
                    for (dx in 0 until cell) pixels[row + left + dx] = Color.BLACK
                }
            }
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}

/**
 * Looks at each camera frame for a QR code. The camera hands frames over in a format where
 * the first block of bytes is the brightness of every pixel, which is all a QR reader needs.
 */
class QrAnalyzer(private val onCode: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val reader = QRCodeReader()
    private val hints = mapOf(DecodeHintType.TRY_HARDER to true)

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes[0]
            val width = image.width
            val height = image.height
            val rowStride = plane.rowStride
            val buffer = plane.buffer
            buffer.rewind()
            val data = ByteArray(width * height)
            if (rowStride == width) {
                buffer.get(data, 0, minOf(data.size, buffer.remaining()))
            } else {
                // Rows are padded: copy only the real pixels of each row.
                val row = ByteArray(rowStride)
                for (y in 0 until height) {
                    val count = minOf(rowStride, buffer.remaining())
                    if (count <= 0) break
                    buffer.get(row, 0, count)
                    System.arraycopy(row, 0, data, y * width, minOf(width, count))
                }
            }
            val source = PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
            val text = try {
                reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
            } catch (notFound: Exception) {
                null
            } finally {
                reader.reset()
            }
            if (!text.isNullOrBlank()) onCode(text)
        } catch (ignored: Exception) {
            // A bad frame is not worth crashing for: the next one arrives in a moment.
        } finally {
            image.close()
        }
    }
}
