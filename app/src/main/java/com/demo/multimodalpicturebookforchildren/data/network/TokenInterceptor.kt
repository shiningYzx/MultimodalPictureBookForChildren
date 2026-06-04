package com.demo.multimodalpicturebookforchildren.data.network

import android.content.Context
import com.demo.multimodalpicturebookforchildren.data.local.UserDataSharedPreferences
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 自动装配 Access Token 的 OkHttp 拦截器
 */
class TokenInterceptor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val pref = UserDataSharedPreferences()
        val accessToken = pref.accessToken ?: ""

        // 如果本地存有 Access Token 且请求头里没有 Authorization，则自动装配
        val newRequest = if (accessToken.isNotEmpty() && originalRequest.header("Authorization") == null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}


