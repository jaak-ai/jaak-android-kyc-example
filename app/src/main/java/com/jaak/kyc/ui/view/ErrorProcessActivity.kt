package com.jaak.kyc.ui.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.jaak.kyc.R
import com.jaak.kyc.databinding.ActivityErrorProcessBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ErrorProcessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityErrorProcessBinding

    companion object {
        const val EXTRA_ERROR_TYPE = "error_type"
        const val EXTRA_ERROR_MESSAGE = "error_message"

        // Error types
        const val ERROR_TYPE_LIVENESS_FAILED = "liveness_failed"
        const val ERROR_TYPE_LIVENESS_SCORE_LOW = "liveness_score_low"
        const val ERROR_TYPE_FACE_COMPARISON_FAILED = "face_comparison_failed"
        const val ERROR_TYPE_FACE_COMPARISON_SCORE_LOW = "face_comparison_score_low"
        const val ERROR_TYPE_VERIFY_FAILED = "verify_failed"
        const val ERROR_TYPE_OCR_FAILED = "ocr_failed"
        const val ERROR_TYPE_DOCUMENT_INVALID = "document_invalid"
        const val ERROR_TYPE_DOCUMENT_VALIDATION_FAILED = "document_validation_failed"
        const val ERROR_TYPE_GENERIC = "generic_error"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityErrorProcessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()
        displayErrorDetails()
    }

    private fun initComponents() {
        // Disable back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // No hacer nada - el usuario debe usar el botón de la pantalla
            }
        })

        // Start Again button - goes back to InitProcessLivenessActivity to restart the full flow
        binding.btnStartAgain.setOnClickListener {
            val intent = Intent(this, InitProcessLivenessActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun displayErrorDetails() {
        val errorType = intent.getStringExtra(EXTRA_ERROR_TYPE) ?: ERROR_TYPE_GENERIC
        val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)

        // Mostrar el mensaje real del servicio en tvErrorDetails
        // Si no hay mensaje del servicio, usar mensaje por defecto según el tipo
        val detailsMessage = if (!errorMessage.isNullOrEmpty()) {
            // Usar el mensaje real del servicio
            errorMessage
        } else {
            // Fallback a mensajes por defecto si no hay mensaje del servicio
            when (errorType) {
                ERROR_TYPE_LIVENESS_FAILED -> getString(R.string.error_liveness_failed)
                ERROR_TYPE_LIVENESS_SCORE_LOW -> getString(R.string.error_liveness_score_low)
                ERROR_TYPE_FACE_COMPARISON_FAILED -> getString(R.string.error_face_comparison_failed)
                ERROR_TYPE_FACE_COMPARISON_SCORE_LOW -> getString(R.string.error_face_comparison_score_low)
                ERROR_TYPE_VERIFY_FAILED -> getString(R.string.error_verify_failed)
                ERROR_TYPE_OCR_FAILED -> getString(R.string.error_ocr_failed)
                ERROR_TYPE_DOCUMENT_INVALID -> getString(R.string.error_document_invalid)
                ERROR_TYPE_DOCUMENT_VALIDATION_FAILED -> getString(R.string.error_document_invalid)
                else -> getString(R.string.error_details_default)
            }
        }

        binding.tvErrorDetails.text = detailsMessage

        // Log for debugging
        android.util.Log.e("ErrorProcessActivity", "Error Type: $errorType")
        android.util.Log.e("ErrorProcessActivity", "Service Error Message: $errorMessage")
    }
}
