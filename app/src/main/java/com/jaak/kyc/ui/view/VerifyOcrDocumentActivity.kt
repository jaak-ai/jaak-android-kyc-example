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
import com.jaak.kyc.ui.viewmodel.ValidationBase64Model
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.Utils
import com.jaak.facedetectorsdk.ui.adapter.FaceDetectorListener
import com.jaak.facedetectorsdk.ui.view.FaceDetectorSDK
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class VerifyOcrDocumentActivity : AppCompatActivity(), FaceDetectorListener {

    private lateinit var binding: ActivityVerifyOcrDocumentBinding
    private val validationBase64ViewModel: ValidationBase64Model by viewModels()
    private lateinit var imageBase64Front : String
    private lateinit var imageBase64Back : String
    private var uriDocumentFront : Uri? = null
    private var uriDocumentBack : Uri? = null

    private lateinit var faceDetectorSDK: FaceDetectorSDK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOcrDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewModel()
        initComponents()
        initFacedetector()
        validationBase64ViewModel.isLoading.value = true
        binding.tvBtnFinish.setOnClickListener{
            finish()
        }
    }

    private fun initComponents(){
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
        uriDocumentFront = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.URI_DOCUMENT_V, Uri::class.java)
        } else {
            intent.getParcelableExtra(Constants.URI_DOCUMENT_V) as? Uri
        }
        uriDocumentBack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.URI_DOCUMENT_2_V, Uri::class.java)
        } else {
            intent.getParcelableExtra(Constants.URI_DOCUMENT_2_V) as? Uri
        }
        val typeProcess = intent.extras?.getInt(Constants.TYPE_PROCCESS_BASE64_V, 0)!!

        if(uriDocumentBack == null){
            imageBase64Back = ""
        }else{
            imageBase64Back = Utils.uriToBase64(contentResolver, uriDocumentBack!!)!!

        }
        proccessBase64(typeProcess, uriDocumentFront!!)
    }

    private fun proccessBase64(typeProcessBase64 : Int,uri : Uri){
        when (typeProcessBase64) {
            1 -> {
                imageBase64Front = Utils.uriToBase64(contentResolver, uri)!!
            }
            else -> {
                Toast.makeText(this,getString(R.string.error_base64_empty), Toast.LENGTH_SHORT).show()
            }
        }
        val verifyRequest = VerifyRequest(imageBase64Front,imageBase64Back,false)
        validationBase64ViewModel.verify(verifyRequest)
    }

    private fun initViewModel(){
        validationBase64ViewModel.verifyResponse.observe(this){
            if(it.document.evaluation.isNullOrEmpty()){
                binding.clError.visibility = View.VISIBLE
                validationBase64ViewModel.isLoading.value = false
                binding.tvDescription.text = it.state.message
            }else{
                if(it.document.evaluation.equals("SUCCESS")){
                    binding.clError.visibility = View.GONE
                    val documentExtraBothRequest = DocumentExtraBothRequest(imageBase64Front,imageBase64Back)
                    validationBase64ViewModel.ocr(documentExtraBothRequest)
                }else{
                    binding.clError.visibility = View.VISIBLE
                    validationBase64ViewModel.isLoading.value = false
                }
            }
        }
        validationBase64ViewModel.documentExtraBothResponse.observe(this){
            if(it.status){
                faceDetectorSDK.startFaceDetector(2)
            }else{
                binding.clError.visibility = View.VISIBLE
            }
        }
        validationBase64ViewModel.errorModel.observe(this){
            binding.clError.visibility = View.VISIBLE
            binding.tvDescription.text = it.message
        }
        validationBase64ViewModel.isLoading.observe(this) {
            if(it){
                binding.clProgress.visibility = View.VISIBLE
            }else{
                binding.clProgress.visibility = View.GONE
            }
        }
    }

    private fun initFacedetector(){
        faceDetectorSDK = FaceDetectorSDK(this, this)
        configFaceDetectorSDK()

    }
    private fun configFaceDetectorSDK(){
        faceDetectorSDK = FaceDetectorSDK(this, this)
        faceDetectorSDK.setEnableCamera(true)
        faceDetectorSDK.setEnableDisk(true)
        faceDetectorSDK.setEnableCameraPhoto(true)
        faceDetectorSDK.setEnableDiskPhoto(true)
        faceDetectorSDK.setImageFormat("image/*")
        faceDetectorSDK.setImageSize(3)

    }

    override fun onErrorFaceDetector(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        binding.clError.visibility = View.VISIBLE
    }

    override fun onSuccessFaceDetector(typeProcess: Int, uri: Uri?) {
        val resultIntent = Intent(this, SuccessDocumentActivity::class.java)
        resultIntent.putExtra(Constants.URI_VIDEO_V, uri)
        resultIntent.putExtra(Constants.URI_DOCUMENT_D, uriDocumentFront)
        startActivity(resultIntent)
        finish()
    }

}