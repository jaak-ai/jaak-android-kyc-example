package com.jaak.kyc.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationUtils {
    
    /**
     * Obtiene la ubicación actual del dispositivo
     * @param context Contexto de la aplicación
     * @return Pair<latitude, longitude> o null si no se puede obtener
     */
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        return try {
            // Verificar permisos
            if (!hasLocationPermissions(context)) {
                android.util.Log.w("LocationUtils", "Location permissions not granted")
                return null
            }
            
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            
            suspendCancellableCoroutine { continuation ->
                try {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                android.util.Log.d("LocationUtils", "Location obtained: ${location.latitude}, ${location.longitude}")
                                continuation.resume(Pair(location.latitude, location.longitude))
                            } else {
                                android.util.Log.w("LocationUtils", "Location is null")
                                continuation.resume(null)
                            }
                        }
                        .addOnFailureListener { exception ->
                            android.util.Log.e("LocationUtils", "Failed to get location: ${exception.message}")
                            continuation.resume(null)
                        }
                } catch (e: SecurityException) {
                    android.util.Log.e("LocationUtils", "Security exception getting location: ${e.message}")
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationUtils", "Error getting location: ${e.message}")
            null
        }
    }
    
    /**
     * Verifica si se tienen los permisos de ubicación necesarios
     */
    private fun hasLocationPermissions(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Coordenadas de fallback para México (CDMX) cuando no se puede obtener ubicación real
     */
    fun getFallbackLocation(): Pair<Double, Double> {
        return Pair(19.4326, -99.1332) // Ciudad de México
    }
}