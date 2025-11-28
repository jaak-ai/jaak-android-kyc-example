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

    // Mapeo de tipos de flujo: Texto mostrado -> Código interno
    private val flowTypeMap = mapOf(
        "KYC Tradicional" to "TRADITIONAL",
        "Rigel" to "RIGEL",
        "KYC Mosaic" to "MOSAIC"
    )

    // Mapeo inverso: Código interno -> Texto mostrado
    private val flowTypeCodeMap = flowTypeMap.entries.associate { (name, code) -> code to name }

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
            // Cambiar texto del botón a "Guardar"
            binding.btnUpdateProfile.text = getString(R.string.start_kyc_session)
            // Ocultar toda la sección de "Perfil por defecto" en modo manual
            binding.layoutDefaultSection.visibility = View.GONE
            // Ocultar el campo "Nombre de perfil" en modo manual
            binding.layoutProfileName.visibility = View.GONE
        } else {
            // Modo rápido: Mostrar campo "Nombre de perfil"
            binding.layoutProfileName.visibility = View.VISIBLE
            // Modo rápido: Activar switch si es el primer perfil
            if (profileId == null) {
                val existingProfiles = profileManager.getKycProfiles()
                if (existingProfiles.isEmpty()) {
                    binding.switchDefault.isChecked = true
                }
            }
        }
    }

    private fun setupSpinners() {
        // Países disponibles (mostrar nombres completos al usuario)
        val countries = countryMap.keys.toTypedArray()
        val countryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, countries)
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCountry.adapter = countryAdapter

        // Obtener profileId para determinar si es edición o creación
        profileId = intent.getStringExtra("profileId")
        
        // Determinar si debe bloquearse el spinner
        val shouldLockSpinner = !isManualMode && profileId == null
        
        // Tipos de flujo - Filtrar según el modo
        val availableFlowTypes = if (shouldLockSpinner) {
            // Modo rápido (dashboard) y creación nueva: Solo KYC Tradicional
            arrayOf("KYC Tradicional")
        } else {
            // Modo manual o edición de perfil existente: Mostrar todas las opciones
            flowTypeMap.keys.toTypedArray()
        }
        
        val flowTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableFlowTypes)
        flowTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFlowType.adapter = flowTypeAdapter
        
        // Bloquear el spinner solo en modo rápido para nuevo perfil
        if (shouldLockSpinner) {
            binding.spinnerFlowType.isEnabled = false
            binding.spinnerFlowType.alpha = 0.5f
        }

        // Métodos de validación (mostrar nombres amigables al usuario)
        val validationMethods = validationMap.keys.toTypedArray()
        val validationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, validationMethods)
        validationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerValidationMethod.adapter = validationAdapter

        // Listener para mostrar/ocultar módulos Mosaic
        binding.spinnerFlowType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedType = flowTypeMap[parent?.getItemAtPosition(position).toString()]
                if (selectedType == "MOSAIC") {
                    setupMosaicModules()
                    binding.layoutMosaicModules.visibility = android.view.View.VISIBLE
                } else {
                    binding.layoutMosaicModules.visibility = android.view.View.GONE
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                binding.layoutMosaicModules.visibility = android.view.View.GONE
            }
        }
    }

    private var mosaicAdapter: com.jaak.kyc.ui.adapter.MosaicModuleAdapter? = null
    private val mosaicModules = com.jaak.kyc.data.model.MosaicModule.getDefaultModules().toMutableList()

    private fun setupMosaicModules() {
        if (mosaicAdapter == null) {
            // Configurar LayoutManager
            binding.rvMosaicModules.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
            
            mosaicAdapter = com.jaak.kyc.ui.adapter.MosaicModuleAdapter(mosaicModules) {
                // Callback cuando cambia algo
            }
            binding.rvMosaicModules.adapter = mosaicAdapter

            // Setup drag & drop
            var lastDraggedModuleId: String? = null
            var dragErrorShown = false
            
            val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(object : androidx.recyclerview.widget.ItemTouchHelper.Callback() {
                override fun getMovementFlags(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Int {
                    // Todos los módulos pueden moverse
                    return makeMovementFlags(
                        androidx.recyclerview.widget.ItemTouchHelper.UP or androidx.recyclerview.widget.ItemTouchHelper.DOWN, 
                        0
                    )
                }

                override fun onMove(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Boolean {
                    val fromPos = viewHolder.adapterPosition
                    val toPos = target.adapterPosition
                    
                    val moved = mosaicAdapter?.moveModule(fromPos, toPos) ?: false
                    
                    // Si no se pudo mover, mostrar AlertDialog UNA SOLA VEZ
                    if (!moved && !dragErrorShown) {
                        dragErrorShown = true
                        
                        // Determinar el mensaje de error basado en las dependencias rotas
                        val message = getMovementErrorMessage(fromPos, toPos)
                        
                        androidx.appcompat.app.AlertDialog.Builder(this@EditSessionProfileActivity)
                            .setTitle(getString(R.string.mosaic_error_title))
                            .setMessage(message)
                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                    
                    return moved
                }
                
                override fun onSelectedChanged(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)
                    
                    // Cuando comienza el drag, resetear el flag
                    if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG) {
                        dragErrorShown = false
                        viewHolder?.let {
                            val position = it.adapterPosition
                            if (position >= 0 && position < mosaicModules.size) {
                                lastDraggedModuleId = mosaicModules[position].id
                            }
                        }
                    }
                    
                    // Cuando termina el drag, resetear el flag
                    if (actionState == androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE) {
                        dragErrorShown = false
                        lastDraggedModuleId = null
                    }
                }

                override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {}
                override fun isLongPressDragEnabled() = true
            })

            itemTouchHelper.attachToRecyclerView(binding.rvMosaicModules)
        }
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
        // El profileId ya se carga en setupSpinners()
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

                // Seleccionar tipo de flujo
                val flowTypeName = flowTypeCodeMap[it.selectedFlowType]
                if (flowTypeName != null) {
                    val flowTypeAdapter = binding.spinnerFlowType.adapter
                    for (i in 0 until flowTypeAdapter.count) {
                        if (flowTypeAdapter.getItem(i).toString() == flowTypeName) {
                            binding.spinnerFlowType.setSelection(i)
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
        val allFieldsFilled = if (!isManualMode && binding.layoutProfileName.visibility == View.VISIBLE) {
            // Modo rápido: validar nombre de perfil, nombre de contacto y nombre de flujo
            profileName.isNotEmpty() && contactName.isNotEmpty() && flowName.isNotEmpty()
        } else {
            // Modo manual: solo validar nombre de contacto y nombre de flujo
            contactName.isNotEmpty() && flowName.isNotEmpty()
        }

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

        val isDefault = binding.switchDefault.isChecked

        // Validar nombre de perfil en modo rápido
        if (!isManualMode && profileName.isEmpty()) {
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

        // Obtener tipo de flujo seleccionado del spinner
        val selectedFlowTypeName = binding.spinnerFlowType.selectedItem.toString()
        val selectedFlowType = flowTypeMap[selectedFlowTypeName] ?: "TRADITIONAL"

        // Si es Mosaic, validar y serializar los módulos seleccionados
        val mosaicModulesJson = if (selectedFlowType == "MOSAIC") {
            // Validar orden de módulos
            val validationError = mosaicAdapter?.validateModuleOrder()
            if (validationError != null) {
                android.widget.Toast.makeText(this, validationError, android.widget.Toast.LENGTH_LONG).show()
                return
            }
            
            val gson = com.google.gson.Gson()
            gson.toJson(mosaicAdapter?.getModules() ?: mosaicModules)
        } else {
            null
        }

        // Crear o actualizar perfil
        val profile = KycProfile(
            id = profileId ?: java.util.UUID.randomUUID().toString(),
            profileName = if (!isManualMode) profileName else contactName,
            contactName = contactName,
            flowName = flowName,
            redirectUrl = redirectUrl.ifEmpty { null },
            countryDocument = countryDocument,
            flowType = "KYC",
            selectedFlowType = selectedFlowType,
            mosaicModulesJson = mosaicModulesJson,
            verificationType = verificationType,
            email = null,
            sms = null,
            whatsapp = null,
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

        // Obtener tipo de flujo seleccionado
        val selectedFlowTypeName = binding.spinnerFlowType.selectedItem.toString()
        val selectedFlowType = flowTypeMap[selectedFlowTypeName] ?: "TRADITIONAL"

        // Convertir nombre de validación a código API
        val selectedValidationName = binding.spinnerValidationMethod.selectedItem.toString()
        val verificationType = validationMap[selectedValidationName] ?: ""

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
                    email = "",
                    sms = "",
                    whatsapp = ""
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
                Log.d("EditSessionProfile", "✓ Transparent: ${createFlowResponse.transparent}")
                Log.d("EditSessionProfile", "✓ Short Key extraído: $shortKey")
                
                // Extraer licencia del header traceparent
                val traceparent = flowResponse.headers()["traceparent"]
                Log.d("EditSessionProfile", "✓ Header traceparent: $traceparent")
                
                // Extraer y guardar licencia del header traceparent
                val flowLicense = com.jaak.kyc.data.model.flow.CreateFlowResponse.extractLicenseFromHeader(traceparent)
                if (flowLicense != null) {
                    com.jaak.kyc.utils.FlowLicenseManager.saveLicense(this@EditSessionProfileActivity, flowLicense)
                    Log.d("EditSessionProfile", "✓ Licencia extraída del header traceparent y guardada: $flowLicense")
                } else {
                    Log.w("EditSessionProfile", "⚠ No se pudo extraer licencia del header traceparent")
                }

                // ✅ CREAR PROCESO EN BD con shortKey
                val processId = kycOfflineRepository.createKycProcess(shortKey)
                Log.d("EditSessionProfile", "✓ Proceso creado en BD con ID: $processId")

                // Decidir el flujo según el tipo seleccionado
                when (selectedFlowType) {
                    "RIGEL" -> {
                        // Flujo Rigel: Abrir WebView con URL de Rigel
                        val rigelUrl = "https://rigel.qa.jaak.ai/session/$shortKey"
                        Log.d("EditSessionProfile", "✓ Abriendo Rigel WebView: $rigelUrl")
                        
                        hideLoadingDialog()
                        
                        val intent = Intent(this@EditSessionProfileActivity, KycWebViewActivity::class.java)
                        intent.putExtra(KycWebViewActivity.EXTRA_URL, rigelUrl)
                        intent.putExtra(KycWebViewActivity.EXTRA_FLOW_TYPE, "RIGEL")
                        startActivity(intent)
                        finish()
                    }
                    "MOSAIC" -> {
                        // Flujo Mosaic: Usar configuración de módulos del formulario
                        val modules = mosaicAdapter?.getModules() ?: mosaicModules
                        val config = com.jaak.kyc.data.model.MosaicConfig(modules, shortKey)
                        val mosaicUrl = config.buildMosaicUrl()
                        
                        Log.d("EditSessionProfile", "✓ Abriendo Mosaic WebView: $mosaicUrl")
                        
                        hideLoadingDialog()
                        
                        val intent = Intent(this@EditSessionProfileActivity, KycWebViewActivity::class.java)
                        intent.putExtra(KycWebViewActivity.EXTRA_URL, mosaicUrl)
                        intent.putExtra(KycWebViewActivity.EXTRA_FLOW_TYPE, "MOSAIC")
                        startActivity(intent)
                        finish()
                    }
                    else -> {
                        // Flujo Tradicional (TRADITIONAL): Continuar con session API y servicios
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
                    }
                }

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
    
    /**
     * Determina el mensaje de error apropiado cuando un movimiento no es válido
     */
    private fun getMovementErrorMessage(fromPos: Int, toPos: Int): String {
        val moduleToMove = mosaicModules[fromPos]
        
        // Crear lista temporal para simular el movimiento
        val tempModules = mosaicModules.toMutableList()
        if (fromPos < toPos) {
            for (i in fromPos until toPos) {
                java.util.Collections.swap(tempModules, i, i + 1)
            }
        } else {
            for (i in fromPos downTo toPos + 1) {
                java.util.Collections.swap(tempModules, i, i - 1)
            }
        }
        
        // Verificar qué dependencia se rompe
        tempModules.forEachIndexed { index, module ->
            when (module.id) {
                "BLACKLIST" -> {
                    val modulesBeforeThis = tempModules.subList(0, index)
                    if (!modulesBeforeThis.any { it.id == "DOCUMENT_EXTRACT" }) {
                        return getString(R.string.mosaic_error_blacklist_requires_document)
                    }
                }
                "IVERIFICATION" -> {
                    val modulesBeforeThis = tempModules.subList(0, index)
                    val hasDocExtract = modulesBeforeThis.any { it.id == "DOCUMENT_EXTRACT" }
                    val hasOto = modulesBeforeThis.any { it.id == "OTO" }
                    
                    if (!hasDocExtract && !hasOto) {
                        return getString(R.string.mosaic_error_1to1_requires_both)
                    } else if (!hasDocExtract) {
                        return getString(R.string.mosaic_error_1to1_requires_document)
                    } else if (!hasOto) {
                        return getString(R.string.mosaic_error_1to1_requires_oto)
                    }
                }
            }
        }
        
        // Mensaje genérico si no se detectó el problema específico
        return getString(R.string.mosaic_error_cannot_move)
    }
}
