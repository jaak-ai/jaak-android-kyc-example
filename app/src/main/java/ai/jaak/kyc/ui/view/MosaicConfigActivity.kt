package ai.jaak.kyc.ui.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import ai.jaak.kyc.R
import ai.jaak.kyc.data.model.MosaicConfig
import ai.jaak.kyc.data.model.MosaicModule
import ai.jaak.kyc.databinding.ActivityMosaicConfigBinding
import ai.jaak.kyc.ui.adapter.MosaicModuleAdapter
import ai.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MosaicConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMosaicConfigBinding
    private lateinit var adapter: MosaicModuleAdapter
    private lateinit var shortKey: String
    private val modules = MosaicModule.getDefaultModules().toMutableList()

    @Inject
    lateinit var profileManager: ProfileManager

    companion object {
        const val EXTRA_SHORT_KEY = "extra_short_key"
        const val EXTRA_MOSAIC_CONFIG = "extra_mosaic_config"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMosaicConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        shortKey = intent.getStringExtra(EXTRA_SHORT_KEY) ?: run {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        validateConfiguration()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupRecyclerView() {
        adapter = MosaicModuleAdapter(modules) {
            validateConfiguration()
        }
        binding.rvModules.adapter = adapter

        // Setup drag & drop
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val module = modules[viewHolder.adapterPosition]
                
                // No permitir mover WELCOME y FINISH
                return if (module.id == "WELCOME" || module.id == "FINISH") {
                    makeMovementFlags(0, 0)
                } else {
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                }
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                
                // No permitir mover sobre WELCOME (posición 0) ni FINISH (última posición)
                if (toPos == 0 || toPos == modules.size - 1) {
                    return false
                }
                
                val moduleToMove = modules[fromPos]
                
                // Validar movimiento y mostrar AlertDialog si no es válido
                if (moduleToMove.isEnabled) {
                    // Validación: Listas oficiales no puede ir antes de Extracción de documento
                    if (moduleToMove.id == "BLACKLIST") {
                        val modulesBeforeTarget = modules.subList(0, toPos)
                        val hasDocExtractBefore = modulesBeforeTarget.any { it.id == "DOCUMENT_EXTRACT" && it.isEnabled }
                        if (!hasDocExtractBefore) {
                            showValidationDialog(getString(R.string.mosaic_blacklist_requires_doc))
                            return false
                        }
                    }
                    
                    // Validación: 1:1 no puede ir antes de Extracción de documento NI de Verificación de identidad
                    if (moduleToMove.id == "IVERIFICATION") {
                        val modulesBeforeTarget = modules.subList(0, toPos)
                        val hasDocExtractBefore = modulesBeforeTarget.any { it.id == "DOCUMENT_EXTRACT" && it.isEnabled }
                        val hasOtoBefore = modulesBeforeTarget.any { it.id == "OTO" && it.isEnabled }
                        if (!hasDocExtractBefore || !hasOtoBefore) {
                            showValidationDialog(getString(R.string.mosaic_1to1_requires_doc_and_oto))
                            return false
                        }
                    }
                }
                
                val moved = adapter.moveModule(fromPos, toPos)
                if (!moved) {
                    // El adapter rechazó el movimiento (por validaciones)
                    return false
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No soportamos swipe
            }

            override fun isLongPressDragEnabled() = true
        })

        itemTouchHelper.attachToRecyclerView(binding.rvModules)
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnContinue.setOnClickListener {
            if (isConfigurationValid()) {
                val config = MosaicConfig(modules.toList(), shortKey)
                val intent = Intent(this, KycWebViewActivity::class.java).apply {
                    putExtra(KycWebViewActivity.EXTRA_URL, config.buildMosaicUrl(profileManager.getCurrentMosaicUrl()))
                    putExtra(KycWebViewActivity.EXTRA_FLOW_TYPE, "MOSAIC")
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun validateConfiguration() {
        val config = MosaicConfig(modules.toList(), shortKey)
        val unavailableModules = config.getUnavailableEnabledModules()
        
        if (unavailableModules.isNotEmpty()) {
            // Mostrar error de validación
            binding.layoutValidation.visibility = View.VISIBLE
            binding.tvValidationMessage.text = getString(R.string.mosaic_validation_error)
            binding.btnContinue.isEnabled = false
        } else if (!config.isValid()) {
            // Configuración inválida (falta WELCOME o FINISH)
            binding.layoutValidation.visibility = View.VISIBLE
            binding.tvValidationMessage.text = getString(R.string.mosaic_min_modules_error)
            binding.btnContinue.isEnabled = false
        } else {
            // Configuración válida
            binding.layoutValidation.visibility = View.GONE
            binding.btnContinue.isEnabled = true
        }
    }

    private fun isConfigurationValid(): Boolean {
        val config = MosaicConfig(modules.toList(), shortKey)
        return config.isValid()
    }

    private fun showValidationDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.mosaic_validation_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
