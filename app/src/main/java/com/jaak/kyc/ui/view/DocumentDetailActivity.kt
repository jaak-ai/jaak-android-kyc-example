package com.jaak.kyc.ui.view

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaak.kyc.R
import com.jaak.kyc.databinding.ActivityDocumentDetailBinding
import com.jaak.kyc.ui.adapter.VerificationResult
import com.jaak.kyc.ui.adapter.VerificationResultsAdapter
import com.jaak.kyc.ui.viewmodel.SessionDetailState
import com.jaak.kyc.ui.viewmodel.SessionDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DocumentDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentDetailBinding
    private val viewModel: SessionDetailViewModel by viewModels()

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupImageClickListeners()
        loadDocumentDetailData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.rvVerificationResults.apply {
            layoutManager = LinearLayoutManager(this@DocumentDetailActivity)
            // Agregar divisor entre items
            addItemDecoration(
                DividerItemDecoration(this@DocumentDetailActivity, DividerItemDecoration.VERTICAL)
            )
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

    private fun loadDocumentDetailData() {
        // Obtener datos del Intent
        val documentData = intent.getParcelableExtra<com.jaak.kyc.data.model.DocumentDetailData>("document_data")

        if (documentData == null) {
            android.util.Log.e("DocumentDetailActivity", "❌ No se recibieron datos de verificación de documento")
            Toast.makeText(this, "Error: Datos no disponibles", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        android.util.Log.d("DocumentDetailActivity", "========== DOCUMENT DETAIL DATA RECEIVED ==========")
        android.util.Log.d("DocumentDetailActivity", "Event ID: ${documentData.eventId}")
        android.util.Log.d("DocumentDetailActivity", "Document Front URL: ${documentData.documentFrontUrl}")
        android.util.Log.d("DocumentDetailActivity", "Document Back URL: ${documentData.documentBackUrl}")
        android.util.Log.d("DocumentDetailActivity", "Verification Score: ${documentData.verificationScore}")
        android.util.Log.d("DocumentDetailActivity", "Verification Data: ${documentData.verificationData}")
        android.util.Log.d("DocumentDetailActivity", "===================================================")

        // Cargar imagen frontal del documento
        if (!documentData.documentFrontUrl.isNullOrEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(documentData.documentFrontUrl)
                .placeholder(R.drawable.ic_document_processing)
                .error(R.drawable.ic_document_processing)
                .into(binding.ivDocumentFront)
        } else {
            binding.ivDocumentFront.setImageResource(R.drawable.ic_document_processing)
        }

        // Cargar imagen trasera del documento
        if (!documentData.documentBackUrl.isNullOrEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(documentData.documentBackUrl)
                .placeholder(R.drawable.ic_document_processing)
                .error(R.drawable.ic_document_processing)
                .into(binding.ivDocumentBack)
        } else {
            binding.ivDocumentBack.setImageResource(R.drawable.ic_document_processing)
        }

        // Mostrar tiempo de procesamiento
        val processingTime = documentData.processingTime ?: "N/A"
        binding.tvProcessingTime.text = "Tiempo de procesamiento: $processingTime"

        // Parsear y mostrar resultados de verificación
        parseVerificationResults(documentData)
    }

    /**
     * Parsea los datos de verificación y crea la tabla de resultados
     */
    private fun parseVerificationResults(documentData: com.jaak.kyc.data.model.DocumentDetailData) {
        val verificationJson = documentData.verificationData

        if (verificationJson.isNullOrEmpty()) {
            android.util.Log.w("DocumentDetailActivity", "❌ No hay datos de verificación disponibles")
            return
        }

        try {
            val gson = com.google.gson.Gson()
            val evaluation = gson.fromJson(verificationJson, Map::class.java) as? Map<*, *>

            if (evaluation == null) {
                android.util.Log.e("DocumentDetailActivity", "❌ Error al parsear JSON de verificación")
                return
            }

            val results = mutableListOf<VerificationResult>()

            // Score
            val score = (evaluation["score"] as? Number)?.toDouble()
            if (score != null) {
                results.add(
                    VerificationResult(
                        property = "Score",
                        receivedData = String.format("%.0f", score),
                        expectedData = "1",
                        isCorrect = score >= 1.0
                    )
                )
            }

            // Consistencia de datos
            val dataConsistent = evaluation["data_consistent"] as? Boolean
            if (dataConsistent != null) {
                results.add(
                    VerificationResult(
                        property = "Consistencia de datos",
                        receivedData = if (dataConsistent) "Consistente" else "Inconsistente",
                        expectedData = "Consistente",
                        isCorrect = dataConsistent
                    )
                )
            }

            // Documento capturado completamente
            val documentComplete = evaluation["document_complete_sides"] as? Boolean
            if (documentComplete != null) {
                results.add(
                    VerificationResult(
                        property = "Documento capturado completamente",
                        receivedData = if (documentComplete) "Completo" else "Incompleto",
                        expectedData = "Completo",
                        isCorrect = documentComplete
                    )
                )
            }

            // Liveness del documento
            val documentLiveness = evaluation["document_liveness"] as? Boolean
            if (documentLiveness != null) {
                results.add(
                    VerificationResult(
                        property = "Liveness",
                        receivedData = if (documentLiveness) "Aprobado" else "Rechazado",
                        expectedData = "Aprobado",
                        isCorrect = documentLiveness
                    )
                )
            }

            // Falsificación de fotografías
            val photoForgery = evaluation["photo_forgery"] as? Boolean
            if (photoForgery != null) {
                results.add(
                    VerificationResult(
                        property = "Falsificación de fotografías",
                        receivedData = if (photoForgery) "Válido" else "Inválido",
                        expectedData = "Válido",
                        isCorrect = photoForgery
                    )
                )
            }

            // Validez del documento
            val documentValidity = evaluation["document_validity"] as? Boolean
            if (documentValidity != null) {
                results.add(
                    VerificationResult(
                        property = "Validez",
                        receivedData = if (documentValidity) "Válido" else "Inválido",
                        expectedData = "Válido",
                        isCorrect = documentValidity
                    )
                )
            }

            // Presencia de manos
            val handPresence = evaluation["hand_presence"] as? Boolean
            if (handPresence != null) {
                results.add(
                    VerificationResult(
                        property = "Presencia de manos",
                        receivedData = if (handPresence) "Verdadero" else "Falso",
                        expectedData = "Verdadero",
                        isCorrect = handPresence
                    )
                )
            }

            // Calidad en la imagen
            val imageQuality = evaluation["image_quality"] as? Boolean
            if (imageQuality != null) {
                results.add(
                    VerificationResult(
                        property = "Calidad en la imagen",
                        receivedData = if (imageQuality) "Verdadero" else "Falso",
                        expectedData = "Verdadero",
                        isCorrect = imageQuality
                    )
                )
            }

            // Verificación de elementos de seguridad
            val securityFeatures = evaluation["security_features"] as? Boolean
            if (securityFeatures != null) {
                results.add(
                    VerificationResult(
                        property = "Verificación de elementos de seguridad",
                        receivedData = if (securityFeatures) "Verdadero" else "Falso",
                        expectedData = "Verdadero",
                        isCorrect = securityFeatures
                    )
                )
            }

            android.util.Log.d("DocumentDetailActivity", "✅ Resultados de verificación parseados: ${results.size} items")

            // Configurar RecyclerView con los resultados
            binding.rvVerificationResults.apply {
                layoutManager = LinearLayoutManager(this@DocumentDetailActivity)
                adapter = VerificationResultsAdapter(results)
            }

        } catch (e: Exception) {
            android.util.Log.e("DocumentDetailActivity", "❌ Error al parsear verificación: ${e.message}", e)
        }
    }

    // ========== MÉTODOS OBSOLETOS ELIMINADOS ==========
    // Se eliminaron populateDocumentData() y populateMockData()
    // Ahora todos los datos deben venir del servicio en DocumentDetailData

    private fun populateDocumentDataOBSOLETE(state: SessionDetailState.Success) {
        // Buscar el evento de document-verify
        val documentEvent = state.data.flow.find {
            val resource = it.flow?.firstOrNull()?.resource?.lowercase()?.replace("-", "_") ?: ""
            resource == "document_verify" || resource == "document_validation" || resource == "document_liveness"
        }

        if (documentEvent == null) {
            // Ya se cargaron los datos mock en onCreate
            return
        }

        // Tiempo de procesamiento (desde el API o mock)
        binding.tvProcessingTime.text = "695 milisegundos"

        // Resultados de verificación
        val results = generateVerificationResults(documentEvent)
        val adapter = VerificationResultsAdapter(results)
        binding.rvVerificationResults.adapter = adapter
    }

    private fun generateVerificationResults(event: com.jaak.kyc.data.model.api.SessionDetailFlowEvent): List<VerificationResult> {
        val results = mutableListOf<VerificationResult>()

        // Obtener el resource del evento
        val resource = event.flow?.firstOrNull()
        val evaluation = resource?.meta?.extra?.evaluation

        if (evaluation != null) {
            // Score
            val score = resource.score
            if (score != null) {
                results.add(
                    VerificationResult(
                        property = "Score",
                        receivedData = String.format("%.1f", score),
                        expectedData = "Mayor a 1",
                        isCorrect = score > 1.0
                    )
                )
            }

            // Consistencia de datos
            results.add(
                VerificationResult(
                    property = "Consistencia de datos",
                    receivedData = "Consistente",
                    expectedData = "Consistente",
                    isCorrect = true
                )
            )

            // Documento capturado completamente
            results.add(
                VerificationResult(
                    property = "Documento capturado completamente",
                    receivedData = "Completo",
                    expectedData = "Completo",
                    isCorrect = true
                )
            )

            // Liveness
            results.add(
                VerificationResult(
                    property = "Liveness",
                    receivedData = "Aprobado",
                    expectedData = "Aprobado",
                    isCorrect = true
                )
            )

            // Falsificación de fotografías
            results.add(
                VerificationResult(
                    property = "Falsificación de fotografías",
                    receivedData = "Válido",
                    expectedData = "Válido",
                    isCorrect = true
                )
            )

            // Validez
            val isValid = evaluation["is_valid"] as? Boolean ?: true
            results.add(
                VerificationResult(
                    property = "Validez",
                    receivedData = if (isValid) "Válido" else "Inválido",
                    expectedData = "Válido",
                    isCorrect = isValid
                )
            )

            // Presencia de manos
            results.add(
                VerificationResult(
                    property = "Presencia de manos",
                    receivedData = "Verdadero",
                    expectedData = "Verdadero",
                    isCorrect = true
                )
            )

            // Calidad en la imagen
            results.add(
                VerificationResult(
                    property = "Calidad en la imagen",
                    receivedData = "Verdadero",
                    expectedData = "Verdadero",
                    isCorrect = true
                )
            )

            // Verificación de elementos de seguridad
            results.add(
                VerificationResult(
                    property = "Verificación de elementos de seguridad",
                    receivedData = "Verdadero",
                    expectedData = "Verdadero",
                    isCorrect = true
                )
            )
        } else {
            // Datos mock si no hay evaluation
            results.addAll(
                listOf(
                    VerificationResult("Score", "1", "Mayor a 1", true),
                    VerificationResult("Consistencia de datos", "Consistente", "Consistente", true),
                    VerificationResult("Documento capturado completamente", "Completo", "Completo", true),
                    VerificationResult("Liveness", "Aprobado", "Aprobado", true),
                    VerificationResult("Falsificación de fotografías", "Válido", "Válido", true),
                    VerificationResult("Validez", "Válido", "Válido", true),
                    VerificationResult("Presencia de manos", "Verdadero", "Verdadero", true),
                    VerificationResult("Calidad en la imagen", "Verdadero", "Verdadero", true),
                    VerificationResult("Verificación de elementos de seguridad", "Verdadero", "Verdadero", true)
                )
            )
        }

        return results
    }

    private fun populateMockData() {
        // Tiempo de procesamiento
        binding.tvProcessingTime.text = "695 milisegundos"

        // Resultados de verificación con datos mock
        val results = listOf(
            VerificationResult("Score", "1", "Mayor a 1", true),
            VerificationResult("Consistencia de datos", "Consistente", "Consistente", true),
            VerificationResult("Documento capturado completamente", "Completo", "Completo", true),
            VerificationResult("Liveness", "Aprobado", "Aprobado", true),
            VerificationResult("Falsificación de fotografías", "Válido", "Válido", true),
            VerificationResult("Validez", "Válido", "Válido", true),
            VerificationResult("Presencia de manos", "Verdadero", "Verdadero", true),
            VerificationResult("Calidad en la imagen", "Verdadero", "Verdadero", true),
            VerificationResult("Verificación de elementos de seguridad", "Verdadero", "Verdadero", true)
        )

        val adapter = VerificationResultsAdapter(results)
        binding.rvVerificationResults.adapter = adapter
    }
}
