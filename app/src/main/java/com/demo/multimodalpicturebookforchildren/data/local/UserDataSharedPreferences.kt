package com.demo.multimodalpicturebookforchildren.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.demo.multimodalpicturebookforchildren.MyApplication
import androidx.core.content.edit

class UserDataSharedPreferences (context: Context = MyApplication.context) {

    companion object {
        private const val TAG = "UserDataSharedPreferences"
    }

    private lateinit var preferences: SharedPreferences

    init {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            preferences = EncryptedSharedPreferences.create(
                context,
                "userDataSecure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Secure storage initialization failed, falling back to standard SP", e)
            preferences = context.getSharedPreferences("userData", Context.MODE_PRIVATE)
        }
    }

    /**
     * 用户登录状态
     */
    var userStatus: Int
        get() = preferences.getInt("userStatus", 0)
        set(value) = preferences.edit { putInt("userStatus", value) }

    /**
     * 访问令牌 AccessToken
     */
    var accessToken: String
        get() = preferences.getString("accessToken", "") ?: ""
        set(value) = preferences.edit { putString("accessToken", value) }

    /**
     * 刷新令牌 RefreshToken
     */
    var refreshToken: String
        get() = preferences.getString("refreshToken", "") ?: ""
        set(value) = preferences.edit { putString("refreshToken", value) }

    /**
     * 兼容旧 Java 代码的 token 属性，自动在字节码层面生成 getToken() 和 setToken(String)
     */
    var token: String
        get() = accessToken
        set(value) {
            accessToken = value
            preferences.edit { putString("token", value) }
        }

    /**
     * 清空存储状态与凭证
     */
    fun clearTokens() {
        preferences.edit { 
            remove("token")
            remove("accessToken")
            remove("refreshToken")
            putInt("userStatus", 0)
        }
    }
}

