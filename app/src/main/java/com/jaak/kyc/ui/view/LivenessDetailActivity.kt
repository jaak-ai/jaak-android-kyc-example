package com.jaak.kyc.ui.view

import android.app.Dialog
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.jaak.kyc.R
import com.jaak.kyc.data.model.ComparisonItem
import com.jaak.kyc.databinding.ActivityLivenessDetailBinding
import com.jaak.kyc.ui.adapter.ComparisonAdapter

class LivenessDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLivenessDetailBinding
    private var mediaController: MediaController? = null

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLivenessDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupVideoPlayer()
        setupImageClickListener()
        loadLivenessData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupVideoPlayer() {
        // Configurar MediaController para controles de video
        mediaController = MediaController(this)
        mediaController?.setAnchorView(binding.videoView)
        binding.videoView.setMediaController(mediaController)

        // Click en play button
        binding.ivPlayButton.setOnClickListener {
            playVideo()
        }

        // Listener para cuando el video termina
        binding.videoView.setOnCompletionListener {
            binding.ivPlayButton.visibility = View.VISIBLE
        }

        // Listener para cuando el video está preparado
        binding.videoView.setOnPreparedListener { mediaPlayer ->
            binding.pbVideo.visibility = View.GONE
            binding.ivPlayButton.visibility = View.GONE
            mediaPlayer.start()
        }
    }

    private var videoUrl: String? = null

    private fun playVideo() {
        if (videoUrl.isNullOrEmpty()) {
            android.util.Log.w("LivenessDetailActivity", "No hay URL de video disponible")
            android.widget.Toast.makeText(this, "Video no disponible", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        binding.ivPlayButton.visibility = View.GONE
        binding.pbVideo.visibility = View.VISIBLE

        try {
            android.util.Log.d("LivenessDetailActivity", "Reproduciendo video: $videoUrl")
            binding.videoView.setVideoURI(Uri.parse(videoUrl))
            binding.videoView.start()
        } catch (e: Exception) {
            android.util.Log.e("LivenessDetailActivity", "Error al reproducir video: ${e.message}", e)
            binding.pbVideo.visibility = View.GONE
            binding.ivPlayButton.visibility = View.VISIBLE
            android.widget.Toast.makeText(this, "Error al reproducir video", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupImageClickListener() {
        binding.ivBestFrame.setOnClickListener {
            showImagePreview(binding.ivBestFrame)
        }
    }

    /**
     * Muestra preview de imagen en pantalla completa
     */
    private fun showImagePreview(imageView: ImageView) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_image_preview)

        val previewImage = dialog.findViewById<ImageView>(R.id.ivPreview)
        val closeButton = dialog.findViewById<ImageView>(R.id.ivClose)

        previewImage.setImageDrawable(imageView.drawable)

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadLivenessData() {
        // Obtener datos del Intent
        val livenessData = intent.getParcelableExtra<com.jaak.kyc.data.model.LivenessDetailData>("liveness_data")

        if (livenessData == null) {
            android.util.Log.e("LivenessDetailActivity", "❌ No se recibieron datos de liveness")
            android.widget.Toast.makeText(this, "Error: Datos no disponibles", android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        android.util.Log.d("LivenessDetailActivity", "========== LIVENESS DATA RECEIVED ==========")
        android.util.Log.d("LivenessDetailActivity", "Event ID: ${livenessData.eventId}")
        android.util.Log.d("LivenessDetailActivity", "Best Frame URL: ${livenessData.bestFrameUrl}")
        android.util.Log.d("LivenessDetailActivity", "Video URL: ${livenessData.videoUrl}")
        android.util.Log.d("LivenessDetailActivity", "Liveness Score: ${livenessData.livenessScore}")
        android.util.Log.d("LivenessDetailActivity", "Processing Time: ${livenessData.processingTime}")
        android.util.Log.d("LivenessDetailActivity", "==========================================")

        // Cargar imagen usando Picasso
        if (!livenessData.bestFrameUrl.isNullOrEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(livenessData.bestFrameUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.ivBestFrame)
        } else {
            binding.ivBestFrame.setImageResource(R.drawable.ic_person)
        }

        // Configurar video si hay URL
        videoUrl = livenessData.videoUrl
        if (!videoUrl.isNullOrEmpty()) {
            android.util.Log.d("LivenessDetailActivity", "Configurando video: $videoUrl")
            // El video se cargará cuando se presione play
        } else {
            android.util.Log.w("LivenessDetailActivity", "Video URL no disponible")
        }

        // ========== DATOS REALES DEL SERVICIO ==========
        // Mostrar score de liveness con validación
        android.util.Log.d("LivenessDetailActivity", "Score recibido: ${livenessData.livenessScore}")

        if (livenessData.livenessScore != null) {
            android.util.Log.d("LivenessDetailActivity", "Creando tabla de score...")
            val livenessItems = listOf(
                ComparisonItem(
                    property = "Score",
                    dataReceived = String.format("%.4f", livenessData.livenessScore),
                    dataExpected = "Mayor a 0.4",
                    result = if (livenessData.livenessScore >= 0.4) "Correcto" else "Incorrecto",
                    isCorrect = livenessData.livenessScore >= 0.4
                )
            )

            android.util.Log.d("LivenessDetailActivity", "Items creados: ${livenessItems.size}")

            binding.rvLivenessData.apply {
                layoutManager = LinearLayoutManager(this@LivenessDetailActivity)
                adapter = ComparisonAdapter(livenessItems, showHeader = true)
            }

            android.util.Log.d("LivenessDetailActivity", "RecyclerView configurado")
        } else {
            android.util.Log.w("LivenessDetailActivity", "❌ No hay score de liveness disponible")
        }

        // Tiempo de procesamiento
        val processingTime = livenessData.processingTime ?: "N/A"
        binding.tvProcessingTime.text = "Tiempo de procesamiento: $processingTime"

        // Ocultar loading
        binding.progressBar.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        // Pausar video si está reproduciéndose
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoView.stopPlayback()
    }
}
