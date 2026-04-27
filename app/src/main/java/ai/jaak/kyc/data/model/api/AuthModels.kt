package ai.jaak.kyc.data.model.api

import com.google.gson.annotations.SerializedName

/**
 * Request para el login
 */
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("captcha")
    val captcha: String
)

/**
 * Response del login.
 * El refreshToken llega como cookie HTTP-only (Set-Cookie: refreshToken=...),
 * no en el body JSON — es manejado automáticamente por PersistentCookieJar.
 */
data class LoginResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("expiresIn")
    val expiresIn: Int? = null,

    @SerializedName("user")
    val user: UserInfo,

    @SerializedName("company")
    val company: CompanyInfo
)

/**
 * Response del refresh de token.
 * El nuevo refreshToken llega como cookie HTTP-only (Set-Cookie: refreshToken=...),
 * manejado automáticamente por PersistentCookieJar.
 */
data class RefreshTokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("expiresIn")
    val expiresIn: Int? = null
)

data class UserInfo(
    @SerializedName("id")
    val id: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("rol")
    val rol: String,

    @SerializedName("passwordChangeRequired")
    val passwordChangeRequired: Boolean
)

data class CompanyInfo(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("logo")
    val logo: String? // URL de la imagen del logo
)
