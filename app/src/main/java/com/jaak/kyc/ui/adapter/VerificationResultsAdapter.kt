package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.R
import com.jaak.kyc.databinding.ItemVerificationResultBinding

data class VerificationResult(
    val property: String,
    val receivedData: String,
    val expectedData: String,
    val isCorrect: Boolean
)

class VerificationResultsAdapter(
    private val results: List<VerificationResult>,
    private val showHeader: Boolean = true
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (showHeader && position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = ItemVerificationResultBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = ItemVerificationResultBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind()
            is ItemViewHolder -> {
                val itemPosition = if (showHeader) position - 1 else position
                holder.bind(results[itemPosition])
            }
        }
    }

    override fun getItemCount(): Int = if (showHeader) results.size + 1 else results.size

    class HeaderViewHolder(
        private val binding: ItemVerificationResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.tvProperty.text = "Propiedad"
            binding.tvProperty.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tvReceivedData.text = "Datos recibidos"
            binding.tvReceivedData.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tvExpectedData.text = "Datos esperados"
            binding.tvExpectedData.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tvResult.text = "Resultados"
            binding.tvResult.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tvResult.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.jaak_text_primary)
            )
            binding.ivResultIcon.visibility = android.view.View.GONE
        }
    }

    class ItemViewHolder(
        private val binding: ItemVerificationResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: VerificationResult) {
            binding.tvProperty.text = result.property
            binding.tvReceivedData.text = result.receivedData
            binding.tvExpectedData.text = result.expectedData

            if (result.isCorrect) {
                binding.ivResultIcon.setImageResource(R.drawable.ic_check_small)
                binding.ivResultIcon.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.jaak_success)
                )
                binding.tvResult.text = "Correcto"
                binding.tvResult.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.jaak_success)
                )
            } else {
                binding.ivResultIcon.setImageResource(R.drawable.ic_close)
                binding.ivResultIcon.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.jaak_error)
                )
                binding.tvResult.text = "Incorrecto"
                binding.tvResult.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.jaak_error)
                )
            }
            binding.ivResultIcon.visibility = android.view.View.VISIBLE
        }
    }
}
