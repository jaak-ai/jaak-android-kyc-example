package com.jaak.kyc.ui.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.R
import com.jaak.kyc.data.model.KycProfile
import com.jaak.kyc.databinding.ActivityProfilesListBinding
import com.jaak.kyc.ui.adapter.ProfilesAdapter
import com.jaak.kyc.utils.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfilesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilesListBinding
    private lateinit var adapter: ProfilesAdapter

    @Inject
    lateinit var profileManager: ProfileManager

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Recargar lista de perfiles
            loadProfiles()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadProfiles()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ProfilesAdapter(
            onProfileClick = { profile ->
                // Abrir pantalla de edición
                openEditProfile(profile.id)
            },
            onProfileDelete = { profile ->
                // Este callback no se usa porque el swipe lo maneja ItemTouchHelper
            }
        )

        binding.rvProfiles.apply {
            layoutManager = LinearLayoutManager(this@ProfilesListActivity)
            adapter = this@ProfilesListActivity.adapter
        }

        // Configurar swipe para eliminar
        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val profile = adapter.currentList[position]
                showDeleteConfirmation(profile)
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.rvProfiles)
    }

    private fun setupListeners() {
        binding.btnAddProfile.setOnClickListener {
            openEditProfile(null)
        }
    }

    private fun loadProfiles() {
        val profiles = profileManager.getKycProfiles()

        if (profiles.isEmpty()) {
            binding.rvProfiles.visibility = View.GONE
            binding.llEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvProfiles.visibility = View.VISIBLE
            binding.llEmptyState.visibility = View.GONE
            adapter.submitList(profiles)
        }
    }

    private fun openEditProfile(profileId: String?) {
        val intent = Intent(this, EditSessionProfileActivity::class.java)
        profileId?.let {
            intent.putExtra("profileId", it)
        }
        editProfileLauncher.launch(intent)
    }

    private fun showDeleteConfirmation(profile: KycProfile) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_profile_title)
            .setMessage(getString(R.string.delete_profile_message, profile.profileName))
            .setPositiveButton(R.string.delete) { _, _ ->
                profileManager.deleteKycProfile(profile.id)
                loadProfiles()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                // Restaurar el item en el RecyclerView
                loadProfiles()
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Recargar perfiles al volver a la actividad
        loadProfiles()
    }
}
