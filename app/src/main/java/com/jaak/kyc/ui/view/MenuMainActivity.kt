package com.jaak.kyc.ui.view

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
        binding.tvBtnStart.setOnClickListener{
            startKycProcess()
        }
        val requestCameraPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission(), ActivityResultCallback { isGranted ->
                if (isGranted) {
                    Log.e("CameraPermission", "Permiso de cámara concedido")
                } else {
                    Log.e("CameraPermission", "Permiso de cámara denegado")
                }
            })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Log.e("CameraPermission", "Permiso de cámara ya concedido")
            } else {
                requestCameraPermission.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun initViewModel(){
        // Observadores del modelo original (para compatibilidad online)
        sessionModel.sessionResponse.observe(this){
            Constants.API_TOKEN = it.accessToken
            Constants.TOKEN = Constants.BEARER + Constants.API_TOKEN
            val resultIntent = Intent(this, InitProcessLivenessActivity::class.java)
            startActivity(resultIntent)
        }
        sessionModel.errorModel.observe(this){
            Toast.makeText(this,getString(R.string.not_create_session), Toast.LENGTH_SHORT).show()
        }
        sessionModel.isLoading.observe(this) {
            if(it){
                binding.clProgress.visibility = View.VISIBLE
            }else{
                binding.clProgress.visibility = View.GONE
            }
        }
        sessionModel.validation.observe(this) {
            Toast.makeText(this,getString(R.string.shortkey_empty), Toast.LENGTH_SHORT).show()
        }
        
        // Nuevos observadores para el sistema offline
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
                        // ✅ Session online exitosa con token - navegar
                        Toast.makeText(this, "Sesión KYC creada exitosamente", Toast.LENGTH_SHORT).show()
                        navigateToNextStep()
                    }
                    it.contains("Session executed offline successfully") -> {
                        // 📱 Session offline exitosa - navegar pero indicar modo offline
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
        
        // Crear nuevo proceso KYC y ejecutar primera sesión
        kycOfflineViewModel.createNewProcess(shortKey)
        kycOfflineViewModel.executeSession(shortKey)
    }
    
    private fun navigateToNextStep() {
        // Navegar al siguiente paso del proceso KYC
        val resultIntent = Intent(this, InitProcessLivenessActivity::class.java)
        startActivity(resultIntent)
    }
    
    private fun updateNetworkIndicator(isOnline: Boolean) {
        // TODO: Agregar indicador visual de conectividad en la UI
        // Por ejemplo, cambiar color de un indicador o mostrar un ícono
        Log.d("NetworkStatus", "Network status: ${if (isOnline) "Online" else "Offline"}")
    }

}