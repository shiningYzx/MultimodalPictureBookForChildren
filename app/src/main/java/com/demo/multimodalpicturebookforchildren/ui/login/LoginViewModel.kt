package com.demo.multimodalpicturebookforchildren.ui.login

import androidx.lifecycle.viewModelScope
import com.demo.multimodalpicturebookforchildren.base.BaseMviViewModel
import com.demo.multimodalpicturebookforchildren.data.repository.LoginRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginRepository: LoginRepository = LoginRepository()
) : BaseMviViewModel<LoginViewState, LoginUiIntent, LoginViewEffect>() {

    private var countdownJob: Job? = null

    override fun createInitialState(): LoginViewState = LoginViewState()

    override suspend fun handleIntent(intent: LoginUiIntent) {
        when (intent) {
            is LoginUiIntent.EmailChanged -> updateState { copy(email = intent.email) }
            is LoginUiIntent.SmsCodeChanged -> updateState { copy(smsCode = intent.code) }
            is LoginUiIntent.ClickSendSms -> handleSendSms()
            is LoginUiIntent.ClickLogin -> handleLogin()
            is LoginUiIntent.TickCountdown -> updateState { copy(smsCountdown = intent.secondsLeft) }
            is LoginUiIntent.FinishCountdown -> updateState { copy(isSmsButtonEnabled = true, smsCountdown = 0) }
            is LoginUiIntent.ClickTestLogin -> testLogin()
        }
    }

    private fun testLogin() {
        viewModelScope.launch {
            emitEffect(LoginViewEffect.NavigateToSelectionScreen)
        }
    }

    private suspend fun handleSendSms() {
        val email = currentState.email.trim()
        if (email.isEmpty()) {
            emitEffect(LoginViewEffect.ShowToast("请输入邮箱！"))
            return
        }

        // 简单的邮箱正则校验
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emitEffect(LoginViewEffect.ShowToast("请输入正确的邮箱格式！"))
            return
        }

        updateState { copy(isSmsButtonEnabled = false) }
        val result = loginRepository.sendSms(email)
        if (result.isSuccess) {
            emitEffect(LoginViewEffect.ShowToast("验证码发送成功！"))
            startSmsCountdown()
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "获取验证码失败！"
            emitEffect(LoginViewEffect.ShowToast(errorMsg))
            updateState { copy(isSmsButtonEnabled = true) }
        }
    }

    private suspend fun handleLogin() {
        val email = currentState.email.trim()
        val code = currentState.smsCode.trim()

        if (email.isEmpty()) {
            emitEffect(LoginViewEffect.ShowToast("请输入邮箱！"))
            return
        }
        if (code.isEmpty()) {
            emitEffect(LoginViewEffect.ShowToast("请输入验证码！"))
            return
        }

        updateState { copy(isLoading = true) }
        val result = loginRepository.login(email, code)
        updateState { copy(isLoading = false) }

        if (result.isSuccess) {
            emitEffect(LoginViewEffect.ShowToast("登录/注册成功！\n即将进入主页！"))
            viewModelScope.launch {
                // 延迟2秒，供用户读取成功提示后跳转
                delay(2000)
                emitEffect(LoginViewEffect.NavigateToSelectionScreen)
            }
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "登录/注册失败！"
            emitEffect(LoginViewEffect.ShowToast(errorMsg))
        }
    }

    private fun startSmsCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 60 downTo 1) {
                dispatchIntent(LoginUiIntent.TickCountdown(i))
                delay(1000)
            }
            dispatchIntent(LoginUiIntent.FinishCountdown)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}


