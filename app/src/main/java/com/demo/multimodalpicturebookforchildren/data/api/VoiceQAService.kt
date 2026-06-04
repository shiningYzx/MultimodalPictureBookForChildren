package com.demo.multimodalpicturebookforchildren.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 语音问答 AI 答复返回体
 */
data class ChatData(
    val answer: String,
    val audioPath: String
)

/**
 * 语音问答模块 Retrofit API 声明接口
 */
interface VoiceQAService {
    
    @FormUrlEncoded
    @POST("qa/chat")
    suspend fun sendQuestion(
        @Field("question") question: String
    ): Response<BaseResponse<ChatData>>

    @Multipart
    @POST("qa/voice2words")
    suspend fun voice2word(
        @Part file: MultipartBody.Part
    ): Response<BaseResponse<String>>
}

