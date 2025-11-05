package com.jaak.kyc.ui.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaAction
import com.google.android.recaptcha.RecaptchaClient
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
    private var recaptchaClient: RecaptchaClient? = null

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
        // Ocultar el contenedor de reCAPTCHA (no necesitamos UI visible)
        binding.flRecaptcha.visibility = View.GONE

        Log.d("LoginActivity", "Inicializando reCAPTCHA Enterprise...")

        // Inicializar reCAPTCHA Enterprise de forma asíncrona
        lifecycleScope.launch {
            try {
                val siteKey = getString(R.string.recaptcha_site_key)
                val result = Recaptcha.getClient(application, siteKey)

                result.onSuccess { client ->
                    recaptchaClient = client
                    Log.d("LoginActivity", "✅ reCAPTCHA Enterprise inicializado correctamente")
                }.onFailure { exception ->
                    Log.e("LoginActivity", "❌ Error inicializando reCAPTCHA: ${exception.message}", exception)
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "❌ Error inicializando reCAPTCHA: ${e.message}", e)
            }
        }
    }

    private fun executeRecaptcha(onSuccess: (String) -> Unit, onError: () -> Unit) {
        Log.d("LoginActivity", "Ejecutando reCAPTCHA Enterprise...")

        lifecycleScope.launch {
            try {
                val client = recaptchaClient
                if (client == null) {
                    Log.e("LoginActivity", "❌ reCAPTCHA client no inicializado")
                    Toast.makeText(this@LoginActivity, getString(R.string.recaptcha_error), Toast.LENGTH_SHORT).show()
                    onError()
                    return@launch
                }

                // Ejecutar reCAPTCHA con acción LOGIN
                val result = client.execute(RecaptchaAction.LOGIN)

                result.onSuccess { token ->
                    Log.d("LoginActivity", "✅ reCAPTCHA token obtenido: ${token.toString().substring(0, minOf(50, token.toString().length))}...")
                    recaptchaToken = token.toString()
                    onSuccess(token.toString())
                }.onFailure { exception ->
                    Log.e("LoginActivity", "❌ Error en reCAPTCHA: ${exception.message}", exception)
                    Toast.makeText(this@LoginActivity, getString(R.string.recaptcha_error), Toast.LENGTH_SHORT).show()
                    onError()
                }

            } catch (e: Exception) {
                Log.e("LoginActivity", "❌ Error en reCAPTCHA: ${e.message}", e)
                Toast.makeText(this@LoginActivity, getString(R.string.recaptcha_error), Toast.LENGTH_SHORT).show()
                onError()
            }
        }
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

        if (hasError) {
            return
        }

        // ⭐ Ejecutar reCAPTCHA antes de hacer login
        Toast.makeText(this, getString(R.string.recaptcha_verifying), Toast.LENGTH_SHORT).show()

        executeRecaptcha(
            onSuccess = { token ->
                // reCAPTCHA exitoso, proceder con login
                performLogin(email, password, token)
            },
            onError = {
                // Error en reCAPTCHA, no continuar
                Log.e("LoginActivity", "No se pudo completar reCAPTCHA")
            }
        )
    }

    private fun performLogin(email: String, password: String, recaptchaToken: String?) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = getString(R.string.processing)

        lifecycleScope.launch {
            try {
                // ⚠️ BYPASS TEMPORAL - Login automático mientras backend está caído (502)
                kotlinx.coroutines.delay(800)

                Log.d("LoginActivity", "🔧 Login bypass activo - servidor backend tiene error 502")
                Log.d("LoginActivity", "Email ingresado: $email")
                Log.d("LoginActivity", "reCAPTCHA token obtenido: ${recaptchaToken?.take(50)}...")

                // API Key temporal proporcionada por el equipo de backend
                // Actualizado: 2025-11-05
                val temporaryApiKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55IjoiNjczNjQwNmE4ZTY3MDFkYWZlZDg0NDFhIiwiY29tcGFueV9pZCI6IjY3MzY0MDZhOGU2NzAxZGFmZWQ4NDQxYSIsImV4cCI6MTc2Mjk2MTY4OCwiaWF0IjoxNzYyMzU2ODg4LCJyb2wiOiIiLCJzZXNzaW9uX2lkIjoiMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwIiwic3ViIjoiNjkwYjZlOTg2MjUzZDI1YzcxOWQ2ODg0IiwidHlwZSI6ImFwaS1rZXkiLCJ1dWlkIjoiZjg4MjNlYmQtMWZhNC00MzI3LWI3ZGMtYjIwZDZiMmU2NDA4In0.EMLfh9MLr0vaWmsPcg9J5soiGs9DUUTZ5NSoZetrPhg"

                // Guardar API Key de larga duración
                profileManager.saveApiKey(temporaryApiKey)
                profileManager.setLoggedIn(true)

                Log.d("LoginActivity", "✅ Bypass completado - API Key temporal guardada")

                Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()

                // Navegar al dashboard principal
                navigateToMainMenu()

                /* TODO: Activar cuando backend esté funcionando (actualmente error 502)
                // Crear request
                val loginRequest = LoginRequest(
                    email = email,
                    password = password,
                    captcha = recaptchaToken ?: ""
                )

                Log.d("LoginActivity", "Enviando login - Email: $email")
                Log.d("LoginActivity", "reCAPTCHA token: ${recaptchaToken?.take(50)}...")

                val response = authService.loginApi(loginRequest)

                Log.d("LoginActivity", "Response code: ${response.code()}")
                Log.d("LoginActivity", "Response message: ${response.message()}")

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // Imprimir respuesta completa en formato JSON
                    val gson = com.google.gson.Gson()
                    val jsonResponse = gson.toJson(loginResponse)
                    Log.d("LoginActivity", "✅ Login exitoso! Respuesta completa:")
                    Log.d("LoginActivity", jsonResponse)

                    Log.d("LoginActivity", "═══════════════════════════════════════")
                    Log.d("LoginActivity", "User: ${loginResponse.user.fullName}")
                    Log.d("LoginActivity", "Email: ${loginResponse.user.email}")
                    Log.d("LoginActivity", "Company: ${loginResponse.company.name}")
                    Log.d("LoginActivity", "Access Token: ${loginResponse.accessToken.take(50)}...")
                    Log.d("LoginActivity", "═══════════════════════════════════════")

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
                    Log.e("LoginActivity", "❌ Error en login: ${response.code()} - ${response.message()}")
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
