package ai.jaak.kyc.ui.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ai.jaak.kyc.R
import ai.jaak.kyc.databinding.ActivitySecurityPermissionsBinding
import ai.jaak.kyc.utils.Constants
import ai.jaak.kyc.utils.FileStorageUtils
import com.jaak.stampssdk.sdk.StampsSDK
import com.jaak.stampssdk.ui.adapter.StampsListener
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity que maneja los permisos de seguridad (cámara y ubicación)
 * Solicita permisos secuencialmente y valida que estén concedidos junto con términos aceptados
 */
@AndroidEntryPoint
class SecurityPermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecurityPermissionsBinding

    private var isCameraPermissionGranted = false
    private var isLocationPermissionGranted = false
    private var areTermsAccepted = false

    // Launcher para permisos de cámara
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isCameraPermissionGranted = isGranted
        updateCameraPermissionUI(isGranted)
        if (isGranted) {
            // Si se concede cámara, solicitar ubicación automáticamente
            requestLocationPermission()
        } else {
            // Si se deniega cámara, actualizar estado del botón
            updateContinueButtonState()
        }
    }

    // Launcher para permisos de ubicación
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isLocationPermissionGranted = isGranted
        updateLocationPermissionUI(isGranted)
        updateContinueButtonState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecurityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        initComponents()
        checkInitialPermissions()
        requestPermissionsSequentially()
    }

    private fun initComponents() {
        // Configurar texto de términos con enlaces
        setupTermsText()
        
        // Configurar listeners
        binding.cbTerms.setOnCheckedChangeListener { _, isChecked ->
            areTermsAccepted = isChecked
            // Ocultar warning cuando se aceptan los términos
            binding.warningContainer.visibility = if (isChecked) View.GONE else View.VISIBLE
            updateContinueButtonState()
        }
        
        binding.btnRestart.setOnClickListener {
            // Navegar de vuelta al menú principal
            val intent = Intent(this, MenuMainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
        
        binding.btnContinue.setOnClickListener {
            // Iniciar captura de documentos con StampsSDK
            // Navegar de vuelta al menú principal
            val intent = Intent(this, DocumentVerificationInstructionsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupTermsText() {
        val fullText = "He leído y acepto las Políticas de Privacidad y tratamiento de los datos"
        val spannableString = SpannableString(fullText)

        // Encontrar posiciones de los textos clickeables
        val privacyStart = fullText.indexOf("Políticas de Privacidad")
        val privacyEnd = privacyStart + "Políticas de Privacidad".length

        val dataStart = fullText.indexOf("tratamiento de los datos")
        val dataEnd = dataStart + "tratamiento de los datos".length

        // Crear ClickableSpan para Políticas de Privacidad
        val privacyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openUrl("https://jaak.ai/aviso-de-privacidad")
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#007AFF") // Color azul iOS
                ds.isUnderlineText = true
            }
        }

        // Crear ClickableSpan para tratamiento de datos
        val dataClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                openUrl("https://jaak.ai/politica-de-seguridad-de-la-informacion")
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = Color.parseColor("#007AFF") // Color azul iOS
                ds.isUnderlineText = true
            }
        }

        // Aplicar los spans
        spannableString.setSpan(privacyClickableSpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(dataClickableSpan, dataStart, dataEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvTermsText.text = spannableString
        binding.tvTermsText.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTermsText.highlightColor = Color.TRANSPARENT
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkInitialPermissions() {
        // Verificar estado inicial de los permisos
        isCameraPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        isLocationPermissionGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        // Actualizar UI inicial
        updateCameraPermissionUI(isCameraPermissionGranted)
        updateLocationPermissionUI(isLocationPermissionGranted)
        updateContinueButtonState()
    }

    private fun requestPermissionsSequentially() {
        Log.d("SecurityPermissions", "Camera granted: $isCameraPermissionGranted, Location granted: $isLocationPermissionGranted")
        when {
            !isCameraPermissionGranted -> {
                // Solicitar permiso de cámara primero
                Log.d("SecurityPermissions", "Requesting camera permission")
                requestCameraPermission()
            }
            !isLocationPermissionGranted -> {
                // Si cámara ya está concedida, solicitar ubicación
                Log.d("SecurityPermissions", "Requesting location permission")
                requestLocationPermission()
            }
            else -> {
                // Ambos permisos ya están concedidos
                Log.d("SecurityPermissions", "Both permissions granted")
                updateContinueButtonState()
            }
        }
    }

    private fun requestCameraPermission() {
        if (!isCameraPermissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestLocationPermission() {
        if (!isLocationPermissionGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun updateCameraPermissionUI(isGranted: Boolean) {
        val statusIcon = binding.ivCameraStatus
        val statusText = binding.tvCameraStatus
        
        if (isGranted) {
            statusIcon.setImageResource(R.drawable.ic_status_granted)
            statusText.text = getString(R.string.permission_granted)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.jaak_permission_granted))
        } else {
            statusIcon.setImageResource(R.drawable.ic_status_denied)
            statusText.text = getString(R.string.permission_denied)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.jaak_permission_denied))
        }
    }

    private fun updateLocationPermissionUI(isGranted: Boolean) {
        val statusIcon = binding.ivLocationStatus
        val statusText = binding.tvLocationStatus
        
        if (isGranted) {
            statusIcon.setImageResource(R.drawable.ic_status_granted)
            statusText.text = getString(R.string.permission_granted)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.jaak_permission_granted))
        } else {
            statusIcon.setImageResource(R.drawable.ic_status_denied)
            statusText.text = getString(R.string.permission_denied)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.jaak_permission_denied))
        }
    }

    private fun updateContinueButtonState() {
        val shouldEnableButton = isCameraPermissionGranted && 
                                isLocationPermissionGranted && 
                                areTermsAccepted
        
        Log.d("SecurityPermissions", "Button state - Camera: $isCameraPermissionGranted, Location: $isLocationPermissionGranted, Terms: $areTermsAccepted, Enabled: $shouldEnableButton")
        
        binding.btnContinue.isEnabled = shouldEnableButton
        
        // El drawable ya maneja los estados enabled/disabled
        binding.btnContinue.background = ContextCompat.getDrawable(this, R.drawable.bg_button_continue_disabled)
    }

}