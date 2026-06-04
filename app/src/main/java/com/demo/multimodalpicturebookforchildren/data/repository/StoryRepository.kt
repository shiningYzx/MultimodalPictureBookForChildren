package com.demo.multimodalpicturebookforchildren.data.repository

import com.demo.multimodalpicturebookforchildren.data.api.StoryListResponse
import com.demo.multimodalpicturebookforchildren.data.api.SpotReadResponse
import com.demo.multimodalpicturebookforchildren.data.api.StoryService
import com.demo.multimodalpicturebookforchildren.data.api.VoiceQAService
import com.demo.multimodalpicturebookforchildren.data.network.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import java.io.IOException

class StoryRepository {
    private val storyService = NetworkManager.createService(StoryService::class.java)
    private val qaService = NetworkManager.createService(VoiceQAService::class.java)

    /**
     * 获取绘本故事列表
     */
    suspend fun getStoryList(page: Int): Result<StoryListResponse> = withContext(Dispatchers.IO) {
        try {
            val reqMap = mapOf("current" to page.toString(), "size" to "4")
            val response = storyService.getStoryList(reqMap)
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse != null && baseResponse.code == 200) {
                    val data = baseResponse.data
                    if (data != null) {
                        Result.success(data)
                    } else {
                        Result.failure(Exception("获取故事列表数据为空"))
                    }
                } else {
                    val errorMsg = baseResponse?.msg ?: "获取故事列表失败！"
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
     * 搜索绘本故事
     */
    suspend fun searchStory(query: String, page: Int): Result<StoryListResponse> = withContext(Dispatchers.IO) {
        try {
            val reqMap = mapOf("searchText" to query, "current" to page.toString(), "size" to "4")
            val response = storyService.searchStory(reqMap)
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse != null && baseResponse.code == 200) {
                    val data = baseResponse.data
                    if (data != null) {
                        Result.success(data)
                    } else {
                        Result.failure(Exception("搜索结果为空"))
                    }
                } else {
                    val errorMsg = baseResponse?.msg ?: "搜索故事失败！"
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
     * 点赞故事
     */
    suspend fun likeStory(audioId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = storyService.likeThis(audioId)
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse != null && baseResponse.code == 200) {
                    Result.success(Unit)
                } else {
                    val errorMsg = baseResponse?.msg ?: "操作失败！"
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
     * 收藏故事
     */
    suspend fun collectStory(audioId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = storyService.collectThis(audioId)
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse != null && baseResponse.code == 200) {
                    Result.success(Unit)
                } else {
                    val errorMsg = baseResponse?.msg ?: "操作失败！"
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
     * 上报播放统计
     */
    suspend fun finalPlay(audioId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = storyService.finalPlay(audioId)
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse != null && baseResponse.code == 200) {
                    Result.success(Unit)
                } else {
                    val errorMsg = baseResponse?.msg ?: "上报统计失败！"
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
     * 语音转文字（用于点读弹窗）
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

    /**
     * 语音点读匹配绘本故事
     */
    suspend fun getStoryByVoice(file: MultipartBody.Part): Result<SpotReadResponse> = withContext(Dispatchers.IO) {
        try {
            val response = storyService.getStory(file)
            if (response.isSuccessful) {
                val baseResponse = response.body()
                if (baseResponse != null && baseResponse.code == 200) {
                    val data = baseResponse.data
                    if (data != null) {
                        Result.success(data)
                    } else {
                        Result.failure(Exception("未返回点读匹配数据"))
                    }
                } else {
                    val errorMsg = baseResponse?.msg ?: "获取匹配故事失败！"
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
