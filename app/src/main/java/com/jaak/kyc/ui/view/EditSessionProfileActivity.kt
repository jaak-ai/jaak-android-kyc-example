package com.jaak.kyc.ui.view

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jaak.kyc.R
import com.jaak.kyc.data.model.KycProfile
import com.jaak.kyc.data.model.flow.CreateFlowRequest
import com.jaak.kyc.data.model.flow.VerificationData
import com.jaak.kyc.data.network.JaakDBApiClient
import com.jaak.kyc.data.network.JaakDBService
import com.jaak.kyc.databinding.ActivityEditSessionProfileBinding
import com.jaak.kyc.utils.Constants
import com.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditSessionProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditSessionProfileBinding
    private var profileId: String? = null
    private var isManualMode: Boolean = false
    private var loadingDialog: Dialog? = null

    @Inject
    lateinit var profileManager: ProfileManager

    @Inject
    lateinit var jaakDBApiClient: JaakDBApiClient

    @Inject
    lateinit var jaakDBService: JaakDBService

    @Inject
    lateinit var kycOfflineRepository: com.jaak.kyc.data.repository.KycOfflineRepository

    // Mapeo de países: Nombre completo -> Código ISO de 3 letras
    private val countryMap = mapOf(
        "México" to "MEX",
        "Colombia" to "COL",
        "Argentina" to "ARG",
        "Chile" to "CHL",
        "Perú" to "PER",
        "España" to "ESP"
    )

    // Mapeo inverso: Código ISO -> Nombre completo
    private val countryCodeMap = countryMap.entries.associate { (name, code) -> code to name }

    // Mapeo de métodos de validación: Texto mostrado -> Valor para API
    private val validationMap = mapOf(
        "Ninguna" to "",
        "Email" to "EMAIL",
        "SMS" to "SMS",
        "Whatsapp" to "WHATSAPP"
    )

    // Mapeo inverso: Valor API -> Texto mostrado
    private val validationCodeMap = validationMap.entries.associate { (name, code) -> code to name }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditSessionProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Detectar si está en modo manual (no guardar perfil)
        isManualMode = intent.getBooleanExtra("MODE_MANUAL", false)

        setupSpinners()
        setupListeners()
        loadProfileData()
        setupFieldValidation()
        setupManualModeUI()
    }

    private fun setupManualModeUI() {
        if (isManualMode) {
            // Cambiar texto del botón a "Iniciar Sesión KYC"
            binding.btnUpdateProfile.text = getString(R.string.start_kyc_session)
            // Ocultar switch de "Perfil por defecto" porque no se guardará
            binding.switchDefault.visibility = View.GONE
        }
    }

    private fun setupSpinners() {
        // Países disponibles (mostrar nombres completos al usuario)
        val countries = countryMap.keys.toTypedArray()
        val countryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCountry.adapter = countryAdapter

        // Métodos de validación (mostrar nombres amigables al usuario)
        val validationMethods = validationMap.keys.toTypedArray()
        val validationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, validationMethods)
        validationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerValidationMethod.adapter = validationAdapter
    }

    private fun setupListeners() {
        // Botón cancelar
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // Botón actualizar perfil / iniciar sesión KYC
        binding.btnUpdateProfile.setOnClickListener {
            if (isManualMode) {
                executeManualKycFlow()
            } else {
                saveProfile()
            }
        }
    }

    private fun loadProfileData() {
        // Obtener ID del perfil si es edición
        profileId = intent.getStringExtra("profileId")

        profileId?.let { id ->
            // Cargar perfil existente
            val profile = profileManager.getKycProfileById(id)
            profile?.let {
                binding.etProfileName.setText(it.profileName)
                binding.etContactName.setText(it.contactName)
                binding.etFlowName.setText(it.flowName)
                binding.etRedirectUrl.setText(it.redirectUrl ?: "")
                binding.switchDefault.isChecked = it.isDefault

                // Seleccionar país (convertir código ISO a nombre completo)
                val countryName = countryCodeMap[it.countryDocument]
                if (countryName != null) {
                    val countryAdapter = binding.spinnerCountry.adapter
                    for (i in 0 until countryAdapter.count) {
                        if (countryAdapter.getItem(i).toString() == countryName) {
                            binding.spinnerCountry.setSelection(i)
                            break
                        }
                    }
                }

                // Seleccionar método de validación (convertir código API a texto amigable)
                val validationName = validationCodeMap[it.verificationType]
                if (validationName != null) {
                    val validationAdapter = binding.spinnerValidationMethod.adapter
                    for (i in 0 until validationAdapter.count) {
                        if (validationAdapter.getItem(i).toString() == validationName) {
                            binding.spinnerValidationMethod.setSelection(i)
                            break
                        }
                    }
                }

                // Cargar campos de verificación según el tipo
                when (it.verificationType) {
                    "EMAIL" -> binding.etVerificationValue.setText(it.email ?: "")
                    "SMS" -> binding.etVerificationValue.setText(it.sms ?: "")
                    "WHATSAPP" -> binding.etVerificationValue.setText(it.whatsapp ?: "")
                    else -> binding.etVerificationValue.setText("")
                }
            }
        }
    }

    private fun setupFieldValidation() {
        // TextWatcher para los campos de texto
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        }

        // Agregar TextWatcher a los campos requeridos
        binding.etProfileName.addTextChangedListener(textWatcher)
        binding.etContactName.addTextChangedListener(textWatcher)
        binding.etFlowName.addTextChangedListener(textWatcher)

        // Listener para el spinner de país
        binding.spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                validateForm()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                validateForm()
            }
        }
    }

    private fun validateForm() {
        // Obtener valores de los campos requeridos
        val profileName = binding.etProfileName.text.toString().trim()
        val contactName = binding.etContactName.text.toString().trim()
        val flowName = binding.etFlowName.text.toString().trim()
        // El país siempre tendrá un valor seleccionado por defecto, no necesitamos validarlo

        // Habilitar botón solo si todos los campos requeridos están llenos
        val allFieldsFilled = profileName.isNotEmpty() &&
                             contactName.isNotEmpty() &&
                             flowName.isNotEmpty()

        binding.btnUpdateProfile.isEnabled = allFieldsFilled
    }

    private fun saveProfile() {
        // Validar campos requeridos
        val profileName = binding.etProfileName.text.toString().trim()
        val contactName = binding.etContactName.text.toString().trim()
        val flowName = binding.etFlowName.text.toString().trim()
        val redirectUrl = binding.etRedirectUrl.text.toString().trim()

        // Convertir nombre de país a código ISO
        val selectedCountryName = binding.spinnerCountry.selectedItem.toString()
        val countryDocument = countryMap[selectedCountryName] ?: "MEX"

        // Convertir nombre de validación a código API
        val selectedValidationName = binding.spinnerValidationMethod.selectedItem.toString()
        val verificationType = validationMap[selectedValidationName] ?: ""

        val verificationValue = binding.etVerificationValue.text.toString().trim()
        val isDefault = binding.switchDefault.isChecked

        if (profileName.isEmpty()) {
            binding.etProfileName.error = getString(R.string.error_field_required)
            return
        }

        if (contactName.isEmpty()) {
            binding.etContactName.error = getString(R.string.error_field_required)
            return
        }

        if (flowName.isEmpty()) {
            binding.etFlowName.error = getString(R.string.error_field_required)
            return
        }

        // Asignar valor de verificación según el tipo seleccionado
        var email: String? = null
        var sms: String? = null
        var whatsapp: String? = null

        when (verificationType) {
            "EMAIL" -> email = verificationValue.ifEmpty { null }
            "SMS" -> sms = verificationValue.ifEmpty { null }
            "WHATSAPP" -> whatsapp = verificationValue.ifEmpty { null }
        }

        // Crear o actualizar perfil
        val profile = KycProfile(
            id = profileId ?: java.util.UUID.randomUUID().toString(),
            profileName = profileName,
            contactName = contactName,
            flowName = flowName,
            redirectUrl = redirectUrl.ifEmpty { null },
            countryDocument = countryDocument,
            flowType = "KYC",
            verificationType = verificationType,
            email = email,
            sms = sms,
            whatsapp = whatsapp,
            isDefault = isDefault
        )

        // Guardar en ProfileManager
        profileManager.saveKycProfile(profile)

        val message = if (profileId == null) {
            getString(R.string.profile_created_success)
        } else {
            getString(R.string.profile_updated_success)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // Retornar resultado exitoso
        setResult(RESULT_OK)
        finish()
    }

    private fun executeManualKycFlow() {
        // Validar campos requeridos
        val contactName = binding.etContactName.text.toString().trim()
        val flowName = binding.etFlowName.text.toString().trim()
        val redirectUrl = binding.etRedirectUrl.text.toString().trim()

        // Convertir nombre de país a código ISO
        val selectedCountryName = binding.spinnerCountry.selectedItem.toString()
        val countryDocument = countryMap[selectedCountryName] ?: "MEX"

        // Convertir nombre de validación a código API
        val selectedValidationName = binding.spinnerValidationMethod.selectedItem.toString()
        val verificationType = validationMap[selectedValidationName] ?: ""

        val verificationValue = binding.etVerificationValue.text.toString().trim()

        if (contactName.isEmpty()) {
            binding.etContactName.error = getString(R.string.error_field_required)
            return
        }

        if (flowName.isEmpty()) {
            binding.etFlowName.error = getString(R.string.error_field_required)
            return
        }

        // Mostrar loading dialog
        showLoadingDialog()

        lifecycleScope.launch {
            try {
                // Obtener API Key
                val apiKey = profileManager.getApiKey() ?: profileManager.getAccessToken()
                if (apiKey.isNullOrEmpty()) {
                    hideLoadingDialog()
                    Toast.makeText(this@EditSessionProfileActivity, getString(R.string.error_api_key_unavailable), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Construir objeto de verificación
                val verification = VerificationData(
                    email = if (verificationType == "EMAIL") verificationValue else "",
                    sms = if (verificationType == "SMS") verificationValue else "",
                    whatsapp = if (verificationType == "WHATSAPP") verificationValue else ""
                )

                val createFlowRequest = CreateFlowRequest(
                    name = contactName,
                    flow = flowName,
                    redirectUrl = redirectUrl.ifEmpty { "" },
                    countryDocument = countryDocument,
                    flowType = "KYC",
                    verificationType = verificationType,
                    verification = verification
                )

                Log.d("EditSessionProfile", "Ejecutando flujo manual KYC...")
                Log.d("EditSessionProfile", "POST /api/v1/kyc/flow")

                // Paso 1: Crear flujo y obtener sessionUrl
                val flowResponse = jaakDBApiClient.createFlowApi(
                    auth = "Bearer $apiKey",
                    request = createFlowRequest
                )

                if (!flowResponse.isSuccessful) {
                    val errorBody = flowResponse.errorBody()?.string()
                    Log.e("EditSessionProfile", "Error creando flujo: $errorBody")
                    hideLoadingDialog()
                    Toast.makeText(this@EditSessionProfileActivity, getString(R.string.error_create_flow_failed, flowResponse.message()), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val createFlowResponse = flowResponse.body()
                if (createFlowResponse == null) {
                    hideLoadingDialog()
                    Toast.makeText(this@EditSessionProfileActivity, getString(R.string.error_invalid_response), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Extraer shortKey de la sessionUrl
                val shortKey = createFlowResponse.extractShortKey()
                Log.d("EditSessionProfile", "✓ Flow creado. Session URL: ${createFlowResponse.sessionUrl}")
                Log.d("EditSessionProfile", "✓ Short Key extraído: $shortKey")

                // ✅ CREAR PROCESO EN BD con shortKey
                val processId = kycOfflineRepository.createKycProcess(shortKey)
                Log.d("EditSessionProfile", "✓ Proceso creado en BD con ID: $processId")

                // Paso 2: Ejecutar sesión con el shortKey
                Log.d("EditSessionProfile", "POST /api/v1/kyc/session")
                val sessionResponse = jaakDBService.sessionApi(shortKey, Constants.ORIGIN_DEVICE)

                if (!sessionResponse.isSuccessful) {
                    val errorBody = sessionResponse.errorBody()?.string()
                    Log.e("EditSessionProfile", "Error ejecutando sesión: $errorBody")
                    hideLoadingDialog()
                    Toast.makeText(this@EditSessionProfileActivity, getString(R.string.error_session_failed, sessionResponse.message()), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val sessionData = sessionResponse.body()
                if (sessionData == null) {
                    hideLoadingDialog()
                    Toast.makeText(this@EditSessionProfileActivity, getString(R.string.error_invalid_response), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.d("EditSessionProfile", "✓ Sesión ejecutada exitosamente")
                Log.d("EditSessionProfile", "✓ Access Token: ${sessionData.accessToken.take(50)}...")

                // ✅ GUARDAR TOKEN EN EL PROCESO DE BD
                kycOfflineRepository.storeTokenByShortKey(shortKey, sessionData.accessToken, null)
                Log.d("EditSessionProfile", "✓ Token guardado en proceso BD")

                // Guardar token en Constants para usar en los servicios
                Constants.API_TOKEN = sessionData.accessToken
                Constants.TOKEN = Constants.BEARER + sessionData.accessToken

                // Guardar accessToken de la sesión
                profileManager.saveAccessToken(sessionData.accessToken)

                Log.d("EditSessionProfile", "✓ Flujo creado exitosamente. Navegando a InitProcessLivenessActivity...")

                // Ocultar loading dialog
                hideLoadingDialog()

                // Navegar a InitProcessLivenessActivity
                val intent = Intent(this@EditSessionProfileActivity, InitProcessLivenessActivity::class.java)
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Log.e("EditSessionProfile", "Excepción al crear flujo KYC: ${e.message}", e)
                hideLoadingDialog()
                Toast.makeText(this@EditSessionProfileActivity, getString(R.string.error_network, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoadingDialog() {
        loadingDialog = Dialog(this)
        loadingDialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        loadingDialog?.setContentView(R.layout.dialog_loading)
        loadingDialog?.setCancelable(false)
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}
