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
        // 🔧 Repository se encarga de la conversión a base64
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
                // En caso de error, mostrar Toast y regresar al menú
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                val intent = Intent(this, MenuMainActivity::class.java)
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
                        android.util.Log.d("SuccessDocumentActivity", "✅ OtoVerify completed - calling executeBlacklist()")
                        // ✅ OtoVerify exitoso → actualizar estado y ejecutar Blacklist
                        updateProcessingStatus(getString(R.string.processing_status_blacklist))
                        kycOfflineViewModel.executeBlacklist()
                    }
                    it.contains("Blacklist services launched") -> {
                        android.util.Log.d("SuccessDocumentActivity", "✅ Blacklist launched - navigating to FinalSuccessActivity")
                        // ✅ Servicios de blacklist lanzados → navegar directo al éxito (sin finish)
                        val intent = Intent(this, FinalSuccessActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
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
                    it.contains("Liveness score too low") || it.contains("OtoVerify score too low") -> {
                        android.util.Log.e("SuccessDocumentActivity", "❌ Score too low - returning to menu")
                        // ❌ Score insuficiente → mostrar error y regresar
                        Toast.makeText(this, getString(R.string.error_human), Toast.LENGTH_LONG).show()
                        val intent = Intent(this, MenuMainActivity::class.java)
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

    private fun sendOneToOne(imageDocumentPath: String, videoData: String) {
        // 🔧 Repository se encarga de las conversiones necesarias
        val otoVerifyRequest = OtoVerifyRequest(imageDocumentPath, videoData)
        
        // ✅ USAR NUEVO SISTEMA OFFLINE que guarda estados en BD
        kycOfflineViewModel.executeOtoVerify(otoVerifyRequest)
    }
    
    private fun getBestFrameAndContinue() {
        // ⚠️ IMPORTANTE: No usar currentProcessDetails.value porque puede estar desactualizado
        // El StateFlow se actualiza DESPUÉS de que el repository guarda en BD
        // Por eso llamamos directamente al ViewModel para forzar recarga desde BD

        if (imagePath != null) {
            android.util.Log.d("SuccessDocumentActivity", "📱 Executing OtoVerify - bestFrame will be obtained from DB by repository")
            android.util.Log.d("SuccessDocumentActivity", "imagePath: '$imagePath'")

            // ✅ Enviar con bestFrame vacío - el repository se encargará de obtenerlo desde BD
            // Si liveness está en BD con bestFrame, OtoVerify será online (SYNCED)
            // Si no hay bestFrame en BD, OtoVerify será offline (PENDING)
            sendOneToOne(imagePath!!, "")
        } else {
            android.util.Log.e("SuccessDocumentActivity", "❌ imagePath is null - cannot execute OtoVerify")
            // Mostrar error y regresar
            Toast.makeText(this@SuccessDocumentActivity, "Error: No se pudo obtener la imagen del documento", Toast.LENGTH_LONG).show()
            val intent = Intent(this@SuccessDocumentActivity, MenuMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}