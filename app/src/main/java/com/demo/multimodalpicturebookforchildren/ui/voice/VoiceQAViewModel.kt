package com.demo.multimodalpicturebookforchildren.ui.voice

import android.media.MediaPlayer
import androidx.lifecycle.viewModelScope
import com.demo.multimodalpicturebookforchildren.MyApplication
import com.demo.multimodalpicturebookforchildren.base.BaseMviViewModel
import com.demo.multimodalpicturebookforchildren.data.model.CallWord
import com.demo.multimodalpicturebookforchildren.data.model.Chat
import com.demo.multimodalpicturebookforchildren.data.repository.VoiceQARepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class VoiceQAViewModel : BaseMviViewModel<VoiceQAViewState, VoiceQAIntent, VoiceQAEffect>() {

    private val qaRepository = VoiceQARepository()
    private var mediaPlayer: MediaPlayer? = null
    private val tempAudioFiles = mutableMapOf<Int, File>()

    override fun createInitialState(): VoiceQAViewState = VoiceQAViewState()

    init {
        dispatchIntent(VoiceQAIntent.LoadCallWords)
    }

    override suspend fun handleIntent(intent: VoiceQAIntent) {
        when (intent) {
            is VoiceQAIntent.LoadCallWords -> loadCallWords()
            is VoiceQAIntent.ChangeCallWords -> loadCallWords()
            is VoiceQAIntent.QuestionTextChanged -> {
                updateState { copy(questionText = intent.text) }
            }
            is VoiceQAIntent.SendQuestion -> sendQuestion()
            is VoiceQAIntent.VoiceRecorded -> handleVoiceRecorded(intent.audioPath)
            is VoiceQAIntent.PlayAudio -> playAudio(intent.index, intent.audioUrl)
            is VoiceQAIntent.StopAudio -> stopAudio()
        }
    }

    private suspend fun loadCallWords() {
        val words = withContext(Dispatchers.Default) {
            CallWord.getQuestionWords(3)
        }
        updateState { copy(callWords = words) }
    }

    private fun sendQuestion() {
        val question = currentState.questionText.trim()
        if (question.isEmpty()) return

        val currentChats = currentState.chatList.toMutableList()
        val userChatIndex = currentChats.size
        // 1. 插入用户提问
        val userChat = Chat(question, Chat.SEND, null, userChatIndex.toLong())
        currentChats.add(userChat)

        // 2. 插入 AI“思考中……”占位状态
        val aiChatIndex = currentChats.size
        val aiChat = Chat("思考中……", Chat.RECEIVE, null, aiChatIndex.toLong())
        currentChats.add(aiChat)

        updateState {
            copy(
                chatList = currentChats,
                questionText = "",
                isInputEnabled = false,
                showCallWords = false
            )
        }
        emitEffect(VoiceQAEffect.ScrollToBottom)

        // 发起 API 请求
        viewModelScope.launch {
            qaRepository.sendQuestion(question)
                .onSuccess { data ->
                    updateAiChat(aiChatIndex, data.answer, data.audioPath)
                }
                .onFailure { exception ->
                    updateAiChatError(aiChatIndex, exception.localizedMessage ?: "请求失败")
                }
        }
    }

    private fun handleVoiceRecorded(audioPath: String) {
        val file = File(audioPath)
        if (!file.exists()) return

        updateState { copy(isInputEnabled = false) }
        viewModelScope.launch {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaType())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            qaRepository.voice2word(body)
                .onSuccess { text ->
                    updateState { copy(questionText = text, isInputEnabled = true) }
                }
                .onFailure { exception ->
                    emitEffect(VoiceQAEffect.ShowToast(exception.localizedMessage ?: "语音识别失败，请重试"))
                    updateState { copy(isInputEnabled = true) }
                }
        }
    }

    private fun updateAiChat(index: Int, answer: String, audioUrl: String) {
        val list = currentState.chatList.toMutableList()
        if (index < list.size) {
            list[index] = Chat(answer, Chat.RECEIVE, audioUrl, index.toLong())
        }
        updateState {
            copy(
                chatList = list,
                isInputEnabled = true
            )
        }
        emitEffect(VoiceQAEffect.ScrollToBottom)
        // 自动播放最新得到的 AI 语音答复
        playAudio(index, audioUrl)
    }

    private fun updateAiChatError(index: Int, errorMsg: String) {
        val list = currentState.chatList.toMutableList()
        if (index < list.size) {
            list[index] = Chat("抱歉，获取回答失败啦：$errorMsg", Chat.RECEIVE, null, index.toLong())
        }
        updateState {
            copy(
                chatList = list,
                isInputEnabled = true
            )
        }
    }

    private fun playAudio(index: Int, audioUrl: String) {
        if (audioUrl.isEmpty()) return
        stopAudio()

        viewModelScope.launch {
            updateState { copy(isAudioPlaying = true, currentlyPlayingIndex = index) }
            try {
                val file = tempAudioFiles[index] ?: withContext(Dispatchers.IO) {
                    val tempFile = File.createTempFile("audio_mvi_${System.currentTimeMillis()}", ".mp3", MyApplication.context.cacheDir)
                    URL(audioUrl).openStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }.also { tempAudioFiles[index] = it }

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        stopAudio()
                    }
                }
            } catch (e: Exception) {
                emitEffect(VoiceQAEffect.ShowToast("语音播放失败"))
                stopAudio()
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        updateState { copy(isAudioPlaying = false, currentlyPlayingIndex = -1) }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
        // 离开页面时彻底清除 Cache 的所有临时音频文件，杜绝残留！
        tempAudioFiles.values.forEach {
            if (it.exists()) it.delete()
        }
        tempAudioFiles.clear()
    }
}


