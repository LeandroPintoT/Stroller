package cl.panmu.stroller.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QrCodeAnalyzer(private val ctx: Context,
                     private val cb: (String) -> Unit) : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val bmp = image.toBitmap()
        // Process image searching for barcodes
        val options = BarcodeScannerOptions.Builder()
            .build()

        val scanner = BarcodeScanning.getClient(options)

        scanner.process(InputImage.fromBitmap(bmp, image.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    val valor: String = barcode.rawValue.toString()
                    if (checkFormato(valor)) {
                        cb(valor)
                    }
                }
            }
            .addOnFailureListener { }

        image.close()
    }

    private fun checkFormato(barcode: String): Boolean {
        return barcode.startsWith("obsws://")
    }
}