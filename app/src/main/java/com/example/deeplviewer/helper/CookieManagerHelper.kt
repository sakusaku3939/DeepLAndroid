package com.example.deeplviewer.helper

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import com.example.deeplviewer.R
import java.net.URLEncoder
import androidx.core.content.edit

class CookieManagerHelper {
    companion object {
        private fun getCookieManager(): CookieManager {
            return CookieManager.getInstance()
        }
    }

    fun addPrivacyCookie() {
        val privacyValue = "{\"v\":2,\"t\":${System.currentTimeMillis().div(1000)},\"m\":\"STRICT\",\"consent\":[\"NECESSARY\"]}"

        getCookieManager().setCookie(
            ".deepl.com",
            "privacySettings=${URLEncoder.encode(privacyValue, Charsets.UTF_8.name())}"
        )
    }

    fun saveCookies(context: Context) {
        if (isAutoDeleteEnabled(context)) {
            clearCookies()
        } else {
            getCookieManager().flush()
        }
    }

    fun persistCookies(context: Context) {
        if (!isAutoDeleteEnabled(context)) {
            getCookieManager().flush()
        }
    }

    private fun isAutoDeleteEnabled(context: Context): Boolean =
        context.getSharedPreferences("config", Context.MODE_PRIVATE)
            .getBoolean(context.getString(R.string.key_auto_delete_cookies), false)

    // Disable cookie values in SharedPreferences according to the bug fix for cookie expiration in v8.5
    fun migrateCookie(context: Context) {
        val sharedPreferences = context.getSharedPreferences("DeepLCookies", Context.MODE_PRIVATE)
        val savedCookie = sharedPreferences.getString("cookie", null)

        if (savedCookie != null) {
            clearCookies()
            sharedPreferences.edit { clear() }
        }
    }

    private fun clearCookies() {
        getCookieManager().removeAllCookies { success ->
            getCookieManager().flush()
            if (success) {
                Log.d("CookieManagerHelper", "Cookies successfully removed.")
            } else {
                Log.e("CookieManagerHelper", "Failed to remove cookies.")
            }
        }
    }
}
