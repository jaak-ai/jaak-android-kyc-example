package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.data.model.KycProfile
import com.jaak.kyc.databinding.ItemDashboardProfileBinding

class DashboardProfilesAdapter(
    private val onProfileClick: (KycProfile) -> Unit
) : ListAdapter<KycProfile, DashboardProfilesAdapter.ProfileViewHolder>(ProfileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = ItemDashboardProfileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileViewHolder(binding, onProfileClick)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProfileViewHolder(
        private val binding: ItemDashboardProfileBinding,
        private val onProfileClick: (KycProfile) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(profile: KycProfile) {
            binding.tvProfileName.text = profile.profileName

            // Mostrar badge "Predeterminado" solo si aplica
            binding.tvDefaultBadge.visibility = if (profile.isDefault) View.VISIBLE else View.GONE

            // Click en la card para iniciar sesión KYC con este perfil
            binding.cardProfile.setOnClickListener {
                onProfileClick(profile)
            }
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
