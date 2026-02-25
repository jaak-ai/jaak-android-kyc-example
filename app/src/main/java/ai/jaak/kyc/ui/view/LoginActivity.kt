package ai.jaak.kyc.ui.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import ai.jaak.kyc.R
import ai.jaak.kyc.data.model.api.LoginRequest
import ai.jaak.kyc.data.network.JaakDBApiClient
import ai.jaak.kyc.data.network.JaakDBService
import ai.jaak.kyc.databinding.ActivityLoginBinding
import ai.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var captchaToken: String? = null

    @Inject
    lateinit var jaakDBService: JaakDBService

    @Inject
    @javax.inject.Named("AuthService")
    lateinit var authService: JaakDBApiClient

    @Inject
    lateinit var profileManager: ProfileManager

    private val recaptchaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val token = result.data?.getStringExtra(RecaptchaWebViewActivity.RESULT_TOKEN)
            if (!token.isNullOrEmpty()) {
                Log.d("LoginActivity", "✅ reCAPTCHA token recibido: ${token.take(50)}...")
                captchaToken = token
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()
                performLogin(email, password, token)
            } else {
                Log.e("LoginActivity", "❌ Token vacío de reCAPTCHA")
                Toast.makeText(this, getString(R.string.recaptcha_error), Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("LoginActivity", "❌ reCAPTCHA cancelado o falló")
            Toast.makeText(this, getString(R.string.recaptcha_error), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initComponents()
    }

    private fun initComponents() {
        binding.flRecaptcha.visibility = View.GONE

        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot password functionality - Coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.tvContactSupport.setOnClickListener {
            Toast.makeText(this, "Contact support functionality - Coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun attemptLogin() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

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

        if (hasError) return

        // Si ya tenemos token válido, proceder directo; si no, mostrar reCAPTCHA
        val existingToken = captchaToken
        if (existingToken != null) {
            performLogin(email, password, existingToken)
        } else {
            Toast.makeText(this, getString(R.string.recaptcha_verifying), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, RecaptchaWebViewActivity::class.java)
            recaptchaLauncher.launch(intent)
        }
    }

    private fun performLogin(email: String, password: String, recaptchaToken: String?) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = getString(R.string.processing)

        lifecycleScope.launch {
            try {
                val loginRequest = LoginRequest(
                    email = email,
                    password = password,
                    captcha = recaptchaToken ?: ""
                )

                Log.d("LoginActivity", "Enviando login - Email: $email, Profile: ${profileManager.getCurrentProfile()}")

                val response = authService.loginApi(loginRequest)

                Log.d("LoginActivity", "Response code: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    Log.d("LoginActivity", "✅ Login exitoso - User: ${loginResponse.user.fullName}")

                    profileManager.saveAccessToken(loginResponse.accessToken)
                    profileManager.saveUserInfo(loginResponse.user)
                    profileManager.saveCompanyInfo(loginResponse.company)
                    profileManager.setLoggedIn(true)
                    captchaToken = null

                    Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    navigateToMainMenu()
                } else {
                    captchaToken = null
                    val errorBody = response.errorBody()?.string()
                    Log.e("LoginActivity", "❌ Error en login: ${response.code()} - $errorBody")

                    val errorMessage = when (response.code()) {
                        401 -> "Credenciales inválidas"
                        403 -> "Acceso denegado"
                        else -> getString(R.string.login_error_credentials)
                    }
                    showLoginError(errorMessage)
                }
            } catch (e: Exception) {
                captchaToken = null
                Log.e("LoginActivity", "Excepción en login: ${e.message}", e)
                showLoginError(getString(R.string.login_error_network))
            }
        }
    }

    private fun navigateToMainMenu() {
        val intent = Intent(this, ai.jaak.kyc.MainActivity::class.java)
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
