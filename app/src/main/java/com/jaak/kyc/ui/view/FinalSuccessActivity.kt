package com.jaak.kyc.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.jaak.kyc.databinding.ActivityFinalSuccessBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Pantalla final de éxito después de completar todo el proceso KYC
 */
@AndroidEntryPoint
class FinalSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinalSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinalSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()
    }

    private fun initComponents() {
        // Deshabilitar botón de atrás
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No hacer nada - no permitir regresar
            }
        })

        // Botón finalizar
        binding.btnFinishSuccess.setOnClickListener {
            navigateToMenu()
        }

        // Botón empezar de nuevo
        binding.btnStartAgain.setOnClickListener {
            navigateToMenu()
        }
    }

    private fun navigateToMenu() {
        // TODO: Verificar si hay sesión activa
        // Por ahora, siempre regresar al Dashboard (MainActivity)
        // Si no hay sesión, deberías redirigir a LoginActivity

        // Regresar al Dashboard (MainActivity con bottom nav)
        val intent = Intent(this, com.jaak.kyc.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
