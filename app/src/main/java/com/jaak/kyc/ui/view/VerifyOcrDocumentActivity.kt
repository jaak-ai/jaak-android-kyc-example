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
import com.jaak.kyc.R
import com.jaak.kyc.data.model.ocr.DocumentExtraBothRequest
import com.jaak.kyc.data.model.verify.VerifyRequest
import com.jaak.kyc.databinding.ActivityVerifyOcrDocumentBinding
import com.jaak.kyc.ui.viewmodel.KycOfflineViewModel
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.Utils
import com.jaak.kyc.utils.FileStorageUtils
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class VerifyOcrDocumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerifyOcrDocumentBinding
    private val kycOfflineViewModel: KycOfflineViewModel by viewModels()
    private var frontImagePath: String? = null
    private var backImagePath: String? = null
    private var uriDocumentFront : Uri? = null
    private var uriDocumentBack : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOcrDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewModel()
        initComponents()
        initFacedetector()
        binding.tvBtnFinish.setOnClickListener{
            finish()
        }
    }

    private fun initComponents(){
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
        
        // Configure restart button
        binding.btnRestart.setOnClickListener {
            // Navigate back to InitProcessLivenessActivity to restart the flow
            val intent = Intent(this, InitProcessLivenessActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        
        // 🔧 COMPATIBILIDAD: Intentar primero String (ruta permanente), luego Uri (temporal)
        val frontPath = intent.getStringExtra(Constants.URI_DOCUMENT_V)
        val backPath = intent.getStringExtra(Constants.URI_DOCUMENT_2_V)
        
        uriDocumentFront = if (frontPath != null) {
            // Ya es una ruta permanente, convertir a Uri para compatibilidad
            Uri.fromFile(java.io.File(frontPath))
        } else {
            // Fallback: usar el método antiguo con URIs temporales
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Constants.URI_DOCUMENT_V, Uri::class.java)
            } else {
                intent.getParcelableExtra(Constants.URI_DOCUMENT_V) as? Uri
            }
        }
        
        uriDocumentBack = if (backPath != null) {
            // Ya es una ruta permanente, convertir a Uri para compatibilidad
            Uri.fromFile(java.io.File(backPath))
        } else {
            // Fallback: usar el método antiguo con URIs temporales
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Constants.URI_DOCUMENT_2_V, Uri::class.java)
            } else {
                intent.getParcelableExtra(Constants.URI_DOCUMENT_2_V) as? Uri
            }
        }
        val typeProcess = intent.extras?.getInt(Constants.TYPE_PROCCESS_BASE64_V, 0)!!

        proccessBase64(typeProcess, uriDocumentFront!!)
    }

    private fun proccessBase64(typeProcessBase64 : Int,uri : Uri){
        when (typeProcessBase64) {
            1 -> {
                // 🔧 NUEVA LÓGICA: Guardar URI como archivo permanente
                frontImagePath = FileStorageUtils.saveUriToPermanentFile(this, uri, "document_front_${System.currentTimeMillis()}.jpg")
                if (frontImagePath == null) {
                    Toast.makeText(this, getString(R.string.error_saving_image), Toast.LENGTH_SHORT).show()
                    return
                }
                
                backImagePath = uriDocumentBack?.let { backUri ->
                    FileStorageUtils.saveUriToPermanentFile(this, backUri, "document_back_${System.currentTimeMillis()}.jpg")
                }
                
                val verifyRequest = VerifyRequest(frontImagePath!!, backImagePath, false)
                
                // ✅ USAR NUEVO SISTEMA OFFLINE que guarda estados en BD
                kycOfflineViewModel.executeVerify(verifyRequest)
            }
            else -> {
                Toast.makeText(this,getString(R.string.error_base64_empty), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViewModel(){
        // ✅ NUEVO SISTEMA OFFLINE - Observadores
        kycOfflineViewModel.isLoading.observe(this) { isLoading ->
            if(isLoading){
                binding.clProgress.visibility = View.VISIBLE
                binding.clError.visibility = View.GONE
            }else{
                binding.clProgress.visibility = View.GONE
            }
        }
        
        kycOfflineViewModel.errorModel.observe(this) { error ->
            error?.let {
                binding.clError.visibility = View.VISIBLE
                binding.clProgress.visibility = View.GONE
                binding.tvDescription.text = it.message
                kycOfflineViewModel.clearMessages()
            }
        }
        
        kycOfflineViewModel.successMessage.observe(this) { message ->
            message?.let { 
                when {
                    it.contains("Document verification completed") -> {
                        // ✅ Verify exitoso → ejecutar OCR
                        binding.clError.visibility = View.GONE
                        updateProcessingStatus(getString(R.string.processing_status_validating))
                        
                        // 🔧 Repository se encarga de la conversión a base64
                        if (frontImagePath != null) {
                            val documentExtraBothRequest = DocumentExtraBothRequest(frontImagePath!!, backImagePath ?: "")
                            kycOfflineViewModel.executeOcr(documentExtraBothRequest)
                        } else {
                            Toast.makeText(this, "Failed to get image paths", Toast.LENGTH_SHORT).show()
                            binding.clError.visibility = View.VISIBLE
                            binding.tvDescription.text = "Failed to get image paths for OCR processing"
                        }
                    }
                    it.contains("OCR processing completed") -> {
                        // ✅ OCR exitoso → navegar a instrucciones de verificación facial
                        updateProcessingStatus(getString(R.string.processing_status_completing))
                        // Hide processing screen and navigate to facial instructions
                        binding.clProgress.visibility = View.GONE
                        navigateToFacialInstructions()
                    }
                }
                kycOfflineViewModel.clearMessages()
            }
        }
        
    }
    
    private fun updateProcessingStatus(statusMessage: String) {
        binding.tvProcessStatus.text = statusMessage
    }
    
    private fun navigateToFacialInstructions() {
        val intent = Intent(this, FacialVerificationInstructionsActivity::class.java)
        // Pasar la ruta de la imagen frontal para uso posterior
        intent.putExtra("frontImagePath", frontImagePath)
        startActivity(intent)
        finish()
    }

    private fun initFacedetector(){
        // Ya no necesitamos inicializar VisageSDK aquí
        // Se hace en FacialVerificationInstructionsActivity
    }

}