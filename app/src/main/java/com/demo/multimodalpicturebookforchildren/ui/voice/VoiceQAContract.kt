package com.demo.multimodalpicturebookforchildren.ui.voice

import com.demo.multimodalpicturebookforchildren.base.UiEffect
import com.demo.multimodalpicturebookforchildren.base.UiIntent
import com.demo.multimodalpicturebookforchildren.base.UiState
import com.demo.multimodalpicturebookforchildren.data.model.Chat

/**
 * 语音问答页状态机唯一可变状态
 */
data class VoiceQAViewState(
    val chatList: List<Chat> = emptyList(),
    val questionText: String = "",
    val isInputEnabled: Boolean = true,
    val showCallWords: Boolean = true,
    val callWords: List<String> = emptyList(),
    val isAudioPlaying: Boolean = false,
    val currentlyPlayingIndex: Int = -1
) : UiState

/**
 * 语音问答页用户动作意图
 */
sealed class VoiceQAIntent : UiIntent {
    object LoadCallWords : VoiceQAIntent()
    object ChangeCallWords : VoiceQAIntent()
    data class QuestionTextChanged(val text: String) : VoiceQAIntent()
    object SendQuestion : VoiceQAIntent()
    data class VoiceRecorded(val audioPath: String) : VoiceQAIntent()
    data class PlayAudio(val index: Int, val audioUrl: String) : VoiceQAIntent()
    object StopAudio : VoiceQAIntent()
}

/**
 * 语音问答页单次副作用
 */
sealed class VoiceQAEffect : UiEffect {
    data class ShowToast(val message: String) : VoiceQAEffect()
    object ScrollToBottom : VoiceQAEffect()
}


