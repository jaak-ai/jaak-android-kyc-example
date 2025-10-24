package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.R
import com.jaak.kyc.data.model.PropertyResultItem
import com.jaak.kyc.databinding.ItemPropertyResultBinding

class PropertyResultAdapter(
    private val items: List<PropertyResultItem>,
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
            val binding = ItemPropertyResultBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = ItemPropertyResultBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            ViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind()
            is ViewHolder -> {
                val itemPosition = if (showHeader) position - 1 else position
                holder.bind(items[itemPosition])
            }
        }
    }

    override fun getItemCount(): Int = if (showHeader) items.size + 1 else items.size

    class HeaderViewHolder(
        private val binding: ItemPropertyResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.tvProperty.text = "Propiedad:"
            binding.tvProperty.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tvResult.text = "Resultado"
            binding.tvResult.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tvResult.setTextColor(
                ContextCompat.getColor(binding.root.context, R.color.jaak_text_primary)
            )
            binding.ivResultIcon.visibility = android.view.View.GONE
        }
    }

    class ViewHolder(
        private val binding: ItemPropertyResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PropertyResultItem) {
            binding.tvProperty.text = item.property
            binding.tvResult.text = item.result

            // Color dinámico según si es correcto o no
            val color = if (item.isCorrect) {
                R.color.jaak_success
            } else {
                R.color.jaak_error
            }

            binding.tvResult.setTextColor(
                ContextCompat.getColor(binding.root.context, color)
            )
            binding.ivResultIcon.setColorFilter(
                ContextCompat.getColor(binding.root.context, color)
            )

            // Cambiar ícono según el resultado
            val iconRes = if (item.isCorrect) {
                R.drawable.ic_check_small
            } else {
                R.drawable.ic_close
            }
            binding.ivResultIcon.setImageResource(iconRes)
        }
    }
}
