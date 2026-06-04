package com.demo.multimodalpicturebookforchildren.data.network

import android.content.Context
import android.content.Intent
import com.demo.multimodalpicturebookforchildren.MyApplication
import com.demo.multimodalpicturebookforchildren.ui.login.LoginActivity
import com.demo.multimodalpicturebookforchildren.data.local.UserDataSharedPreferences
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * 拦截 HTTP 401 并自动执行双 Token 刷新重试的 Authenticator
 */
class TokenAuthenticator() : Authenticator {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            // 如果已经重试过一次了，仍然失败，说明 Refresh Token 也过期了，直接清理状态强制回登录页
            handleLogout()
            return null
        }

        val pref = UserDataSharedPreferences()
        val refreshToken = pref.refreshToken ?: ""
        // 如果本地没有 Refresh Token，则直接强制跳转登录
        if (refreshToken.isEmpty()) {
            handleLogout()
            return null
        }

        // 进行同步锁控制，避免多个并发请求同时触发多次刷新
        synchronized(this) {
            val currentRequestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val savedAccessToken = pref.accessToken ?: ""

            // 如果当前请求的 Token 和本地最新保存的 Token 不一致，说明有其他先发请求已经刷新成功了
            // 直接用最新的 AccessToken 重试该请求即可
            if (currentRequestToken != savedAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $savedAccessToken")
                    .build()
            }

            // 同步发起刷新 Token 的 HTTP 请求（此处使用独立的 OkHttpClient 以防发生 401 循环嵌套导致栈溢出）
            val refreshSuccess = performTokenRefresh(refreshToken, pref)

            return if (refreshSuccess) {
                // 刷新成功，装配新的 Access Token 重新发起原请求
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${pref.accessToken}")
                    .build()
            } else {
                // 刷新失败，Refresh Token 也已过期，清理凭证并强制退回登录页
                handleLogout()
                null
            }
        }
    }

    // 统计当前请求失败重试的次数
    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    // 同步发送刷新 Token 的网络请求
    private fun performTokenRefresh(refreshToken: String, pref: UserDataSharedPreferences): Boolean {
        val refreshUrl = "http://xxx" // Token 刷新地址
        val jsonStr = JSONObject().apply {
            put("refreshToken", refreshToken)
        }.toString()

        val request = Request.Builder()
            .url(refreshUrl)
            .post(jsonStr.toRequestBody(jsonMediaType))
            .build()

        val client = OkHttpClient.Builder().build()
        return try {
            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: return false
            val jsonObject = JSONObject(bodyStr)

            if (jsonObject.optInt("code") == 200) {
                val dataObj = jsonObject.optJSONObject("data")
                val newAccessToken = dataObj?.optString("token") ?: ""
                val newRefreshToken = dataObj?.optString("refreshToken") ?: ""

                if (newAccessToken.isNotEmpty() && newRefreshToken.isNotEmpty()) {
                    pref.accessToken = newAccessToken
                    pref.refreshToken = newRefreshToken
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // 清理凭证状态并强制回到登录界面
    private fun handleLogout() {
        val context = MyApplication.context
        val pref = UserDataSharedPreferences(context)
        pref.clearTokens()

        // 强行跳转回 LoginActivity，并清空当前所有 Activity 栈
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}


