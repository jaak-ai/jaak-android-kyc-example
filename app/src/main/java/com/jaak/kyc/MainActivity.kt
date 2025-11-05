package com.jaak.kyc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jaak.kyc.databinding.ActivityMainBinding
import com.jaak.kyc.ui.view.DashboardFragment
import com.jaak.kyc.ui.view.LoginActivity
import com.jaak.kyc.ui.view.SessionsFragmentNew
import com.jaak.kyc.ui.view.SettingsFragment
import com.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var profileManager: ProfileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar fragment inicial (Dashboard)
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        // Setup bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.navigation_sessions -> {
                    loadFragment(SessionsFragmentNew())
                    true
                }
                R.id.navigation_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    /**
     * Navega al tab de Settings programáticamente
     * Llamado desde DashboardFragment cuando se hace click en "Ir a Configuración"
     */
    fun navigateToSettings() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_settings
    }

    /**
     * Override del botón back para mostrar confirmación de cierre de sesión
     * igual que el botón de logout en el Dashboard
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        showLogoutConfirmationDialog()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_logout_confirmation_title)
            .setMessage(R.string.settings_logout_confirmation_message)
            .setPositiveButton(R.string.dashboard_logout_button) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun performLogout() {
        // Limpiar todos los datos del usuario
        profileManager.clearAll()

        // Regresar a LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
