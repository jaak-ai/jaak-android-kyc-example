package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.databinding.ItemPendingProcessBinding
import com.jaak.kyc.ui.model.PendingProcessModel

class PendingProcessAdapter(
    private val onSyncClick: (PendingProcessModel) -> Unit,
    private val onDeleteClick: (PendingProcessModel) -> Unit
) : ListAdapter<PendingProcessModel, PendingProcessAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPendingProcessBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemPendingProcessBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(process: PendingProcessModel) {
            binding.apply {
                tvShortKey.text = process.shortKey
                tvProgress.text = process.progressText
                tvServiceName.text = process.serviceDisplayText
                
                // Deshabilitar botones durante sincronización
                btnSync.isEnabled = !process.isSyncing
                btnDelete.isEnabled = !process.isSyncing
                
                // Cambiar texto del botón si está sincronizando
                btnSync.text = if (process.isSyncing) "Sincronizando..." else "Sincronizar"
                
                btnSync.setOnClickListener {
                    if (!process.isSyncing) {
                        onSyncClick(process)
                    }
                }
                
                btnDelete.setOnClickListener {
                    if (!process.isSyncing) {
                        onDeleteClick(process)
                    }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<PendingProcessModel>() {
        override fun areItemsTheSame(oldItem: PendingProcessModel, newItem: PendingProcessModel): Boolean {
            return oldItem.processId == newItem.processId
        }

        override fun areContentsTheSame(oldItem: PendingProcessModel, newItem: PendingProcessModel): Boolean {
            return oldItem == newItem
        }
    }
}