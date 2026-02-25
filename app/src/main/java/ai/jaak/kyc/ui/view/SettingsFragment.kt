package ai.jaak.kyc.ui.view

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import ai.jaak.kyc.R
import ai.jaak.kyc.databinding.FragmentSettingsBinding
import ai.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var profileManager: ProfileManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        // TODO: Cargar datos reales del usuario desde SharedPreferences o ViewModel
        // Por ahora, usar datos de ejemplo
        binding.tvUserName.text = getString(R.string.settings_fallback_name)
        binding.tvUserEmail.text = getString(R.string.settings_fallback_email)
        binding.tvCompanyName.text = getString(R.string.settings_fallback_company)

        // Establecer estado del usuario (por ahora siempre activo, en el futuro vendrá del servidor)
        setUserStatus(isActive = true)

        // Obtener versión de la aplicación
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            binding.tvAppVersion.text = "$versionName ($versionCode)"
        } catch (e: PackageManager.NameNotFoundException) {
            binding.tvAppVersion.text = getString(R.string.settings_version_unknown)
        }
    }

    private fun setUserStatus(isActive: Boolean) {
        if (isActive) {
            // Estado Activo: fondo verde claro, texto verde, ícono check
            binding.tvStatusBadge.apply {
                setBackgroundResource(R.drawable.bg_badge_active)
                setTextColor(requireContext().getColor(R.color.jaak_success))
                text = getString(R.string.settings_active)
                setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_check_small, 0, 0, 0)
            }
        } else {
            // Estado Inactivo: fondo gris, texto gris oscuro, ícono X o ninguno
            binding.tvStatusBadge.apply {
                setBackgroundResource(R.drawable.bg_badge_inactive)
                setTextColor(requireContext().getColor(R.color.jaak_text_tertiary))
                text = getString(R.string.settings_inactive)
                setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0) // Sin ícono para inactivo
            }
        }
    }

    private fun setupListeners() {
        // Session Profiles - Abrir pantalla de lista de perfiles
        binding.llSessionProfiles.setOnClickListener {
            val intent = Intent(requireContext(), ProfilesListActivity::class.java)
            startActivity(intent)
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_logout_confirmation_title)
            .setMessage(R.string.settings_logout_confirmation_message)
            .setPositiveButton(R.string.settings_logout_confirm) { _, _ ->
                performLogout()
            }
            .setNegativeButton(R.string.settings_logout_cancel, null)
            .show()
    }

    private fun performLogout() {
        // Limpiar todos los datos usando ProfileManager
        profileManager.logout()

        // Navegar a MenuMainActivity
        val intent = Intent(requireContext(), MenuMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
