package com.jaak.kyc.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jaak.kyc.data.model.api.CompanyInfo
import com.jaak.kyc.data.model.api.UserInfo
import com.jaak.kyc.data.model.KycProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "KYC_APP"
        private const val KEY_PROFILE = "kyc_profile"

        // Auth keys
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_IS_LOGGED_IN = "IS_LOGGED_IN"
        private const val KEY_USER_INFO = "user_info"
        private const val KEY_COMPANY_INFO = "company_info"

        // KYC Profiles keys (legacy - no usar directamente)
        private const val KEY_KYC_PROFILES = "kyc_profiles"
        private const val KEY_KYC_PROFILES_PREFIX = "kyc_profiles_user_"

        // Perfiles disponibles
        const val PROFILE_QA = "QA"
        const val PROFILE_SANDBOX = "Sandbox"
        const val PROFILE_DEV = "Dev"

        // URLs por perfil (API principal)
        private const val URL_QA = "https://api.qa.jaak.ai/"
        private const val URL_SANDBOX = "https://api.sandbox.jaak.ai/"
        private const val URL_DEV = "https://api.dev.jaak.ai/"

        // URLs de autenticación por perfil
        private const val URL_AUTH_QA = "https://tayrona.qa.jaak.ai/"
        private const val URL_AUTH_SANDBOX = "https://api.sandbox.jaak.ai/"
        private const val URL_AUTH_DEV = "https://api.dev.jaak.ai/"

        // Perfil por defecto
        private const val DEFAULT_PROFILE = PROFILE_QA
    }

    /**
     * Obtiene el perfil actual
     */
    fun getCurrentProfile(): String {
        return prefs.getString(KEY_PROFILE, DEFAULT_PROFILE) ?: DEFAULT_PROFILE
    }

    /**
     * Establece el perfil actual
     */
    fun setCurrentProfile(profile: String) {
        prefs.edit().putString(KEY_PROFILE, profile).apply()
    }

    /**
     * Obtiene la URL base según el perfil actual
     */
    fun getCurrentBaseUrl(): String {
        return getBaseUrlForProfile(getCurrentProfile())
    }

    /**
     * Obtiene la URL base para un perfil específico
     */
    fun getBaseUrlForProfile(profile: String): String {
        return when (profile) {
            PROFILE_QA -> URL_QA
            PROFILE_SANDBOX -> URL_SANDBOX
            PROFILE_DEV -> URL_DEV
            else -> URL_QA // Fallback a QA
        }
    }

    /**
     * Obtiene la URL de autenticación según el perfil actual
     */
    fun getCurrentAuthUrl(): String {
        return getAuthUrlForProfile(getCurrentProfile())
    }

    /**
     * Obtiene la URL de autenticación para un perfil específico
     */
    fun getAuthUrlForProfile(profile: String): String {
        return when (profile) {
            PROFILE_QA -> URL_AUTH_QA
            PROFILE_SANDBOX -> URL_AUTH_SANDBOX
            PROFILE_DEV -> URL_AUTH_DEV
            else -> URL_AUTH_QA // Fallback a QA
        }
    }

    /**
     * Obtiene todos los perfiles disponibles
     */
    fun getAvailableProfiles(): List<String> {
        return listOf(PROFILE_QA, PROFILE_SANDBOX, PROFILE_DEV)
    }

    // ==================== AUTHENTICATION METHODS ====================

    /**
     * Guarda el access token
     */
    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    /**
     * Obtiene el access token guardado
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Obtiene el token formateado con "Bearer "
     */
    fun getBearerToken(): String? {
        val token = getAccessToken()
        return if (token != null) "Bearer $token" else null
    }

    /**
     * Guarda el API Key de larga duración
     */
    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    /**
     * Obtiene el API Key de larga duración
     */
    fun getApiKey(): String? {
        return prefs.getString(KEY_API_KEY, null)
    }

    /**
     * Establece el estado de login
     */
    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    /**
     * Verifica si el usuario está logueado
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Guarda la información del usuario
     */
    fun saveUserInfo(userInfo: UserInfo) {
        val json = gson.toJson(userInfo)
        prefs.edit().putString(KEY_USER_INFO, json).apply()
    }

    /**
     * Obtiene la información del usuario guardada
     */
    fun getUserInfo(): UserInfo? {
        val json = prefs.getString(KEY_USER_INFO, null)
        return if (json != null) {
            gson.fromJson(json, UserInfo::class.java)
        } else {
            null
        }
    }

    /**
     * Guarda la información de la compañía
     */
    fun saveCompanyInfo(companyInfo: CompanyInfo) {
        val json = gson.toJson(companyInfo)
        prefs.edit().putString(KEY_COMPANY_INFO, json).apply()
    }

    /**
     * Obtiene la información de la compañía guardada
     */
    fun getCompanyInfo(): CompanyInfo? {
        val json = prefs.getString(KEY_COMPANY_INFO, null)
        return if (json != null) {
            gson.fromJson(json, CompanyInfo::class.java)
        } else {
            null
        }
    }

    /**
     * Cierra sesión y limpia todos los datos de autenticación
     * NOTA: Los perfiles KYC del usuario se mantienen para cuando vuelva a iniciar sesión
     */
    fun logout() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_INFO)
            .remove(KEY_COMPANY_INFO)
            .remove(KEY_API_KEY)
            .apply()
    }

    /**
     * Limpia los perfiles KYC del usuario actual
     * Útil si se quiere borrar la configuración del usuario específico
     */
    fun clearUserKycProfiles() {
        val key = getKycProfilesKey()
        prefs.edit().remove(key).apply()
    }

    /**
     * Limpia todos los datos de la aplicación
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ==================== KYC PROFILES METHODS ====================

    /**
     * Obtiene la clave de perfiles KYC asociada al usuario actual
     */
    private fun getKycProfilesKey(): String {
        val userInfo = getUserInfo()
        return if (userInfo != null) {
            // Usar email del usuario como identificador único
            "${KEY_KYC_PROFILES_PREFIX}${userInfo.email}"
        } else {
            // Fallback: usar clave legacy si no hay usuario logueado
            KEY_KYC_PROFILES
        }
    }

    /**
     * Guarda un perfil KYC
     * Si ya existe (mismo ID), lo actualiza
     * Los perfiles se asocian al usuario logueado
     */
    fun saveKycProfile(profile: KycProfile) {
        val profiles = getKycProfiles().toMutableList()

        // Si el perfil se marca como predeterminado, desmarcar los demás
        if (profile.isDefault) {
            profiles.forEachIndexed { index, p ->
                if (p.id != profile.id && p.isDefault) {
                    profiles[index] = p.copy(isDefault = false)
                }
            }
        }

        // Buscar si ya existe el perfil
        val existingIndex = profiles.indexOfFirst { it.id == profile.id }
        if (existingIndex != -1) {
            // Actualizar perfil existente
            profiles[existingIndex] = profile.copy(updatedAt = System.currentTimeMillis())
        } else {
            // Agregar nuevo perfil
            profiles.add(profile)
        }

        // Guardar lista actualizada con clave específica del usuario
        val json = gson.toJson(profiles)
        val key = getKycProfilesKey()
        prefs.edit().putString(key, json).apply()
    }

    /**
     * Obtiene todos los perfiles KYC guardados del usuario actual
     */
    fun getKycProfiles(): List<KycProfile> {
        val key = getKycProfilesKey()
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<KycProfile>>() {}.type
        return gson.fromJson(json, type)
    }

    /**
     * Obtiene un perfil KYC por ID
     */
    fun getKycProfileById(id: String): KycProfile? {
        return getKycProfiles().firstOrNull { it.id == id }
    }

    /**
     * Obtiene el perfil predeterminado
     */
    fun getDefaultKycProfile(): KycProfile? {
        return getKycProfiles().firstOrNull { it.isDefault }
    }

    /**
     * Elimina un perfil KYC por ID del usuario actual
     */
    fun deleteKycProfile(id: String) {
        val profiles = getKycProfiles().toMutableList()
        profiles.removeAll { it.id == id }

        val json = gson.toJson(profiles)
        val key = getKycProfilesKey()
        prefs.edit().putString(key, json).apply()
    }

    /**
     * Verifica si hay al menos un perfil KYC configurado
     */
    fun hasKycProfiles(): Boolean {
        return getKycProfiles().isNotEmpty()
    }

    /**
     * Marca un perfil como predeterminado para el usuario actual
     */
    fun setDefaultProfile(profileId: String) {
        val profiles = getKycProfiles().toMutableList()

        profiles.forEachIndexed { index, profile ->
            profiles[index] = if (profile.id == profileId) {
                profile.copy(isDefault = true, updatedAt = System.currentTimeMillis())
            } else {
                profile.copy(isDefault = false, updatedAt = System.currentTimeMillis())
            }
        }

        val json = gson.toJson(profiles)
        val key = getKycProfilesKey()
        prefs.edit().putString(key, json).apply()
    }
}
