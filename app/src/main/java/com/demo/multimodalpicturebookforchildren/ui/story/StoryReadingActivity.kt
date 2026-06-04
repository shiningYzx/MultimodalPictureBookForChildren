package com.demo.multimodalpicturebookforchildren.ui.story

import com.demo.multimodalpicturebookforchildren.ui.home.SelectionPageActivity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.demo.multimodalpicturebookforchildren.R
import com.demo.multimodalpicturebookforchildren.data.model.StoryBean
import com.demo.multimodalpicturebookforchildren.ui.components.RecordingPermissionError
import com.demo.multimodalpicturebookforchildren.ui.components.voice.RecordButton

class StoryReadingActivity : ComponentActivity() {

    private val viewModel: StoryReadingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求录音权限以支持点读机器人
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                200
            )
        }
        enableEdgeToEdge()

        setContent {
            val state by viewModel.viewState.collectAsState()

            // 监听一次性副作用
            LaunchedEffect(key1 = true) {
                viewModel.viewEffect.collect { effect ->
                    when (effect) {
                        is StoryReadingEffect.ShowToast -> {
                            Toast.makeText(this@StoryReadingActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        is StoryReadingEffect.ScrollPagerToPage -> {
                            // 由于 HorizontalPager 会自动受 state.currentPage 影响，我们无需多余操作，或者在此进行其他逻辑同步
                        }
                    }
                }
            }

            StoryReadingScreen(
                state = state,
                viewModel = viewModel,
                onBack = {
                    finish()
                }
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // 用户拒绝了权限，弹出提示并引导前往设置
                RecordingPermissionError.recordingPermissionHint(this)
            }
        }
    }

}

@Composable
fun StoryReadingScreen(
    state: StoryReadingViewState,
    viewModel: StoryReadingViewModel,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F9FD))
    ) {
        // 两大板块：左侧故事列表 Page (占据 58%)，右侧播放控制大面板 (占据 42%)
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 左面板 (故事列表 + 搜索)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.58f)
                    .padding(start = 16.dp, top = 16.dp, bottom = 10.dp, end = 8.dp)
            ) {
                // 顶部工具栏 (返回按钮 + 搜索框 + 点读机器人)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 返回按钮
                    Row(
                        modifier = Modifier
                            .clickable { onBack() }
                            .padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = if (state.isSearchMode) R.drawable.back1 else R.drawable.back0),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "返回",
                            color = if (state.isSearchMode) Color(0xFF5894F6) else Color(0xFF494C4F),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // 搜索框
                    val focusManager = LocalFocusManager.current
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color.White)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (state.searchText.isEmpty()) {
                            Text(
                                text = "输入故事名称",
                                color = Color(0xFF494C4F).copy(alpha = 0.6f),
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = state.searchText,
                            onValueChange = {
                                viewModel.dispatchIntent(StoryReadingIntent.SearchTextChanged(it))
                            },
                            textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                viewModel.dispatchIntent(StoryReadingIntent.TriggerSearch)
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 35.dp)
                        )

                        // 搜索放大镜按钮
                        Image(
                            painter = painterResource(id = R.drawable.search),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterEnd)
                                .clickable {
                                    viewModel.dispatchIntent(StoryReadingIntent.TriggerSearch)
                                    focusManager.clearFocus()
                                }
                        )
                    }

                    Spacer(modifier = Modifier.width(15.dp))

                    // 点读机器人 (iv_robot)
                    Image(
                        painter = painterResource(id = R.drawable.robot),
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clickable {
                                viewModel.dispatchIntent(StoryReadingIntent.OpenSpotReadDialog)
                            }
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                // 中间：当前页故事列表卡片 LazyVerticalGrid (固定 4 个，两行两列)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (state.storyList.isNotEmpty()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            userScrollEnabled = false // 禁止滑，完全通过页码/Pager进行精确4卡片切换
                        ) {
                            itemsIndexed(state.storyList) { index, story ->
                                val isSelected = state.selectedStory?.aid == story.aid
                                StoryCard(
                                    story = story,
                                    isSelected = isSelected,
                                    onClick = {
                                        viewModel.dispatchIntent(
                                            StoryReadingIntent.SelectStory(story, state.currentPage, index)
                                        )
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "没有找到你想要的故事哦！", color = Color.Gray, fontSize = 18.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 底部：页码翻页控制器
                val totalPages = (state.totalStories + 3) / 4
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 上一页
                    Image(
                        painter = painterResource(id = R.drawable.previous_page),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                if (state.currentPage > 1) {
                                    viewModel.dispatchIntent(StoryReadingIntent.LoadStories(state.currentPage - 1))
                                }
                            }
                    )

                    Spacer(modifier = Modifier.width(30.dp))

                    // 页码标签列表 (简约指示器)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (p in 1..totalPages) {
                            val isActive = state.currentPage == p
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(if (isActive) 12.dp else 8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isActive) Color(0xFFF1817B) else Color.Gray.copy(alpha = 0.5f))
                                    .clickable {
                                        viewModel.dispatchIntent(StoryReadingIntent.LoadStories(p))
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(30.dp))

                    // 下一页
                    Image(
                        painter = painterResource(id = R.drawable.next_page),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                if (state.currentPage < totalPages) {
                                    viewModel.dispatchIntent(StoryReadingIntent.LoadStories(state.currentPage + 1))
                                }
                            }
                    )
                }
            }

            // 右面板 (大播放器大盘 + 滚动歌词)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.42f)
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 唱片匀速无限旋转动画
                var currentRotation by remember { mutableStateOf(0f) }
                val infiniteTransition = rememberInfiniteTransition()
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 15000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                val diskRotation = if (state.isAudioPlaying) rotationAngle else currentRotation
                LaunchedEffect(state.isAudioPlaying) {
                    if (!state.isAudioPlaying) {
                        currentRotation = rotationAngle
                    }
                }

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(80.dp))
                        .background(Color(0xFF222222)),
                    contentAlignment = Alignment.Center
                ) {
                    // 桥接 Glide 加载大盘封面
                    AndroidView(
                        factory = { context ->
                            ImageView(context).apply {
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        update = { imageView ->
                            state.selectedStory?.let {
                                Glide.with(imageView.context)
                                    .load(it.picturePath)
                                    .into(imageView)
                            }
                        },
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(55.dp))
                            .rotate(diskRotation)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 故事标题 + 播放数 + 交互操作 (点赞/收藏/下载)
                Text(
                    text = state.selectedStory?.title ?: "多模态盲童绘本",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF494C4F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.listened),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "播放量: ${state.selectedStory?.playNum ?: 0}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 点赞、收藏、下载交互栏
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 点赞
                    Row(
                        modifier = Modifier.clickable { viewModel.dispatchIntent(StoryReadingIntent.ToggleLike) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = if (state.isLikeNowStory) R.drawable.like1 else R.drawable.like0),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "点赞", fontSize = 13.sp, color = Color(0xFF494C4F))
                    }

                    // 收藏
                    Row(
                        modifier = Modifier.clickable { viewModel.dispatchIntent(StoryReadingIntent.ToggleCollect) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = if (state.isCollectNowStory) R.drawable.collect1 else R.drawable.collect0),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "收藏", fontSize = 13.sp, color = Color(0xFF494C4F))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 歌词高亮平滚 LazyColumn
                val lyricListState = rememberLazyListState()
                val lyrics = state.selectedStory?.lyricList ?: emptyList()

                LaunchedEffect(state.activeLyricIndex) {
                    if (lyrics.isNotEmpty() && state.activeLyricIndex < lyrics.size) {
                        lyricListState.animateScrollToItem(state.activeLyricIndex)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    if (lyrics.isNotEmpty()) {
                        LazyColumn(
                            state = lyricListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 20.dp)
                        ) {
                            itemsIndexed(lyrics) { index, lyric ->
                                val isActive = state.activeLyricIndex == index
                                Text(
                                    text = lyric.text,
                                    fontSize = if (isActive) 18.sp else 14.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) Color(0xFFF1817B) else Color(0xFF888888), // 激活高亮行变番茄红，淡雅高级！
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "小宝正在为你准备歌词哦~", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 进度条 SeekBar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = state.currentAudioProgress.toFloat(),
                        onValueChange = {
                            viewModel.dispatchIntent(StoryReadingIntent.SeekToProgress(it.toInt()))
                        },
                        valueRange = 0f..(state.audioDuration.toFloat().coerceAtLeast(1f)),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF1817B),
                            activeTrackColor = Color(0xFFF1817B),
                            inactiveTrackColor = Color(0xFFE0E0E0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 5. 播放控制按键 (上一首、播放/暂停、下一首)
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 上一首
                    Image(
                        painter = painterResource(id = R.drawable.previous),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { viewModel.dispatchIntent(StoryReadingIntent.PlayPreviousStory) }
                    )

                    // 播放 / 暂停
                    Image(
                        painter = painterResource(id = if (state.isAudioPlaying) R.drawable.playing else R.drawable.play),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { viewModel.dispatchIntent(StoryReadingIntent.TogglePlayPause) }
                    )

                    // 下一首
                    Image(
                        painter = painterResource(id = R.drawable.next),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { viewModel.dispatchIntent(StoryReadingIntent.PlayNextStory) }
                    )
                }
            }
        }

        // 机器人点读弹出 Dialog
        if (state.showSpotReadDialog) {
            SpotReadComposeDialog(
                state = state,
                viewModel = viewModel
            )
        }
    }
}

/**
 * 绘本精美卡片渲染组件
 */
@Composable
fun StoryCard(
    story: StoryBean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color(0xFFF1817B) else Color(0x11494C4F), // 选中获得加粗番茄红呼吸框！
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 故事圆角插图 (Glide 桥接加载)
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEEEEEE))
            ) {
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                    },
                    update = { imageView ->
                        Glide.with(imageView.context)
                            .load(story.picturePath)
                            .into(imageView)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 故事标题、作者、章节名
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = story.title ?: "",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF494C4F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "作者: ${story.author ?: ""}",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "章节: ${story.chapter ?: ""}",
                    fontSize = 13.sp,
                    color = Color(0xFF5894F6), // 天空蓝章节文案
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 纯 Compose 机器人点读弹出 Dialog
 */
@Composable
fun SpotReadComposeDialog(
    state: StoryReadingViewState,
    viewModel: StoryReadingViewModel
) {
    Dialog(
        onDismissRequest = { viewModel.dispatchIntent(StoryReadingIntent.CloseSpotReadDialog) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(30.dp))
                .background(Color.White)
                .clickable(enabled = true) {
                    // 防穿透点击
                }
        ) {
            // 背景右上和左下两个小气泡装饰
            Image(
                painter = painterResource(id = R.drawable.tomato_point),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-50).dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部状态与文字
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.robot),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "小宝点读助手",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF494C4F)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.spotReadResponseText.isEmpty()) "请按住麦克风，说出你想播放的故事章节哦~" else state.spotReadResponseText,
                        fontSize = 15.sp,
                        color = if (state.isSpotReadLoading) Color(0xFF5894F6) else Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }

                // 中间识别出来的提问词
                if (state.spotReadRequestText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF4F9FD))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "“ ${state.spotReadRequestText} ”",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5894F6)
                        )
                    }
                }

                // 底部录音麦克风 RecordButton 原生桥接
                Box(
                    modifier = Modifier.height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            RecordButton(ctx).apply {
                                val drawable = ctx.resources.getDrawable(R.drawable.voice, null)
                                drawable.setBounds(0, 0, 60, 60)
                                setCompoundDrawables(null, drawable, null, null)
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                gravity = android.view.Gravity.CENTER

                                setOnFinishedRecordListener { audioPath, _ ->
                                    viewModel.dispatchIntent(StoryReadingIntent.SpotReadVoiceRecorded(audioPath))
                                }
                            }
                        },
                        modifier = Modifier.size(64.dp)
                    )
                }

                // 关闭本弹窗按钮
                Text(
                    text = "点击周围空白处或点击此关闭弹窗",
                    fontSize = 13.sp,
                    color = Color.LightGray,
                    modifier = Modifier.clickable {
                        viewModel.dispatchIntent(StoryReadingIntent.CloseSpotReadDialog)
                    }
                )
            }
        }
    }
}


