package com.jaak.kyc.utils


import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import com.google.gson.Gson
import com.jaak.kyc.data.model.ErrorModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

object
Utils {

    fun responseBodyToResultsModel(responseBody: ResponseBody): ErrorModel {
        val json = responseBody.string()
        return Gson().fromJson(json, ErrorModel::class.java)
    }

    fun resultsModelToResponseBody(errorModel: ErrorModel): ResponseBody {
        val json = Gson().toJson(errorModel)
        return json.toResponseBody("application/json".toMediaTypeOrNull())
    }

    // Función para convertir una imagen representada por su URI en una cadena Base64
    fun uriToBase64(contentResolver: ContentResolver, uri: Uri): String? {
        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            bytes?.let {
                return Base64.encodeToString(bytes, Base64.DEFAULT)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
        return null
    }

    fun videoFileUriToBase64(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream?.read(buffer).also { bytesRead = it!! } != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
            val videoByteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(videoByteArray, Base64.DEFAULT)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun videoCameraUriToBase64(uri: Uri): String? {
        return try {
            val videoFile = File(uri.path!!)
            val inputStream = FileInputStream(videoFile)
            val byteArrayOutputStream = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
            val videoByteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(videoByteArray, Base64.DEFAULT)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun base64ToBitmap(base64String: String): Bitmap? {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    @Suppress("DEPRECATION")
    fun resolveToReadableUri(context: Context, input: Uri): Uri? {
        // 1) Si ya es content:// úsalo
        if (input.scheme == "content") return input

        val raw = input.path ?: input.toString()
        val base = raw.substringAfterLast('/')
        val parentDirPath = raw.substringBeforeLast('/', missingDelimiterValue = "/storage/emulated/0/Movies")
        val moviesDir = File("/storage/emulated/0/Movies")

        // Candidatos normalizados
        val normalized = buildList {
            add(raw)
            if (raw.endsWith("_temp")) add(raw.removeSuffix("_temp"))
            if (raw.endsWith(".mp4_temp")) add(raw.removeSuffix(".mp4_temp") + ".mp4")
        }

        // 2) Función para checar existencia como File -> Uri
        fun tryFileCandidates(): Uri? {
            normalized.forEach { p ->
                val f = File(p)
                if (f.exists() && f.length() > 0L) return Uri.fromFile(f)
            }
            return null
        }

        // 3) Retry corto por si el rename está en progreso
        repeat(10) { // ~10 * 120ms = ~1.2s
            tryFileCandidates()?.let { return it }
            Thread.sleep(120)
        }

        // 4) Buscar en la carpeta Movies por prefijo
        //    Prefijo = nombre sin sufijo "_temp"
        val prefix = when {
            base.endsWith(".mp4_temp") -> base.removeSuffix(".mp4_temp")
            base.endsWith("_temp") -> base.removeSuffix("_temp")
            else -> base
        }

        if (moviesDir.isDirectory) {
            val best = moviesDir.listFiles { f ->
                val n = f.name
                n.startsWith(prefix) && n.endsWith(".mp4")
            }?.maxByOrNull { it.lastModified() }

            if (best != null && best.exists() && best.length() > 0L) {
                return Uri.fromFile(best)
            }
        }

        // 5) MediaStore LIKE + ventana de tiempo reciente
        val twoMinAgo = (System.currentTimeMillis() / 1000) - 180 // segundos
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )
        val selection = buildString {
            append("${MediaStore.Video.Media.DATE_ADDED} > ?")
            append(" AND ${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?")
        }
        val selectionArgs = arrayOf(twoMinAgo.toString(), "$prefix%")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(0)
                return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            }
        }

        // 6) Último intento: si el path venía de otra carpeta, repite el scan allí
        if (parentDirPath.isNotBlank()) {
            val parent = File(parentDirPath)
            if (parent.isDirectory) {
                val best = parent.listFiles { f ->
                    f.name.startsWith(prefix) && f.name.endsWith(".mp4")
                }?.maxByOrNull { it.lastModified() }
                if (best != null && best.exists() && best.length() > 0L) {
                    return Uri.fromFile(best)
                }
            }
        }

        return null
    }



}