package cl.panmu.stroller.util

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import cl.panmu.stroller.R
import cl.panmu.stroller.databinding.ScanQrActivityBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class QrScanner : ComponentActivity(R.layout.scan_qr_activity) {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: ScanQrActivityBinding
    private lateinit var reqPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ScanQrActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        checkCameraPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    /**
     * 1. This function is responsible to request the required CAMERA permission
     */
    private fun checkCameraPermission() {
        try {
            val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
            if (!this::reqPermissionLauncher.isInitialized) {
                val req = ActivityResultContracts.RequestMultiplePermissions()
                reqPermissionLauncher = registerForActivityResult(req) {
                    checkIfCameraPermissionIsGranted()
                }
            }
            reqPermissionLauncher.launch(requiredPermissions)
        } catch (e: IllegalArgumentException) {
            checkIfCameraPermissionIsGranted()
        }
    }

    /**
     * 2. This function will check if the CAMERA permission has been granted.
     * If so, it will call the function responsible to initialize the camera preview.
     * Otherwise, it will raise an alert.
     */
    private fun checkIfCameraPermissionIsGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted: start the preview
            startCamera()
        } else {
            // Permission denied
            AlertDialog
                .Builder(this@QrScanner)
                .setTitle(resources.getString(R.string.alerta_titulo_sin_permiso))
                .setMessage(resources.getString(R.string.alerta_msg_sin_permiso_camara_qr))
                .setPositiveButton("Ok") { _, _ -> }
                .setNegativeButton("Abrir configuración") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", this@QrScanner.packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
                .setCancelable(false)
                .create()
                .apply {
                    setCanceledOnTouchOutside(false)
                    show()
                }
        }
    }

    /**
     * This function is responsible for the setup of the camera preview and the image analyzer.
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            // Image analyzer
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        QrCodeAnalyzer(
                            this,
                            ::callback
                        )
                    )
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun callback(valor: String) {
        val intent = Intent()
        intent.putExtra("barcode", valor)
        setResult(RESULT_OK, intent)
        finish()
    }
}