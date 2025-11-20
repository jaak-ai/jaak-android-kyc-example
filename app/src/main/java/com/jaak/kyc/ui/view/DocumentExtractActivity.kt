package com.jaak.kyc.ui.view

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jaak.kyc.R
import com.jaak.kyc.databinding.ActivityDocumentExtractBinding
import com.jaak.kyc.ui.viewmodel.SessionDetailState
import com.jaak.kyc.ui.viewmodel.SessionDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DocumentExtractActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentExtractBinding
    private val viewModel: SessionDetailViewModel by viewModels()

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentExtractBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupImageClickListeners()
        loadDocumentExtractData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupImageClickListeners() {
        // Click listeners para preview de imágenes en pantalla completa
        binding.ivDocumentFront.setOnClickListener {
            showImagePreview(binding.ivDocumentFront)
        }

        binding.ivDocumentBack.setOnClickListener {
            showImagePreview(binding.ivDocumentBack)
        }

        binding.ivOcrPhoto.setOnClickListener {
            showImagePreview(binding.ivOcrPhoto)
        }
    }

    /**
     * Muestra preview de imagen en pantalla completa
     */
    private fun showImagePreview(imageView: ImageView) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_image_preview)

        val previewImage = dialog.findViewById<ImageView>(R.id.ivPreview)
        val closeButton = dialog.findViewById<ImageView>(R.id.ivClose)

        previewImage.setImageDrawable(imageView.drawable)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadDocumentExtractData() {
        // Obtener datos del Intent
        val extractData = intent.getParcelableExtra<com.jaak.kyc.data.model.DocumentExtractData>("extract_data")

        if (extractData == null) {
            android.util.Log.e("DocumentExtractActivity", "❌ No se recibieron datos de extracción")
            Toast.makeText(this, "Error: Datos no disponibles", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        android.util.Log.d("DocumentExtractActivity", "========== DOCUMENT EXTRACT DATA RECEIVED ==========")
        android.util.Log.d("DocumentExtractActivity", "Event ID: ${extractData.eventId}")
        android.util.Log.d("DocumentExtractActivity", "Document Front URL: ${extractData.documentFrontUrl}")
        android.util.Log.d("DocumentExtractActivity", "Document Back URL: ${extractData.documentBackUrl}")
        android.util.Log.d("DocumentExtractActivity", "Face URL: ${extractData.faceUrl}")
        android.util.Log.d("DocumentExtractActivity", "===================================================")

        // Cargar imágenes de documentos usando Picasso
        if (!extractData.documentFrontUrl.isNullOrEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(extractData.documentFrontUrl)
                .placeholder(R.drawable.ic_document_processing)
                .error(R.drawable.ic_document_processing)
                .into(binding.ivDocumentFront)
        } else {
            binding.ivDocumentFront.setImageResource(R.drawable.ic_document_processing)
        }

        if (!extractData.documentBackUrl.isNullOrEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(extractData.documentBackUrl)
                .placeholder(R.drawable.ic_document_processing)
                .error(R.drawable.ic_document_processing)
                .into(binding.ivDocumentBack)
        } else {
            binding.ivDocumentBack.setImageResource(R.drawable.ic_document_processing)
        }

        // Cargar imagen de rostro desde URL
        if (!extractData.faceUrl.isNullOrEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(extractData.faceUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.ivOcrPhoto)
        } else {
            binding.ivOcrPhoto.setImageResource(R.drawable.ic_person)
        }

        // Mostrar tiempo de procesamiento
        val processingTime = extractData.processingTime ?: "N/A"
        binding.tvProcessingTime.text = "Tiempo de procesamiento: $processingTime"

        // Parsear y mostrar datos extraídos
        parseExtractedData(extractData)
    }

    /**
     * Parsea los datos extraídos del JSON y los muestra en la UI
     */
    private fun parseExtractedData(extractData: com.jaak.kyc.data.model.DocumentExtractData) {
        val extractedJson = extractData.extractedData

        if (extractedJson.isNullOrEmpty()) {
            android.util.Log.w("DocumentExtractActivity", "❌ No hay datos extraídos disponibles")
            return
        }

        try {
            val gson = com.google.gson.Gson()
            val evaluation = gson.fromJson(extractedJson, Map::class.java) as? Map<*, *>

            if (evaluation == null) {
                android.util.Log.e("DocumentExtractActivity", "❌ Error al parsear JSON de extracción")
                return
            }

            // Extraer datos personales
            val personal = evaluation["personal"] as? Map<*, *>
            val personalExtra = personal?.get("extra") as? Map<*, *>

            android.util.Log.d("DocumentExtractActivity", "personal: $personal")
            android.util.Log.d("DocumentExtractActivity", "personalExtra: $personalExtra")

            // Extraer datos del documento
            val document = evaluation["document"] as? Map<*, *>
            val documentExtra = document?.get("extra") as? Map<*, *>
            val country = document?.get("country") as? Map<*, *>
            val expiration = document?.get("expiration") as? Map<*, *>

            android.util.Log.d("DocumentExtractActivity", "document: $document")
            android.util.Log.d("DocumentExtractActivity", "documentExtra: $documentExtra")
            android.util.Log.d("DocumentExtractActivity", "country: $country")

            // Extraer dirección
            val address = evaluation["address"] as? Map<*, *>
            val addressExtra = address?.get("extra") as? Map<*, *>

            android.util.Log.d("DocumentExtractActivity", "address: $address")
            android.util.Log.d("DocumentExtractActivity", "addressExtra: $addressExtra")

            // ========== DATOS PERSONALES ==========
            binding.tvFullName.text = (personal?.get("fullName") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvFirstName.text = (personal?.get("firstName") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvMiddleName.text = (personal?.get("secondName") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvMotherLastName.text = (personal?.get("motherSurname") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvFatherLastName.text = (personal?.get("surname") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvBirthDate.text = (personal?.get("dateOfBirth") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvBirthPlace.text = (personal?.get("placeOfBirth") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvGender.text = when (personal?.get("sex") as? String) {
                "F" -> "Femenino"
                "M" -> "Masculino"
                else -> "-"
            }
            binding.tvNationality.text = (country?.get("name") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvMaritalStatus.text = (personal?.get("maritalStatus") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvOcr.text = (personalExtra?.get("ocr") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvRegistrationYear.text = (personalExtra?.get("registerYear") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvRfc.text = (personalExtra?.get("rfc") as? String).takeIf { !it.isNullOrBlank() } ?: "-"

            // ========== DIRECCIÓN ==========
            binding.tvFullAddress.text = (address?.get("fullAddress") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvPostalCode.text = (address?.get("postalCode")?.toString()).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvCity.text = (addressExtra?.get("city") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvExteriorNumber.text = (addressExtra?.get("externalNumber")?.toString()).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvInteriorNumber.text = "-"  // No viene en el JSON
            binding.tvNeighborhood.text = (addressExtra?.get("neighborhood") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvState.text = (addressExtra?.get("state") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvStreet.text = (addressExtra?.get("street") as? String).takeIf { !it.isNullOrBlank() } ?: "-"

            // ========== DATOS DEL DOCUMENTO ==========
            binding.tvAdditionalNumber.text = (document?.get("additionalNumber") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvIssueDate.text = (document?.get("dateOfIssue")?.toString()).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvIssuingAuthority.text = (document?.get("issuingAuthority") as? String).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvDocumentNumber.text = (document?.get("number")?.toString()).takeIf { !it.isNullOrBlank() } ?: "-"
            binding.tvPersonalIdNumber.text = (document?.get("personalIdNumber") as? String).takeIf { !it.isNullOrBlank() } ?: "-"

            // ========== ESTADOS OCR ==========
            parseEstadosOcr(evaluation, extractData.thresholdsData, extractData.validationData)

            android.util.Log.d("DocumentExtractActivity", "✅ Datos extraídos parseados correctamente")

        } catch (e: Exception) {
            android.util.Log.e("DocumentExtractActivity", "❌ Error al parsear datos extraídos: ${e.message}", e)
        }
    }

    /**
     * Parsea los estados OCR del servicio y los muestra si existen
     */
    private fun parseEstadosOcr(evaluation: Map<*, *>, thresholdsJson: String?, validationJson: String?) {
        try {
            val estadosOcrList = mutableListOf<com.jaak.kyc.ui.adapter.EstadoOcrItem>()

            // Buscar el objeto "ocr-states" que viene del recurso ocr-states
            val ocrStates = evaluation["ocr-states"] as? Map<*, *>

            android.util.Log.d("DocumentExtractActivity", "ocr-states evaluation: $ocrStates")
            android.util.Log.d("DocumentExtractActivity", "thresholdsJson: $thresholdsJson")
            android.util.Log.d("DocumentExtractActivity", "validationJson: $validationJson")

            // Parsear thresholds y validation si existen
            val gson = com.google.gson.Gson()
            val thresholds = if (!thresholdsJson.isNullOrEmpty()) {
                gson.fromJson(thresholdsJson, Map::class.java) as? Map<*, *>
            } else null

            val validation = if (!validationJson.isNullOrEmpty()) {
                gson.fromJson(validationJson, Map::class.java) as? Map<*, *>
            } else null

            // Si existe "ocr-states" evaluation, agregarlo como primer item
            ocrStates?.forEach { (key, value) ->
                if (value is Boolean) {
                    estadosOcrList.add(
                        com.jaak.kyc.ui.adapter.EstadoOcrItem(
                            property = "Evaluación - ${formatPropertyName(key.toString())}",
                            isValid = value
                        )
                    )
                }
            }

            // Si existe thresholds, agregarlo como segundo grupo
            thresholds?.forEach { (key, value) ->
                if (value is Boolean) {
                    estadosOcrList.add(
                        com.jaak.kyc.ui.adapter.EstadoOcrItem(
                            property = "Umbral - ${formatPropertyName(key.toString())}",
                            isValid = value
                        )
                    )
                }
            }

            // Si existe validation, agregarlo como tercer grupo
            validation?.forEach { (key, value) ->
                if (value is Boolean) {
                    estadosOcrList.add(
                        com.jaak.kyc.ui.adapter.EstadoOcrItem(
                            property = "Validación - ${formatPropertyName(key.toString())}",
                            isValid = value
                        )
                    )
                }
            }

            // Si hay estados OCR, mostrar la sección
            if (estadosOcrList.isNotEmpty()) {
                android.util.Log.d("DocumentExtractActivity", "✅ Estados OCR encontrados: ${estadosOcrList.size}")
                binding.cardEstadosOcr.visibility = android.view.View.VISIBLE
                binding.rvEstadosOcr.apply {
                    layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@DocumentExtractActivity)
                    adapter = com.jaak.kyc.ui.adapter.EstadoOcrAdapter(estadosOcrList, showHeader = true)
                }
            } else {
                android.util.Log.d("DocumentExtractActivity", "ℹ️ No se encontraron estados OCR")
                binding.cardEstadosOcr.visibility = android.view.View.GONE
            }

        } catch (e: Exception) {
            android.util.Log.e("DocumentExtractActivity", "❌ Error al parsear estados OCR: ${e.message}", e)
            binding.cardEstadosOcr.visibility = android.view.View.GONE
        }
    }

    /**
     * Formatea nombres de propiedades de camelCase a formato legible
     */
    private fun formatPropertyName(name: String): String {
        // Convertir camelCase a espacios: documentCompleteSides -> Document Complete Sides
        return name.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replaceFirstChar { it.uppercase() }
    }

    // ========== MÉTODOS OBSOLETOS ELIMINADOS ==========
    // Se eliminaron populateDocumentExtractData() y populateMockData()
    // Ahora todos los datos deben venir del servicio en DocumentExtractData

    private fun populateDocumentExtractDataOBSOLETE(state: SessionDetailState.Success) {
        // Buscar el evento de document-extract o document-ocr
        val documentEvent = state.data.flow.find {
            val resource = it.flow?.firstOrNull()?.resource?.lowercase()?.replace("-", "_") ?: ""
            resource == "document_extract" || resource == "document_ocr"
        }

        if (documentEvent == null) {
            // Ya se cargaron los datos mock en onCreate
            return
        }

        // Obtener el resource del evento
        val resource = documentEvent.flow?.firstOrNull()
        val ocrData = resource?.meta?.extra?.evaluation

        if (ocrData != null && ocrData.isNotEmpty()) {
            // Tiempo de procesamiento (mock data - se puede obtener del API)
            binding.tvProcessingTime.text = "2.553 segundos"

            // Datos personales
            binding.tvFullName.text = extractStringValue(ocrData, "full_name") ?: "DANIELA JUDITH ROBLES VASQUEZ"
            binding.tvFirstName.text = extractStringValue(ocrData, "first_name", "name") ?: "DANIELA"
            binding.tvMiddleName.text = extractStringValue(ocrData, "middle_name", "second_name") ?: "JUDITH"
            binding.tvMotherLastName.text = extractStringValue(ocrData, "mother_last_name", "maternal_surname") ?: "VASQUEZ"
            binding.tvFatherLastName.text = extractStringValue(ocrData, "father_last_name", "paternal_surname", "last_name") ?: "ROBLES"
            binding.tvBirthDate.text = extractStringValue(ocrData, "birth_date", "date_of_birth") ?: "10/02/1999"
            binding.tvBirthPlace.text = extractStringValue(ocrData, "birth_place", "place_of_birth") ?: "-"
            binding.tvGender.text = extractStringValue(ocrData, "gender", "sex")?.let {
                when(it.lowercase()) {
                    "f", "female", "femenino" -> "Femenino"
                    "m", "male", "masculino" -> "Masculino"
                    else -> it
                }
            } ?: "Femenino"
            binding.tvNationality.text = extractStringValue(ocrData, "nationality") ?: "MEXICAN"
            binding.tvMaritalStatus.text = extractStringValue(ocrData, "marital_status", "civil_status") ?: "-"
            binding.tvOcr.text = extractStringValue(ocrData, "ocr", "ocr_number") ?: "1327107379263"
            binding.tvRegistrationYear.text = extractStringValue(ocrData, "registration_year", "year_of_registration") ?: "2016 01"
            binding.tvRfc.text = extractStringValue(ocrData, "rfc", "tax_id") ?: "ROVD990210L4A"

            // Domicilio
            binding.tvFullAddress.text = extractStringValue(ocrData, "full_address", "address") ?: "AV CHAPULETEPEC 7 COL VISTA HERMOSA 55028 ECATEPEC DE MORELOS, MEX"
            binding.tvPostalCode.text = extractStringValue(ocrData, "postal_code", "zip_code") ?: "55028"
            binding.tvCity.text = extractStringValue(ocrData, "city") ?: "ECATEPEC DE MORELOS"
            binding.tvExteriorNumber.text = extractStringValue(ocrData, "exterior_number", "external_number") ?: "7"
            binding.tvInteriorNumber.text = extractStringValue(ocrData, "interior_number", "internal_number") ?: "No proporcionado"
            binding.tvNeighborhood.text = extractStringValue(ocrData, "neighborhood", "colony", "colonia") ?: "VISTA HERMOSA"
            binding.tvState.text = extractStringValue(ocrData, "state") ?: "ESTADO DE MEXICO"
            binding.tvStreet.text = extractStringValue(ocrData, "street") ?: "AVENIDA CHAPULETEPEC"

            // Documento
            binding.tvAdditionalNumber.text = extractStringValue(ocrData, "additional_number", "cic") ?: "RBVSDN99021015M800"
            binding.tvIssueDate.text = extractStringValue(ocrData, "issue_date", "emission_date") ?: "01/01/2019"
            binding.tvIssuingAuthority.text = extractStringValue(ocrData, "issuing_authority", "authority") ?: "INE"
            binding.tvDocumentNumber.text = extractStringValue(ocrData, "document_number", "number") ?: "194108329"
            binding.tvPersonalIdNumber.text = extractStringValue(ocrData, "personal_id_number", "curp") ?: "ROVD990210MMCBSN07"

            // TODO: Cargar imágenes reales del documento y foto OCR
            // Por ahora quedan como placeholders
        }
        // Si no hay datos, se mantienen los mock data cargados en onCreate
    }

    private fun extractStringValue(data: Map<String, Any?>, vararg keys: String): String? {
        for (key in keys) {
            val value = data[key]
            if (value != null) {
                return value.toString()
            }
        }
        return null
    }

    private fun populateMockData() {
        binding.tvProcessingTime.text = "2.553 segundos"

        // Datos personales
        binding.tvFullName.text = "DANIELA JUDITH ROBLES VASQUEZ"
        binding.tvFirstName.text = "DANIELA"
        binding.tvMiddleName.text = "JUDITH"
        binding.tvMotherLastName.text = "VASQUEZ"
        binding.tvFatherLastName.text = "ROBLES"
        binding.tvBirthDate.text = "10/02/1999"
        binding.tvBirthPlace.text = "-"
        binding.tvGender.text = "Femenino"
        binding.tvNationality.text = "MEXICAN"
        binding.tvMaritalStatus.text = "-"
        binding.tvOcr.text = "1327107379263"
        binding.tvRegistrationYear.text = "2016 01"
        binding.tvRfc.text = "ROVD990210L4A"

        // Domicilio
        binding.tvFullAddress.text = "AV CHAPULETEPEC 7 COL VISTA HERMOSA 55028 ECATEPEC DE MORELOS, MEX"
        binding.tvPostalCode.text = "55028"
        binding.tvCity.text = "ECATEPEC DE MORELOS"
        binding.tvExteriorNumber.text = "7"
        binding.tvInteriorNumber.text = "No proporcionado"
        binding.tvNeighborhood.text = "VISTA HERMOSA"
        binding.tvState.text = "ESTADO DE MEXICO"
        binding.tvStreet.text = "AVENIDA CHAPULETEPEC"

        // Documento
        binding.tvAdditionalNumber.text = "RBVSDN99021015M800"
        binding.tvIssueDate.text = "01/01/2019"
        binding.tvIssuingAuthority.text = "INE"
        binding.tvDocumentNumber.text = "194108329"
        binding.tvPersonalIdNumber.text = "ROVD990210MMCBSN07"
    }
}
