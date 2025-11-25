package com.jaak.kyc.ui.view

import android.os.Bundle
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.jaak.kyc.databinding.ActivityKycWebviewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KycWebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKycWebviewBinding
    private var webViewUrl: String? = null

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FLOW_TYPE = "extra_flow_type"
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
            Log.d("KycWebViewActivity", "Cargando URL: $url")
            binding.webView.loadUrl(url)
        } ?: run {
            Log.e("KycWebViewActivity", "URL no proporcionada")
            finish()
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
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    // Permitir navegación dentro del WebView
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("KycWebViewActivity", "Página cargada: $url")
                }
            }

            webChromeClient = WebChromeClient()
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
