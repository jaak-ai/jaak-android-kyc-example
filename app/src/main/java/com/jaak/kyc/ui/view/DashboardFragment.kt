package com.jaak.kyc.ui.view

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaak.kyc.MainActivity
import com.jaak.kyc.R
import com.jaak.kyc.data.model.KycProfile
import com.jaak.kyc.data.model.flow.CreateFlowRequest
import com.jaak.kyc.data.model.flow.VerificationData
import com.jaak.kyc.data.network.JaakDBApiClient
import com.jaak.kyc.databinding.FragmentDashboardBinding
import com.jaak.kyc.ui.adapter.DashboardProfilesAdapter
import com.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var profilesAdapter: DashboardProfilesAdapter

    @Inject
    lateinit var profileManager: ProfileManager

    @Inject
    lateinit var jaakDBApiClient: JaakDBApiClient

    @Inject
    lateinit var kycOfflineRepository: com.jaak.kyc.data.repository.KycOfflineRepository

    private var loadingDialog: Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadUserData()
        setupListeners()
    }

    private fun setupRecyclerView() {
        profilesAdapter = DashboardProfilesAdapter { profile ->
            // Iniciar flujo KYC completo con el perfil seleccionado
            startKycFlowFromProfile(profile)
        }

        binding.rvKycProfiles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = profilesAdapter
        }
    }

    private fun loadUserData() {
        // Cargar datos del usuario desde ProfileManager
        val userInfo = profileManager.getUserInfo()
        val companyInfo = profileManager.getCompanyInfo()

        if (userInfo != null) {
            binding.tvUserName.text = userInfo.fullName.uppercase()
            binding.tvUserRole.text = userInfo.rol
            binding.tvUserEmail.text = userInfo.email
        } else {
            // Fallback a datos de ejemplo si no hay login
            binding.tvUserName.text = getString(R.string.dashboard_fallback_name)
            binding.tvUserRole.text = getString(R.string.dashboard_fallback_role)
            binding.tvUserEmail.text = getString(R.string.dashboard_fallback_email)
        }

        // Cargar perfiles KYC y mostrar en RecyclerView
        val profiles = profileManager.getKycProfiles()

        if (profiles.isNotEmpty()) {
            // Mostrar lista de perfiles
            binding.rvKycProfiles.visibility = View.VISIBLE
            binding.cardNoProfile.visibility = View.GONE
            profilesAdapter.submitList(profiles)
        } else {
            // Mostrar estado vacío
            binding.rvKycProfiles.visibility = View.GONE
            binding.cardNoProfile.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        // Recargar perfiles al volver (por si se crearon nuevos)
        loadUserData()
    }

    private fun setupListeners() {
        // Botón cerrar sesión
        binding.ivLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Tarjeta de Inicio Rápido KYC (cuando hay perfil configurado)
        binding.cardQuickStart.setOnClickListener {
            // Obtener el perfil por defecto y crear flujo
            val defaultProfile = profileManager.getKycProfiles().firstOrNull { it.isDefault }
            if (defaultProfile != null) {
                startKycFlowFromProfile(defaultProfile)
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_no_default_profile), Toast.LENGTH_SHORT).show()
            }
        }

        // Link "Crear Perfil" (cuando NO hay perfil)
        binding.tvGoToSettings.setOnClickListener {
            val intent = Intent(requireContext(), EditSessionProfileActivity::class.java)
            startActivity(intent)
        }

        // Botón de Creación Manual - Nueva Sesión KYC
        binding.cardManualCreation.setOnClickListener {
            // Abrir formulario de perfil en modo manual (sin guardar)
            val intent = Intent(requireContext(), EditSessionProfileActivity::class.java)
            intent.putExtra("MODE_MANUAL", true)
            startActivity(intent)
        }
    }

    private fun showLogoutConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
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

        // Regresar a MenuMainActivity
        val intent = Intent(requireContext(), MenuMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun navigateToSettings() {
        // Navegar al tab de Settings en el bottom navigation
        val mainActivity = activity as? MainActivity
        mainActivity?.navigateToSettings()
    }

    private fun showLoadingDialog() {
        loadingDialog = Dialog(requireContext())
        loadingDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loadingDialog?.setContentView(R.layout.dialog_loading)
        loadingDialog?.setCancelable(false)
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun startKycFlowFromProfile(profile: KycProfile) {
        Log.d("DashboardFragment", "Iniciando flujo KYC con perfil: ${profile.profileName}")

        // Mostrar loading dialog
        showLoadingDialog()

        lifecycleScope.launch {
            try {
                // Paso 1: Crear flujo KYC y obtener sessionUrl usando API Key
                // Fallback: Si no existe apiKey, usar accessToken (para usuarios ya logueados)
                val apiKey = profileManager.getApiKey() ?: profileManager.getAccessToken()
                if (apiKey.isNullOrEmpty()) {
                    hideLoadingDialog()
                    Toast.makeText(requireContext(), getString(R.string.error_api_key_unavailable), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Construir objeto de verificación (siempre enviarlo, con strings vacíos en lugar de null)
                val verification = VerificationData(
                    email = profile.email ?: "",
                    sms = profile.sms ?: "",
                    whatsapp = profile.whatsapp ?: ""
                )

                val createFlowRequest = CreateFlowRequest(
                    name = profile.contactName,
                    flow = profile.flowName,
                    redirectUrl = profile.redirectUrl ?: "",
                    countryDocument = profile.countryDocument,
                    flowType = profile.flowType,
                    verificationType = profile.verificationType,
                    verification = verification
                )

                Log.d("DashboardFragment", "========== CREATE FLOW REQUEST ==========")
                Log.d("DashboardFragment", "URL: POST /api/v1/kyc/flow")
                Log.d("DashboardFragment", "Headers: Authorization: Bearer $apiKey")
                Log.d("DashboardFragment", "Body: {")
                Log.d("DashboardFragment", "  name: ${createFlowRequest.name}")
                Log.d("DashboardFragment", "  flow: ${createFlowRequest.flow}")
                Log.d("DashboardFragment", "  redirectUrl: ${createFlowRequest.redirectUrl}")
                Log.d("DashboardFragment", "  countryDocument: ${createFlowRequest.countryDocument}")
                Log.d("DashboardFragment", "  flowType: ${createFlowRequest.flowType}")
                Log.d("DashboardFragment", "  verificationType: ${createFlowRequest.verificationType}")
                Log.d("DashboardFragment", "  verification: {")
                Log.d("DashboardFragment", "    email: ${verification.email}")
                Log.d("DashboardFragment", "    sms: ${verification.sms}")
                Log.d("DashboardFragment", "    whatsapp: ${verification.whatsapp}")
                Log.d("DashboardFragment", "  }")
                Log.d("DashboardFragment", "}")
                Log.d("DashboardFragment", "=========================================")

                val flowResponse = jaakDBApiClient.createFlowApi(
                    auth = "Bearer $apiKey",
                    request = createFlowRequest
                )

                Log.d("DashboardFragment", "========== CREATE FLOW RESPONSE ==========")
                Log.d("DashboardFragment", "Status Code: ${flowResponse.code()}")
                Log.d("DashboardFragment", "Status Message: ${flowResponse.message()}")

                if (!flowResponse.isSuccessful) {
                    val errorBody = flowResponse.errorBody()?.string()
                    Log.e("DashboardFragment", "Error Body: $errorBody")
                    Log.d("DashboardFragment", "=========================================")
                    hideLoadingDialog()
                    Toast.makeText(requireContext(), getString(R.string.error_create_flow_failed, flowResponse.message()), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val flowData = flowResponse.body()
                if (flowData == null) {
                    Log.e("DashboardFragment", "Response body es null")
                    Log.d("DashboardFragment", "=========================================")
                    hideLoadingDialog()
                    Toast.makeText(requireContext(), getString(R.string.error_empty_server_response), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.d("DashboardFragment", "Response Body: {")
                Log.d("DashboardFragment", "  sessionUrl: ${flowData.sessionUrl}")
                Log.d("DashboardFragment", "}")
                Log.d("DashboardFragment", "=========================================")

                // Extraer shortKey de la sessionUrl
                val shortKey = flowData.extractShortKey()
                Log.d("DashboardFragment", "ShortKey extraído: $shortKey")

                // ✅ CREAR PROCESO EN BD con shortKey
                val processId = kycOfflineRepository.createKycProcess(shortKey)
                Log.d("DashboardFragment", "✓ Proceso creado en BD con ID: $processId")

                // Decidir el flujo según el tipo seleccionado en el perfil
                when (profile.selectedFlowType) {
                    "RIGEL" -> {
                        // Flujo Rigel: Abrir WebView con URL de Rigel
                        val rigelUrl = "https://rigel.dev.jaak.ai/session/$shortKey"
                        Log.d("DashboardFragment", "✓ Abriendo Rigel WebView: $rigelUrl")
                        
                        hideLoadingDialog()
                        
                        val intent = Intent(requireContext(), KycWebViewActivity::class.java)
                        intent.putExtra(KycWebViewActivity.EXTRA_URL, rigelUrl)
                        intent.putExtra(KycWebViewActivity.EXTRA_FLOW_TYPE, "RIGEL")
                        startActivity(intent)
                    }
                    "MOSAIC" -> {
                        // Flujo Mosaic: Usar configuración guardada del perfil
                        val mosaicModules = profile.getMosaicModules() ?: com.jaak.kyc.data.model.MosaicModule.getDefaultModules()
                        val config = com.jaak.kyc.data.model.MosaicConfig(mosaicModules, shortKey)
                        val mosaicUrl = config.buildMosaicUrl()
                        
                        Log.d("DashboardFragment", "✓ Abriendo Mosaic WebView: $mosaicUrl")
                        
                        hideLoadingDialog()
                        
                        val intent = Intent(requireContext(), KycWebViewActivity::class.java)
                        intent.putExtra(KycWebViewActivity.EXTRA_URL, mosaicUrl)
                        intent.putExtra(KycWebViewActivity.EXTRA_FLOW_TYPE, "MOSAIC")
                        startActivity(intent)
                    }
                    else -> {
                        // Flujo Tradicional (TRADITIONAL): Continuar con session API y servicios
                        Log.d("DashboardFragment", "========== CREATE SESSION REQUEST ==========")
                        Log.d("DashboardFragment", "URL: POST /api/v1/kyc/session")
                        Log.d("DashboardFragment", "Headers: {")
                        Log.d("DashboardFragment", "  Short-Key: $shortKey")
                        Log.d("DashboardFragment", "  Origin-Device: Android")
                        Log.d("DashboardFragment", "}")
                        Log.d("DashboardFragment", "=========================================")

                        val sessionResponse = jaakDBApiClient.sessionApi(
                            shortKey = shortKey,
                            originDevice = "Android"
                        )

                        Log.d("DashboardFragment", "========== CREATE SESSION RESPONSE ==========")
                        Log.d("DashboardFragment", "Status Code: ${sessionResponse.code()}")
                        Log.d("DashboardFragment", "Status Message: ${sessionResponse.message()}")

                        if (!sessionResponse.isSuccessful) {
                            val errorBody = sessionResponse.errorBody()?.string()
                            Log.e("DashboardFragment", "Error Body: $errorBody")
                            Log.d("DashboardFragment", "=========================================")
                            hideLoadingDialog()
                            Toast.makeText(requireContext(), getString(R.string.error_create_session_failed, sessionResponse.message()), Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val sessionData = sessionResponse.body()
                        if (sessionData == null) {
                            Log.e("DashboardFragment", "Session response body es null")
                            Log.d("DashboardFragment", "=========================================")
                            hideLoadingDialog()
                            Toast.makeText(requireContext(), getString(R.string.error_empty_session_response), Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        Log.d("DashboardFragment", "Response Body: {")
                        Log.d("DashboardFragment", "  sessionId: ${sessionData.sessionId}")
                        Log.d("DashboardFragment", "  accessToken: ${sessionData.accessToken}")
                        Log.d("DashboardFragment", "  step: ${sessionData.step}")
                        Log.d("DashboardFragment", "  document: ${sessionData.document}")
                        Log.d("DashboardFragment", "  assets: ${sessionData.assets}")
                        Log.d("DashboardFragment", "}")
                        Log.d("DashboardFragment", "=========================================")

                        // ✅ GUARDAR TOKEN EN EL PROCESO DE BD
                        kycOfflineRepository.storeTokenByShortKey(shortKey, sessionData.accessToken, null)
                        Log.d("DashboardFragment", "✓ Token guardado en proceso BD")

                        // Guardar accessToken de la sesión
                        profileManager.saveAccessToken(sessionData.accessToken)

                        Log.d("DashboardFragment", "✓ Flujo creado exitosamente. Navegando a InitProcessLivenessActivity...")

                        // Ocultar loading dialog
                        hideLoadingDialog()

                        // Navegar a InitProcessLivenessActivity
                        val intent = Intent(requireContext(), InitProcessLivenessActivity::class.java)
                        startActivity(intent)
                    }
                }

            } catch (e: Exception) {
                Log.e("DashboardFragment", "Excepción al crear flujo KYC: ${e.message}", e)
                hideLoadingDialog()
                Toast.makeText(requireContext(), getString(R.string.error_network, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startKycProcess() {
        // Iniciar el flujo KYC existente (MenuMainActivity)
        val intent = Intent(requireContext(), MenuMainActivity::class.java)
        // Flag para indicar que viene del Dashboard (usuario ya logueado creando nuevo KYC)
        intent.putExtra("FROM_DASHBOARD", true)
        startActivity(intent)
        // NO hacer finish() aquí - cuando MenuMainActivity termine, volverá aquí
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
