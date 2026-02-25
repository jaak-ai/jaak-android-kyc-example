package ai.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ai.jaak.kyc.R
import ai.jaak.kyc.data.model.BlacklistItem
import ai.jaak.kyc.data.model.BlacklistStatus
import ai.jaak.kyc.databinding.ItemBlacklistCardBinding

class BlacklistAdapter(
    private val items: List<BlacklistItem>
) : RecyclerView.Adapter<BlacklistAdapter.BlacklistViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlacklistViewHolder {
        val binding = ItemBlacklistCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BlacklistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlacklistViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class BlacklistViewHolder(
        private val binding: ItemBlacklistCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        fun bind(item: BlacklistItem) {
            // Nombre de la lista
            binding.tvListName.text = item.name

            // Badge de estado
            binding.tvStatusBadge.text = item.status.displayName

            // Color del badge según el estado
            val (bgColor, textColor) = when (item.status) {
                BlacklistStatus.RISK -> Pair(R.color.jaak_error, R.color.white)
                BlacklistStatus.VALID -> Pair(R.color.jaak_success, R.color.white)
                BlacklistStatus.RELIABLE -> Pair(R.color.jaak_success, R.color.white)
            }

            binding.cvStatusBadge.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, bgColor)
            )
            binding.tvStatusBadge.setTextColor(
                ContextCompat.getColor(binding.root.context, textColor)
            )

            // Color del stroke según el tipo de lista
            val strokeColor = if (item.isRiskList) {
                R.color.warning_light
            } else {
                R.color.jaak_success_light
            }
            binding.root.strokeColor = ContextCompat.getColor(binding.root.context, strokeColor)

            // Descripción
            binding.tvDescription.text = item.description

            // Intentos
            binding.tvAttempts.text = item.attempts.toString()

            // Tiempo de procesamiento
            binding.tvProcessingTime.text = item.processingTime

            // Datos detallados (si existen)
            if (!item.detailedData.isNullOrEmpty()) {
                binding.btnToggleDetails.visibility = View.VISIBLE
                binding.divider.visibility = View.VISIBLE

                // Configurar RecyclerView de datos detallados
                binding.rvDetailedData.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = DataRowAdapter(item.detailedData)
                }

                // Toggle de expandir/colapsar
                binding.btnToggleDetails.setOnClickListener {
                    isExpanded = !isExpanded

                    if (isExpanded) {
                        binding.llDetailedData.visibility = View.VISIBLE
                        binding.btnToggleDetails.text = "Ocultar detalles"
                    } else {
                        binding.llDetailedData.visibility = View.GONE
                        binding.btnToggleDetails.text = "Ver detalles"
                    }
                }
            } else {
                binding.btnToggleDetails.visibility = View.GONE
                binding.divider.visibility = View.GONE
                binding.llDetailedData.visibility = View.GONE
            }
        }
    }
}
