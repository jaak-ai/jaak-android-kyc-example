package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.R
import com.jaak.kyc.data.model.ImageQualityItem
import com.jaak.kyc.databinding.ItemImageQualityBinding

class ImageQualityAdapter(
    private val items: List<ImageQualityItem>
) : RecyclerView.Adapter<ImageQualityAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageQualityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemImageQualityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ImageQualityItem) {
            binding.tvProperty.text = item.property
            binding.tvImageData.text = item.imageData
            binding.tvExpectedData.text = item.expectedData

            // Texto y color dinámico según si es correcto o no
            val resultText = if (item.isCorrect) "Correcto" else "Incorrecto"
            val color = if (item.isCorrect) {
                R.color.jaak_success
            } else {
                R.color.jaak_error
            }

            binding.tvResultText.text = resultText
            binding.tvResultText.setTextColor(
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
