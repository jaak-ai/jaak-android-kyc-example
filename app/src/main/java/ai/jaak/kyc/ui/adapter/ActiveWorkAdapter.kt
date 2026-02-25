package ai.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ai.jaak.kyc.R
import ai.jaak.kyc.databinding.ItemActiveWorkBinding
import ai.jaak.kyc.ui.viewmodel.KycSyncViewModel

class ActiveWorkAdapter(
    private val onItemClick: (KycSyncViewModel.ActiveWorkInfo) -> Unit
) : RecyclerView.Adapter<ActiveWorkAdapter.ActiveWorkViewHolder>() {

    private var works = listOf<KycSyncViewModel.ActiveWorkInfo>()

    fun updateWorks(newWorks: List<KycSyncViewModel.ActiveWorkInfo>) {
        works = newWorks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveWorkViewHolder {
        val binding = ItemActiveWorkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ActiveWorkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActiveWorkViewHolder, position: Int) {
        holder.bind(works[position])
    }

    override fun getItemCount(): Int = works.size

    inner class ActiveWorkViewHolder(
        private val binding: ItemActiveWorkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(workInfo: KycSyncViewModel.ActiveWorkInfo) {
            binding.apply {
                tvWorkTitle.text = workInfo.title
                tvWorkDetails.text = workInfo.details
                tvWorkTime.text = workInfo.duration
                
                // Set progress
                progressWork.progress = workInfo.progress
                progressWork.isIndeterminate = workInfo.progress == 0
                
                // Set status icon based on progress
                when {
                    workInfo.progress == 0 -> {
                        ivWorkStatus.setImageResource(R.drawable.ic_service_pending)
                    }
                    workInfo.progress < 100 -> {
                        ivWorkStatus.setImageResource(R.drawable.ic_service_pending)
                    }
                    else -> {
                        ivWorkStatus.setImageResource(R.drawable.ic_service_completed)
                    }
                }

                root.setOnClickListener {
                    onItemClick(workInfo)
                }
            }
        }
    }
}