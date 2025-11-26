package com.jaak.kyc.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jaak.kyc.R
import com.jaak.kyc.data.model.api.SessionDetailFlowEvent
import com.jaak.kyc.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.*

class EventsAdapter(
    private val events: List<SessionDetailFlowEvent>,
    private val onEventClick: ((SessionDetailFlowEvent) -> Unit)? = null
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount(): Int = events.size

    inner class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: SessionDetailFlowEvent) {
            // Nombre del evento (traducido) - pasamos el evento completo para analizar resource
            binding.tvEventName.text = translateEventAction(event)

            // Fecha del evento
            binding.tvEventDate.text = formatDate(event.createdAt)

            // Ícono según el tipo de evento
            val (iconRes, iconColor) = getEventIconAndColor(event)
            binding.ivEventIcon.setImageResource(iconRes)
            binding.ivEventIcon.setColorFilter(
                ContextCompat.getColor(binding.root.context, iconColor)
            )

            // Click listener
            binding.root.setOnClickListener {
                onEventClick?.invoke(event)
            }
        }

        /**
         * Traduce el action a un nombre legible según los actions de la API
         */
        private fun translateEventAction(event: SessionDetailFlowEvent): String {
            val action = event.action.lowercase()

            // Log para debug
            android.util.Log.d("EventsAdapter", "Translating action: $action")

            // Mapeo según los actions reales de la API:
            // - verify-blacklist → Listas Oficiales
            // - v2-verify-oto → Comparación 1:1
            // - best-frame-verify-liveness → Prueba de vida
            // - front-verify-document → Verificación de documento
            // - complet-extract-document → Extracción de documento
            return when (action) {
                "best-frame-verify-liveness" -> "Prueba de vida"
                "v2-verify-oto" -> "Comparación 1:1"
                "verify-blacklist" -> "Listas Oficiales"
                "front-verify-document" -> "Verificación de documento"
                "complet-extract-document" -> "Extracción de documento"
                // Otros actions comunes
                "liveness-process", "liveness" -> "Prueba de vida"
                "oto-process", "one-to-one-process" -> "Comparación 1:1"
                "blacklist-check", "blacklist" -> "Listas Oficiales"
                "document-process" -> "Verificación de documento"
                "consent" -> "Consentimiento"
                "start" -> "Inicio de Sesión"
                "end" -> "Fin de Sesión"
                else -> action.replace("-", " ").replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            }
        }

        /**
         * Obtiene el ícono y color según el tipo de evento
         */
        private fun getEventIconAndColor(event: SessionDetailFlowEvent): Pair<Int, Int> {
            val action = event.action.lowercase()

            // Para document-process, usar el mismo ícono pero podríamos diferenciarlo
            if (action == "document-process") {
                return Pair(R.drawable.ic_info, R.color.jaak_primary)
            }

            return when (action) {
                "liveness-process", "liveness" -> Pair(R.drawable.ic_person, R.color.jaak_success)
                "oto-process", "one-to-one-process" -> Pair(R.drawable.ic_people_small, R.color.jaak_primary)
                "blacklist-check", "blacklist" -> Pair(R.drawable.ic_info, R.color.jaak_warning)
                "consent" -> Pair(R.drawable.ic_check_small, R.color.jaak_success)
                "start" -> Pair(R.drawable.ic_play_arrow, R.color.jaak_primary)
                "end" -> Pair(R.drawable.ic_check_small, R.color.jaak_success)
                else -> Pair(R.drawable.ic_info, R.color.jaak_text_secondary)
            }
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

                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                outputFormat.format(date ?: return dateString)
            } catch (e: Exception) {
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val date = inputFormat.parse(dateString)

                    val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    outputFormat.format(date ?: return dateString)
                } catch (e: Exception) {
                    dateString
                }
            }
        }
    }
}
