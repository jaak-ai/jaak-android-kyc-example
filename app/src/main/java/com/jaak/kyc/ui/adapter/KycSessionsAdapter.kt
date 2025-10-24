package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.R
import com.jaak.kyc.data.model.KycSessionItem
import com.jaak.kyc.data.model.KycStatus
import com.jaak.kyc.databinding.ItemKycSessionBinding

class KycSessionsAdapter(
    private val onItemClick: (KycSessionItem) -> Unit
) : ListAdapter<KycSessionItem, KycSessionsAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemKycSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(
        private val binding: ItemKycSessionBinding,
        private val onItemClick: (KycSessionItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: KycSessionItem) {
            binding.apply {
                // Datos principales
                tvShortkey.text = session.shortkey
                tvUserName.text = session.userName
                tvFlowName.text = session.flowName
                tvDateTime.text = session.dateTime
                tvScore.text = session.score.toString()
                tvProcessStatus.text = session.processStatus

                // Configurar icono y color según el estado
                when (session.kycStatus) {
                    KycStatus.PENDIENTE -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_clock)
                        ivStatusIcon.setColorFilter(
                            ContextCompat.getColor(itemView.context, R.color.jaak_text_tertiary)
                        )
                        tvStatusLabel.text = itemView.context.getString(R.string.session_status_pending)
                        tvStatusLabel.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.jaak_text_tertiary)
                        )
                    }
                    KycStatus.EXPIRADO -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_clock)
                        ivStatusIcon.setColorFilter(
                            ContextCompat.getColor(itemView.context, R.color.yellow)
                        )
                        tvStatusLabel.text = itemView.context.getString(R.string.session_status_expired)
                        tvStatusLabel.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.yellow)
                        )
                    }
                    KycStatus.RECHAZADO -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_close)
                        ivStatusIcon.setColorFilter(
                            ContextCompat.getColor(itemView.context, R.color.red)
                        )
                        tvStatusLabel.text = itemView.context.getString(R.string.session_status_rejected)
                        tvStatusLabel.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.red)
                        )
                    }
                    KycStatus.EXITOSO -> {
                        ivStatusIcon.setImageResource(R.drawable.ic_check)
                        ivStatusIcon.setColorFilter(
                            ContextCompat.getColor(itemView.context, R.color.green)
                        )
                        tvStatusLabel.text = itemView.context.getString(R.string.session_status_success)
                        tvStatusLabel.setTextColor(
                            ContextCompat.getColor(itemView.context, R.color.green)
                        )
                    }
                }

                // Color del puntaje según su valor
                when {
                    session.score >= 80 -> tvScore.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.green)
                    )
                    session.score >= 50 -> tvScore.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.yellow)
                    )
                    else -> tvScore.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.red)
                    )
                }

                // Click en el item
                root.setOnClickListener {
                    onItemClick(session)
                }
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<KycSessionItem>() {
        override fun areItemsTheSame(oldItem: KycSessionItem, newItem: KycSessionItem): Boolean {
            return oldItem.shortkey == newItem.shortkey
        }

        override fun areContentsTheSame(oldItem: KycSessionItem, newItem: KycSessionItem): Boolean {
            return oldItem == newItem
        }
    }
}
