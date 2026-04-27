package ai.jaak.kyc.data.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * CookieJar persistente que guarda cookies en SharedPreferences.
 * Permite que el refreshToken (HTTP-only cookie) sea automáticamente
 * persistido y reenviado al backend en cada request de refresh.
 */
class PersistentCookieJar(context: Context) : CookieJar {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("okhttp_cookies", Context.MODE_PRIVATE)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val editor = prefs.edit()
        for (cookie in cookies) {
            editor.putString(cookieKey(url.host, cookie.name), cookie.toString())
        }
        editor.apply()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        val allEntries = prefs.all

        for ((key, value) in allEntries) {
            if (key.startsWith("${url.host}|") && value is String) {
                val cookie = Cookie.parse(url, value)
                if (cookie != null) {
                    cookies.add(cookie)
                }
            }
        }
        return cookies
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun cookieKey(host: String, name: String) = "$host|$name"
}
