package com.demo.multimodalpicturebookforchildren.ui.story

import com.demo.multimodalpicturebookforchildren.base.UiEffect
import com.demo.multimodalpicturebookforchildren.base.UiIntent
import com.demo.multimodalpicturebookforchildren.base.UiState
import com.demo.multimodalpicturebookforchildren.data.model.StoryBean

/**
 * 绘本点读页状态
 */
data class StoryReadingViewState(
    val storyList: List<StoryBean> = emptyList(),
    val totalStories: Int = 0,
    val currentPage: Int = 1,
    val searchText: String = "",
    val isSearchMode: Boolean = false,
    val hasMore: Boolean = false,
    
    // 当前播放的故事绘本
    val selectedStory: StoryBean? = null,
    val isAudioPlaying: Boolean = false,
    val isMediaPlayerReady: Boolean = false,
    val audioDuration: Int = 0,
    val currentAudioProgress: Int = 0,
    val activeLyricIndex: Int = 0,
    
    // 当前选中的故事在 Pager 列表中的位置 (用以高亮与联动)
    val selectedPage: Int = 1,
    val selectedPosition: Int = 0,
    
    // 用户点赞收藏交互
    val isLikeNowStory: Boolean = false,
    val isCollectNowStory: Boolean = false,
    
    // 机器人点读弹窗状态
    val showSpotReadDialog: Boolean = false,
    val spotReadRequestText: String = "",
    val spotReadResponseText: String = "",
    val isSpotReadLoading: Boolean = false
) : UiState

/**
 * 绘本点读页用户动作意图
 */
sealed class StoryReadingIntent : UiIntent {
    data class LoadStories(val page: Int) : StoryReadingIntent()
    data class SearchTextChanged(val text: String) : StoryReadingIntent()
    object TriggerSearch : StoryReadingIntent()
    object ExitSearchMode : StoryReadingIntent()
    
    // 播放与切歌控制
    data class SelectStory(val story: StoryBean, val page: Int, val position: Int) : StoryReadingIntent()
    object TogglePlayPause : StoryReadingIntent()
    data class SeekToProgress(val progress: Int) : StoryReadingIntent()
    object PlayNextStory : StoryReadingIntent()
    object PlayPreviousStory : StoryReadingIntent()
    
    // 点赞与收藏
    object ToggleLike : StoryReadingIntent()
    object ToggleCollect : StoryReadingIntent()
    
    // 机器人点读 Dialog 控制
    object OpenSpotReadDialog : StoryReadingIntent()
    object CloseSpotReadDialog : StoryReadingIntent()
    data class SpotReadVoiceRecorded(val audioPath: String) : StoryReadingIntent()
}

/**
 * 绘本点读页单次副作用
 */
sealed class StoryReadingEffect : UiEffect {
    data class ShowToast(val message: String) : StoryReadingEffect()
    data class ScrollPagerToPage(val pageIndex: Int) : StoryReadingEffect()
}


