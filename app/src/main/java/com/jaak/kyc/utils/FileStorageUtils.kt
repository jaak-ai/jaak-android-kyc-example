package com.jaak.kyc.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import android.util.Base64
import com.jaak.kyc.utils.Utils.resolveToReadableUri
import android.os.Environment
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object FileStorageUtils {

    private const val TAG = "FileStorageUtils"
    private const val KYC_IMAGES_DIR = "kyc_images"

    /**
     * Copia una URI temporal a un archivo permanente en almacenamiento interno
     * @param context Contexto de la aplicación
     * @param temporalUri URI temporal (de cámara, galería, etc.)
     * @param fileName Nombre base del archivo (opcional, se genera UUID si es null)
     * @return Ruta del archivo permanente o null si falla
     */
    fun saveUriToPermanentFile(
        context: Context,
        temporalUri: Uri,
        fileName: String? = null
    ): String? {
        return try {
            val kycDir = File(context.filesDir, KYC_IMAGES_DIR).apply { mkdirs() }
            val finalFileName = fileName ?: "liveness_${System.currentTimeMillis()}.mp4"
            val permanentFile = File(kycDir, finalFileName)

            Log.d(TAG, "Attempting to save URI: $temporalUri")
            Log.d(TAG, "URI scheme: ${temporalUri.scheme}")
            Log.d(TAG, "URI path: ${temporalUri.path}")
            Log.d(TAG, "Target file: ${permanentFile.absolutePath}")

            // 1) Resolver a un Uri legible (maneja file://, path plano y _temp)
            val sourceUri = resolveToReadableUri(context, temporalUri)
            if (sourceUri == null) {
                Log.e(TAG, "Failed to resolve a readable Uri for: $temporalUri")
                return null
            }

            // 2) Abrir InputStream de manera segura
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(permanentFile).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Log.e(TAG, "openInputStream returned null for: $sourceUri")
                return null
            }

            Log.d(TAG, "✅ File saved permanently: ${permanentFile.absolutePath}")
            Log.d(TAG, "✅ File size: ${permanentFile.length()} bytes")
            permanentFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving URI to permanent file: ${e.message}", e)
            null
        }
    }


    /**
     * Convierte un archivo a base64 para envío HTTP
     * @param filePath Ruta del archivo permanente
     * @return String base64 o null si falla
     */
    fun fileToBase64(filePath: String): String? {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.w(TAG, "File does not exist: $filePath")
                return null
            }

            FileInputStream(file).use { inputStream ->
                val bytes = inputStream.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error converting file to base64: ${e.message}", e)
            null
        }
    }

    /**
     * Verifica si un archivo permanente existe
     */
    fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    /**
     * Elimina un archivo permanente
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "File deleted: $filePath, success: $deleted")
                deleted
            } else {
                Log.w(TAG, "File to delete does not exist: $filePath")
                true // Considerar como éxito si no existe
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: ${e.message}", e)
            false
        }
    }

    /**
     * Limpia archivos huérfanos (que no estén referenciados en BD)
     * Se debe llamar desde un UseCase que tenga acceso a la BD
     */
    fun cleanOrphanFiles(context: Context, referencedFiles: List<String>) {
        try {
            val kycDir = File(context.filesDir, KYC_IMAGES_DIR)
            if (!kycDir.exists()) return

            val allFiles = kycDir.listFiles() ?: return
            val referencedSet = referencedFiles.toSet()

            for (file in allFiles) {
                if (file.absolutePath !in referencedSet) {
                    val deleted = file.delete()
                    Log.d(TAG, "Orphan file deleted: ${file.absolutePath}, success: $deleted")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning orphan files: ${e.message}", e)
        }
    }

    /**
     * Obtiene el tamaño del directorio KYC en bytes
     */
    fun getKycDirectorySize(context: Context): Long {
        return try {
            val kycDir = File(context.filesDir, KYC_IMAGES_DIR)
            if (!kycDir.exists()) return 0L

            kycDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating directory size: ${e.message}", e)
            0L
        }
    }

    /**
     * Convierte base64 a archivo permanente
     * @param context Contexto de la aplicación
     * @param base64Data Datos en base64
     * @param fileName Nombre del archivo (opcional)
     * @return Ruta del archivo permanente o null si falla
     */
    fun saveBase64ToPermanentFile(context: Context, base64Data: String, fileName: String? = null): String? {
        return try {
            // Crear directorio KYC si no existe
            val kycDir = File(context.filesDir, KYC_IMAGES_DIR)
            if (!kycDir.exists()) {
                kycDir.mkdirs()
            }

            // Generar nombre único si no se proporciona
            val finalFileName = fileName ?: "${UUID.randomUUID()}.mp4"
            val permanentFile = File(kycDir, finalFileName)

            // Convertir base64 a bytes y escribir archivo
            val bytes = Base64.decode(base64Data, Base64.NO_WRAP)
            FileOutputStream(permanentFile).use { outputStream ->
                outputStream.write(bytes)
            }

            Log.d(TAG, "Base64 saved as permanent file: ${permanentFile.absolutePath}")
            permanentFile.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Error saving base64 to permanent file: ${e.message}", e)
            null
        }
    }

    /**
     * Comprime un video y lo guarda como archivo permanente
     * @param context Contexto de la aplicación
     * @param videoUri URI del video sin comprimir
     * @param fileName Nombre del archivo final (opcional)
     * @return Ruta del archivo comprimido o null si falla
     */
    suspend fun compressAndSaveVideo(
        context: Context,
        videoUri: Uri,
        fileName: String? = null
    ): String? = suspendCancellableCoroutine { continuation ->
        try {
            val kycDir = File(context.filesDir, KYC_IMAGES_DIR).apply { mkdirs() }
            val finalFileName = fileName ?: "liveness_compressed_${System.currentTimeMillis()}.mp4"
            val targetFile = File(kycDir, finalFileName)

            Log.d(TAG, "Starting video compression...")
            Log.d(TAG, "Source URI: $videoUri")
            Log.d(TAG, "Target file: ${targetFile.absolutePath}")

            // Configurar compresión de video
            val uris = mutableListOf(videoUri)

            VideoCompressor.start(
                context = context,
                uris = uris,
                isStreamable = true,
                sharedStorageConfiguration = SharedStorageConfiguration(
                    saveAt = SaveLocation.movies,
                    subFolderName = "JaakVideos"
                ),
                configureWith = Configuration(
                    quality = VideoQuality.LOW,
                    videoNames = listOf(finalFileName),
                    isMinBitrateCheckEnabled = false,
                    disableAudio = false,
                    keepOriginalResolution = false,
                    videoWidth = 640.0,
                    videoHeight = 480.0
                ),
                listener = object : CompressionListener {
                    override fun onProgress(index: Int, percent: Float) {
                        Log.d(TAG, "Compression progress: ${percent.toInt()}%")
                    }

                    override fun onStart(index: Int) {
                        Log.d(TAG, "Compression started for video $index")
                    }

                    override fun onSuccess(index: Int, size: Long, path: String?) {
                        Log.d(TAG, "✅ Video compressed successfully!")
                        Log.d(TAG, "Compressed size: ${size / 1024} KB")
                        Log.d(TAG, "Compressed path: $path")

                        if (path != null) {
                            try {
                                // Copiar el video comprimido al almacenamiento interno permanente
                                val compressedFile = File(path)
                                if (compressedFile.exists()) {
                                    FileInputStream(compressedFile).use { input ->
                                        FileOutputStream(targetFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    // Eliminar el archivo temporal de la compresión
                                    compressedFile.delete()

                                    Log.d(TAG, "✅ Video copied to permanent storage: ${targetFile.absolutePath}")
                                    continuation.resume(targetFile.absolutePath)
                                } else {
                                    Log.e(TAG, "Compressed file doesn't exist: $path")
                                    continuation.resume(null)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error copying compressed video: ${e.message}", e)
                                continuation.resume(null)
                            }
                        } else {
                            Log.e(TAG, "Compression returned null path")
                            continuation.resume(null)
                        }
                    }

                    override fun onFailure(index: Int, failureMessage: String) {
                        Log.e(TAG, "❌ Video compression failed: $failureMessage")
                        continuation.resume(null)
                    }

                    override fun onCancelled(index: Int) {
                        Log.w(TAG, "⚠️ Video compression cancelled")
                        continuation.resume(null)
                    }
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error compressing video: ${e.message}", e)
            continuation.resume(null)
        }
    }
}