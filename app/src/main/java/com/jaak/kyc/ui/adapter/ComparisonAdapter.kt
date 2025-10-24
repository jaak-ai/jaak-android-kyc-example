package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.R
import com.jaak.kyc.data.model.ComparisonItem
import com.jaak.kyc.databinding.ItemComparisonRowBinding

class ComparisonAdapter(
    private val items: List<ComparisonItem>,
    private val showHeader: Boolean = true
) : RecyclerView.Adapter<ComparisonAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemComparisonRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position == 0 && showHeader)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemComparisonRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ComparisonItem, showHeader: Boolean) {
            // Mostrar header solo en la primera fila
            binding.llHeader.visibility = if (showHeader) View.VISIBLE else View.GONE

            // Datos
            binding.tvProperty.text = item.property
            binding.tvDataReceived.text = item.dataReceived
            binding.tvDataExpected.text = item.dataExpected
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
