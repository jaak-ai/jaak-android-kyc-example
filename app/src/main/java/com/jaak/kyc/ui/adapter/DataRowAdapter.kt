package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.data.model.DataRow
import com.jaak.kyc.databinding.ItemDataRowBinding

class DataRowAdapter(
    private val dataRows: List<DataRow>
) : RecyclerView.Adapter<DataRowAdapter.DataRowViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataRowViewHolder {
        val binding = ItemDataRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DataRowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataRowViewHolder, position: Int) {
        holder.bind(dataRows[position])
    }

    override fun getItemCount(): Int = dataRows.size

    class DataRowViewHolder(
        private val binding: ItemDataRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dataRow: DataRow) {
            binding.tvProperty.text = dataRow.property
            binding.tvValue.text = dataRow.value
        }
    }
}
