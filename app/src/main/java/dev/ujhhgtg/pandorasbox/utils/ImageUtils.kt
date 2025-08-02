package dev.ujhhgtg.pandorasbox.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.Image
import androidx.core.graphics.get
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ImageUtils {
    companion object {
        fun convertBitmapToInputTensor(bitmap: Bitmap): ByteBuffer {
//            val inputSize = 256 // width/height expected by your model
//            val modelInputChannels = 3 // RGB
//            val inputImage = bitmap.scale(inputSize, inputSize)
//
//            val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * modelInputChannels)
//            byteBuffer.order(ByteOrder.nativeOrder())
//
//            for (y in 0 until inputSize) {
//                for (x in 0 until inputSize) {
//                    val pixel = inputImage[x, y]
//
//                    val r = (Color.red(pixel) / 255.0f)
//                    val g = (Color.green(pixel) / 255.0f)
//                    val b = (Color.blue(pixel) / 255.0f)
//
//                    byteBuffer.putFloat(r)
//                    byteBuffer.putFloat(g)
//                    byteBuffer.putFloat(b)
//                }
//            }
//
//            byteBuffer.rewind()
//            return byteBuffer
            val inputSize = 640 // match your model's input
            val resizedBitmap = bitmap.scale(inputSize, inputSize)

            val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3) // 3 channels
            byteBuffer.order(ByteOrder.nativeOrder())

            for (y in 0 until inputSize) {
                for (x in 0 until inputSize) {
                    val pixel = resizedBitmap[x, y]

                    // Normalize RGB to [0, 1]
                    val r = Color.red(pixel) / 255.0f
                    val g = Color.green(pixel) / 255.0f
                    val b = Color.blue(pixel) / 255.0f

                    byteBuffer.putFloat(r)
                    byteBuffer.putFloat(g)
                    byteBuffer.putFloat(b)
                }
            }

            byteBuffer.rewind()
            return byteBuffer
        }

        fun convertImageToBitmap1(image: Image): Bitmap {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }

        fun convertImageToBitmap2(image: Image): Bitmap {
            val yBuffer: ByteBuffer = image.planes[0].buffer
            val uBuffer: ByteBuffer = image.planes[1].buffer
            val vBuffer: ByteBuffer = image.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            // U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
            val imageBytes = out.toByteArray()

            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
    }
}