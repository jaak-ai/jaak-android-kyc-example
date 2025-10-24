package com.jaak.kyc.ui.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jaak.kyc.R
import com.jaak.kyc.data.model.api.LoginRequest
import com.jaak.kyc.data.network.JaakDBApiClient
import com.jaak.kyc.data.network.JaakDBService
import com.jaak.kyc.databinding.ActivityLoginBinding
import com.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var recaptchaToken: String? = null

    @Inject
    lateinit var jaakDBService: JaakDBService

    @Inject
    @javax.inject.Named("AuthService")
    lateinit var authService: JaakDBApiClient

    @Inject
    lateinit var profileManager: ProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()
        setupRecaptcha()
    }

    private fun initComponents() {
        // Botón de login
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        // Forgot password link
        binding.tvForgotPassword.setOnClickListener {
            // TODO: Implementar funcionalidad de recuperación de contraseña
            Toast.makeText(this, "Forgot password functionality - Coming soon", Toast.LENGTH_SHORT).show()
        }

        // Contact support link
        binding.tvContactSupport.setOnClickListener {
            // TODO: Implementar funcionalidad de soporte
            Toast.makeText(this, "Contact support functionality - Coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecaptcha() {
        // TODO: Implementar reCAPTCHA Android con SafetyNet API
        // El reCAPTCHA v2 web no funciona en Android nativo porque requiere un dominio web válido
        //
        // Para implementar reCAPTCHA en Android:
        // 1. Configurar reCAPTCHA Android en Google Cloud Console
        // 2. Agregar dependencia: implementation 'com.google.android.gms:play-services-safetynet:18.0.1'
        // 3. Usar SafetyNet Attestation API
        //
        // Documentación: https://developer.android.com/training/safetynet/recaptcha

        // Por ahora, ocultar reCAPTCHA y NO enviar token
        binding.flRecaptcha.visibility = View.GONE

        // NO enviar token hasta que se configure reCAPTCHA Android
        recaptchaToken = null

        Log.d("LoginActivity", "reCAPTCHA Android no configurado - enviando sin token")
    }

    private fun attemptLogin() {
        // Resetear errores
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        // Obtener valores
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validar campos
        var hasError = false

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.login_error_empty_email)
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.login_error_invalid_email)
            hasError = true
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.login_error_empty_password)
            hasError = true
        }

        // Validar reCAPTCHA (solo si está visible)
        // Por ahora está deshabilitado hasta configurar reCAPTCHA Android
        // if (recaptchaToken == null && binding.flRecaptcha.visibility == View.VISIBLE) {
        //     Toast.makeText(this, getString(R.string.login_error_recaptcha), Toast.LENGTH_SHORT).show()
        //     hasError = true
        // }

        if (hasError) {
            return
        }

        // Proceder con el login
        performLogin(email, password, recaptchaToken)
    }

    private fun performLogin(email: String, password: String, recaptchaToken: String?) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = getString(R.string.processing)

        // TODO: Reactivar login real cuando el backend y reCAPTCHA estén configurados
        // Por ahora usar API Key temporal para acceder al dashboard

        lifecycleScope.launch {
            try {
                // Simular delay de red
                kotlinx.coroutines.delay(1000)

                Log.d("LoginActivity", "Login bypass - usando API Key temporal")

                // API Key temporal proporcionada por el equipo de backend
                val temporaryApiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55IjoiNjczNjQwNmE4ZTY3MDFkYWZlZDg0NDFhIiwiY29tcGFueV9pZCI6IjY3MzY0MDZhOGU2NzAxZGFmZWQ4NDQxYSIsImV4cCI6MTc2MTM0NzEwNywiaWF0IjoxNzYxMjYwNzA3LCJyb2wiOiIiLCJzZXNzaW9uX2lkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwIiwic3ViIjoiNjhmYWI0YTM3ZWExNmFjM2JiZDk0ZTc4IiwidHlwZSI6ImFwaS1rZXkiLCJ1dWlkIjoiZWM3OGQ5M2QtN2MxZS00YWJkLTg1ZGUtMDFiYTc0OWU0MDA4In0.rAC6A2ZCS_RiH35eoeOC3X1smw8UYfq6PUX9CQLlMUI"

                // Guardar API Key de larga duración (para listar sesiones y crear flows)
                profileManager.saveApiKey(temporaryApiKey)
                profileManager.setLoggedIn(true)

                Log.d("LoginActivity", "API Key guardada exitosamente")

                Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()

                // Navegar al dashboard principal
                navigateToMainMenu()

                /* TODO: Descomentar cuando el login real esté listo
                // Crear request
                val loginRequest = LoginRequest(
                    email = email,
                    password = password,
                    captcha = recaptchaToken ?: ""
                )

                Log.d("LoginActivity", "Enviando login - Email: $email")

                val response = authService.loginApi(loginRequest)

                Log.d("LoginActivity", "Response code: ${response.code()}")
                Log.d("LoginActivity", "Response message: ${response.message()}")

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    Log.d("LoginActivity", "Login exitoso: ${loginResponse.user.fullName}")

                    // Guardar token y datos del usuario usando ProfileManager
                    profileManager.saveAccessToken(loginResponse.accessToken)
                    profileManager.saveUserInfo(loginResponse.user)
                    profileManager.saveCompanyInfo(loginResponse.company)
                    profileManager.setLoggedIn(true)

                    Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()

                    // Navegar al dashboard principal
                    navigateToMainMenu()
                } else {
                    // Leer el cuerpo de error
                    val errorBody = response.errorBody()?.string()
                    Log.e("LoginActivity", "Error en login: ${response.code()} - ${response.message()}")
                    Log.e("LoginActivity", "Error body: $errorBody")

                    // Mostrar mensaje más específico
                    val errorMessage = when (response.code()) {
                        403 -> "Acceso denegado. Verifica el reCAPTCHA"
                        401 -> "Credenciales inválidas"
                        else -> getString(R.string.login_error_credentials)
                    }
                    showLoginError(errorMessage)
                }
                */
            } catch (e: Exception) {
                Log.e("LoginActivity", "Excepción en login: ${e.message}", e)
                showLoginError(getString(R.string.login_error_network))
            }
        }
    }

    private fun navigateToMainMenu() {
        // Navegar al dashboard principal después del login exitoso
        val intent = Intent(this, com.jaak.kyc.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoginError(message: String) {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.text = getString(R.string.login_button)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
