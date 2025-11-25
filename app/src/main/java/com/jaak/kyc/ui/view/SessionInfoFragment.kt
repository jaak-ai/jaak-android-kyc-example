package com.jaak.kyc.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaak.kyc.R
import com.jaak.kyc.databinding.FragmentSessionInfoBinding
import com.jaak.kyc.ui.adapter.EventsAdapter
import com.jaak.kyc.ui.viewmodel.SessionDetailState
import com.jaak.kyc.ui.viewmodel.SessionDetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SessionInfoFragment : Fragment() {

    private var _binding: FragmentSessionInfoBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionDetailViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvRecentEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            // Adapter se configurará cuando haya datos
        }

        // "Ver todos" cambia al tab Flujo
        binding.tvViewAllEvents.setOnClickListener {
            // Cambiar al tab "Flujo" (index 1)
            (activity as? SessionDetailActivity)?.switchToTab(1)
        }

        // Click listeners en puntajes rápidos - mostrar detalle expandido
        binding.cvScoreDocument.setOnClickListener {
            showScoreDetail("document")
        }

        binding.cvScoreLiveness.setOnClickListener {
            showScoreDetail("liveness")
        }

        binding.cvScoreOto.setOnClickListener {
            showScoreDetail("oto")
        }
    }

    /**
     * Muestra el detalle de un puntaje específico
     */
    private fun showScoreDetail(scoreType: String) {
        android.util.Log.d("SessionInfoFragment", "Mostrando detalle de puntaje: $scoreType")
        
        // Cambiar al tab de Resumen (index 2) que tiene el detalle completo de puntajes
        (activity as? SessionDetailActivity)?.switchToTab(2)
        
        // TODO: Si hay una sección específica para cada score, desplazarse a ella
        // Por ahora solo cambiamos al tab de Resumen donde está "Ver todos"
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state is SessionDetailState.Success) {
                    populateUI(state)
                }
            }
        }
    }

    private fun populateUI(state: SessionDetailState.Success) {
        val session = state.data.session
        val summary = state.data.summary
        val flow = state.data.flow

        // ========== INFORMACIÓN DEL USUARIO ==========
        // Foto (URL de Google Cloud Storage)
        if (!summary.photo.isNullOrEmpty()) {
            android.util.Log.d("SessionInfoFragment", "Cargando foto desde URL: ${summary.photo}")

            // Cargar imagen desde URL usando Picasso
            com.squareup.picasso.Picasso.get()
                .load(summary.photo)
                .placeholder(R.drawable.ic_person) // Mientras carga
                .error(R.drawable.ic_person) // Si falla
                .into(binding.ivUserPhoto)
        } else {
            android.util.Log.d("SessionInfoFragment", "Photo URL es null o vacío")
            binding.ivUserPhoto.setImageResource(R.drawable.ic_person)
        }

        // Nombre completo
        val fullName = "${summary.name ?: ""} ${summary.lastName ?: ""}".trim()
        binding.tvUserName.text = fullName.ifEmpty { getString(R.string.user_no_name) }

        // Tiempo total
        val timeText = if (summary.totalTime != null) {
            getString(R.string.info_total_time_format, formatDuration(summary.totalTime))
        } else {
            getString(R.string.info_total_time_na)
        }
        binding.tvTotalTime.text = timeText

        // ========== ESTADO ==========
        // Estado (state)
        val stateText = session.statusDetail?.state?.let { translateState(it) } ?: "N/A"
        binding.tvState.text = stateText

        // Etapa (stage) con color
        val stage = session.statusDetail?.stage ?: "N/A"
        binding.tvStage.text = translateStage(stage)

        val stageColor = when (stage.lowercase()) {
            "approved", "aprobado", "pass" -> R.color.jaak_success
            "rejected", "rechazado", "fail" -> R.color.jaak_error
            "pending", "pendiente" -> R.color.jaak_warning
            else -> R.color.jaak_text_secondary
        }
        binding.tvStage.setTextColor(ContextCompat.getColor(requireContext(), stageColor))

        // ========== INFO DEL FLUJO KYC ==========
        // Status icon (checkmark o cross)
        val status = summary.scores?.status ?: "unknown"
        when (status.lowercase()) {
            "pass" -> {
                binding.tvFlowStatusIcon.text = "✓"
                binding.tvFlowStatusIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.jaak_success))
                binding.cvFlowStatusIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.jaak_success_light))
            }
            "fail" -> {
                binding.tvFlowStatusIcon.text = "✗"
                binding.tvFlowStatusIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.jaak_error))
                binding.cvFlowStatusIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.warning_light))
            }
            else -> {
                binding.tvFlowStatusIcon.text = "?"
                binding.tvFlowStatusIcon.setTextColor(ContextCompat.getColor(requireContext(), R.color.jaak_text_tertiary))
                binding.cvFlowStatusIcon.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.jaak_background))
            }
        }

        // Nombre del flujo (shortkey)
        binding.tvFlowName.text = session.shortKey ?: "N/A"

        // Subtitle (puede ser el nombre del contacto o flowName)
        binding.tvFlowSubtitle.text = session.contactName ?: getString(R.string.info_default_subtitle)

        // Label del flujo
        binding.tvFlowLabel.text = getString(R.string.info_flow_label_format, session.flowName ?: "N/A")

        // Puntaje total con color
        val totalScore = summary.scores?.total
        binding.tvFlowScore.text = formatScore(totalScore)

        val scoreColor = when {
            totalScore == null -> R.color.jaak_text_secondary
            totalScore >= 80.0 -> R.color.jaak_success
            totalScore >= 50.0 -> R.color.jaak_warning
            else -> R.color.jaak_error
        }
        binding.tvFlowScore.setTextColor(ContextCompat.getColor(requireContext(), scoreColor))

        // Fecha de creación
        binding.tvCreatedAt.text = formatDate(session.createdAt)

        // Duración
        binding.tvDuration.text = if (summary.totalTime != null) {
            formatDuration(summary.totalTime)
        } else {
            "N/A"
        }

        // ========== PUNTAJES RÁPIDOS ==========
        val scores = summary.scores

        // Score Documento
        val docScore = scores?.document
        binding.tvScoreDocument.text = formatScore(docScore)
        applyScoreColors(binding.cvScoreDocument, binding.tvScoreDocument, docScore)

        // Score Liveness
        val livenessScore = scores?.liveness
        binding.tvScoreLiveness.text = formatScore(livenessScore)
        applyScoreColors(binding.cvScoreLiveness, binding.tvScoreLiveness, livenessScore)

        // Score OTO (1:1)
        val otoScore = scores?.oneToOne
        binding.tvScoreOto.text = formatScore(otoScore)
        applyScoreColors(binding.cvScoreOto, binding.tvScoreOto, otoScore)

        // ========== EVENTOS RECIENTES ==========
        val recentEvents = flow.take(3)
        // Eventos recientes es solo informativo, sin acciones individuales
        val adapter = EventsAdapter(recentEvents, null)
        binding.rvRecentEvents.adapter = adapter
    }

    /**
     * Aplica colores a las cards de puntajes según el valor
     * La API devuelve scores en escala 0-100
     */
    private fun applyScoreColors(
        cardView: com.google.android.material.card.MaterialCardView,
        textView: android.widget.TextView,
        score: Double?
    ) {
        val (bgColor, textColor) = when {
            score == null -> Pair(R.color.jaak_surface, R.color.jaak_text_secondary)
            score >= 80.0 -> Pair(R.color.jaak_success_light, R.color.jaak_success)
            score >= 50.0 -> Pair(R.color.warning_light, R.color.jaak_warning)
            else -> Pair(R.color.warning_light, R.color.jaak_error)
        }

        cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), bgColor))
        textView.setTextColor(ContextCompat.getColor(requireContext(), textColor))
    }

    /**
     * Formatea un score a porcentaje (100.0 -> "100.0%")
     * La API ya devuelve los scores en escala 0-100, no 0-1
     */
    private fun formatScore(score: Double?): String {
        return if (score != null) {
            String.format(Locale.US, "%.1f%%", score)
        } else {
            "N/A"
        }
    }

    /**
     * Formatea duración en minutos a formato "Xm Ys"
     */
    private fun formatDuration(minutes: Double): String {
        val totalSeconds = (minutes * 60).toInt()
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return "${mins}m ${secs}s"
    }

    /**
     * Formatea fecha ISO 8601 a formato legible
     */
    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "N/A"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)

            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            outputFormat.format(date ?: return "N/A")
        } catch (e: Exception) {
            // Intentar otro formato sin milisegundos
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateString)

                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                outputFormat.format(date ?: return "N/A")
            } catch (e: Exception) {
                dateString
            }
        }
    }

    /**
     * Traduce el estado a español
     */
    private fun translateState(state: String): String {
        return when (state.lowercase()) {
            "completed" -> "Finalizado"
            "pending" -> "Pendiente"
            "in_progress" -> "En Progreso"
            "failed" -> "Fallido"
            else -> state
        }
    }

    /**
     * Traduce la etapa a español
     */
    private fun translateStage(stage: String): String {
        return when (stage.lowercase()) {
            "approved", "pass" -> "Aprobado"
            "rejected", "fail" -> "Rechazado"
            "pending" -> "Pendiente"
            "review" -> "En Revisión"
            else -> stage
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
