package ai.jaak.kyc.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import ai.jaak.kyc.R
import ai.jaak.kyc.databinding.FragmentSessionDocumentBinding
import ai.jaak.kyc.ui.viewmodel.SessionDetailViewModel
import kotlinx.coroutines.launch

class SessionDocumentFragment : Fragment() {

    private var _binding: FragmentSessionDocumentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionDetailViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionDocumentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadDocumentData()
    }

    private fun loadDocumentData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val event = viewModel.getFlowEventByAction("document-process")
            if (event != null && event.flow?.isNotEmpty() == true) {
                val resource = event.flow.first()

                val info = buildString {
                    appendLine("📄 Resource: ${resource.resource}")
                    appendLine("📊 Score: ${formatScore(resource.score)}")
                    appendLine("✅ Status: ${resource.status ?: "N/A"}")

                    // Mostrar tiempo de procesamiento desde meta.processTime
                    val processTime = resource.meta?.processTime
                    val processingTimeStr = if (processTime != null) {
                        val seconds = processTime / 1000.0 // Convertir milisegundos a segundos
                        String.format("%.2fs", seconds)
                    } else {
                        "N/A"
                    }
                    appendLine("⏱️ Tiempo de proceso: $processingTimeStr")

                    val evaluation = resource.meta?.extra?.evaluation
                    
                    // Extraer nacionalidad del objeto country dentro del document
                    val nationality = extractNationality(evaluation)

                    appendLine()
                    appendLine("📋 Evaluation:")
                    
                    if (nationality != null) {
                        appendLine("  • Nacionalidad: $nationality")
                    }

                    evaluation?.forEach { (key, value) ->
                        when (key) {
                            "document" -> {
                                // Procesar información del documento sin repetir nacionalidad
                            }
                            else -> appendLine("  • $key: $value")
                        }
                    }

                    appendLine()
                    appendLine("🕒 Creado: ${event.createdAt ?: "N/A"}")
                    appendLine("🆔 Event ID: ${event.eventId ?: "N/A"}")
                }

                binding.tvDocumentContent.text = info
            } else {
                binding.tvDocumentContent.text = getString(R.string.document_no_data_available)
            }
        }
    }
    
    private fun extractNationality(evaluation: Map<String, Any>?): String? {
        try {
            val document = evaluation?.get("document") as? Map<*, *> ?: return null
            val country = document["country"] as? Map<*, *> ?: return null
            val isoCode = country["isoAlpha2Code"] as? String ?: return null
            
            return when (isoCode.uppercase()) {
                "MX" -> getString(R.string.nationality_mexican)
                "AR" -> getString(R.string.nationality_argentinian)
                "PE" -> getString(R.string.nationality_peruvian)
                "CO" -> getString(R.string.nationality_colombian)
                "US" -> getString(R.string.nationality_american)
                "ES" -> getString(R.string.nationality_spanish)
                else -> getString(R.string.nationality_unknown)
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun formatScore(score: Double?): String {
        return if (score != null) {
            "${(score * 100).toInt()}%"
        } else {
            "N/A"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
