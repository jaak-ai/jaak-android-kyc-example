package ai.jaak.kyc.ui.view

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import ai.jaak.kyc.databinding.FragmentSessionLivenessBinding
import ai.jaak.kyc.ui.viewmodel.SessionDetailState
import ai.jaak.kyc.ui.viewmodel.SessionDetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SessionLivenessFragment : Fragment() {

    private var _binding: FragmentSessionLivenessBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SessionDetailViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionLivenessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Click en foto de perfil para ver en pantalla completa
        binding.ivUserPhoto.setOnClickListener {
            showImagePreview(binding.ivUserPhoto)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                if (state is SessionDetailState.Success) {
                    populateUI(state)
                }
            }
        }
    }

    private fun populateUI(state: SessionDetailState.Success) {
        val session = state.data.session
        val summary = state.data.summary

        // ========== INFORMACIÓN DEL USUARIO ==========
        // Foto (URL de Google Cloud Storage)
        if (!summary.photo.isNullOrEmpty()) {
            android.util.Log.d("SessionLivenessFragment", "Cargando foto desde URL: ${summary.photo}")

            // Cargar imagen desde URL usando Picasso
            com.squareup.picasso.Picasso.get()
                .load(summary.photo)
                .placeholder(ai.jaak.kyc.R.drawable.ic_person) // Mientras carga
                .error(ai.jaak.kyc.R.drawable.ic_person) // Si falla
                .into(binding.ivUserPhoto)
        } else {
            android.util.Log.d("SessionLivenessFragment", "Photo URL es null o vacío")
            binding.ivUserPhoto.setImageResource(ai.jaak.kyc.R.drawable.ic_person)
        }

        // Nombre completo
        val fullName = "${summary.name ?: ""} ${summary.lastName ?: ""}".trim()
        binding.tvUserName.text = fullName.ifEmpty { getString(ai.jaak.kyc.R.string.user_no_name) }

        // Tiempo total
        val timeText = if (summary.totalTime != null) {
            getString(ai.jaak.kyc.R.string.liveness_total_time_format, formatDuration(summary.totalTime))
        } else {
            getString(ai.jaak.kyc.R.string.liveness_total_time_na)
        }
        binding.tvTotalTime.text = timeText

        // ========== DATOS DE SESIÓN ==========
        binding.tvSessionId.text = session.sessionID ?: "N/A"
        binding.tvShortKey.text = session.shortKey ?: "N/A"
        binding.tvFlowNameDetail.text = session.flowName ?: "N/A"
        binding.tvStartDate.text = formatDate(session.startDate)
        binding.tvUpdateDate.text = formatDate(session.updateDate)
        binding.tvEndDate.text = formatDate(session.endDate)
        binding.tvOrigin.text = session.origin?.replaceFirstChar { it.uppercase() } ?: "N/A"
        binding.tvValidation.text = session.validation?.uppercase() ?: "N/A"
        binding.tvFlowType.text = session.flowType?.uppercase() ?: "N/A"
        binding.tvConsent.text = formatDate(session.consent)

        // ========== DATOS DE CONTACTO ==========
        binding.tvContactName.text = session.contactName ?: "N/A"

        // El contacto puede ser email, whatsapp, sms según verificationType
        val contact = when (session.verificationType) {
            "email" -> session.verification?.email
            "whatsapp" -> session.verification?.whatsapp
            "sms" -> session.verification?.sms
            else -> null
        }
        binding.tvContact.text = contact ?: "- -"

        // Nombres y apellidos capturados del summary
        binding.tvCapturedFirstName.text = summary.name ?: "N/A"
        binding.tvCapturedLastName.text = summary.lastName ?: "N/A"

        // Ubicación
        binding.tvCity.text = session.location?.city ?: "N/A"
        binding.tvCountry.text = session.location?.country ?: "N/A"

        // Link "Ver" ubicación
        val location = session.location
        val latitude = location?.latitude
        val longitude = location?.longitude

        // Si no hay coordenadas, usar coordenadas simuladas (Ciudad de México)
        val finalLat = latitude ?: 19.4326
        val finalLng = longitude ?: -99.1332

        binding.tvLocationLink.setOnClickListener {
            showLocationDialog(finalLat, finalLng)
        }
    }

    /**
     * Muestra diálogo para elegir cómo abrir la ubicación
     */
    private fun showLocationDialog(latitude: Double, longitude: Double) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(ai.jaak.kyc.R.string.location_dialog_title))
            .setMessage(getString(ai.jaak.kyc.R.string.location_dialog_message))
            .setPositiveButton(getString(ai.jaak.kyc.R.string.location_google_maps)) { _, _ ->
                openGoogleMaps(latitude, longitude)
            }
            .setNegativeButton(getString(ai.jaak.kyc.R.string.cancel), null)
            .show()
    }

    /**
     * Abre Google Maps con las coordenadas
     */
    private fun openGoogleMaps(latitude: Double, longitude: Double) {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        // Si Google Maps no está instalado, abre en el navegador
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            startActivity(webIntent)
        }
    }

    /**
     * Formatea duración en minutos a formato "Xm Ys"
     */
    private fun formatDuration(minutes: Double): String {
        val totalSeconds = (minutes * 60).toInt()
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        return "${mins}m ${secs}s"
    }

    /**
     * Formatea fecha ISO 8601 a formato legible
     */
    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "N/A"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString)

            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            outputFormat.format(date ?: return "N/A")
        } catch (e: Exception) {
            // Intentar otro formato sin milisegundos
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateString)

                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                outputFormat.format(date ?: return "N/A")
            } catch (e: Exception) {
                dateString
            }
        }
    }

    /**
     * Muestra preview de imagen en pantalla completa
     */
    private fun showImagePreview(imageView: ImageView) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(ai.jaak.kyc.R.layout.dialog_image_preview)

        val previewImage = dialog.findViewById<ImageView>(ai.jaak.kyc.R.id.ivPreview)
        val closeButton = dialog.findViewById<ImageView>(ai.jaak.kyc.R.id.ivClose)

        previewImage.setImageDrawable(imageView.drawable)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
