package ai.jaak.kyc.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ai.jaak.kyc.databinding.FragmentSessionFlowBinding
import ai.jaak.kyc.ui.adapter.EventsAdapter
import ai.jaak.kyc.ui.viewmodel.SessionDetailState
import ai.jaak.kyc.ui.viewmodel.SessionDetailViewModel
import kotlinx.coroutines.launch

class SessionFlowFragment : Fragment() {

    private var _binding: FragmentSessionFlowBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionDetailViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionFlowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvFlowEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state is SessionDetailState.Success) {
                    // Filtrar eventos: agrupar todos los "verify-blacklist" en uno solo
                    val events = state.data.flow
                    val filteredEvents = filterAndGroupEvents(events)

                    val adapter = EventsAdapter(filteredEvents) { event ->
                        val action = event.action.lowercase()
                        val eventId = event.eventId ?: return@EventsAdapter

                        // Log para debug
                        android.util.Log.d("SessionFlowFragment", "Clicked event: action=$action, eventId=$eventId")

                        // Obtener datos específicos del evento desde el ViewModel
                        val intent = when (action) {
                            "best-frame-verify-liveness" -> {
                                val data = viewModel.getLivenessData(eventId)
                                Intent(requireContext(), LivenessDetailActivity::class.java).apply {
                                    putExtra("liveness_data", data)
                                }
                            }
                            "v2-verify-oto" -> {
                                val data = viewModel.getOtoBasicData(eventId)
                                Intent(requireContext(), OtoDetailActivity::class.java).apply {
                                    putExtra("oto_data", data)
                                }
                            }
                            "verify-blacklist" -> {
                                val data = viewModel.getBlacklistData(eventId)
                                Intent(requireContext(), BlacklistDetailActivity::class.java).apply {
                                    putExtra("blacklist_data", data)
                                }
                            }
                            "front-verify-document" -> {
                                val data = viewModel.getDocumentDetailData(eventId)
                                Intent(requireContext(), DocumentDetailActivity::class.java).apply {
                                    putExtra("document_data", data)
                                }
                            }
                            "complet-extract-document" -> {
                                val data = viewModel.getDocumentExtractData(eventId)
                                Intent(requireContext(), DocumentExtractActivity::class.java).apply {
                                    putExtra("extract_data", data)
                                }
                            }
                            // Fallback para otros actions posibles
                            "liveness-process", "liveness" -> {
                                val data = viewModel.getLivenessData(eventId)
                                Intent(requireContext(), LivenessDetailActivity::class.java).apply {
                                    putExtra("liveness_data", data)
                                }
                            }
                            "oto-process", "one-to-one-process" -> {
                                val data = viewModel.getOtoBasicData(eventId)
                                Intent(requireContext(), OtoDetailActivity::class.java).apply {
                                    putExtra("oto_data", data)
                                }
                            }
                            "blacklist", "blacklist-check" -> {
                                val data = viewModel.getBlacklistData(eventId)
                                Intent(requireContext(), BlacklistDetailActivity::class.java).apply {
                                    putExtra("blacklist_data", data)
                                }
                            }
                            "document-process" -> {
                                val data = viewModel.getDocumentDetailData(eventId)
                                Intent(requireContext(), DocumentDetailActivity::class.java).apply {
                                    putExtra("document_data", data)
                                }
                            }
                            else -> {
                                android.util.Log.e("SessionFlowFragment", "No activity found for action: ${event.action}")
                                null
                            }
                        }

                        intent?.let {
                            startActivity(it)
                        } ?: run {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "No se pudo abrir el detalle del evento",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    binding.rvFlowEvents.adapter = adapter
                }
            }
        }
    }

    /**
     * Filtra y agrupa eventos: mantiene solo 1 evento "verify-blacklist"
     * aunque haya múltiples en el flow
     */
    private fun filterAndGroupEvents(events: List<ai.jaak.kyc.data.model.api.SessionDetailFlowEvent>): List<ai.jaak.kyc.data.model.api.SessionDetailFlowEvent> {
        val filteredList = mutableListOf<ai.jaak.kyc.data.model.api.SessionDetailFlowEvent>()
        var blacklistEventAdded = false

        events.forEach { event ->
            when (event.action.lowercase()) {
                "verify-blacklist" -> {
                    // Solo agregar el primer evento de blacklist
                    if (!blacklistEventAdded) {
                        filteredList.add(event)
                        blacklistEventAdded = true
                    }
                }
                else -> {
                    // Agregar todos los demás eventos normalmente
                    filteredList.add(event)
                }
            }
        }

        return filteredList
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
