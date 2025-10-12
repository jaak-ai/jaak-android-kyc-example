package com.jaak.kyc.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.jaak.kyc.R
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.databinding.ActivitySuccessDocumentBinding
import com.jaak.kyc.ui.viewmodel.KycOfflineViewModel
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.Utils
import com.jaak.kyc.utils.FileStorageUtils
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SuccessDocumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuccessDocumentBinding
    private val kycOfflineViewModel: KycOfflineViewModel by viewModels()
    private var imagePath : String? = null
    private var videoPath : String? = null
    private var bestFramePath : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewModel()
        initComponents()
    }

    private fun initComponents(){
        // Esta pantalla SOLO es de procesamiento/loading
        // Solo configurar el botón de reiniciar en pantalla de procesamiento
        binding.btnRestartVerification.setOnClickListener {
            val intent = Intent(this, InitProcessLivenessActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
        // 🔧 Recibir rutas de archivos permanentes (String) en lugar de URIs temporales
        imagePath = intent.getStringExtra(Constants.URI_DOCUMENT_D)
        videoPath = intent.getStringExtra(Constants.URI_VIDEO_V)
        
        if (videoPath != null) {
            proccessBase64(videoPath!!)
        } else {
            Toast.makeText(this, "Error: Video path not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun proccessBase64(videoFilePath : String){
        // 🔧 Pasar el PATH del video (no base64)
        // El repository se encargará de convertir a base64 solo para envío HTTP
        val livenessVerifyRequest = LivenessVerifyRequest(videoFilePath)

        // ✅ USAR NUEVO SISTEMA OFFLINE que guarda estados en BD
        kycOfflineViewModel.executeLiveness(livenessVerifyRequest)
    }

    private fun initViewModel(){
        // ✅ NUEVO SISTEMA OFFLINE - Observadores
        // Esta pantalla SOLO muestra el loading/procesamiento
        // La pantalla de éxito final es FinalSuccessActivity
        kycOfflineViewModel.isLoading.observe(this) { isLoading ->
            if(isLoading){
                binding.clProgress.visibility = View.VISIBLE
            }else{
                binding.clProgress.visibility = View.GONE
            }
        }

        kycOfflineViewModel.errorModel.observe(this) { error ->
            error?.let {
                // Determinar el tipo de error para mostrar mensaje específico
                val errorType = when {
                    it.message?.contains("Liveness", ignoreCase = true) == true -> {
                        if (it.message?.contains("score", ignoreCase = true) == true) {
                            ErrorProcessActivity.ERROR_TYPE_LIVENESS_SCORE_LOW
                        } else {
                            ErrorProcessActivity.ERROR_TYPE_LIVENESS_FAILED
                        }
                    }
                    it.message?.contains("Face comparison", ignoreCase = true) == true ||
                    it.message?.contains("OtoVerify", ignoreCase = true) == true -> {
                        if (it.message?.contains("score", ignoreCase = true) == true) {
                            ErrorProcessActivity.ERROR_TYPE_FACE_COMPARISON_SCORE_LOW
                        } else {
                            ErrorProcessActivity.ERROR_TYPE_FACE_COMPARISON_FAILED
                        }
                    }
                    else -> ErrorProcessActivity.ERROR_TYPE_GENERIC
                }

                // Navegar a ErrorProcessActivity con detalles del error
                val intent = Intent(this, ErrorProcessActivity::class.java)
                intent.putExtra(ErrorProcessActivity.EXTRA_ERROR_TYPE, errorType)
                intent.putExtra(ErrorProcessActivity.EXTRA_ERROR_MESSAGE, it.message)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
                kycOfflineViewModel.clearMessages()
            }
        }
        
        kycOfflineViewModel.successMessage.observe(this) { message ->
            message?.let {
                android.util.Log.d("SuccessDocumentActivity", "📨 SUCCESS MESSAGE RECEIVED: '$it'")

                when {
                    it.contains("Liveness verification completed") -> {
                        android.util.Log.d("SuccessDocumentActivity", "✅ Liveness completed - calling getBestFrameAndContinue()")
                        // ✅ Liveness exitoso → actualizar estado y ejecutar OtoVerify
                        updateProcessingStatus(getString(R.string.processing_status_face_comparison))
                        getBestFrameAndContinue()
                    }
                    it.contains("Face comparison completed") -> {
                        android.util.Log.d("SuccessDocumentActivity", "✅ OtoVerify completed - calling executeFinish()")
                        // ✅ OtoVerify exitoso → ejecutar Finish directamente (blacklist removed)
                        updateProcessingStatus(getString(R.string.processing_status_completing))
                        kycOfflineViewModel.executeFinish()
                    }
                    // Blacklist section removed - no longer needed
                    /*
                    it.contains("Blacklist services launched") -> {
                        android.util.Log.d("SuccessDocumentActivity", "✅ Blacklist launched - navigating to FinalSuccessActivity")
                        // ✅ Servicios de blacklist lanzados → navegar directo al éxito (sin finish)
                        val intent = Intent(this, FinalSuccessActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    */
                    it.contains("Face comparison pending") -> {
                        android.util.Log.d("SuccessDocumentActivity", "📱 OtoVerify pending (offline) - navigating to FinalSuccessActivity")
                        // 📱 OtoVerify offline sin bestFrame → navegar al éxito con mensaje offline
                        val intent = Intent(this, FinalSuccessActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    it.contains("KYC process completed successfully") -> {
                        android.util.Log.d("SuccessDocumentActivity", "✅ KYC complete - navigating to FinalSuccessActivity")
                        // ✅ Finish exitoso → navegar a pantalla final de éxito
                        val intent = Intent(this, FinalSuccessActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    it.contains("Liveness score too low") -> {
                        android.util.Log.e("SuccessDocumentActivity", "❌ Liveness score too low - showing error screen")
                        // ❌ Score de Liveness insuficiente → mostrar pantalla de error
                        val intent = Intent(this, ErrorProcessActivity::class.java)
                        intent.putExtra(ErrorProcessActivity.EXTRA_ERROR_TYPE, ErrorProcessActivity.ERROR_TYPE_LIVENESS_SCORE_LOW)
                        intent.putExtra(ErrorProcessActivity.EXTRA_ERROR_MESSAGE, getString(R.string.error_liveness_score_low))
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    it.contains("OtoVerify score too low") -> {
                        android.util.Log.e("SuccessDocumentActivity", "❌ OtoVerify score too low - showing error screen")
                        // ❌ Score de OtoVerify insuficiente → mostrar pantalla de error
                        val intent = Intent(this, ErrorProcessActivity::class.java)
                        intent.putExtra(ErrorProcessActivity.EXTRA_ERROR_TYPE, ErrorProcessActivity.ERROR_TYPE_FACE_COMPARISON_SCORE_LOW)
                        intent.putExtra(ErrorProcessActivity.EXTRA_ERROR_MESSAGE, getString(R.string.error_face_comparison_score_low))
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                    else -> {
                        android.util.Log.w("SuccessDocumentActivity", "⚠️ Unhandled success message: '$it'")
                    }
                }
                kycOfflineViewModel.clearMessages()
            }
        }
        
    }
    
    private fun updateProcessingStatus(statusMessage: String) {
        binding.tvProcessStatus.text = statusMessage
    }

    private fun sendOneToOne(facePath: String, bestFramePath: String) {
        // 🔧 Pasar los PATHS directamente (no base64)
        // El repository se encargará de convertir a base64 solo para envío HTTP
        val otoVerifyRequest = OtoVerifyRequest(facePath, bestFramePath)

        // ✅ USAR NUEVO SISTEMA OFFLINE que guarda estados en BD
        kycOfflineViewModel.executeOtoVerify(otoVerifyRequest)
    }
    
    private fun getBestFrameAndContinue() {
        // ⚠️ IMPORTANTE: Necesitamos obtener facePath del documento desde BD
        // y bestFrame de liveness desde BD para enviar ambos a OtoVerify

        lifecycleScope.launch {
            try {
                // Obtener currentProcess para tener el processId
                val currentProcess = kycOfflineViewModel.currentProcessDetails.value

                if (currentProcess == null) {
                    android.util.Log.e("SuccessDocumentActivity", "❌ currentProcess is null")
                    Toast.makeText(this@SuccessDocumentActivity, "Error: No se pudo obtener el proceso actual", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Obtener facePath del documento desde BD
                val facePath = currentProcess.ocr?.facePath

                if (facePath.isNullOrEmpty()) {
                    android.util.Log.e("SuccessDocumentActivity", "❌ facePath from document is null/empty - navigating to error screen")

                    // Navegar a pantalla de error
                    val intent = Intent(this@SuccessDocumentActivity, ErrorProcessActivity::class.java)
                    intent.putExtra(ErrorProcessActivity.EXTRA_ERROR_TYPE, ErrorProcessActivity.ERROR_TYPE_DOCUMENT_VALIDATION_FAILED)
                    intent.putExtra(ErrorProcessActivity.EXTRA_ERROR_MESSAGE, "No se pudo extraer la imagen facial del documento. Por favor, intente nuevamente.")
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                    return@launch
                }

                // Obtener bestFrame de liveness desde BD
                val bestFramePath = currentProcess.liveness?.bestFrame

                android.util.Log.d("SuccessDocumentActivity", "📱 Executing OtoVerify:")
                android.util.Log.d("SuccessDocumentActivity", "  - facePath (from document): '$facePath'")
                android.util.Log.d("SuccessDocumentActivity", "  - bestFramePath (from liveness): '${bestFramePath ?: "null"}'")

                // ✅ Enviar ambas imágenes
                // Si bestFramePath es null, OtoVerify será offline (PENDING)
                // Si bestFramePath tiene valor, OtoVerify será online (SYNCED)
                sendOneToOne(facePath, bestFramePath ?: "")

            } catch (e: Exception) {
                android.util.Log.e("SuccessDocumentActivity", "❌ Error in getBestFrameAndContinue: ${e.message}")
                Toast.makeText(this@SuccessDocumentActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}