package com.demo.multimodalpicturebookforchildren.ui.voice

import com.demo.multimodalpicturebookforchildren.ui.home.SelectionPageActivity
import com.demo.multimodalpicturebookforchildren.ui.story.StoryReadingActivity

import android.Manifest
import com.demo.multimodalpicturebookforchildren.MyApplication
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.demo.multimodalpicturebookforchildren.R
import com.demo.multimodalpicturebookforchildren.data.model.Chat
import com.demo.multimodalpicturebookforchildren.ui.components.RecordingPermissionError
import com.demo.multimodalpicturebookforchildren.ui.components.voice.RecordButton
import com.demo.multimodalpicturebookforchildren.ui.voice.VoiceQAEffect
import com.demo.multimodalpicturebookforchildren.ui.voice.VoiceQAIntent
import com.demo.multimodalpicturebookforchildren.ui.voice.VoiceQAViewModel
import com.demo.multimodalpicturebookforchildren.ui.voice.VoiceQAViewState
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt

class VoiceQAActivity : ComponentActivity() {

    companion object {
        @Volatile
        var activeInstance: VoiceQAActivity? = null
    }

    private val viewModel: VoiceQAViewModel by viewModels()
    var recordButton: RecordButton? = null
    var isAutoRecordingMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        activeInstance = this
        super.onCreate(savedInstanceState)
        handleAudioIntent(intent)
        val triggerRecord = intent.getBooleanExtra("TRIGGER_RECORD", false)
        if (triggerRecord) {
            isAutoRecordingMode = true
        }

        // 双向请求录音权限，确保原生 RecordButton 启动正常
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
            val lazyListState = rememberLazyListState()

            // 监听单次副作用 (Effect)
            LaunchedEffect(key1 = true) {
                viewModel.viewEffect.collect { effect ->
                    when (effect) {
                        is VoiceQAEffect.ShowToast -> {
                            Toast.makeText(this@VoiceQAActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        is VoiceQAEffect.ScrollToBottom -> {
                            launch {
                                if (state.chatList.isNotEmpty()) {
                                    lazyListState.animateScrollToItem(state.chatList.size - 1)
                                }
                            }
                        }
                    }
                }
            }

            VoiceQAScreen(
                activity = this@VoiceQAActivity,
                state = state,
                viewModel = viewModel,
                lazyListState = lazyListState
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

    override fun onResume() {
        super.onResume()
        activeInstance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    private var autoRecordStartRawX = -1f
    private var autoRecordStartRawY = -1f
    private var hasSimulatedDownSent = false

    fun sendSimulatedDownEvent() {
        if (hasSimulatedDownSent) return
        val btn = recordButton ?: return
        hasSimulatedDownSent = true
        val downEvent = MotionEvent.obtain(
            android.os.SystemClock.uptimeMillis(),
            android.os.SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            0f,
            0f,
            0
        )
        btn.onTouchEvent(downEvent)
        downEvent.recycle()
    }

    fun startAutoRecording() {
        isAutoRecordingMode = true
        autoRecordStartRawX = -1f
        autoRecordStartRawY = -1f
        hasSimulatedDownSent = false
        sendSimulatedDownEvent()
    }

    fun handleRedirectedTouchEvent(ev: MotionEvent) {
        val btn = recordButton ?: return
        val action = ev.actionMasked

        // Ensure ACTION_DOWN is sent first
        sendSimulatedDownEvent()

        if (autoRecordStartRawX < 0f || autoRecordStartRawY < 0f) {
            autoRecordStartRawX = ev.rawX
            autoRecordStartRawY = ev.rawY
        }

        val localX = 0f + (ev.rawX - autoRecordStartRawX)
        val localY = 0f + (ev.rawY - autoRecordStartRawY)

        val mappedEvent = MotionEvent.obtain(
            ev.downTime,
            ev.eventTime,
            ev.action,
            localX,
            localY,
            ev.metaState
        )
        btn.onTouchEvent(mappedEvent)
        mappedEvent.recycle()

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            isAutoRecordingMode = false
            autoRecordStartRawX = -1f
            autoRecordStartRawY = -1f
            hasSimulatedDownSent = false
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAudioIntent(intent)
    }

    private fun handleAudioIntent(intent: Intent?) {
        val audioPath = intent?.getStringExtra("AUDIO_PATH")
        if (!audioPath.isNullOrEmpty()) {
            viewModel.dispatchIntent(VoiceQAIntent.VoiceRecorded(audioPath))
        }
    }
}

@Composable
fun VoiceQAScreen(
    activity: VoiceQAActivity,
    state: VoiceQAViewState,
    viewModel: VoiceQAViewModel,
    lazyListState: LazyListState
) {
    val focusManager = LocalFocusManager.current

    // 全局外容器
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F9FD))
    ) {
        // 限宽 800dp 水平居中以适配平板
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 800.dp)
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(15.dp))

            // 如果显示提问词，就渲染提示词面板；否则渲染聊天 LazyColumn
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.showCallWords) {
                    CallWordsPanel(
                        state = state,
                        onWordClick = { word ->
                            viewModel.dispatchIntent(VoiceQAIntent.QuestionTextChanged(word))
                            viewModel.dispatchIntent(VoiceQAIntent.SendQuestion)
                        },
                        onChangeClick = {
                            viewModel.dispatchIntent(VoiceQAIntent.ChangeCallWords)
                        }
                    )
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        itemsIndexed(state.chatList) { index, chat ->
                            ChatBubble(
                                chat = chat,
                                isPlaying = state.isAudioPlaying && state.currentlyPlayingIndex == index,
                                onBubbleClick = {
                                    chat.audioPath?.let { audioUrl ->
                                        viewModel.dispatchIntent(VoiceQAIntent.PlayAudio(index, audioUrl))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 底部输入控制条 (包含View类：RecordButton 桥接)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 桥接 RecordButton
                AndroidView(
                    factory = { ctx ->
                        RecordButton(ctx).apply {
                            val drawable = ctx.resources.getDrawable(R.drawable.voice, null)
                            drawable.setBounds(0, 0, 60, 60)
                            setCompoundDrawables(null, drawable, null, null)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            gravity = android.view.Gravity.CENTER

                            setOnTouchListener { _, event ->
                                if (event.action == MotionEvent.ACTION_DOWN) {
                                    viewModel.dispatchIntent(VoiceQAIntent.StopAudio)
                                }
                                false
                            }

                            setOnFinishedRecordListener { audioPath, _ ->
                                viewModel.dispatchIntent(VoiceQAIntent.VoiceRecorded(audioPath))
                            }

                            activity.recordButton = this

                            if (activity.isAutoRecordingMode) {
                                post {
                                    activity.sendSimulatedDownEvent()
                                }
                            }
                        }
                    },
                    modifier = Modifier.padding(top = 35.dp).size(50.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // 输入白底椭圆框
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.White)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (state.questionText.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.inputText),
                            color = Color(0xFF494C4F),
                            fontSize = 18.sp
                        )
                    }

                    BasicTextField(
                        value = state.questionText,
                        onValueChange = {
                            viewModel.dispatchIntent(VoiceQAIntent.QuestionTextChanged(it))
                        },
                        enabled = state.isInputEnabled,
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 18.sp
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.dispatchIntent(VoiceQAIntent.SendQuestion)
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 45.dp)
                    )

                    // 发送按钮
                    Image(
                        painter = painterResource(id = R.drawable.chat),
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.CenterEnd)
                            .clickable(enabled = state.isInputEnabled) {
                                viewModel.dispatchIntent(VoiceQAIntent.SendQuestion)
                                focusManager.clearFocus()
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. 底部导航栏 (语音问答 + 语音点读)
            BottomNavigation(
                onJumpToReading = {
                    val intent = Intent(MyApplication.context, StoryReadingActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    MyApplication.context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * 初始问答提问词面板
 */
@Composable
fun CallWordsPanel(
    state: VoiceQAViewState,
    onWordClick: (String) -> Unit,
    onChangeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // 机器人头像
        Image(
            painter = painterResource(id = R.drawable.robot),
            contentDescription = null,
            modifier = Modifier.size(50.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // APP名称
        Text(
            text = stringResource(id = R.string.app),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF494C4F)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // APP描述文案
        Text(
            text = stringResource(id = R.string.appDetail),
            fontSize = 15.sp,
            color = Color(0xFF494C4F),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3个提示词圆角白色长条
        state.callWords.forEach { word ->
            Row(
                modifier = Modifier
                    .width(630.dp)
                    .height(45.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White)
                    .clickable { onWordClick(word) }
                    .padding(horizontal = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = word,
                    fontSize = 18.sp,
                    color = Color(0xFF494C4F),
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Image(
                    painter = painterResource(id = R.drawable.ai),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(15.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 换一批按钮
        Text(
            text = stringResource(id = R.string.changeOther),
            color = Color(0xFF5894F6),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onChangeClick() }
                .padding(8.dp)
        )
    }
}

/**
 * 聊天气泡绘制 (包含 AI 气泡的富文本 Markwon AndroidView 桥接)
 */
@Composable
fun ChatBubble(
    chat: Chat,
    isPlaying: Boolean,
    onBubbleClick: () -> Unit
) {
    if (chat.type == Chat.RECEIVE) {
        // AI 的回答 (居左)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 70.dp, start = 5.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧头像
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(25.dp))
            )

            Spacer(modifier = Modifier.width(15.dp))

            // AI 回答气泡白色卡片
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (isPlaying) Color(0xFFE8F1FF) else Color.White) // 播放中微微变蓝背景，动感十足！
                    .clickable { onBubbleClick() }
                    .padding(vertical = 12.dp, horizontal = 18.dp)
            ) {
                // 桥接原生 Markwon 以尽可能还原 Markdown 排版
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            textSize = 27f
                            setTextColor("#494C4F".toColorInt())
                            setLineSpacing(5f, 1.1f)
                        }
                    },
                    update = { textView ->
                        val markwon = Markwon.create(textView.context)
                        markwon.setMarkdown(textView, chat.msg ?: "")
                        if (isPlaying) {
                            textView.setTextColor(Color(0xFF5894F6).toArgb()) // 播放时文字亮蓝色反馈
                        } else {
                            textView.setTextColor("#494C4F".toColorInt())
                        }
                    }
                )
            }
        }
    } else {
        // 用户提问 (居右)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 70.dp, end = 5.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Top
        ) {
            // 用户提问蓝色气泡
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFF6199F6).copy(alpha = 0.7f))
                    .padding(vertical = 12.dp, horizontal = 18.dp)
            ) {
                Text(
                    text = chat.msg ?: "",
                    color = Color.Black,
                    fontSize = 27.sp,
                    lineHeight = 35.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 右侧头像
            Image(
                painter = painterResource(id = R.drawable.robot),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(25.dp))
            )
        }
    }
}

/**
 * 底部双导航栏
 */
@Composable
fun BottomNavigation(
    onJumpToReading: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(50.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White)
            .padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 语音问答 (左侧)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(30.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFF6198F6).copy(alpha = 0.24f))
                )
                Image(
                    painter = painterResource(id = R.drawable.voice_question),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.question_answer),
                color = Color(0xFF6199F6),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 语音点读 (右侧)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onJumpToReading() }
                .padding(horizontal = 10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.read_story),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.story_reading),
                color = Color(0xFF6199F6),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


