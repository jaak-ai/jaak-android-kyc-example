package ai.jaak.kyc.ui.view

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import ai.jaak.kyc.databinding.ActivityFinalSuccessBinding
import ai.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Pantalla final de éxito después de completar todo el proceso KYC
 */
@AndroidEntryPoint
class FinalSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinalSuccessBinding

    @Inject
    lateinit var profileManager: ProfileManager

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinalSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()
        startCountdown()
    }

    private fun initComponents() {
        // Deshabilitar botón de atrás
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No hacer nada - no permitir regresar
            }
        })

        // Botón finalizar - ahora navega inmediatamente cancelando el contador
        binding.btnFinishSuccess.setOnClickListener {
            countDownTimer?.cancel()
            navigateToMenu()
        }

        // Botón empezar de nuevo - vuelve al Dashboard para iniciar nuevo flujo
        binding.btnStartAgain.setOnClickListener {
            countDownTimer?.cancel()
            restartKycFlow()
        }
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                binding.btnFinishSuccess.text = "Finalizar ($secondsLeft)"
            }

            override fun onFinish() {
                binding.btnFinishSuccess.text = "Finalizar"
                navigateToMenu()
            }
        }.start()
    }

    private fun navigateToMenu() {
        // Verificar si el usuario está logueado
        if (profileManager.isLoggedIn()) {
            // Usuario logueado -> Regresar al Dashboard
            val intent = Intent(this, ai.jaak.kyc.MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } else {
            // Usuario NO logueado -> Regresar a MenuMainActivity
            navigateToMenuMain()
        }
        finish()
    }

    private fun navigateToMenuMain() {
        val intent = Intent(this, MenuMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Reinicia el flujo KYC sin cerrar sesión
     */
    private fun restartKycFlow() {
        // Limpiar proceso offline actual si existe
        // kycOfflineRepository.deleteCurrentProcess()
        
        // Volver al Dashboard
        val intent = Intent(this, ai.jaak.kyc.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
