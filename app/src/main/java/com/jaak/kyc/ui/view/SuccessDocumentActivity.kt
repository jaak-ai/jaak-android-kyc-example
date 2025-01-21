package com.jaak.kyc.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.jaak.kyc.R
import com.jaak.kyc.data.model.livenessverify.LivenessVerifyRequest
import com.jaak.kyc.data.model.otoverify.OtoVerifyRequest
import com.jaak.kyc.databinding.ActivitySuccessDocumentBinding
import com.jaak.kyc.ui.viewmodel.DocumentBase64Model
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.Utils
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SuccessDocumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuccessDocumentBinding
    private val documentBase64Model: DocumentBase64Model by viewModels()
    private lateinit var imageBase64 : String
    private lateinit var videoBase64 : String
    private var uriImage : Uri? = null
    private var uriVideo : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuccessDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewModel()
        initComponents()
        documentBase64Model.isLoading.value = true
    }

    private fun initComponents(){
        binding.tvBtnFinish.setOnClickListener{
            val intent = Intent(this, MenuMainActivity::class.java)
            startActivity(intent)
            finish()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
        uriImage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.URI_DOCUMENT_D, Uri::class.java)
        } else {
            intent.getParcelableExtra(Constants.URI_DOCUMENT_D) as? Uri
        }
        uriVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.URI_VIDEO_V, Uri::class.java)
        } else {
            intent.getParcelableExtra(Constants.URI_VIDEO_V) as? Uri
        }
        proccessBase64(uriVideo!!)
    }

    private fun proccessBase64(uri : Uri){
        videoBase64 = Utils.videoCameraUriToBase64(uri)!!
        val livenessVerifyRequest = LivenessVerifyRequest(videoBase64)
        documentBase64Model.livenessVerify(livenessVerifyRequest)
    }

    private fun initViewModel(){
        documentBase64Model.livenessVerifyResponse.observe(this){
            if(it.score > 0.7){
                videoBase64 = it.bestFrame
                sendOneToOne(uriImage!!)
            }else{
                binding.ivContent.setImageResource(R.drawable.warning_error)
                binding.tvCongratulation.text = getString(R.string.sorry_error)
                binding.tvDescription.text = getString(R.string.error_human)
            }
        }
        documentBase64Model.errorModel.observe(this){
            binding.ivContent.setImageResource(R.drawable.warning_error)
            binding.tvCongratulation.text = getString(R.string.sorry_error)
            binding.tvDescription.text = it.message
        }
        documentBase64Model.isLoading.observe(this) {
            if(it){
                binding.clProgress.visibility = View.VISIBLE
                binding.clSuccess.visibility = View.GONE
            }else{
                binding.clProgress.visibility = View.GONE
                binding.clSuccess.visibility = View.VISIBLE
            }
        }
        documentBase64Model.otoVerifyResponse.observe(this){
            if(it.score > 80){
                documentBase64Model.finish()
             }else{
                binding.ivContent.setImageResource(R.drawable.warning_error)
                binding.tvCongratulation.text = getString(R.string.sorry_error)
                binding.tvDescription.text = getString(R.string.error_human)
            }
        }
        documentBase64Model.finishResponse.observe(this){
            binding.tvCongratulation.text = getString(R.string.congratulations)
            binding.tvDescription.text = getString(R.string.content_success_document)
        }
    }

    private fun sendOneToOne(imageDocument : Uri){
        imageBase64 = Utils.uriToBase64(contentResolver, imageDocument)!!
        val otoVerifyRequest = OtoVerifyRequest(imageBase64,videoBase64)
        documentBase64Model.otoVerify(otoVerifyRequest)
    }
}