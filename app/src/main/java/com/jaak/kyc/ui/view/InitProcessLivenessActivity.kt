package com.jaak.kyc.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaak.stampssdk.ui.adapter.StampsListener
import com.jaak.kyc.databinding.ActivityInitProcessLivenessBinding
import com.jaak.kyc.utils.Constants
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
            stampsSDK.startStamps(1)
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
        backOriginalUri: Uri?, backCropUri: Uri?) {
        val resultIntent = Intent(this, VerifyOcrDocumentActivity::class.java)
        resultIntent.putExtra(Constants.URI_DOCUMENT_V, frontCropUri)
        resultIntent.putExtra(Constants.URI_DOCUMENT_2_V, backCropUri)
        resultIntent.putExtra(Constants.TYPE_PROCCESS_BASE64_V, typeProcess)
        startActivity(resultIntent)
    }
}