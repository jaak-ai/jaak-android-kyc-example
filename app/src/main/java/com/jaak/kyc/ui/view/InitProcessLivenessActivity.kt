package com.jaak.kyc.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaak.stampssdk.ui.adapter.StampsListener
import com.jaak.kyc.databinding.ActivityInitProcessLivenessBinding
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.FileStorageUtils
import com.jaak.stampssdk.sdk.StampsSDK
import dagger.hilt.android.AndroidEntryPoint

/**
 * Clase principal que contiene la lógica de los fragments.
 * Esta actividad es la pantalla principal de la aplicación y gestiona la navegación y
 * la interacción del usuario con los fragmentos.
 */
@AndroidEntryPoint
class InitProcessLivenessActivity : AppCompatActivity(), StampsListener {

    private lateinit var binding: ActivityInitProcessLivenessBinding
    private lateinit var stampsSDK: StampsSDK


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInitProcessLivenessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initComponents()
    }

    private fun initComponents(){
        stampsSDK = StampsSDK(this, this)
        binding.tvBtnStart.setOnClickListener{
            // Navegar a la pantalla de permisos de seguridad
            val intent = Intent(this, SecurityPermissionsActivity::class.java)
            startActivity(intent)
        }

        binding.btnRestart.setOnClickListener {
            // Navigate back to MenuMainActivity to start the process over
            val intent = Intent(this, MenuMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

    }
    /**
     * Callback to handle success when selecting a video.
     */
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

        // 🔧 Enviar rutas de archivos permanentes (String) en lugar de URIs temporales
        val resultIntent = Intent(this, VerifyOcrDocumentActivity::class.java)
        resultIntent.putExtra(Constants.URI_DOCUMENT_V, frontImagePath)    // String path
        resultIntent.putExtra(Constants.URI_DOCUMENT_2_V, backImagePath)   // String path (puede ser null)
        resultIntent.putExtra(Constants.TYPE_PROCCESS_BASE64_V, typeProcess)
        startActivity(resultIntent)
    }
}