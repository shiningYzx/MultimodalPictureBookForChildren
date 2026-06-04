package com.demo.multimodalpicturebookforchildren.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 接口基础通用包裹响应体
 */
data class BaseResponse<T>(
    val code: Int,
    val msg: String,
    val data: T?
)

/**
 * 发送验证码请求体
 */
data class SendSmsRequest(val email: String)

/**
 * 登录/注册请求体
 */
data class LoginRequest(val email: String, val code: String)

/**
 * 登录成功返回体（包含双 Token）
 */
data class LoginData(
    val token: String,          
    val refreshToken: String?   
)

/**
 * 登录模块 Retrofit API 声明接口
 */
interface LoginService {
    @POST("user/sendCode")
    suspend fun sendSms(
        @Body request: SendSmsRequest
    ): Response<BaseResponse<Unit>>

    
    @POST("user/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<BaseResponse<LoginData>>
}

