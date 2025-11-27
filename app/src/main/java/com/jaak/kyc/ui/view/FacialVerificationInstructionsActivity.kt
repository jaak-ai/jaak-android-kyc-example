package com.jaak.kyc.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaak.kyc.databinding.ActivityFacialVerificationInstructionsBinding
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.FileStorageUtils
import com.jaak.stampssdk.sdk.StampsSDK
import com.jaak.visagesdk.ui.adapter.VisageListener
import com.jaak.visagesdk.ui.view.VisageSDK
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

/**
 * Activity que muestra las instrucciones para la verificación facial
 * antes de iniciar el SDK de Visage para captura de liveness
 */
@AndroidEntryPoint
class FacialVerificationInstructionsActivity : AppCompatActivity(), VisageListener {

    private lateinit var binding: ActivityFacialVerificationInstructionsBinding
    private lateinit var visageSDK: VisageSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFacialVerificationInstructionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initComponents()
        initFaceDetector()
    }

    private fun initComponents() {
        // Configurar botón de empezar de nuevo - vuelve al Dashboard sin cerrar sesión
        binding.btnStartAgain.setOnClickListener {
            restartKycFlow()
        }
        
        // Configurar botón de iniciar verificación facial
        binding.btnStartFacialVerification.setOnClickListener {
            // Iniciar el SDK de Visage para liveness
            visageSDK.startVisage()
        }
    }

    /**
     * Reinicia el flujo KYC sin cerrar sesión
     */
    private fun restartKycFlow() {
        val intent = Intent(this, com.jaak.kyc.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun initFaceDetector() {
        // Inicializar VisageSDK con licencia dinámica o por defecto
        val flowLicense = com.jaak.kyc.utils.FlowLicenseManager.getLicense(this)
        val visageLicense = flowLicense ?: "6KIY-7OIX-71WZ-IFU8"
        android.util.Log.d("FacialVerification", "→ Inicializando VisageSDK con licencia: $visageLicense")
        VisageSDK.initialize(visageLicense)
        VisageSDK.setEnvironment(VisageSDK.Environment.QA, this)
        visageSDK = VisageSDK(this, this)
        visageSDK.setShowTutorial(true)
        // Configurar SDK para no mostrar preview y retornar directamente
        visageSDK.setShowPreview(false)
    }

    // Implementación de VisageListener
    override fun onErrorVisage(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onSuccessVisage(typeProcess: Int, uri: Uri?) {
        android.util.Log.d("FacialVerification", "==== onSuccessVisage called ====")
        android.util.Log.d("FacialVerification", "URI received: $uri")
        android.util.Log.d("FacialVerification", "URI scheme: ${uri?.scheme}")
        android.util.Log.d("FacialVerification", "URI path: ${uri?.path}")

        if (uri == null) {
            android.util.Log.e("FacialVerification", "❌ URI is null - cannot proceed")
            Toast.makeText(this, "Error: No se pudo obtener el video de liveness", Toast.LENGTH_LONG).show()
            return
        }

        val raw = uri.toString()
        val normalizedPath = when {
            raw.endsWith(".mp4_temp") -> raw.removeSuffix(".mp4_temp") + ".mp4"
            raw.endsWith("_temp") -> raw.removeSuffix("_temp")
            else -> raw
        }
        val normalizedUri = if (normalizedPath.startsWith("/")) {
            Uri.fromFile(File(normalizedPath))
        } else Uri.parse(normalizedPath)

        // El SDK ya comprimió el video, solo guardarlo en almacenamiento permanente
        val savedPath = FileStorageUtils.saveUriToPermanentFile(
            context = this,
            temporalUri = normalizedUri,
            fileName = "liveness_video_${System.currentTimeMillis()}.mp4"
        )

        android.util.Log.d("FacialVerification", "Saved video path: $savedPath")
        if (savedPath == null) {
            android.util.Log.e("FacialVerification", "❌ Failed to save video to permanent storage")
            Toast.makeText(this, "Error al guardar el video", Toast.LENGTH_LONG).show()
            return
        }

        // Continuar al SuccessDocumentActivity con los datos
        val resultIntent = Intent(this, SuccessDocumentActivity::class.java)
        resultIntent.putExtra(Constants.URI_VIDEO_V, savedPath) // String path

        // Si hay datos del documento desde el intent anterior, pasarlos también
        val frontImagePath = intent.getStringExtra("frontImagePath")
        if (frontImagePath != null) {
            resultIntent.putExtra(Constants.URI_DOCUMENT_D, frontImagePath) // String path
            android.util.Log.d("FacialVerification", "Front image path: $frontImagePath")
        } else {
            android.util.Log.w("FacialVerification", "⚠️ No front image path found in intent")
        }

        android.util.Log.d("FacialVerification", "Navigating to SuccessDocumentActivity")
        startActivity(resultIntent)
        finish()
    }
}