package com.jaak.kyc.ui.view

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jaak.kyc.databinding.ActivityKycWebviewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KycWebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKycWebviewBinding
    private var webViewUrl: String? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var permissionRequest: PermissionRequest? = null

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FLOW_TYPE = "extra_flow_type"
        private const val TAG = "KycWebViewActivity"
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionRequest?.grant(permissionRequest?.resources)
        } else {
            permissionRequest?.deny()
        }
        permissionRequest = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKycWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        webViewUrl = intent.getStringExtra(EXTRA_URL)
        val flowType = intent.getStringExtra(EXTRA_FLOW_TYPE) ?: "RIGEL"

        setupToolbar(flowType)
        setupWebView()

        webViewUrl?.let { url ->
            Log.d(TAG, "Cargando URL: $url")
            binding.webView.loadUrl(url)
        } ?: run {
            Log.e(TAG, "URL no proporcionada")
            finish()
        }
    }

    private fun checkAndRequestCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            false
        } else {
            true
        }
    }

    private fun setupToolbar(flowType: String) {
        val title = when (flowType) {
            "RIGEL" -> "Rigel KYC"
            "MOSAIC" -> "KYC Mosaic"
            else -> "KYC"
        }
        supportActionBar?.title = title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                
                // Permisos para cámara y micrófono
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = true
                allowFileAccess = true
                allowContentAccess = true
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Página cargada: $url")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                    Log.d(TAG, "onPermissionRequest: ${request?.resources?.joinToString()}")
                    
                    request?.let {
                        val requestedResources = request.resources
                        val shouldGrantCameraPermission = requestedResources.any { resource ->
                            resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        }

                        if (shouldGrantCameraPermission) {
                            if (checkAndRequestCameraPermission()) {
                                Log.d(TAG, "Concediendo permisos: ${requestedResources.joinToString()}")
                                request.grant(requestedResources)
                            } else {
                                permissionRequest = request
                            }
                        } else {
                            Log.d(TAG, "Concediendo permisos sin cámara: ${requestedResources.joinToString()}")
                            request.grant(requestedResources)
                        }
                    }
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@KycWebViewActivity.filePathCallback?.onReceiveValue(null)
                    this@KycWebViewActivity.filePathCallback = filePathCallback
                    return true
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
