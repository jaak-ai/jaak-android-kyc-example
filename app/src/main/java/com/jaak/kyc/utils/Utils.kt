package com.jaak.kyc.utils


import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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

}