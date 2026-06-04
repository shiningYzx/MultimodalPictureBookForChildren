package com.demo.multimodalpicturebookforchildren.ui.home

import com.demo.multimodalpicturebookforchildren.ui.home.SelectionPageActivity

import com.demo.multimodalpicturebookforchildren.ui.login.LoginActivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.multimodalpicturebookforchildren.R
import com.demo.multimodalpicturebookforchildren.data.local.UserDataSharedPreferences

class HomepageActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HomepageScreen(
                onStartClick = {
                    val pref = UserDataSharedPreferences(this)
                    val intent = if (pref.userStatus == 0) {
                        Intent(this, LoginActivity::class.java)
                    } else {
                        Intent(this, SelectionPageActivity::class.java)
                    }
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun HomepageScreen(
    onStartClick: () -> Unit
) {
    // 全屏点击可跳转容器
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onStartClick() }
            .background(Color(0xFFF4F9FD))
    ) {
        // 1. 全屏插图背景 (iv_homepage)
        Image(
            painter = painterResource(id = R.drawable.homepage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .align(Alignment.BottomCenter)
        )

        // 2. 顶部天空蓝背景带 (iv_sky)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color(0xFFB9E4ED)) // @color/skyBlue
                .align(Alignment.TopCenter)
        )

        // 3. 居中大开始按钮 (iv_start)
        Image(
            painter = painterResource(id = R.drawable.start_btn),
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.Center)
        )

        // 4. 顶部核心文字与 Logo 卡片区
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo 圆角卡片 (cv_logo)
            Card(
                shape = RoundedCornerShape(40.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.size(75.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            // APP名 (tv_app)
            Text(
                text = stringResource(id = R.string.app),
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(5.dp))

            // 工作室名 (tv_studioName)
            Text(
                text = stringResource(id = R.string.studioName),
                fontSize = 25.sp,
                color = Color(0xFF494C4F)
            )
        }
    }
}


