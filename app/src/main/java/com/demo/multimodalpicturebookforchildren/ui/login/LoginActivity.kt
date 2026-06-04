package com.demo.multimodalpicturebookforchildren.ui.login

import com.demo.multimodalpicturebookforchildren.ui.home.SelectionPageActivity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.demo.multimodalpicturebookforchildren.R
import com.demo.multimodalpicturebookforchildren.ui.login.LoginUiIntent
import com.demo.multimodalpicturebookforchildren.ui.login.LoginViewEffect
import com.demo.multimodalpicturebookforchildren.ui.login.LoginViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.delay

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val showLoginError = intent.getBooleanExtra("showLoginError", false)

        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFFF1817B),      // Tomato accent
                    background = Color(0xFFB9E4ED),   // SkyBlue
                    surface = Color(0xCCFFFFFF)       // 80% opaque white
                )
            ) {
                LoginScreen(
                    showLoginError = showLoginError,
                    onNavigateToMain = {
                        startActivity(Intent(this, SelectionPageActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    showLoginError: Boolean = false,
    onNavigateToMain: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.viewState.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(showLoginError) }
    var logoutCountdown by remember { mutableStateOf(5) }

    if (showLogoutDialog) {
        LaunchedEffect(key1 = true) {
            while (logoutCountdown > 0) {
                delay(1000)
                logoutCountdown -= 1
            }
            showLogoutDialog = false
        }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showLogoutDialog = false }) {
            Box(
                modifier = Modifier
                    .size(width = 400.dp, height = 200.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = stringResource(id = R.string.loginHint),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF494C4F),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showLogoutDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1817B)),
                        shape = RoundedCornerShape(30.dp),
                        modifier = Modifier.size(width = 120.dp, height = 40.dp)
                    ) {
                        Text(
                            text = "确定($logoutCountdown)",
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // 订阅响应单次 UI 副作用事件
    LaunchedEffect(Unit) {
        viewModel.viewEffect
            .onEach { effect ->
                when (effect) {
                    is LoginViewEffect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is LoginViewEffect.NavigateToSelectionScreen -> {
                        onNavigateToMain()
                    }
                }
            }
            .launchIn(this)
    }

    // 主容器：自适应填充全屏
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. 背景层：上部 150dp 的天空蓝背景 + 下部 homepage 全屏图片
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color(0xFFB9E4ED)) // @color/skyBlue
            )
            Image(
                painter = painterResource(id = R.drawable.homepage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. 表单卡片层：自适应屏幕宽高限制，最大锁死 600dp x 650dp（适配平板），最小支持在小屏幕上滚动
        Box(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(0.9f)
                .heightIn(max = 650.dp)
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xCCFFFFFF)) // @drawable/shape_30dp_corners (80% 填充白)
                .border(
                    width = 1.dp,
                    color = Color(0x33494C4F),
                    shape = RoundedCornerShape(30.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // 内置 Column 支持在小屏幕横屏下纵向滚动，确保完全不溢出
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // 标题：登录/注册 (40sp, Bold)
                Text(
                    text = stringResource(id = R.string.login_register),
                    style = TextStyle(
                        fontSize = 32.sp, // 适配各种屏幕，微调至 32sp 避免在窄屏上折行，同时保留 40sp 视觉冲击
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF494C4F),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // 邮箱输入框 (高度 80dp, @drawable/shape_20dp_corners)
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.dispatchIntent(LoginUiIntent.EmailChanged(it)) },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.please_enter_email),
                            fontSize = 22.sp,
                            color = Color(0x99494C4F)
                        )
                    },
                    trailingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.email),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(30.dp, 24.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    textStyle = TextStyle(fontSize = 22.sp, color = Color(0xFF494C4F)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFF1817B), // @color/tomato
                        unfocusedBorderColor = Color(0xFFF1817B),
                        cursorColor = Color(0xFFF1817B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(72.dp) // 微调高度至 72dp 保持精致比例，可在平板上无缝延展
                )

                // 验证码与获取按钮 (并排 Row, 自适应权重比)
                Row(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 验证码输入框 (占 1.3 权重)
                    OutlinedTextField(
                        value = state.smsCode,
                        onValueChange = { viewModel.dispatchIntent(LoginUiIntent.SmsCodeChanged(it)) },
                        placeholder = {
                            Text(
                                text = stringResource(id = R.string.please_enter_sms),
                                fontSize = 22.sp,
                                color = Color(0x99494C4F)
                            )
                        },
                        trailingIcon = {
                            Image(
                                painter = painterResource(id = R.drawable.sms),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(36.dp, 36.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        textStyle = TextStyle(fontSize = 22.sp, color = Color(0xFF494C4F)),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFF1817B),
                            unfocusedBorderColor = Color(0xFFF1817B),
                            cursorColor = Color(0xFFF1817B)
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(72.dp)
                    )

                    // 获取验证码按钮 (占 1.0 权重)
                    val isSmsEnabled = state.isSmsButtonEnabled
                    val countdown = state.smsCountdown
                    val buttonText = if (countdown > 0) "${countdown}s后重发" else stringResource(id = R.string.getSms)

                    Button(
                        onClick = { viewModel.dispatchIntent(LoginUiIntent.ClickSendSms) },
                        enabled = isSmsEnabled,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            disabledContainerColor = Color(0xFFFAFAFA),
                            contentColor = Color(0xFFF1817B), // @color/tomato
                            disabledContentColor = Color(0x66494C4F)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSmsEnabled) Color(0xFFF1817B) else Color(0x33494C4F),
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Text(
                            text = buttonText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSmsEnabled) Color(0xFFF1817B) else Color(0x66494C4F),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 登录按钮 (底色 @color/tomato, 字体白色, 20dp圆角)
                Button(
                    onClick = { viewModel.dispatchIntent(LoginUiIntent.ClickLogin) },
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(20.dp), // @drawable/shape_btn_login
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1817B), // @color/tomato
                        disabledContainerColor = Color(0x88F1817B),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .height(72.dp) // 微调至 72dp，优雅舒适
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.login),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Button(
                    onClick = { viewModel.dispatchIntent(LoginUiIntent.ClickTestLogin) },
                    modifier = Modifier
                ){
                    Text(text = "直接进入主页（测试用）")
                }
            }
        }
    }
}


