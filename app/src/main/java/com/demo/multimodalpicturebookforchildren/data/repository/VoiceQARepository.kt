package com.demo.multimodalpicturebookforchildren.data.repository

import com.demo.multimodalpicturebookforchildren.data.api.ChatData
import com.demo.multimodalpicturebookforchildren.data.api.VoiceQAService
import com.demo.multimodalpicturebookforchildren.data.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import java.io.IOException

class VoiceQARepository {
    private val qaService = NetworkManager.createService(VoiceQAService::class.java)

    /**
     * 发送文本问题
     * @param question 问题文本
     * @return Result<ChatData> 包装的响应数据
     */
    suspend fun sendQuestion(question: String): Result<ChatData> = withContext(Dispatchers.IO) {
        try {
            val response = qaService.sendQuestion(question)
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse != null && baseResponse.code == 200) {
                    val data = baseResponse.data
                    if (data != null) {
                        Result.success(data)
                    } else {
                        Result.failure(Exception("服务器未返回具体回答"))
                    }
                } else {
                    val errorMsg = baseResponse?.msg ?: "获取回答失败！"
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
     * 语音转文字
     * @param file 音频文件 Part
     * @return Result<String> 识别出的文本
     */
    suspend fun voice2word(file: MultipartBody.Part): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = qaService.voice2word(file)
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse != null && baseResponse.code == 200) {
                    val data = baseResponse.data
                    if (data != null) {
                        Result.success(data)
                    } else {
                        Result.success("")
                    }
                } else {
                    val errorMsg = baseResponse?.msg ?: "语音识别失败！"
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
