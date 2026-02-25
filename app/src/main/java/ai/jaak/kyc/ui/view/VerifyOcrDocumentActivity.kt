package ai.jaak.kyc.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import ai.jaak.kyc.R
import ai.jaak.kyc.data.model.ocr.v4.DocumentExtractV4Request
import ai.jaak.kyc.data.model.verify.VerifyRequest
import ai.jaak.kyc.databinding.ActivityVerifyOcrDocumentBinding
import ai.jaak.kyc.ui.viewmodel.KycOfflineViewModel
import ai.jaak.kyc.utils.Constants
import ai.jaak.kyc.utils.Utils
import ai.jaak.kyc.utils.FileStorageUtils
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

                // 🔧 Pasar paths al ViewModel, el repository manejará la conversión a base64
                // ✅ CAMBIO: Primero ejecutar Document Extract, luego Verify
                val documentExtractV4Request = DocumentExtractV4Request(
                    imageFront = frontImagePath!!,
                    imageBack = backImagePath ?: "",
                    allowedCountries = listOf("MEX", "COL")
                )

                // ✅ USAR NUEVO SISTEMA OFFLINE que guarda estados en BD
                kycOfflineViewModel.executeOcr(documentExtractV4Request)
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
                // Determinar el tipo de error para mostrar mensaje específico
                val errorType = when {
                    it.message?.contains("Verify", ignoreCase = true) == true -> {
                        ErrorProcessActivity.ERROR_TYPE_VERIFY_FAILED
                    }
                    it.message?.contains("OCR", ignoreCase = true) == true -> {
                        ErrorProcessActivity.ERROR_TYPE_OCR_FAILED
                    }
                    it.message?.contains("document", ignoreCase = true) == true -> {
                        ErrorProcessActivity.ERROR_TYPE_DOCUMENT_INVALID
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
                when {
                    it.contains("OCR processing completed") -> {
                        // ✅ CAMBIO: OCR exitoso → ejecutar Verify
                        binding.clError.visibility = View.GONE
                        updateProcessingStatus(getString(R.string.processing_status_verifying))

                        // 🔧 Pasar paths al ViewModel, el repository manejará la conversión a base64
                        if (frontImagePath != null) {
                            val verifyRequest = VerifyRequest(frontImagePath!!, backImagePath, false)
                            kycOfflineViewModel.executeVerify(verifyRequest)
                        } else {
                            Toast.makeText(this, "Failed to get image paths", Toast.LENGTH_SHORT).show()
                            binding.clError.visibility = View.VISIBLE
                            binding.tvDescription.text = "Failed to get image paths for Verify processing"
                        }
                    }
                    it.contains("Document verification completed") -> {
                        // ✅ CAMBIO: Verify exitoso → navegar a instrucciones de verificación facial
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