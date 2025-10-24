package com.jaak.kyc.ui.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jaak.kyc.R
import com.jaak.kyc.data.model.KycProfile
import com.jaak.kyc.databinding.ActivityEditSessionProfileBinding
import com.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EditSessionProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditSessionProfileBinding
    private var profileId: String? = null

    @Inject
    lateinit var profileManager: ProfileManager

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

        setupSpinners()
        setupListeners()
        loadProfileData()
        setupFieldValidation()
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

        // Botón actualizar perfil
        binding.btnUpdateProfile.setOnClickListener {
            saveProfile()
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
}
