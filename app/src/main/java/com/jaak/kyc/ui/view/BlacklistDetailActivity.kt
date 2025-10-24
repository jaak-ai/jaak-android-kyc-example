package com.jaak.kyc.ui.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaak.kyc.data.model.BlacklistItem
import com.jaak.kyc.data.model.BlacklistStatus
import com.jaak.kyc.data.model.DataRow
import com.jaak.kyc.databinding.ActivityBlacklistDetailBinding
import com.jaak.kyc.ui.adapter.BlacklistAdapter

class BlacklistDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlacklistDetailBinding

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlacklistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadBlacklistData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadBlacklistData() {
        // Obtener datos del Intent
        val blacklistData = intent.getParcelableExtra<com.jaak.kyc.data.model.BlacklistDetailData>("blacklist_data")

        if (blacklistData == null) {
            android.util.Log.e("BlacklistDetailActivity", "❌ No se recibieron datos de blacklist")
            android.widget.Toast.makeText(this, "Error: Datos no disponibles", android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        android.util.Log.d("BlacklistDetailActivity", "========== BLACKLIST DATA RECEIVED ==========")
        android.util.Log.d("BlacklistDetailActivity", "Event ID: ${blacklistData.eventId}")
        android.util.Log.d("BlacklistDetailActivity", "Processing Time: ${blacklistData.processingTime}")
        android.util.Log.d("BlacklistDetailActivity", "")
        android.util.Log.d("BlacklistDetailActivity", "========== BLACKLIST RESULTS JSON ==========")
        android.util.Log.d("BlacklistDetailActivity", blacklistData.blacklistResults ?: "null")
        android.util.Log.d("BlacklistDetailActivity", "============================================")

        // Intentar parsear y mostrar de forma estructurada
        if (blacklistData.blacklistResults != null) {
            try {
                val gson = com.google.gson.Gson()
                val resultsList = gson.fromJson(blacklistData.blacklistResults, List::class.java)

                android.util.Log.d("BlacklistDetailActivity", "")
                android.util.Log.d("BlacklistDetailActivity", "========== PARSED BLACKLIST RESULTS ==========")
                android.util.Log.d("BlacklistDetailActivity", "Total blacklist items: ${resultsList.size}")

                resultsList.forEachIndexed { index, item ->
                    val itemMap = item as? Map<*, *>
                    android.util.Log.d("BlacklistDetailActivity", "")
                    android.util.Log.d("BlacklistDetailActivity", "--- Item ${index + 1} ---")
                    android.util.Log.d("BlacklistDetailActivity", "  eventId: ${itemMap?.get("eventId")}")
                    android.util.Log.d("BlacklistDetailActivity", "  resourceName: ${itemMap?.get("resourceName")}")
                    android.util.Log.d("BlacklistDetailActivity", "  status: ${itemMap?.get("status")}")
                    android.util.Log.d("BlacklistDetailActivity", "  createdAt: ${itemMap?.get("createdAt")}")

                    val evaluation = itemMap?.get("evaluation") as? Map<*, *>
                    if (evaluation != null) {
                        android.util.Log.d("BlacklistDetailActivity", "  evaluation:")
                        evaluation.forEach { (key, value) ->
                            android.util.Log.d("BlacklistDetailActivity", "    $key: $value")
                        }
                    }
                }
                android.util.Log.d("BlacklistDetailActivity", "=============================================")
            } catch (e: Exception) {
                android.util.Log.e("BlacklistDetailActivity", "Error parsing blacklist results: ${e.message}", e)
            }
        }
        android.util.Log.d("BlacklistDetailActivity", "")

        // Parsear blacklistResults (JSON) y mostrar resultados reales
        val riskLists = mutableListOf<BlacklistItem>()
        val validationLists = mutableListOf<BlacklistItem>()

        if (blacklistData.blacklistResults != null) {
            try {
                val gson = com.google.gson.Gson()
                val resultsList = gson.fromJson(blacklistData.blacklistResults, List::class.java)

                // Agrupar por resourceName y tomar solo los que tienen status "success"
                val groupedResults = mutableMapOf<String, Map<*, *>>()

                resultsList.forEach { item ->
                    val itemMap = item as? Map<*, *>
                    val resourceName = itemMap?.get("resourceName") as? String
                    val status = itemMap?.get("status") as? String

                    // Solo procesar items con status "success"
                    if (status == "success" && resourceName != null) {
                        groupedResults[resourceName] = itemMap
                    }
                }

                // Parsear INE
                groupedResults["ine-blacklist"]?.let { item ->
                    parseIneBlacklist(item)?.let { validationLists.add(it) }
                }

                // Parsear OFAC
                groupedResults["ofac-blacklist"]?.let { item ->
                    parseOfacBlacklist(item)?.let { riskLists.add(it) }
                }

                // Parsear SAT69B
                groupedResults["sat69b-blacklist"]?.let { item ->
                    parseSat69bBlacklist(item)?.let { riskLists.add(it) }
                }

                // Parsear Interpol
                groupedResults["interpol-blacklist"]?.let { item ->
                    parseInterpolBlacklist(item)?.let { riskLists.add(it) }
                }

                // Parsear CURP
                groupedResults["curp-blacklist"]?.let { item ->
                    parseCurpBlacklist(item)?.let { validationLists.add(it) }
                }

            } catch (e: Exception) {
                android.util.Log.e("BlacklistDetailActivity", "Error parsing blacklist data: ${e.message}", e)
            }
        }

        // Configurar RecyclerViews
        binding.rvRiskLists.apply {
            layoutManager = LinearLayoutManager(this@BlacklistDetailActivity)
            adapter = BlacklistAdapter(riskLists)
        }

        binding.rvValidationLists.apply {
            layoutManager = LinearLayoutManager(this@BlacklistDetailActivity)
            adapter = BlacklistAdapter(validationLists)
        }

        // Ocultar loading
        binding.progressBar.visibility = View.GONE
    }

    /**
     * Parsea datos de INE blacklist
     */
    private fun parseIneBlacklist(item: Map<*, *>): BlacklistItem? {
        val evaluation = item["evaluation"] as? Map<*, *>
        val ineData = evaluation?.get("ine") as? Map<*, *>
        val identification = ineData?.get("identification") as? Map<*, *>
        val content = identification?.get("content") as? Map<*, *>

        return if (content != null && content.isNotEmpty()) {
            BlacklistItem(
                name = "INE",
                status = BlacklistStatus.VALID,
                description = "El usuario ha sido identificado en bases oficiales de INE, lo que respalda la autenticidad de su información.",
                attempts = 1,
                processingTime = "N/A",
                detailedData = listOf(
                    DataRow("Número CIC", content["cicNumber"]?.toString() ?: "-"),
                    DataRow("Distrito Federal", content["federalDistrict"]?.toString() ?: "-"),
                    DataRow("Número de Emisión", content["issuanceNumber"]?.toString() ?: "-"),
                    DataRow("Año de Emisión", content["issuanceYear"]?.toString() ?: "-"),
                    DataRow("Distrito Local", content["localDistrict"]?.toString() ?: "-"),
                    DataRow("Número OCR", content["ocrNumber"]?.toString() ?: "-"),
                    DataRow("Año de Registro", content["registrationYear"]?.toString() ?: "-"),
                    DataRow("Número de Identificación del Votante", content["voterIdNumber"]?.toString() ?: "-"),
                    DataRow("Válido", "✓"),
                    DataRow("INE Encontrado", "✓")
                ),
                isRiskList = false
            )
        } else null
    }

    /**
     * Parsea datos de OFAC blacklist
     */
    private fun parseOfacBlacklist(item: Map<*, *>): BlacklistItem? {
        val evaluation = item["evaluation"] as? Map<*, *>
        val coincidences = evaluation?.get("coincidences") as? Map<*, *>
        val firstCoincidence = coincidences?.get("0") as? Map<*, *>
        val content = firstCoincidence?.get("content") as? Map<*, *>

        return if (content != null && content.isNotEmpty()) {
            // Hay coincidencia = RIESGO
            val name = content["name"]?.toString() ?: "Desconocido"
            val score = content["score"]?.toString() ?: "0"

            BlacklistItem(
                name = "OFAC",
                status = BlacklistStatus.RISK,
                description = "El usuario ha sido identificado en la lista de OFAC con coincidencia: $name (Score: $score). El usuario ha sido clasificado como de Riesgo en el proceso de verificación.",
                attempts = 1,
                processingTime = "N/A",
                detailedData = listOf(
                    DataRow("Nombre Coincidente", name),
                    DataRow("Score", score),
                    DataRow("País", content["country"]?.toString() ?: "-"),
                    DataRow("Listas de Programas", content["programLists"]?.toString() ?: "-"),
                    DataRow("Programas", content["programs"]?.toString() ?: "-"),
                    DataRow("Programa de Sanción", content["sanctionProgram"]?.toString() ?: "-"),
                    DataRow("Tipo de Búsqueda", content["lookupType"]?.toString() ?: "-")
                ),
                isRiskList = true
            )
        } else {
            // No hay coincidencia = CONFIABLE
            BlacklistItem(
                name = "OFAC",
                status = BlacklistStatus.RELIABLE,
                description = "El usuario no ha sido identificado en la lista de OFAC. El usuario ha sido clasificado como Confiable en el proceso de verificación.",
                attempts = 1,
                processingTime = "N/A",
                detailedData = null,
                isRiskList = true
            )
        }
    }

    /**
     * Parsea datos de SAT69B blacklist
     */
    private fun parseSat69bBlacklist(item: Map<*, *>): BlacklistItem? {
        val evaluation = item["evaluation"] as? Map<*, *>

        return if (evaluation.isNullOrEmpty()) {
            // Evaluation vacío = no encontrado = CONFIABLE
            BlacklistItem(
                name = "SAT69B",
                status = BlacklistStatus.RELIABLE,
                description = "El usuario no ha sido identificado en la lista de SAT69B. El usuario ha sido clasificado como Confiable en el proceso de verificación.",
                attempts = 1,
                processingTime = "N/A",
                detailedData = null,
                isRiskList = true
            )
        } else null
    }

    /**
     * Parsea datos de Interpol blacklist
     */
    private fun parseInterpolBlacklist(item: Map<*, *>): BlacklistItem? {
        val evaluation = item["evaluation"] as? Map<*, *>

        return if (evaluation.isNullOrEmpty()) {
            // Evaluation vacío = no encontrado = CONFIABLE
            BlacklistItem(
                name = "Interpol",
                status = BlacklistStatus.RELIABLE,
                description = "El usuario no ha sido identificado en la lista de Interpol. El usuario ha sido clasificado como Confiable en el proceso de verificación.",
                attempts = 1,
                processingTime = "N/A",
                detailedData = null,
                isRiskList = true
            )
        } else null
    }

    /**
     * Parsea datos de CURP blacklist
     */
    private fun parseCurpBlacklist(item: Map<*, *>): BlacklistItem? {
        val evaluation = item["evaluation"] as? Map<*, *>
        val curpData = evaluation?.get("curp") as? Map<*, *>
        val validaCurp = curpData?.get("validaCurp") as? Map<*, *>
        val content = validaCurp?.get("content") as? Map<*, *>

        return if (content != null && content.isNotEmpty()) {
            BlacklistItem(
                name = "CURP",
                status = BlacklistStatus.VALID,
                description = "El usuario ha sido identificado en bases oficiales de RENAPO, lo que respalda la autenticidad de su información.",
                attempts = 1,
                processingTime = "N/A",
                detailedData = listOf(
                    DataRow("Código de Error", content["codeError"]?.toString() ?: "-"),
                    DataRow("CURP", content["curp"]?.toString() ?: "-"),
                    DataRow("Error", content["error"]?.toString() ?: "-"),
                    DataRow("Mensaje de Error", content["errorMessage"]?.toString()?.takeIf { it.isNotBlank() } ?: "-"),
                    DataRow("Fecha de Nacimiento", content["personalDataBirthDate"]?.toString() ?: "-"),
                    DataRow("Entidad de Nacimiento", content["personalDataBirthEntity"]?.toString() ?: "-"),
                    DataRow("Nombre", content["personalDataFirstName"]?.toString() ?: "-"),
                    DataRow("Género", content["personalDataGender"]?.toString() ?: "-"),
                    DataRow("Apellido Paterno", content["personalDataLastName"]?.toString() ?: "-"),
                    DataRow("Nacionalidad", content["personalDataNationality"]?.toString() ?: "-"),
                    DataRow("Apellido Materno", content["personalDataSecondLastName"]?.toString() ?: "-"),
                    DataRow("Número de Acta", content["probativeDocActNumber"]?.toString() ?: "-"),
                    DataRow("Documento Probatorio", content["probativeDocProbativeDoc"]?.toString()?.takeIf { it.isNotBlank() } ?: "-"),
                    DataRow("Entidad de Registro", content["probativeDocRegistrationEntity"]?.toString() ?: "-"),
                    DataRow("Municipio de Registro", content["probativeDocRegistrationMunicipality"]?.toString() ?: "-"),
                    DataRow("Año de Registro", content["probativeDocRegistrationYear"]?.toString() ?: "-"),
                    DataRow("Válido", if (content["valid"].toString().lowercase() == "true") "✓" else "✗"),
                    DataRow("CURP Encontrado", "✓"),
                    DataRow("Encontrado", "✓"),
                    DataRow("Debe Estar Encontrado", "✓"),
                    DataRow("Debe Estar la CURP", "✓")
                ),
                isRiskList = false
            )
        } else null
    }
}
