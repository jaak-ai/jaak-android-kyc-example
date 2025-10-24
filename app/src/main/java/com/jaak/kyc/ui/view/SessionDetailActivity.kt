package com.jaak.kyc.ui.view

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.jaak.kyc.R
import com.jaak.kyc.databinding.ActivitySessionDetailBinding
import com.jaak.kyc.ui.viewmodel.SessionDetailState
import com.jaak.kyc.ui.viewmodel.SessionDetailViewModel
import com.jaak.kyc.ui.viewmodel.SessionTabType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SessionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionDetailBinding
    private val viewModel: SessionDetailViewModel by viewModels()

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        observeViewModel()

        // Obtener sessionId del intent
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        if (sessionId.isNullOrEmpty()) {
            Log.e("SessionDetailActivity", "Session ID no proporcionado en el intent")
            Toast.makeText(this, getString(R.string.error_invalid_session_id), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("SessionDetailActivity", "========== INICIANDO CARGA DE DETALLE ==========")
        Log.d("SessionDetailActivity", "Session ID: $sessionId")
        Log.d("SessionDetailActivity", "===============================================")

        // Cargar detalle de la sesión desde API
        viewModel.loadSessionDetail(sessionId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "" // Se actualizará cuando se carguen los datos
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is SessionDetailState.Loading -> {
                        Log.d("SessionDetailActivity", "Estado: LOADING - Mostrando indicador de carga")
                        binding.progressBar.visibility = View.VISIBLE
                        binding.tabLayout.visibility = View.GONE
                        binding.viewPager.visibility = View.GONE
                    }
                    is SessionDetailState.Success -> {
                        Log.d("SessionDetailActivity", "========== DETALLE CARGADO EXITOSAMENTE ==========")
                        Log.d("SessionDetailActivity", "Session: ${state.data.session.shortKey}")
                        Log.d("SessionDetailActivity", "Contact: ${state.data.session.contactName}")
                        Log.d("SessionDetailActivity", "Status: ${state.data.session.status}")
                        Log.d("SessionDetailActivity", "Score Total: ${state.data.summary.scores?.total}")
                        Log.d("SessionDetailActivity", "Flow Events: ${state.data.flow.size}")
                        Log.d("SessionDetailActivity", "=================================================")

                        binding.progressBar.visibility = View.GONE
                        binding.tabLayout.visibility = View.VISIBLE
                        binding.viewPager.visibility = View.VISIBLE

                        // Actualizar título del toolbar con ShortKey
                        supportActionBar?.title = state.data.session.shortKey ?: "N/A"

                        // Configurar tabs dinámicos
                        setupDynamicTabs()
                    }
                    is SessionDetailState.Error -> {
                        Log.e("SessionDetailActivity", "========== ERROR AL CARGAR DETALLE ==========")
                        Log.e("SessionDetailActivity", "Error: ${state.message}")
                        Log.e("SessionDetailActivity", "============================================")

                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@SessionDetailActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    private fun setupDynamicTabs() {
        lifecycleScope.launch {
            viewModel.tabs.collect { tabs ->
                if (tabs.isEmpty()) return@collect

                // Configurar adaptador del ViewPager2
                val adapter = SessionDetailPagerAdapter(this@SessionDetailActivity, tabs)
                binding.viewPager.adapter = adapter

                // Conectar TabLayout con ViewPager2
                TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                    tab.text = tabs[position].title
                }.attach()
            }
        }
    }

    /**
     * Cambia al tab específico por índice
     */
    fun switchToTab(index: Int) {
        binding.viewPager.currentItem = index
    }

    /**
     * Adaptador para ViewPager2 con tabs dinámicos
     */
    private class SessionDetailPagerAdapter(
        activity: AppCompatActivity,
        private val tabs: List<com.jaak.kyc.ui.viewmodel.SessionTab>
    ) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = tabs.size

        override fun createFragment(position: Int): Fragment {
            return when (tabs[position].type) {
                SessionTabType.INFO -> SessionInfoFragment()
                SessionTabType.DOCUMENT -> SessionFlowFragment() // Tab "Flujo"
                SessionTabType.LIVENESS -> SessionLivenessFragment() // Tab "Detalles"
                SessionTabType.OTO -> SessionOtoFragment()
            }
        }
    }
}
