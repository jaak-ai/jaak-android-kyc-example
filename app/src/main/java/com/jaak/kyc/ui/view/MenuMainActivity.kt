package com.jaak.kyc.ui.view

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jaak.kyc.R
import com.jaak.kyc.databinding.ActivityMenuMainBinding
import com.jaak.kyc.ui.viewmodel.SessionModel
import com.jaak.kyc.ui.viewmodel.KycOfflineViewModel
import com.jaak.kyc.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

/**
 * Clase principal que contiene la lógica de los fragments.
 * Esta actividad es la pantalla principal de la aplicación y gestiona la navegación y
 * la interacción del usuario con los fragmentos.
 */
@AndroidEntryPoint
class MenuMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuMainBinding
    private val sessionModel: SessionModel by viewModels()
    private val kycOfflineViewModel: KycOfflineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityMenuMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViewModel()
        initComponents()
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

        // Los permisos ahora se manejan en SecurityPermissionsActivity
    }

    private fun initViewModel(){
        // ✅ SISTEMA OFFLINE UNIFICADO - Observadores
        kycOfflineViewModel.isLoading.observe(this) { isLoading ->
            if(isLoading){
                binding.clProgress.visibility = View.VISIBLE
            }else{
                binding.clProgress.visibility = View.GONE
            }
        }

        kycOfflineViewModel.errorModel.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                kycOfflineViewModel.clearMessages()
            }
        }

        kycOfflineViewModel.successMessage.observe(this) { message ->
            message?.let {
                when {
                    it.contains("Session executed successfully") -> {
                        // ✅ Session online exitosa con token - navegar a InitProcessLivenessActivity
                        Toast.makeText(this, "Sesión KYC creada exitosamente", Toast.LENGTH_SHORT).show()
                        navigateToNextStep()
                    }
                    it.contains("Session executed offline successfully") -> {
                        // 📱 Session offline exitosa - navegar a InitProcessLivenessActivity pero indicar modo offline
                        Toast.makeText(this, "Sesión KYC guardada offline", Toast.LENGTH_SHORT).show()
                        navigateToNextStep()
                    }
                    it.contains("created") -> {
                        // ✅ Proceso creado - mensaje silencioso, NO navegar aún
                        // Toast.makeText(this, "Proceso creado, ejecutando sesión...", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    }
                }
                kycOfflineViewModel.clearMessages()
            }
        }

        // Observar estado de red usando StateFlow
        kycOfflineViewModel.updateNetworkStatus()
        lifecycleScope.launch {
            kycOfflineViewModel.isNetworkAvailable.collect { isOnline ->
                updateNetworkIndicator(isOnline)
            }
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

        // Crear proceso y ejecutar sesión de forma secuencial (sin race condition)
        kycOfflineViewModel.createProcessAndExecuteSession(shortKey)
    }

    private fun navigateToNextStep() {
        // Navegar a la pantalla de bienvenida después de crear la sesión
        val resultIntent = Intent(this, InitProcessLivenessActivity::class.java)
        startActivity(resultIntent)
    }

    private fun updateNetworkIndicator(isOnline: Boolean) {
        // TODO: Agregar indicador visual de conectividad en la UI
        // Por ejemplo, cambiar color de un indicador o mostrar un ícono
        Log.d("NetworkStatus", "Network status: ${if (isOnline) "Online" else "Offline"}")
    }

}