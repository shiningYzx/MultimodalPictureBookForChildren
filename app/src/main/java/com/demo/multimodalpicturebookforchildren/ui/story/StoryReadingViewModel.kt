package com.demo.multimodalpicturebookforchildren.ui.story

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.demo.multimodalpicturebookforchildren.base.BaseMviViewModel
import com.demo.multimodalpicturebookforchildren.data.model.StoryBean
import com.demo.multimodalpicturebookforchildren.data.repository.StoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class StoryReadingViewModel : BaseMviViewModel<StoryReadingViewState, StoryReadingIntent, StoryReadingEffect>() {

    private val storyRepository = StoryRepository()
    
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    override fun createInitialState(): StoryReadingViewState = StoryReadingViewState()

    init {
        dispatchIntent(StoryReadingIntent.LoadStories(1))
    }

    override suspend fun handleIntent(intent: StoryReadingIntent) {
        when (intent) {
            is StoryReadingIntent.LoadStories -> loadStories(intent.page)
            is StoryReadingIntent.SearchTextChanged -> {
                updateState { copy(searchText = intent.text) }
            }
            is StoryReadingIntent.TriggerSearch -> triggerSearch(1)
            is StoryReadingIntent.ExitSearchMode -> {
                updateState { copy(isSearchMode = false, searchText = "") }
                loadStories(1)
            }
            is StoryReadingIntent.SelectStory -> selectStory(intent.story, intent.page, intent.position)
            is StoryReadingIntent.TogglePlayPause -> togglePlayPause()
            is StoryReadingIntent.SeekToProgress -> seekTo(intent.progress)
            is StoryReadingIntent.PlayNextStory -> playNext()
            is StoryReadingIntent.PlayPreviousStory -> playPrevious()
            is StoryReadingIntent.ToggleLike -> toggleLike()
            is StoryReadingIntent.ToggleCollect -> toggleCollect()
            is StoryReadingIntent.OpenSpotReadDialog -> {
                updateState {
                    copy(
                        showSpotReadDialog = true,
                        spotReadRequestText = "",
                        spotReadResponseText = "",
                        isSpotReadLoading = false
                    )
                }
                // 打开点读机器人时，暂停当前的音乐播放
                pauseAudio()
            }
            is StoryReadingIntent.CloseSpotReadDialog -> {
                updateState { copy(showSpotReadDialog = false) }
            }
            is StoryReadingIntent.SpotReadVoiceRecorded -> handleSpotReadRecord(intent.audioPath)
        }
    }

    private fun loadStories(page: Int) {
        viewModelScope.launch {
            storyRepository.getStoryList(page)
                .onSuccess { resData ->
                    val hasMore = (page * 4) < resData.total
                    updateState {
                        copy(
                            storyList = resData.data,
                            totalStories = resData.total,
                            currentPage = page,
                            isSearchMode = false,
                            hasMore = hasMore
                        )
                    }
                    // 默认选中并初始化这一页的第一首故事 (如果当前还没有选中过故事)
                    if (currentState.selectedStory == null && resData.data.isNotEmpty()) {
                        selectStory(resData.data[0], page, 0)
                        // 默认第一首初始化完毕后不要自动播放，让用户点击播放
                        pauseAudio()
                    }
                }
                .onFailure { exception ->
                    emitEffect(StoryReadingEffect.ShowToast(exception.localizedMessage ?: "获取故事列表失败"))
                }
        }
    }

    private fun triggerSearch(page: Int) {
        val query = currentState.searchText.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            storyRepository.searchStory(query, page)
                .onSuccess { resData ->
                    if (resData.data.isNotEmpty()) {
                        val hasMore = (page * 4) < resData.total
                        updateState {
                            copy(
                                storyList = resData.data,
                                totalStories = resData.total,
                                currentPage = page,
                                isSearchMode = true,
                                hasMore = hasMore
                            )
                        }
                        selectStory(resData.data[0], page, 0)
                    } else {
                        emitEffect(StoryReadingEffect.ShowToast("没有找到你想要的故事哦！"))
                    }
                }
                .onFailure { exception ->
                    emitEffect(StoryReadingEffect.ShowToast(exception.localizedMessage ?: "搜索失败"))
                }
        }
    }

    private fun selectStory(story: StoryBean, page: Int, position: Int) {
        // 先停掉上一首的歌词和音频
        stopProgressJob()
        
        updateState {
            copy(
                selectedStory = story,
                selectedPage = page,
                selectedPosition = position,
                isLikeNowStory = false,
                isCollectNowStory = false,
                isMediaPlayerReady = false,
                isAudioPlaying = false,
                currentAudioProgress = 0,
                activeLyricIndex = 0
            )
        }

        initMediaPlayer(story.voicePath)
    }

    private fun initMediaPlayer(url: String?) {
        if (url.isNullOrEmpty()) {
            emitEffect(StoryReadingEffect.ShowToast("加载音频源失败：路径为空"))
            return
        }

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        } else {
            mediaPlayer?.reset()
        }

        try {
            mediaPlayer?.setDataSource(url)
            mediaPlayer?.prepareAsync()
            mediaPlayer?.setOnPreparedListener { mp ->
                updateState {
                    copy(
                        isMediaPlayerReady = true,
                        isAudioPlaying = true,
                        audioDuration = mp.duration
                    )
                }
                mp.start()
                startProgressJob()
            }
            mediaPlayer?.setOnErrorListener { _, what, extra ->
                Log.e("StoryReadingViewModel", "MediaPlayer error: $what, $extra")
                true
            }
        } catch (e: Exception) {
            emitEffect(StoryReadingEffect.ShowToast("加载音频源失败"))
        }
    }

    private fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (!currentState.isMediaPlayerReady) return

        if (currentState.isAudioPlaying) {
            mp.pause()
            updateState { copy(isAudioPlaying = false) }
            stopProgressJob()
        } else {
            mp.start()
            updateState { copy(isAudioPlaying = true) }
            startProgressJob()
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        updateState { copy(isAudioPlaying = false) }
        stopProgressJob()
    }

    private fun seekTo(progress: Int) {
        val mp = mediaPlayer ?: return
        if (currentState.isMediaPlayerReady) {
            mp.seekTo(progress)
            updateState { copy(currentAudioProgress = progress) }
            // 顺便算一下这一秒的高亮歌词
            updateLyricIndex(progress)
        }
    }

    private fun playNext() {
        val page = currentState.selectedPage
        val pos = currentState.selectedPosition
        val storyList = currentState.storyList

        if (storyList.isEmpty()) return

        if (pos < storyList.size - 1) {
            // 在当前页内切歌，不需要请求网络
            val nextPos = pos + 1
            selectStory(storyList[nextPos], page, nextPos)
        } else {
            // 已经是当前页最后一首，检查是否有更多
            if (currentState.hasMore) {
                val nextPage = page + 1
                val nextPos = 0
                // 触发 Effect 让 View 的 HorizontalPager 同步滑到下一页
                emitEffect(StoryReadingEffect.ScrollPagerToPage(nextPage - 1))
                // 加载下一首数据并开始播放
                fetchStoryAt(nextPage, nextPos)
            } else {
                emitEffect(StoryReadingEffect.ShowToast("已经是最后一章啦！"))
            }
        }
    }

    private fun playPrevious() {
        val page = currentState.selectedPage
        val pos = currentState.selectedPosition
        val storyList = currentState.storyList

        if (storyList.isEmpty()) return

        if (pos > 0) {
            // 在当前页内切歌，不需要请求网络
            val prevPos = pos - 1
            selectStory(storyList[prevPos], page, prevPos)
        } else {
            // 当前是第一首，需要跨页到上一页
            if (page > 1) {
                val prevPage = page - 1
                val prevPos = 3 // 每一页最多4首，去上一页播最后一首（0, 1, 2, 3）
                emitEffect(StoryReadingEffect.ScrollPagerToPage(prevPage - 1))
                fetchStoryAt(prevPage, prevPos)
            } else {
                emitEffect(StoryReadingEffect.ShowToast("已经是第一章啦！"))
            }
        }
    }

    private fun fetchStoryAt(page: Int, pos: Int) {
        viewModelScope.launch {
            val result = if (currentState.isSearchMode) {
                storyRepository.searchStory(currentState.searchText, page)
            } else {
                storyRepository.getStoryList(page)
            }

            result.onSuccess { resData ->
                val data = resData.data
                if (pos < data.size) {
                    val hasMore = (page * 4) < resData.total
                    updateState {
                        copy(
                            storyList = data,
                            currentPage = page,
                            totalStories = resData.total,
                            hasMore = hasMore
                        )
                    }
                    selectStory(data[pos], page, pos)
                }
            }
            .onFailure { exception ->
                emitEffect(StoryReadingEffect.ShowToast(exception.localizedMessage ?: "切歌失败"))
            }
        }
    }

    private fun startProgressJob() {
        stopProgressJob()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && currentState.isAudioPlaying) {
                    val currentPos = mp.currentPosition
                    val duration = currentState.audioDuration
                    
                    updateState { copy(currentAudioProgress = currentPos) }
                    updateLyricIndex(currentPos)

                    // 如果播放到达终点，上报播放统计并自动切换下一首
                    if (currentPos >= duration - 300 && duration > 0) {
                        currentState.selectedStory?.let {
                            storyRepository.finalPlay(it.aid)
                        }
                        playNext()
                        break
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressJob() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateLyricIndex(currentPos: Int) {
        val lyrics = currentState.selectedStory?.lyricList ?: return
        if (lyrics.isEmpty()) return

        var activeIndex = 0
        val maxIndex = lyrics.size - 1
        
        if (currentPos >= lyrics[maxIndex].time) {
            activeIndex = maxIndex
        } else {
            for (i in 0..maxIndex) {
                if (currentPos <= lyrics[i].time) {
                    activeIndex = if (i == 0) 0 else i - 1
                    break
                }
            }
        }

        if (activeIndex != currentState.activeLyricIndex) {
            updateState { copy(activeLyricIndex = activeIndex) }
        }
    }

    private fun toggleLike() {
        val story = currentState.selectedStory ?: return
        viewModelScope.launch {
            storyRepository.likeStory(story.aid)
                .onSuccess {
                    val nextLike = !currentState.isLikeNowStory
                    updateState { copy(isLikeNowStory = nextLike) }
                    val msg = if (nextLike) "点赞成功" else "取消点赞成功"
                    emitEffect(StoryReadingEffect.ShowToast(msg))
                }
                .onFailure { exception ->
                    emitEffect(StoryReadingEffect.ShowToast(exception.localizedMessage ?: "操作失败"))
                }
        }
    }

    private fun toggleCollect() {
        val story = currentState.selectedStory ?: return
        viewModelScope.launch {
            storyRepository.collectStory(story.aid)
                .onSuccess {
                    val nextCollect = !currentState.isCollectNowStory
                    updateState { copy(isCollectNowStory = nextCollect) }
                    val msg = if (nextCollect) "收藏成功" else "取消收藏成功"
                    emitEffect(StoryReadingEffect.ShowToast(msg))
                }
                .onFailure { exception ->
                    emitEffect(StoryReadingEffect.ShowToast(exception.localizedMessage ?: "操作失败"))
                }
        }
    }

    private fun handleSpotReadRecord(audioPath: String) {
        val file = File(audioPath)
        if (!file.exists()) return

        updateState {
            copy(
                isSpotReadLoading = true,
                spotReadRequestText = "听音中...",
                spotReadResponseText = ""
            )
        }

        viewModelScope.launch {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaType())
            val voicePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            // 1. 将音频转换为文本
            storyRepository.voice2word(voicePart)
                .onSuccess { textResult ->
                    updateState { copy(spotReadRequestText = textResult) }

                    // 2. 发起点读查询
                    storyRepository.getStoryByVoice(voicePart)
                        .onSuccess { data ->
                            if (data.find == 1 && data.audioVO != null) {
                                // 找到了故事，自动关闭 Dialog，并只显示和播放这一首故事！
                                updateState {
                                    copy(
                                        spotReadResponseText = "好的，没问题",
                                        isSpotReadLoading = false,
                                        showSpotReadDialog = false,
                                        storyList = listOf(data.audioVO),
                                        isSearchMode = true,
                                        hasMore = false
                                    )
                                }
                                emitEffect(StoryReadingEffect.ShowToast("好的，没问题"))
                                selectStory(data.audioVO, 1, 0)
                            } else {
                                // 匹配失败
                                updateState {
                                    copy(
                                        spotReadResponseText = "小宝暂时无法实现这个功能哦~\n<- 按住我然后再说你想要播放第几章吧~",
                                        isSpotReadLoading = false
                                    )
                                }
                            }
                        }
                        .onFailure { exception ->
                            updateState {
                                copy(
                                    spotReadResponseText = exception.localizedMessage ?: "小宝服务器出现异常，请稍候重试",
                                    isSpotReadLoading = false
                                )
                            }
                        }
                }
                .onFailure { exception ->
                    updateState {
                        copy(
                            spotReadRequestText = "识别失败",
                            spotReadResponseText = exception.localizedMessage ?: "小宝没听清哦，请重试",
                            isSpotReadLoading = false
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProgressJob()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    /**
     * 点读过滤完成后，可以通过这个入口实现点读界面的强制拉回 (点读匹配了特定绘本后，若用户重新搜索或返回，重刷全部绘本)
     */
    fun resetToStoryList() {
        updateState { copy(isSearchMode = false, searchText = "") }
        loadStories(1)
    }
}


