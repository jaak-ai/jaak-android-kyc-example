package ai.jaak.kyc.ui.view

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
import ai.jaak.kyc.databinding.ActivityKycWebviewBinding
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

        // Verificar versión mínima del WebView (requiere Chrome 85+ para JS moderno)
        val webViewPackage = android.webkit.WebView.getCurrentWebViewPackage()
        val versionName = webViewPackage?.versionName ?: "0"
        val majorVersion = versionName.split(".").firstOrNull()?.toIntOrNull() ?: 0

        Log.d(TAG, "WebView version: $versionName (major: $majorVersion)")

        if (majorVersion < 85) {
            showWebViewUpdateDialog(majorVersion)
            return
        }

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

                // Configuraciones adicionales para compatibilidad
                databaseEnabled = true
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                // User Agent moderno (simular Chrome actualizado)
                val defaultUserAgent = userAgentString
                userAgentString = defaultUserAgent.replace("; wv", "")

                Log.d(TAG, "WebView User-Agent: $userAgentString")
            }

            // Log versión del WebView
            Log.d(TAG, "WebView Package: ${android.webkit.WebView.getCurrentWebViewPackage()?.packageName}")
            Log.d(TAG, "WebView Version: ${android.webkit.WebView.getCurrentWebViewPackage()?.versionName}")

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    Log.d(TAG, "shouldOverrideUrlLoading: ${request?.url}")
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d(TAG, "onPageStarted: $url")
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "onPageFinished: $url")
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Log.e(TAG, "onReceivedError: ${request?.url} - ${error?.description} (code: ${error?.errorCode})")
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: android.webkit.WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    Log.e(TAG, "onReceivedHttpError: ${request?.url} - ${errorResponse?.statusCode} ${errorResponse?.reasonPhrase}")
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: android.webkit.SslErrorHandler?,
                    error: android.net.http.SslError?
                ) {
                    Log.e(TAG, "onReceivedSslError: ${error?.url} - ${error?.primaryError}")
                    // En desarrollo, podrías usar handler?.proceed() pero NO en producción
                    super.onReceivedSslError(view, handler, error)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                    Log.d(TAG, "Console: ${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                    return super.onConsoleMessage(consoleMessage)
                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    Log.d(TAG, "Loading progress: $newProgress%")
                }

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

    private fun showWebViewUpdateDialog(currentVersion: Int) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Navegador desactualizado")
            .setMessage("Tu navegador integrado (versión $currentVersion) es muy antiguo y no es compatible con esta funcionalidad.\n\nNecesitas Chrome 85 o superior.\n\n¿Deseas abrir en Chrome externo?")
            .setPositiveButton("Abrir en Chrome") { _, _ ->
                openInExternalBrowser()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun openInExternalBrowser() {
        webViewUrl?.let { url ->
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                // Intentar abrir con Chrome específicamente
                intent.setPackage("com.android.chrome")
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                // Si Chrome no está disponible, abrir con cualquier navegador
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    startActivity(intent)
                    finish()
                } catch (e2: Exception) {
                    Log.e(TAG, "No se pudo abrir navegador externo", e2)
                    android.widget.Toast.makeText(this, "No se encontró navegador disponible", android.widget.Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } ?: finish()
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
