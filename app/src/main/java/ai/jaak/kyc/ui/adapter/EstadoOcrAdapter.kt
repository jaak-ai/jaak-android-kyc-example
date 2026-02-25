package ai.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import ai.jaak.kyc.R
import ai.jaak.kyc.databinding.ItemEstadoOcrBinding

data class EstadoOcrItem(
    val property: String,
    val isValid: Boolean
)

class EstadoOcrAdapter(
    private val items: List<EstadoOcrItem>,
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
            val binding = ItemEstadoOcrBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            HeaderViewHolder(binding)
        } else {
            val binding = ItemEstadoOcrBinding.inflate(
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
                holder.bind(items[itemPosition])
            }
        }
    }

    override fun getItemCount(): Int = if (showHeader) items.size + 1 else items.size

    class HeaderViewHolder(
        private val binding: ItemEstadoOcrBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            binding.tvProperty.text = "Propiedad"
            binding.tvProperty.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tvValueHeader.text = "Valor"
            binding.tvValueHeader.setTypeface(null, android.graphics.Typeface.BOLD)
            binding.tvValueHeader.visibility = android.view.View.VISIBLE
            binding.ivValue.visibility = android.view.View.INVISIBLE
        }
    }

    class ItemViewHolder(
        private val binding: ItemEstadoOcrBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EstadoOcrItem) {
            binding.tvProperty.text = item.property
            binding.tvValueHeader.visibility = android.view.View.GONE

            if (item.isValid) {
                binding.ivValue.setImageResource(R.drawable.ic_check_small)
                binding.ivValue.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.jaak_success)
                )
            } else {
                binding.ivValue.setImageResource(R.drawable.ic_close)
                binding.ivValue.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.jaak_error)
                )
            }
            binding.ivValue.visibility = android.view.View.VISIBLE
        }
    }
}
