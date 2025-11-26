package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.R
import com.jaak.kyc.data.model.MosaicModule
import com.jaak.kyc.databinding.ItemMosaicModuleBinding
import java.util.Collections

class MosaicModuleAdapter(
    private val modules: MutableList<MosaicModule>,
    private val onModuleChanged: () -> Unit
) : RecyclerView.Adapter<MosaicModuleAdapter.ModuleViewHolder>() {

    inner class ModuleViewHolder(val binding: ItemMosaicModuleBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(module: MosaicModule, position: Int) {
            binding.apply {
                tvModuleName.text = module.name
                tvModuleDescription.text = module.description
                
                // Remover listener temporalmente para evitar disparos al establecer isChecked
                checkboxModule.setOnCheckedChangeListener(null)
                checkboxModule.isChecked = module.isEnabled
                
                // Mostrar número solo si está habilitado (basado en posición en lista)
                if (module.isEnabled) {
                    // Contar solo los módulos habilitados que están ANTES de este en la lista
                    val orderNumber = modules.take(position + 1).count { it.isEnabled }
                    tvOrder.text = orderNumber.toString()
                    tvOrder.visibility = View.VISIBLE
                } else {
                    tvOrder.visibility = View.GONE
                }
                
                // Mostrar requisitos según el módulo (siempre visibles en amarillo con icono)
                val requirementsText = when (module.id) {
                    "BLACKLIST" -> "Requiere: Extracción de documento"
                    "IVERIFICATION" -> "Requiere: Extracción de documento, Verificación de identidad"
                    else -> null
                }
                
                if (requirementsText != null) {
                    llRequirements.visibility = View.VISIBLE
                    tvRequirements.text = requirementsText
                } else {
                    llRequirements.visibility = View.GONE
                }
                
                // Mostrar/ocultar indicador de no disponible (solo si no está disponible)
                if (!module.isAvailable && module.isEnabled) {
                    tvUnavailable.visibility = View.VISIBLE
                    tvUnavailable.text = "Este módulo no está disponible"
                    root.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.error_light)
                    )
                } else {
                    tvUnavailable.visibility = View.GONE
                    root.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.white)
                    )
                }
                
                // Establecer listener DESPUÉS de configurar el estado inicial
                checkboxModule.setOnCheckedChangeListener { _, isChecked ->
                    if (module.isEnabled == isChecked) return@setOnCheckedChangeListener
                    
                    module.isEnabled = isChecked
                    
                    // La numeración se basa en la posición del listado, no en el orden de activación
                    // Solo actualizar el estado
                    
                    // Usar post para diferir la actualización y evitar el crash
                    binding.root.post {
                        notifyDataSetChanged()
                        onModuleChanged()
                    }
                }
                
                // Todos los módulos pueden ser habilitados/deshabilitados y reordenados
                checkboxModule.isEnabled = true
                ivDragHandle.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemMosaicModuleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(modules[position], position)
    }

    override fun getItemCount() = modules.size

    /**
     * Mueve un módulo de una posición a otra
     * Retorna true si el movimiento es válido, false si no
     */
    fun moveModule(fromPosition: Int, toPosition: Int): Boolean {
        val moduleToMove = modules[fromPosition]
        
        // Solo validar si el módulo que se mueve está habilitado
        if (moduleToMove.isEnabled) {
            // Validación: Listas oficiales no puede estar antes de extracción de documento
            if (moduleToMove.id == "BLACKLIST") {
                val modulesBeforeTarget = modules.subList(0, toPosition)
                val hasDocExtractBefore = modulesBeforeTarget.any { it.id == "DOCUMENT_EXTRACT" && it.isEnabled }
                if (!hasDocExtractBefore) {
                    return false
                }
            }
            
            // Validación: 1:1 no puede estar antes de extracción de documento NI de verificación de identidad
            if (moduleToMove.id == "IVERIFICATION") {
                val modulesBeforeTarget = modules.subList(0, toPosition)
                val hasDocExtractBefore = modulesBeforeTarget.any { it.id == "DOCUMENT_EXTRACT" && it.isEnabled }
                val hasOtoBefore = modulesBeforeTarget.any { it.id == "OTO" && it.isEnabled }
                if (!hasDocExtractBefore || !hasOtoBefore) {
                    return false
                }
            }
        }
        
        // Realizar el movimiento
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(modules, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(modules, i, i - 1)
            }
        }
        
        // Actualizar orden basado en la posición en la lista
        modules.forEachIndexed { index, module ->
            module.order = index
        }
        
        notifyItemMoved(fromPosition, toPosition)
        // Actualizar todos los números después del movimiento
        notifyItemRangeChanged(0, modules.size)
        onModuleChanged()
        return true
    }

    /**
     * Obtiene la lista actual de módulos
     */
    fun getModules(): List<MosaicModule> = modules.toList()

    /**
     * Valida que 1:1 o Listas Negras no estén en primer lugar (después de WELCOME)
     * Retorna mensaje de error o null si es válido
     */
    fun validateModuleOrder(): String? {
        // Obtener módulos habilitados excluyendo WELCOME y FINISH
        val enabledModules = modules.filter { it.isEnabled && it.id != "WELCOME" && it.id != "FINISH" }
        
        if (enabledModules.isEmpty()) return null
        
        // El primer módulo habilitado (después de WELCOME) no puede ser IVERIFICATION o BLACKLIST
        val firstModule = enabledModules.firstOrNull() ?: return null
        
        return when (firstModule.id) {
            "IVERIFICATION" -> "El módulo \"1:1\" no puede estar en primer lugar. Debe ir después de la captura de documento."
            "BLACKLIST" -> "El módulo \"Listas Negras\" no puede estar en primer lugar. Debe ir después de la captura de documento."
            else -> null
        }
    }
}
