package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.R
import com.jaak.kyc.data.local.entity.KycProcessEntity
import com.jaak.kyc.data.local.entity.KycProcessStatus
import com.jaak.kyc.data.local.entity.ServiceStatus
import com.jaak.kyc.databinding.ItemKycProcessBinding
import java.text.SimpleDateFormat
import java.util.*

class KycProcessAdapter(
    private val onItemClick: (KycProcessEntity) -> Unit,
    private val onSyncClick: (KycProcessEntity) -> Unit,
    private val onViewDetailsClick: (KycProcessEntity) -> Unit
) : ListAdapter<KycProcessEntity, KycProcessAdapter.ProcessViewHolder>(ProcessDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessViewHolder {
        val binding = ItemKycProcessBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ProcessViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProcessViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProcessViewHolder(
        private val binding: ItemKycProcessBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            
            binding.btnSync.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSyncClick(getItem(position))
                }
            }
            
            binding.btnViewDetails.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onViewDetailsClick(getItem(position))
                }
            }
        }

        fun bind(process: KycProcessEntity) {
            with(binding) {
                // Basic info
                tvProcessId.text = "Process #${process.id.take(8)}"
                tvShortKey.text = "Short Key: ${process.shortKey}"
                
                // Status
                tvStatus.text = process.overallStatus.name
                tvStatus.setBackgroundResource(getStatusBackground(process.overallStatus))
                
                // Progress calculation
                val completedServices = listOf(
                    process.sessionStatus,
                    process.verifyStatus,
                    process.ocrStatus,
                    process.livenessStatus,
                    process.otoVerifyStatus,
                    process.finishStatus
                ).count { it == ServiceStatus.COMPLETED || it == ServiceStatus.SYNCED }
                
                val progressPercentage = (completedServices * 100) / 6
                tvProgressPercentage.text = "$progressPercentage%"
                progressBarProcess.progress = progressPercentage
                
                // Service status icons
                setServiceStatusIcon(ivSessionStatus, process.sessionStatus)
                setServiceStatusIcon(ivVerifyStatus, process.verifyStatus)
                setServiceStatusIcon(ivOcrStatus, process.ocrStatus)
                setServiceStatusIcon(ivLivenessStatus, process.livenessStatus)
                setServiceStatusIcon(ivOtoVerifyStatus, process.otoVerifyStatus)
                setServiceStatusIcon(ivFinishStatus, process.finishStatus)
                
                // Dates
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvCreatedAt.text = "Created: ${dateFormat.format(Date(process.createdAt))}"
                
                // Sync status
                if (process.requiresSync) {
                    tvSyncStatus.visibility = View.VISIBLE
                    tvSyncStatus.text = "Needs Sync"
                    btnSync.visibility = View.VISIBLE
                } else {
                    tvSyncStatus.visibility = View.GONE
                    btnSync.visibility = View.GONE
                }
            }
        }

        private fun setServiceStatusIcon(imageView: android.widget.ImageView, status: ServiceStatus) {
            val iconRes = when (status) {
                ServiceStatus.COMPLETED, ServiceStatus.SYNCED -> R.drawable.ic_service_completed
                ServiceStatus.FAILED -> R.drawable.ic_service_failed
                ServiceStatus.RETRYING -> R.drawable.ic_service_pending
                ServiceStatus.PENDING -> R.drawable.ic_service_pending
            }
            imageView.setImageResource(iconRes)
        }

        private fun getStatusBackground(status: KycProcessStatus): Int {
            return when (status) {
                KycProcessStatus.COMPLETED, KycProcessStatus.COMPLETED_OFFLINE -> R.drawable.bg_rounded_primary
                KycProcessStatus.FAILED -> R.drawable.bg_rounded_error
                KycProcessStatus.SYNCING, KycProcessStatus.IN_PROGRESS -> R.drawable.bg_rounded_warning
                else -> R.drawable.bg_rounded_secondary
            }
        }
    }

    class ProcessDiffCallback : DiffUtil.ItemCallback<KycProcessEntity>() {
        override fun areItemsTheSame(oldItem: KycProcessEntity, newItem: KycProcessEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: KycProcessEntity, newItem: KycProcessEntity): Boolean {
            return oldItem == newItem
        }
    }
}