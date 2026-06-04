package com.demo.multimodalpicturebookforchildren.data.api

import com.demo.multimodalpicturebookforchildren.data.model.StoryBean
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 故事列表响应包裹数据实体
 */
data class StoryListResponse(
    val total: Int,
    val lastPage: Int,
    val size: Int,
    val data: List<StoryBean>
)

/**
 * 点读识别响应包裹数据实体
 */
data class SpotReadResponse(
    val find: Int,
    val audioVO: StoryBean?
)

/**
 * 绘本点读模块 Retrofit API 声明接口
 */
interface StoryService {

    @POST("audio/list")
    suspend fun getStoryList(
        @Body request: Map<String, String>
    ): Response<BaseResponse<StoryListResponse>>

    @POST("audio/search")
    suspend fun searchStory(
        @Body request: Map<String, String>
    ): Response<BaseResponse<StoryListResponse>>

    @POST("audio/add/play/amount")
    suspend fun finalPlay(
        @Query("audioId") audioId: Int
    ): Response<BaseResponse<Unit>>

    @POST("audio/thumb")
    suspend fun likeThis(
        @Query("audioId") audioId: Int
    ): Response<BaseResponse<Unit>>

    @POST("audio/favour")
    suspend fun collectThis(
        @Query("audioId") audioId: Int
    ): Response<BaseResponse<Unit>>

    @Multipart
    @POST("audio/xiaobao")
    suspend fun getStory(
        @Part file: MultipartBody.Part
    ): Response<BaseResponse<SpotReadResponse>>
}


