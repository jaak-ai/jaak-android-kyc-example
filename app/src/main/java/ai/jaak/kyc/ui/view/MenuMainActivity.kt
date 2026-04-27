package ai.jaak.kyc.ui.view

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ai.jaak.kyc.R
import ai.jaak.kyc.data.network.JaakDBApiClient
import ai.jaak.kyc.data.repository.KycOfflineRepository
import ai.jaak.kyc.databinding.ActivityMenuMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Clase principal que contiene la lógica de los fragments.
 * Esta actividad es la pantalla principal de la aplicación y gestiona la navegación y
 * la interacción del usuario con los fragmentos.
 */
@AndroidEntryPoint
class MenuMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuMainBinding

    @Inject
    lateinit var profileManager: ai.jaak.kyc.utils.ProfileManager

    @Inject
    lateinit var jaakDBApiClient: JaakDBApiClient

    @Inject
    lateinit var kycOfflineRepository: KycOfflineRepository

    private var loadingDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        // Verificar si el usuario ya está logueado
        // Si está logueado, redirigir directamente al Dashboard
        checkLoginAndRedirect()

        binding = ActivityMenuMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initComponents()
    }

    private fun checkLoginAndRedirect() {
        // Verificar si viene del Dashboard (usuario ya logueado queriendo crear nuevo KYC)
        val fromDashboard = intent.getBooleanExtra("FROM_DASHBOARD", false)

        // Si viene del Dashboard, NO hacer redirect - dejar que ingrese shortkey
        if (fromDashboard) {
            return
        }

        // Solo hacer redirect si NO viene del Dashboard y está logueado
        if (profileManager.isLoggedIn()) {
            // Usuario ya está logueado, ir directamente al Dashboard
            val intent = Intent(this, ai.jaak.kyc.MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun initComponents(){
        // Inicializar botón como deshabilitado
        binding.tvBtnStart.isEnabled = false

        binding.tvBtnStart.setOnClickListener{
            startKycProcess()
        }

        // Validación de código de acceso - exactamente 7 caracteres
        binding.etShort.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                // Habilitar botón solo cuando tenga exactamente 7 caracteres
                binding.tvBtnStart.isEnabled = text.length == 7
            }
        })

        // Botón para iniciar sesión
        binding.btnLogin.setOnClickListener {
            navigateToLogin()
        }

        // Verificar si el usuario está logueado para mostrar/ocultar el botón de login
        checkUserLoginStatus()

        // Los permisos ahora se manejan en SecurityPermissionsActivity
    }

    private fun checkUserLoginStatus() {
        // Verificar si viene del Dashboard
        val fromDashboard = intent.getBooleanExtra("FROM_DASHBOARD", false)

        // Verificar si el usuario está logueado usando ProfileManager
        val isLoggedIn = profileManager.isLoggedIn()

        // Si está logueado O viene del Dashboard, ocultar el texto "¿Tienes una cuenta?" y el botón "Iniciar Sesión"
        if (isLoggedIn || fromDashboard) {
            binding.tvHaveAccount.visibility = View.GONE
            binding.btnLogin.visibility = View.GONE
        } else {
            binding.tvHaveAccount.visibility = View.VISIBLE
            binding.btnLogin.visibility = View.VISIBLE
        }
    }

    private fun startKycProcess() {
        val shortKey = binding.etShort.text.toString()
        if (shortKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.shortkey_empty), Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar que tenga exactamente 7 caracteres
        if (shortKey.length != 7) {
            Toast.makeText(this, "El código debe tener exactamente 7 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        // Iniciar flujo KYC nativo
        startNativeKycFlow(shortKey)
    }

    private fun startNativeKycFlow(shortKey: String) {
        Log.d("MenuMainActivity", "Iniciando flujo KYC nativo con shortKey: $shortKey")

        // Mostrar loading dialog
        showLoadingDialog()

        lifecycleScope.launch {
            try {
                // Crear proceso en BD con shortKey
                val processId = kycOfflineRepository.createKycProcess(shortKey)
                Log.d("MenuMainActivity", "✓ Proceso creado en BD con ID: $processId")

                // Llamar API de sesión
                Log.d("MenuMainActivity", "========== CREATE SESSION REQUEST ==========")
                Log.d("MenuMainActivity", "URL: POST /api/v1/kyc/session")
                Log.d("MenuMainActivity", "Headers: {")
                Log.d("MenuMainActivity", "  Short-Key: $shortKey")
                Log.d("MenuMainActivity", "  Origin-Device: Android")
                Log.d("MenuMainActivity", "}")
                Log.d("MenuMainActivity", "==========================================")

                val sessionResponse = jaakDBApiClient.sessionApi(
                    shortKey = shortKey,
                    originDevice = "Android"
                )

                Log.d("MenuMainActivity", "========== CREATE SESSION RESPONSE ==========")
                Log.d("MenuMainActivity", "Status Code: ${sessionResponse.code()}")
                Log.d("MenuMainActivity", "Status Message: ${sessionResponse.message()}")

                if (!sessionResponse.isSuccessful) {
                    val errorBody = sessionResponse.errorBody()?.string()
                    Log.e("MenuMainActivity", "Error Body: $errorBody")
                    Log.d("MenuMainActivity", "==========================================")
                    hideLoadingDialog()
                    Toast.makeText(
                        this@MenuMainActivity,
                        getString(R.string.error_create_session_failed, sessionResponse.message()),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val sessionData = sessionResponse.body()
                if (sessionData == null) {
                    Log.e("MenuMainActivity", "Session response body es null")
                    Log.d("MenuMainActivity", "==========================================")
                    hideLoadingDialog()
                    Toast.makeText(
                        this@MenuMainActivity,
                        getString(R.string.error_empty_session_response),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                Log.d("MenuMainActivity", "Response Body: {")
                Log.d("MenuMainActivity", "  sessionId: ${sessionData.sessionId}")
                Log.d("MenuMainActivity", "  accessToken: ${sessionData.accessToken}")
                Log.d("MenuMainActivity", "  step: ${sessionData.step}")
                Log.d("MenuMainActivity", "  document: ${sessionData.document}")
                Log.d("MenuMainActivity", "  assets: ${sessionData.assets}")
                Log.d("MenuMainActivity", "}")

                // Extraer licencia del header x-trace-id
                val xTraceId = sessionResponse.headers()["x-trace-id"]
                Log.d("MenuMainActivity", "Header x-trace-id: $xTraceId")
                Log.d("MenuMainActivity", "==========================================")

                // Guardar licencia (x-trace-id con "L" al inicio)
                if (!xTraceId.isNullOrEmpty()) {
                    val license = "L$xTraceId"
                    ai.jaak.kyc.utils.FlowLicenseManager.saveLicense(this@MenuMainActivity, license)
                    Log.d("MenuMainActivity", "✓ Licencia guardada: $license")
                } else {
                    Log.w("MenuMainActivity", "⚠ No se encontró header x-trace-id en la respuesta")
                }

                // Guardar token en el proceso de BD
                kycOfflineRepository.storeTokenByShortKey(shortKey, sessionData.accessToken, null)
                Log.d("MenuMainActivity", "✓ Token guardado en proceso BD")

                // Guardar token de sesión KYC (separado del access token del usuario)
                profileManager.saveKycSessionToken(sessionData.accessToken)

                Log.d("MenuMainActivity", "✓ Sesión creada exitosamente. Navegando a InitProcessLivenessActivity...")

                // Ocultar loading dialog
                hideLoadingDialog()

                // Navegar a InitProcessLivenessActivity (flujo nativo)
                val intent = Intent(this@MenuMainActivity, InitProcessLivenessActivity::class.java)
                startActivity(intent)

            } catch (e: Exception) {
                Log.e("MenuMainActivity", "Excepción al crear sesión KYC: ${e.message}", e)
                hideLoadingDialog()
                Toast.makeText(
                    this@MenuMainActivity,
                    getString(R.string.error_network, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showLoadingDialog() {
        loadingDialog = Dialog(this)
        loadingDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loadingDialog?.setContentView(R.layout.dialog_loading)
        loadingDialog?.setCancelable(false)
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

}