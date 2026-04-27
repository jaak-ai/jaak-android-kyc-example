package ai.jaak.kyc

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import ai.jaak.kyc.data.network.AuthInterceptor
import ai.jaak.kyc.data.network.PersistentCookieJar
import ai.jaak.kyc.databinding.ActivityMainBinding
import ai.jaak.kyc.domain.service.NetworkConnectivityService
import ai.jaak.kyc.ui.view.DashboardFragment
import ai.jaak.kyc.ui.view.LoginActivity
import ai.jaak.kyc.ui.view.SessionsFragmentNew
import ai.jaak.kyc.ui.view.SettingsFragment
import ai.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var profileManager: ProfileManager

    @Inject
    lateinit var networkConnectivityService: NetworkConnectivityService

    @Inject
    lateinit var authInterceptor: AuthInterceptor

    @Inject
    lateinit var cookieJar: PersistentCookieJar

    private val offlineBannerHandler = Handler(Looper.getMainLooper())
    private var hideBannerRunnable: Runnable? = null

    private val sessionExpiredListener: () -> Unit = {
        runOnUiThread { performLogout() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cargar fragment inicial (Dashboard)
        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        networkConnectivityService.startMonitoring()
        setupOfflineBanner()
        authInterceptor.addSessionExpiredListener(sessionExpiredListener)

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

    private fun setupOfflineBanner() {
        lifecycleScope.launch {
            networkConnectivityService.networkState.collect { state ->
                if (!state.isConnected) {
                    // Cancelar ocultamiento pendiente y mostrar banner inmediatamente
                    hideBannerRunnable?.let { offlineBannerHandler.removeCallbacks(it) }
                    hideBannerRunnable = null
                    showOfflineBanner()
                } else {
                    // Ocultar con delay de 1.5s al recuperar conexión (igual que iOS)
                    hideBannerRunnable?.let { offlineBannerHandler.removeCallbacks(it) }
                    hideBannerRunnable = Runnable { hideOfflineBanner() }
                    offlineBannerHandler.postDelayed(hideBannerRunnable!!, 1500)
                }
            }
        }
    }

    private fun showOfflineBanner() {
        if (binding.bannerOffline.visibility == View.VISIBLE) return
        binding.bannerOffline.visibility = View.VISIBLE
        binding.bannerOffline.startAnimation(AnimationUtils.loadAnimation(this, R.anim.banner_slide_down))
    }

    private fun hideOfflineBanner() {
        if (binding.bannerOffline.visibility == View.GONE) return
        val anim = AnimationUtils.loadAnimation(this, R.anim.banner_slide_up)
        anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(a: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
            override fun onAnimationEnd(a: android.view.animation.Animation?) {
                binding.bannerOffline.visibility = View.GONE
            }
        })
        binding.bannerOffline.startAnimation(anim)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideBannerRunnable?.let { offlineBannerHandler.removeCallbacks(it) }
        networkConnectivityService.stopMonitoring()
        authInterceptor.removeSessionExpiredListener(sessionExpiredListener)
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
        // Limpiar todos los datos del usuario (tokens, perfil, etc.)
        profileManager.clearAll()
        // Limpiar cookies HTTP-only (refreshToken)
        cookieJar.clearAll()

        // Regresar a MenuMainActivity
        val intent = Intent(this, ai.jaak.kyc.ui.view.MenuMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
