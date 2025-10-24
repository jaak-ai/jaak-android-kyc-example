package com.jaak.kyc.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaak.kyc.databinding.ActivityDocumentVerificationInstructionsBinding
import com.jaak.kyc.databinding.ActivityFacialVerificationInstructionsBinding
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.FileStorageUtils
import com.jaak.stampssdk.sdk.StampsSDK
import com.jaak.stampssdk.ui.adapter.StampsListener
import com.jaak.visagesdk.ui.adapter.VisageListener
import com.jaak.visagesdk.ui.view.VisageSDK
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

/**
 * Activity que muestra las instrucciones para la verificación facial
 * antes de iniciar el SDK de Visage para captura de liveness
 */
@AndroidEntryPoint
class DocumentVerificationInstructionsActivity : AppCompatActivity(), StampsListener {

    private lateinit var binding: ActivityDocumentVerificationInstructionsBinding
    private lateinit var stampsSDK: StampsSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentVerificationInstructionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initComponents()
    }

    private fun initComponents() {
        // Inicializar StampsSDK
        stampsSDK = StampsSDK(this, this)

        // Configuración extrema para máxima velocidad
        stampsSDK.setAutoStampsClassification(true)  // Sin OCR
        stampsSDK.setCaptureDelay(0)                  // Sin countdown
        stampsSDK.setAlignmentTolerance(50)           // Muy permisivo
        stampsSDK.setMaskSize(80)                     // Marco grande
        stampsSDK.setShowPreview(false)               // Sin preview

        binding.btnStartAgain.setOnClickListener {
            // Navegar de vuelta al menú principal
            val intent = Intent(this, InitProcessLivenessActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        binding.btnStartDocumentCapture.setOnClickListener {
            // Iniciar captura de documentos con StampsSDK
            stampsSDK.startStamps(1)
        }
    }

    // Implementación de StampsListener
    override fun onErrorStamps(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onSuccessStamps(typeProcess: Int,
                                 frontOriginalUri: Uri?, frontCropUri: Uri?,
                                 backOriginalUri: Uri?, backCropUri: Uri?,
                                 frontFaceCropUri: Uri?) {

        // 🔧 Convertir URIs temporales → archivos permanentes
        val frontImagePath = frontCropUri?.let {
            FileStorageUtils.saveUriToPermanentFile(this, it, "stamps_front_${System.currentTimeMillis()}.jpg")
        }
        val backImagePath = backCropUri?.let {
            FileStorageUtils.saveUriToPermanentFile(this, it, "stamps_back_${System.currentTimeMillis()}.jpg")
        }

        // Verificar que al menos la imagen frontal se guardó correctamente
        if (frontImagePath == null) {
            Toast.makeText(this, "Error saving document images", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔧 Navegar directamente a verificación de documentos con los datos
        val resultIntent = Intent(this, VerifyOcrDocumentActivity::class.java)
        resultIntent.putExtra(Constants.URI_DOCUMENT_V, frontImagePath)    // String path
        resultIntent.putExtra(Constants.URI_DOCUMENT_2_V, backImagePath)   // String path (puede ser null)
        resultIntent.putExtra(Constants.TYPE_PROCCESS_BASE64_V, typeProcess)
        startActivity(resultIntent)
        finish()
    }

}