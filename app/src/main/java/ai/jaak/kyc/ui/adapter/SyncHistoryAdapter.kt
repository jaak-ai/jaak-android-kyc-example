package ai.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ai.jaak.kyc.R
import ai.jaak.kyc.databinding.ItemSyncHistoryBinding
import ai.jaak.kyc.ui.viewmodel.KycSyncViewModel
import java.text.SimpleDateFormat
import java.util.*

class SyncHistoryAdapter(
    private val onItemClick: (KycSyncViewModel.SyncHistoryItem) -> Unit
) : RecyclerView.Adapter<SyncHistoryAdapter.SyncHistoryViewHolder>() {

    private var history = listOf<KycSyncViewModel.SyncHistoryItem>()

    fun updateHistory(newHistory: List<KycSyncViewModel.SyncHistoryItem>) {
        history = newHistory
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SyncHistoryViewHolder {
        val binding = ItemSyncHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SyncHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SyncHistoryViewHolder, position: Int) {
        holder.bind(history[position])
    }

    override fun getItemCount(): Int = history.size

    inner class SyncHistoryViewHolder(
        private val binding: ItemSyncHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(historyItem: KycSyncViewModel.SyncHistoryItem) {
            binding.apply {
                tvHistoryTitle.text = historyItem.title
                tvHistoryDetails.text = historyItem.details
                tvHistoryTime.text = timeFormat.format(Date(historyItem.timestamp))
                tvHistoryDuration.text = formatDuration(historyItem.duration)

                // Set status icon
                when (historyItem.status) {
                    KycSyncViewModel.SyncHistoryStatus.SUCCESS -> {
                        ivHistoryStatus.setImageResource(R.drawable.ic_service_completed)
                    }
                    KycSyncViewModel.SyncHistoryStatus.ERROR -> {
                        ivHistoryStatus.setImageResource(R.drawable.ic_service_failed)
                    }
                    KycSyncViewModel.SyncHistoryStatus.WARNING -> {
                        ivHistoryStatus.setImageResource(R.drawable.ic_service_pending)
                    }
                    KycSyncViewModel.SyncHistoryStatus.CANCELLED -> {
                        ivHistoryStatus.setImageResource(R.drawable.ic_service_failed)
                    }
                    KycSyncViewModel.SyncHistoryStatus.IN_PROGRESS -> {
                        ivHistoryStatus.setImageResource(R.drawable.ic_service_pending)
                    }
                    KycSyncViewModel.SyncHistoryStatus.PENDING -> {
                        ivHistoryStatus.setImageResource(R.drawable.ic_service_pending)
                    }
                }

                root.setOnClickListener {
                    onItemClick(historyItem)
                }
            }
        }

        private fun formatDuration(durationMs: Long): String {
            if (durationMs == 0L) return ""
            
            val seconds = durationMs / 1000.0
            return when {
                seconds < 1 -> "<1s"
                seconds < 60 -> String.format("%.1fs", seconds)
                else -> {
                    val minutes = (seconds / 60).toInt()
                    val remainingSeconds = (seconds % 60).toInt()
                    "${minutes}m ${remainingSeconds}s"
                }
            }
        }
    }
}