package ai.jaak.kyc.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ai.jaak.kyc.databinding.ActivityHomeBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Clase principal que contiene la lógica de los fragments.
 * Esta actividad es la pantalla principal de la aplicación y gestiona la navegación y
 * la interacción del usuario con los fragmentos.
 */
@AndroidEntryPoint
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initComponents()
    }

    private fun initComponents(){
        binding.tvBtnStartKyc.setOnClickListener{
                val resultIntent = Intent(this, MenuMainActivity::class.java)
            startActivity(resultIntent)
        }
        binding.tvBtnPending.setOnClickListener{
            val resultIntent = Intent(this, KycSyncActivity::class.java)
            startActivity(resultIntent)
        }
    }
}