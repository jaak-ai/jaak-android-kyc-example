package ai.jaak.kyc.ui.view

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import ai.jaak.kyc.databinding.ActivityRecaptchaWebviewBinding

class RecaptchaWebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecaptchaWebviewBinding

    companion object {
        const val RESULT_TOKEN = "recaptcha_token"
        const val SITE_KEY = "6Ld2pIYfAAAAAGhR-TBQXiqwMbaHecs_zSqDXu-2"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecaptchaWebviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
            }
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            addJavascriptInterface(RecaptchaJsInterface(), "AndroidBridge")
            loadDataWithBaseURL(
                "https://www.google.com",
                generateRecaptchaHtml(SITE_KEY),
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    private fun generateRecaptchaHtml(siteKey: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <script src="https://www.google.com/recaptcha/api.js?onload=onloadCallback&render=explicit" async defer></script>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        background-color: #F5F5F5;
                        font-family: sans-serif;
                    }
                    .container {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        gap: 16px;
                        padding: 24px;
                    }
                    .title {
                        font-size: 16px;
                        color: #333;
                        text-align: center;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <p class="title">Verifica que no eres un robot</p>
                    <div id="recaptcha-container"></div>
                </div>
                <script>
                    function onloadCallback() {
                        grecaptcha.render('recaptcha-container', {
                            'sitekey': '$siteKey',
                            'callback': onRecaptchaSuccess,
                            'error-callback': onRecaptchaError,
                            'expired-callback': onRecaptchaExpired,
                            'theme': 'light'
                        });
                    }
                    function onRecaptchaSuccess(token) {
                        AndroidBridge.onSuccess(token);
                    }
                    function onRecaptchaError() {
                        AndroidBridge.onError('reCAPTCHA verification failed');
                    }
                    function onRecaptchaExpired() {
                        AndroidBridge.onError('reCAPTCHA verification expired');
                    }
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    inner class RecaptchaJsInterface {
        @JavascriptInterface
        fun onSuccess(token: String) {
            runOnUiThread {
                val result = Intent()
                result.putExtra(RESULT_TOKEN, token)
                setResult(Activity.RESULT_OK, result)
                finish()
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            runOnUiThread {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }
}
