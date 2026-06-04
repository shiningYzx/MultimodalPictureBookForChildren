package com.demo.multimodalpicturebookforchildren.data.repository

import android.content.Context
import com.demo.multimodalpicturebookforchildren.data.local.UserDataSharedPreferences
import com.demo.multimodalpicturebookforchildren.data.network.NetworkManager
import com.demo.multimodalpicturebookforchildren.data.api.LoginService
import com.demo.multimodalpicturebookforchildren.data.api.LoginRequest
import com.demo.multimodalpicturebookforchildren.data.api.SendSmsRequest
import com.demo.multimodalpicturebookforchildren.data.api.LoginData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class LoginRepository {
    private val loginService = NetworkManager.createService(LoginService::class.java)

    /**
     * 发送邮箱验证码
     * @param email 邮箱地址
     * @return Result<Unit>
     */
    suspend fun sendSms(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = loginService.sendSms(SendSmsRequest(email))
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse != null && baseResponse.code == 200) {
                    Result.success(Unit)
                } else {
                    val errorMsg = baseResponse?.msg ?: "获取验证码失败！"
                    Result.failure(Exception(errorMsg))
                }
            } else {
                Result.failure(Exception("服务器响应异常: ${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("网络连接异常，请检查网络！"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 邮箱验证码登录/注册
     * @param email 邮箱地址
     * @param code 验证码
     * @return Result<String> 返回登录成功后的 Token 字符串
     */
    suspend fun login(email: String, code: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = loginService.login(LoginRequest(email, code))
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse != null && baseResponse.code == 200) {
                    val loginData = baseResponse.data ?: return@withContext Result.failure(Exception("登录响应数据为空"))
                    
                    // 保存双 Token (利用升级后的 SharedPreferences)
                    val pref = UserDataSharedPreferences()
                    pref.userStatus = 1
                    pref.accessToken = loginData.token
                    
                    // 如果后端支持并返回了 refreshToken，则写入本地
                    loginData.refreshToken?.let {
                        pref.refreshToken = it
                    }

                    Result.success(loginData.token)
                } else {
                    val errorMsg = baseResponse?.msg ?: "邮箱或验证码错误！"
                    Result.failure(Exception(errorMsg))
                }
            } else {
                Result.failure(Exception("服务器响应异常: ${response.code()}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("网络连接异常，请检查网络！"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


