package com.jaak.kyc.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaak.kyc.databinding.ActivityInitProcessLivenessBinding
import com.jaak.kyc.utils.Constants
import com.jaak.documentdetectorsdk.sdk.DocumentDetectorSDK
import com.jaak.documentdetectorsdk.ui.adapter.DocumentDetectorListener
import dagger.hilt.android.AndroidEntryPoint

/**
 * Clase principal que contiene la lógica de los fragments.
 * Esta actividad es la pantalla principal de la aplicación y gestiona la navegación y
 * la interacción del usuario con los fragmentos.
 */
@AndroidEntryPoint
class InitProcessLivenessActivity : AppCompatActivity(), DocumentDetectorListener {

    private lateinit var binding: ActivityInitProcessLivenessBinding
    private lateinit var documentDetectorSDK: DocumentDetectorSDK


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInitProcessLivenessBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initComponents()
        configFaceDetectorSDK()
    }

    private fun initComponents(){
        documentDetectorSDK = DocumentDetectorSDK(this, this)
        configFaceDetectorSDK()
        binding.tvBtnStart.setOnClickListener{
            documentDetectorSDK.startDocumentDetector(1)
        }

    }

    private fun configFaceDetectorSDK(){
        documentDetectorSDK = DocumentDetectorSDK(this, this)
        documentDetectorSDK.setEnableCamera(true)
        documentDetectorSDK.setEnableDisk(true)
        documentDetectorSDK.setEnableCameraPhoto(true)
        documentDetectorSDK.setEnableDiskPhoto(true)
        documentDetectorSDK.setImageFormat("image/*")
        documentDetectorSDK.setImageSize(3)
        documentDetectorSDK.setLicence("")
    }

    override fun onSuccessDocumentDetector(typeProcess: Int, uri: Uri?, uri2: Uri?) {
        val resultIntent = Intent(this, VerifyOcrDocumentActivity::class.java)
        resultIntent.putExtra(Constants.URI_DOCUMENT_V, uri)
        resultIntent.putExtra(Constants.URI_DOCUMENT_2_V, uri2)
        resultIntent.putExtra(Constants.TYPE_PROCCESS_BASE64_V, typeProcess)
        startActivity(resultIntent)
    }

    override fun onErrorDocumentDetector(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}