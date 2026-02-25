package ai.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ai.jaak.kyc.data.model.KycProfile
import ai.jaak.kyc.databinding.ItemProfileBinding

class ProfilesAdapter(
    private val onProfileClick: (KycProfile) -> Unit,
    private val onProfileDelete: (KycProfile) -> Unit
) : ListAdapter<KycProfile, ProfilesAdapter.ProfileViewHolder>(ProfileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = ItemProfileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileViewHolder(binding, onProfileClick, onProfileDelete)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProfileViewHolder(
        private val binding: ItemProfileBinding,
        private val onProfileClick: (KycProfile) -> Unit,
        private val onProfileDelete: (KycProfile) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: KycProfile) {
            binding.tvProfileName.text = profile.profileName
            binding.tvFlowType.text = profile.flowType

            // Mostrar badge "Predeterminado" solo si aplica
            binding.tvDefaultBadge.visibility = if (profile.isDefault) View.VISIBLE else View.GONE

            // Click en la card
            binding.cardProfile.setOnClickListener {
                onProfileClick(profile)
            }

            // TODO: Implementar swipe para eliminar
            // Por ahora, el swipe se manejará en la Activity con ItemTouchHelper
        }
    }

    class ProfileDiffCallback : DiffUtil.ItemCallback<KycProfile>() {
        override fun areItemsTheSame(oldItem: KycProfile, newItem: KycProfile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: KycProfile, newItem: KycProfile): Boolean {
            return oldItem == newItem
        }
    }
}
