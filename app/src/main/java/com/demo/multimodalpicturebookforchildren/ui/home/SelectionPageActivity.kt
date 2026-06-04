package com.demo.multimodalpicturebookforchildren.ui.home

import com.demo.multimodalpicturebookforchildren.ui.voice.VoiceQAActivity

import com.demo.multimodalpicturebookforchildren.ui.story.StoryReadingActivity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.demo.multimodalpicturebookforchildren.R

class SelectionPageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFFF4F9FD) 
                )
            ) {
                SelectionScreen(
                    onLeftClick = {
                        startActivity(Intent(this, VoiceQAActivity::class.java))
                    },
                    onRightClick = {
                        startActivity(Intent(this, StoryReadingActivity::class.java))
                    }
                )
            }
        }

    }
}

@Composable
fun SelectionScreen(
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    // 整体容器
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F9FD))
    ) {
        // 1. 装饰背景层：右上角红色光圈 + 左下角蓝色光圈 (模拟原版 xml @drawable/tomato_point 与 @drawable/blue_point)
        Image(
            painter = painterResource(id = R.drawable.tomato_point),
            contentDescription = null,
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopEnd)
                .offset(x = 150.dp, y = (-200).dp)
        )

        Image(
            painter = painterResource(id = R.drawable.blue_point),
            contentDescription = null,
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-180).dp, y = 150.dp)
        )

        // 2. 顶部标题栏：机器人头像 + “开始选择”文字
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 60.dp, top = 25.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.robot),
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.start_chose),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF494C4F)
            )
        }

        // 3. 两大核心功能选择卡片：水平排列，自适应宽度
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 340.dp)
                .align(Alignment.Center)
                .padding(top = 40.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左卡片：语音问答
            SelectionCard(
                title = stringResource(id = R.string.question_answer),
                backgroundColor = Color(0xFF5894F6), // 蓝色 @color/blue
                iconResId = R.drawable.question_answer,
                decorPoint1 = R.drawable.left_point1,
                decorPoint2 = R.drawable.left_point2,
                decorPoint3 = R.drawable.left_point3,
                onClick = onLeftClick,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(50.dp)) // 卡片间距与原 XML 的 50dp 一致

            // 右卡片：语音点读
            SelectionCard(
                title = stringResource(id = R.string.story_reading),
                backgroundColor = Color(0xFFF1817B), // 番茄红 @color/tomato
                iconResId = R.drawable.story_reading,
                decorPoint1 = R.drawable.right_point1,
                decorPoint2 = R.drawable.right_point2,
                decorPoint3 = R.drawable.right_point3,
                onClick = onRightClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SelectionCard(
    title: String,
    backgroundColor: Color,
    iconResId: Int,
    decorPoint1: Int,
    decorPoint2: Int,
    decorPoint3: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 动画交互：点击时呈现微缩放效果
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .scale(scale)
            .clip(RoundedCornerShape(30.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // 卡片内的背景气泡装饰图层 (100% 还原 XML 的背景小红点/小蓝点布局)
        Image(
            painter = painterResource(id = decorPoint1),
            contentDescription = null,
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-60).dp)
        )

        Image(
            painter = painterResource(id = decorPoint2),
            contentDescription = null,
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
        )

        Image(
            painter = painterResource(id = decorPoint3),
            contentDescription = null,
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 20.dp)
        )

        // 中央内容展示：半透明圆角中心块 + Icon + 标题文案
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 半透明中心底盒 (原 XML @id/cv_left 30dp圆角 50%透明白)
            Box(
                modifier = Modifier
                    .size(125.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 功能文案 (30sp Bold 白色文本)
            Text(
                text = title,
                fontSize = 24.sp, // 适配较小横屏，微调至 24sp，在宽屏上显示同样震撼
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}


