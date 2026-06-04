package com.demo.multimodalpicturebookforchildren.data.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 线程安全、懒加载的 Retrofit 全局管理单例
 */
object NetworkManager {
    private const val BASE_URL = "http://xxx/"

    @Volatile
    private var retrofit: Retrofit? = null

    /**
     * 获取全局的 Retrofit 实例
     */
    fun getRetrofit(): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: createRetrofit().also { retrofit = it }
        }
    }

    private fun createRetrofit(): Retrofit {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 配置自动注入令牌与无感刷新的网络客户端
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(TokenInterceptor())
            .authenticator(TokenAuthenticator())
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 动态生成具体的 Retrofit API Service 接口实例
     */
    fun <T> createService(serviceClass: Class<T>,): T {
        return getRetrofit().create(serviceClass)
    }
}

