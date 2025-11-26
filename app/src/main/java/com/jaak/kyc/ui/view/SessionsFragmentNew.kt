package com.jaak.kyc.ui.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.jaak.kyc.R
import com.jaak.kyc.data.model.KycSessionItem
import com.jaak.kyc.data.model.KycStatus
import com.jaak.kyc.data.repository.KycSessionsRepository
import com.jaak.kyc.databinding.FragmentSessionsNewBinding
import com.jaak.kyc.databinding.BottomSheetSessionFiltersBinding
import com.jaak.kyc.ui.adapter.KycSessionsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SessionsFragmentNew : Fragment() {

    private var _binding: FragmentSessionsNewBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var sessionsRepository: KycSessionsRepository

    private lateinit var adapter: KycSessionsAdapter
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    // Job para controlar carga de sesiones
    private var loadSessionsJob: kotlinx.coroutines.Job? = null

    // Paginación
    private var currentPage = 1
    private var totalPages = 1
    private var isLoading = false
    private var canLoadMore = true

    // Filtros
    private var searchQuery: String? = null
    private var selectedFlowName: String? = null

    // Búsqueda avanzada
    private enum class SearchType {
        SHORT_KEY, CONTACT_NAME, FLOW_NAME, SESSION_ID
    }
    private var currentSearchType = SearchType.SHORT_KEY
    private var currentSearchQuery: String = ""

    // Lista de sesiones cargadas
    private val allSessions = mutableListOf<KycSessionItem>()
    private val filteredSessions get() = allSessions

    // Datos de prueba
    private val dummySessions = listOf(
        KycSessionItem(
            sessionID = "507f1f77bcf86cd799439011",
            shortkey = "FCtsxUJ",
            userName = "qa",
            flowName = "qa",
            dateTime = "09/10/2025 10:05",
            score = 0,
            documentStatus = "Documento",
            processStatus = "En proceso",
            kycStatus = KycStatus.PENDIENTE
        ),
        KycSessionItem(
            sessionID = "507f1f77bcf86cd799439012",
            shortkey = "xy6YkM5",
            userName = "qa",
            flowName = "qa",
            dateTime = "09/10/2025 10:01",
            score = 0,
            documentStatus = "Pendiente",
            processStatus = "Pendiente",
            kycStatus = KycStatus.PENDIENTE
        ),
        KycSessionItem(
            sessionID = "507f1f77bcf86cd799439013",
            shortkey = "qfWip9t",
            userName = "daniela",
            flowName = "qa test",
            dateTime = "09/10/2025 09:51",
            score = 0,
            documentStatus = "Caducado",
            processStatus = "Finalizado",
            kycStatus = KycStatus.EXPIRADO
        ),
        KycSessionItem(
            sessionID = "507f1f77bcf86cd799439014",
            shortkey = "bQcpnRe",
            userName = "qa",
            flowName = "qa",
            dateTime = "09/10/2025 09:32",
            score = 0,
            documentStatus = "Caducado",
            processStatus = "Finalizado",
            kycStatus = KycStatus.EXPIRADO
        ),
        KycSessionItem(
            sessionID = "507f1f77bcf86cd799439015",
            shortkey = "1SHmP2M",
            userName = "test",
            flowName = "test9",
            dateTime = "08/10/2025 19:08",
            score = 100,
            documentStatus = "in_review",
            processStatus = "Finalizado",
            kycStatus = KycStatus.EXITOSO
        ),
        KycSessionItem(
            sessionID = "507f1f77bcf86cd799439016",
            shortkey = "aI8xA9r",
            userName = "io",
            flowName = "io",
            dateTime = "08/10/2025 18:00",
            score = 0,
            documentStatus = "Rechazado",
            processStatus = "Finalizado",
            kycStatus = KycStatus.RECHAZADO
        ),
        KycSessionItem(
            sessionID = "507f1f77bcf86cd799439017",
            shortkey = "XJIQ9aU",
            userName = "andr",
            flowName = "andr",
            dateTime = "08/10/2025 17:49",
            score = 100,
            documentStatus = "Exitoso",
            processStatus = "Finalizado",
            kycStatus = KycStatus.EXITOSO
        )
    )

    // Filtros de fecha y hora
    private var startDateMillis: Long? = null
    private var startHour: Int? = null
    private var startMinute: Int? = null
    private var endDateMillis: Long? = null
    private var endHour: Int? = null
    private var endMinute: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionsNewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupListeners()
        setupScrollListener()
        setupSwipeRefresh()
        loadSessions(page = 1, clearList = true)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            R.color.jaak_button_start_enabled,
            R.color.jaak_success,
            R.color.jaak_primary
        )

        binding.swipeRefresh.setOnRefreshListener {
            Log.d("SessionsFragment", "🔄 Pull-to-refresh activado")
            loadSessions(page = 1, clearList = true)
        }
    }

    private fun setupRecyclerView() {
        adapter = KycSessionsAdapter { session ->
            onSessionClick(session)
        }
        binding.rvSessions.adapter = adapter
    }

    private fun setupSearchBar() {
        // Ocultar chips inicialmente
        binding.chipGroupSearchType.visibility = View.GONE

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                
                if (query.isEmpty()) {
                    // Sin texto: ocultar filtros y restaurar lista
                    binding.chipGroupSearchType.visibility = View.GONE
                    currentSearchQuery = ""
                    searchQuery = null
                    loadSessions(page = 1, clearList = true)
                    Log.d("SessionsFragment", "🔄 Texto borrado - Lista restaurada")
                } else if (query.length == 1) {
                    // Con 1 carácter: mostrar filtros y buscar con el primero
                    binding.chipGroupSearchType.visibility = View.VISIBLE
                    currentSearchQuery = query
                    performAutomaticSearch()
                    Log.d("SessionsFragment", "🔍 1er carácter - Mostrando filtros y buscando")
                } else {
                    // Con más caracteres: actualizar query y buscar con filtro actual
                    currentSearchQuery = query
                    performAutomaticSearch()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Configurar chips como checkable (toggle on/off)
        setupSearchChips()
    }

    /**
     * Configurar comportamiento de chips: toggle on/off
     * Solo uno puede estar activo a la vez
     * Al seleccionar uno, buscar automáticamente
     */
    private fun setupSearchChips() {
        // Por defecto, ShortKey está seleccionado
        binding.chipShortKey.isChecked = true
        currentSearchType = SearchType.SHORT_KEY

        // Listener para ShortKey
        binding.chipShortKey.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                currentSearchType = SearchType.SHORT_KEY
                // Desmarcar otros
                binding.chipContactName.isChecked = false
                binding.chipFlowName.isChecked = false
                binding.chipSessionId.isChecked = false
                // Buscar automáticamente si hay texto
                if (currentSearchQuery.isNotEmpty()) {
                    performAutomaticSearch()
                }
            }
        }

        // Listener para ContactName
        binding.chipContactName.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                currentSearchType = SearchType.CONTACT_NAME
                // Desmarcar otros
                binding.chipShortKey.isChecked = false
                binding.chipFlowName.isChecked = false
                binding.chipSessionId.isChecked = false
                // Buscar automáticamente si hay texto
                if (currentSearchQuery.isNotEmpty()) {
                    performAutomaticSearch()
                }
            }
        }

        // Listener para FlowName
        binding.chipFlowName.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                currentSearchType = SearchType.FLOW_NAME
                // Desmarcar otros
                binding.chipShortKey.isChecked = false
                binding.chipContactName.isChecked = false
                binding.chipSessionId.isChecked = false
                // Buscar automáticamente si hay texto
                if (currentSearchQuery.isNotEmpty()) {
                    performAutomaticSearch()
                }
            }
        }

        // Listener para SessionId
        binding.chipSessionId.setOnCheckedChangeListener { chip, isChecked ->
            if (isChecked) {
                currentSearchType = SearchType.SESSION_ID
                // Desmarcar otros
                binding.chipShortKey.isChecked = false
                binding.chipContactName.isChecked = false
                binding.chipFlowName.isChecked = false
                // Buscar automáticamente si hay texto
                if (currentSearchQuery.isNotEmpty()) {
                    performAutomaticSearch()
                }
            }
        }
    }



    private fun setupListeners() {
        // Botón + para agregar nueva sesión (Creación Manual)
        binding.fabAddSession.setOnClickListener {
            // Abrir formulario de perfil en modo manual (sin guardar)
            val intent = Intent(requireContext(), EditSessionProfileActivity::class.java)
            intent.putExtra("MODE_MANUAL", true)
            startActivity(intent)
        }

        // Botón de filtros
        binding.btnFilters.setOnClickListener {
            showFiltersDialog()
        }
    }

    private fun performSearch(query: String) {
        searchQuery = query.trim().ifEmpty { null }

        // Recargar sesiones con el nuevo filtro de búsqueda
        loadSessions(page = 1, clearList = true)
    }

    /**
     * Búsqueda automática según el filtro seleccionado
     * Se ejecuta al escribir o al cambiar de filtro
     */
    private fun performAutomaticSearch() {
        if (currentSearchQuery.isEmpty()) {
            return
        }

        Log.d("SessionsFragment", "🔍 Búsqueda automática: tipo=$currentSearchType, query=$currentSearchQuery")

        // Construir query según el tipo seleccionado
        searchQuery = buildSearchQuery(currentSearchQuery, currentSearchType)
        
        // Realizar búsqueda
        currentPage = 1
        loadSessions(page = 1, clearList = true)
    }

    /**
     * Construye el query de búsqueda según el tipo
     */
    private fun buildSearchQuery(query: String, type: SearchType): String {
        return when (type) {
            SearchType.SHORT_KEY -> query // Buscar por shortkey
            SearchType.CONTACT_NAME -> query // El backend debe filtrar por contactName
            SearchType.FLOW_NAME -> query // El backend debe filtrar por flowName
            SearchType.SESSION_ID -> query // El backend debe filtrar por sessionId
        }
    }

    private fun showFiltersDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val filterBinding = BottomSheetSessionFiltersBinding.inflate(layoutInflater)

        // Mostrar valores actuales
        updateDateTimeDisplays(filterBinding)

        // Switch para incluir fecha final
        filterBinding.switchIncludeEndDate.isChecked = endDateMillis != null
        filterBinding.layoutEndDate.visibility = if (endDateMillis != null) View.VISIBLE else View.GONE

        filterBinding.switchIncludeEndDate.setOnCheckedChangeListener { _, isChecked ->
            filterBinding.layoutEndDate.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                // Limpiar fecha final si se desactiva el switch
                endDateMillis = null
                endHour = null
                endMinute = null
                updateDateTimeDisplays(filterBinding)
            }
        }

        // Click en Fecha Inicial - Abrir DatePicker simple
        filterBinding.cardStartDate.setOnClickListener {
            showSimpleDatePicker { selectedDateMillis ->
                startDateMillis = selectedDateMillis
                updateDateTimeDisplays(filterBinding)
                Log.d("SessionsFragment", "📅 Fecha inicio seleccionada: ${formatDateFromMillis(selectedDateMillis)}")
            }
        }

        // Click en Hora Inicial
        filterBinding.cardStartTime.setOnClickListener {
            showTimePicker(startHour ?: 0, startMinute ?: 0) { hour, minute ->
                startHour = hour
                startMinute = minute
                updateDateTimeDisplays(filterBinding)
            }
        }

        // Click en Fecha Final - Abrir DatePicker simple
        filterBinding.cardEndDate.setOnClickListener {
            showSimpleDatePicker { selectedDateMillis ->
                endDateMillis = selectedDateMillis
                updateDateTimeDisplays(filterBinding)
                Log.d("SessionsFragment", "📅 Fecha fin seleccionada: ${formatDateFromMillis(selectedDateMillis)}")
            }
        }

        // Click en Hora Final
        filterBinding.cardEndTime.setOnClickListener {
            showTimePicker(endHour ?: 0, endMinute ?: 0) { hour, minute ->
                endHour = hour
                endMinute = minute
                updateDateTimeDisplays(filterBinding)
            }
        }

        // Botón limpiar filtros
        filterBinding.btnClearFilters.setOnClickListener {
            clearDateTimeFilters()
            updateDateTimeDisplays(filterBinding)
        }

        // Botón aplicar
        filterBinding.btnApplyFilters.setOnClickListener {
            if (validateDateTimeFilters()) {
                applyFilters()
                dialog.dismiss()
            }
        }

        dialog.setContentView(filterBinding.root)
        dialog.show()
    }

    /**
     * Muestra DatePicker simple para seleccionar UNA fecha
     */
    private fun showSimpleDatePicker(onDateSelected: (Long) -> Unit) {
        val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha")
            .setSelection(com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selectedDateMillis ->
            onDateSelected(selectedDateMillis)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun formatDateFromMillis(millis: Long?): String {
        if (millis == null) return "N/A"
        // MaterialDatePicker devuelve fecha en UTC medianoche, mantener en UTC al formatear
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return dateFormat.format(java.util.Date(millis))
    }

    private fun showDatePicker(onDateSet: (year: Int, month: Int, dayOfMonth: Int) -> Unit) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                onDateSet(selectedYear, selectedMonth, selectedDay)
            },
            year,
            month,
            day
        ).show()
    }

    private fun showTimePicker(currentHour: Int, currentMinute: Int, onTimeSet: (hour: Int, minute: Int) -> Unit) {
        android.app.TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                onTimeSet(selectedHour, selectedMinute)
            },
            currentHour,
            currentMinute,
            true // Formato 24 horas
        ).show()
    }

    private fun updateDateTimeDisplays(binding: BottomSheetSessionFiltersBinding) {
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())

        // Fecha Inicial
        binding.tvStartDate.text = if (startDateMillis != null) {
            dateFormat.format(java.util.Date(startDateMillis!!))
        } else {
            "--/--/----"
        }

        // Hora Inicial
        binding.tvStartTime.text = if (startHour != null && startMinute != null) {
            String.format("%02d:%02d", startHour, startMinute)
        } else {
            "--:--"
        }

        // Fecha Final
        binding.tvEndDate.text = if (endDateMillis != null) {
            dateFormat.format(java.util.Date(endDateMillis!!))
        } else {
            "--/--/----"
        }

        // Hora Final
        binding.tvEndTime.text = if (endHour != null && endMinute != null) {
            String.format("%02d:%02d", endHour, endMinute)
        } else {
            "--:--"
        }
    }

    private fun validateDateTimeFilters(): Boolean {
        // Si no hay filtros, es válido
        if (startDateMillis == null && endDateMillis == null) {
            return true
        }

        // La fecha de inicio es obligatoria, pero la fecha final es opcional
        if (startDateMillis == null && endDateMillis != null) {
            Toast.makeText(requireContext(), "Debe seleccionar la fecha de inicio", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar que la fecha final no sea anterior a la inicial (solo si ambas existen)
        if (startDateMillis != null && endDateMillis != null && endDateMillis!! < startDateMillis!!) {
            Toast.makeText(requireContext(), getString(R.string.error_end_date_before_start), Toast.LENGTH_SHORT).show()
            return false
        }

        // Si es el mismo día y ambas horas están definidas, validar que la hora final sea posterior
        if (startDateMillis != null && endDateMillis != null &&
            startDateMillis == endDateMillis &&
            startHour != null && endHour != null) {
            val startTotalMinutes = (startHour ?: 0) * 60 + (startMinute ?: 0)
            val endTotalMinutes = (endHour ?: 0) * 60 + (endMinute ?: 0)

            if (endTotalMinutes <= startTotalMinutes) {
                Toast.makeText(requireContext(), getString(R.string.error_end_time_before_start), Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun clearDateTimeFilters() {
        startDateMillis = null
        startHour = null
        startMinute = null
        endDateMillis = null
        endHour = null
        endMinute = null
    }

    private fun applyFilters() {
        // Recargar sesiones y aplicar filtro de fechas localmente
        loadSessions(page = 1, clearList = true)
    }

    private fun updateSessionsList(sessions: List<KycSessionItem>) {
        if (sessions.isEmpty()) {
            binding.rvSessions.visibility = View.GONE
            binding.llEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvSessions.visibility = View.VISIBLE
            binding.llEmptyState.visibility = View.GONE
            adapter.submitList(sessions.toList())
        }
    }

    private fun setupScrollListener() {
        binding.rvSessions.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(1) && !isLoading && canLoadMore) {
                    // Llegó al final, cargar más
                    Log.d("SessionsFragment", "🔽 Usuario llegó al final, cargando siguiente página...")
                    loadSessions(page = currentPage + 1, clearList = false)
                } else if (!recyclerView.canScrollVertically(1) && !canLoadMore) {
                    Log.d("SessionsFragment", "🏁 Usuario llegó al final pero no hay más páginas")
                }
            }
        })
    }

    private fun loadSessions(page: Int, clearList: Boolean) {
        if (isLoading) return

        // Cancelar job anterior si existe
        loadSessionsJob?.cancel()

        isLoading = true

        // Mostrar loading indicator para paginación
        if (!clearList) {
            binding.progressBarPagination.visibility = View.VISIBLE
            Log.d("SessionsFragment", "📥 Cargando página $page...")
        }

        Log.d("SessionsFragment", "Filtros - SearchQuery: $searchQuery, SearchType: ${currentSearchType.name}")

        loadSessionsJob = lifecycleScope.launch {
            val result = sessionsRepository.getSessions(
                page = page,
                limit = 20,
                searchQuery = searchQuery,
                searchType = currentSearchType.name,
                flowName = selectedFlowName,
                minCreatedAt = null,
                maxCreatedAt = null
            )

            result.onSuccess { sessionsPage ->
                Log.d("SessionsFragment", "✅ Cargadas ${sessionsPage.sessions.size} sesiones (página $page)")
                Log.d("SessionsFragment", "   Total en lista: ${allSessions.size + sessionsPage.sessions.size}")
                Log.d("SessionsFragment", "   Hay más páginas: ${sessionsPage.hasNextPage}")

                if (clearList) {
                    allSessions.clear()
                }
                allSessions.addAll(sessionsPage.sessions)

                currentPage = sessionsPage.currentPage
                totalPages = sessionsPage.totalPages
                canLoadMore = sessionsPage.hasNextPage

                // Proteger actualización de UI con try-catch por si el fragment ya no existe
                try {
                    if (_binding != null && isAdded) {
                        // Ocultar loading indicators
                        binding.progressBarPagination.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false

                        // Aplicar filtro de fechas local
                        val filteredList = applyLocalDateFilter(allSessions)
                        updateSessionsList(filteredList)

                        // Mostrar Toast informativo si se cargaron más páginas
                        if (!clearList && sessionsPage.sessions.isNotEmpty()) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.sessions_loaded_more, sessionsPage.sessions.size),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SessionsFragment", "Error actualizando UI: ${e.message}")
                }

                // Log si no hay más páginas
                if (!sessionsPage.hasNextPage) {
                    Log.d("SessionsFragment", "🏁 No hay más páginas disponibles")
                }
            }.onFailure { error ->
                Log.e("SessionsFragment", "❌ Error loading sessions: ${error.message}", error)

                // Proteger actualización de UI
                try {
                    if (_binding != null && isAdded) {
                        binding.progressBarPagination.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        Toast.makeText(requireContext(), getString(R.string.error_loading_sessions, error.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("SessionsFragment", "Error mostrando mensaje de error: ${e.message}")
                }
            }

            isLoading = false
        }
    }

    private fun onSessionClick(session: KycSessionItem) {
        // Navegar a pantalla de detalles usando sessionID (MongoDB ObjectID)
        val intent = Intent(requireContext(), SessionDetailActivity::class.java)
        intent.putExtra(SessionDetailActivity.EXTRA_SESSION_ID, session.sessionID)
        startActivity(intent)
    }

    private fun formatToISO8601(dateMillis: Long, hour: Int, minute: Int): String {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = dateMillis
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return isoFormat.format(calendar.time)
    }

    /**
     * Filtra las sesiones por rango de fechas localmente
     * Formato esperado de dateTime: "dd/MM/yyyy HH:mm"
     */
    private fun applyLocalDateFilter(sessions: List<KycSessionItem>): List<KycSessionItem> {
        // Si no hay filtro de fecha, retornar todas
        if (startDateMillis == null) {
            return sessions
        }

        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        
        return sessions.filter { session ->
            try {
                // Parsear la fecha de la sesión
                val sessionDate = dateFormat.parse(session.dateTime)
                
                if (sessionDate != null) {
                    val sessionMillis = sessionDate.time
                    
                    // Crear fecha de inicio con hora
                    val startCalendar = java.util.Calendar.getInstance()
                    startCalendar.timeInMillis = startDateMillis!!
                    startCalendar.set(java.util.Calendar.HOUR_OF_DAY, startHour ?: 0)
                    startCalendar.set(java.util.Calendar.MINUTE, startMinute ?: 0)
                    startCalendar.set(java.util.Calendar.SECOND, 0)
                    startCalendar.set(java.util.Calendar.MILLISECOND, 0)
                    val startMillis = startCalendar.timeInMillis
                    
                    // Si solo hay fecha inicial, buscar desde esa fecha en adelante
                    if (endDateMillis == null) {
                        sessionMillis >= startMillis
                    } else {
                        // Crear fecha final con hora
                        val endCalendar = java.util.Calendar.getInstance()
                        endCalendar.timeInMillis = endDateMillis!!
                        endCalendar.set(java.util.Calendar.HOUR_OF_DAY, endHour ?: 23)
                        endCalendar.set(java.util.Calendar.MINUTE, endMinute ?: 59)
                        endCalendar.set(java.util.Calendar.SECOND, 59)
                        endCalendar.set(java.util.Calendar.MILLISECOND, 999)
                        val endMillis = endCalendar.timeInMillis
                        
                        // Verificar que esté en el rango
                        sessionMillis in startMillis..endMillis
                    }
                } else {
                    // Si no se puede parsear, incluir por defecto
                    true
                }
            } catch (e: Exception) {
                Log.e("SessionsFragment", "Error parseando fecha: ${session.dateTime}", e)
                // Si hay error, incluir por defecto
                true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancelar búsqueda pendiente
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        // Cancelar carga de sesiones en progreso
        loadSessionsJob?.cancel()
        // Limpiar binding
        _binding = null
    }
}
