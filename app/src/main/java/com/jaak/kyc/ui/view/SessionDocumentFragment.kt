package com.jaak.kyc.ui.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.jaak.kyc.R
import com.jaak.kyc.databinding.FragmentSessionDocumentBinding
import com.jaak.kyc.ui.viewmodel.SessionDetailViewModel
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
                    appendLine()
                    appendLine("📋 Evaluation:")

                    resource.meta?.extra?.evaluation?.forEach { (key, value) ->
                        appendLine("  • $key: $value")
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
