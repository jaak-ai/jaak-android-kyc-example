package ai.jaak.kyc.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor de licencias dinámicas obtenidas del flujo KYC
 * Almacena la licencia extraída del campo 'transparent' del servicio createFlow
 */
object FlowLicenseManager {
    
    private const val PREF_NAME = "flow_license_prefs"
    private const val KEY_CURRENT_LICENSE = "current_flow_license"
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Guarda la licencia del flujo actual
     * @param context Contexto de la aplicación
     * @param license Licencia extraída (ya debe venir con "L" al inicio)
     */
    fun saveLicense(context: Context, license: String) {
        getPreferences(context).edit().putString(KEY_CURRENT_LICENSE, license).apply()
        android.util.Log.d("FlowLicenseManager", "✓ Licencia guardada: $license")
    }
    
    /**
     * Obtiene la licencia del flujo actual
     * @param context Contexto de la aplicación
     * @return Licencia guardada o null si no existe
     */
    fun getLicense(context: Context): String? {
        val license = getPreferences(context).getString(KEY_CURRENT_LICENSE, null)
        android.util.Log.d("FlowLicenseManager", "→ Licencia obtenida: $license")
        return license
    }
    
    /**
     * Limpia la licencia guardada
     * @param context Contexto de la aplicación
     */
    fun clearLicense(context: Context) {
        getPreferences(context).edit().remove(KEY_CURRENT_LICENSE).apply()
        android.util.Log.d("FlowLicenseManager", "✓ Licencia limpiada")
    }
    
    /**
     * Verifica si hay una licencia guardada
     * @param context Contexto de la aplicación
     * @return true si existe una licencia, false en caso contrario
     */
    fun hasLicense(context: Context): Boolean {
        return getLicense(context) != null
    }
}
