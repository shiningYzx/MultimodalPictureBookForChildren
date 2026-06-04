package com.demo.multimodalpicturebookforchildren.ui.login

import com.demo.multimodalpicturebookforchildren.base.UiEffect
import com.demo.multimodalpicturebookforchildren.base.UiIntent
import com.demo.multimodalpicturebookforchildren.base.UiState

/**
 * 登录界面，ui状态数据
 */
data class LoginViewState(
    val email: String = "",
    val smsCode: String = "",
    val isLoading: Boolean = false,
    val isSmsButtonEnabled: Boolean = true,
    val smsCountdown: Int = 0
) : UiState

/**
 * 操作意图
 */
sealed interface LoginUiIntent : UiIntent {
    data class EmailChanged(val email: String) : LoginUiIntent
    data class SmsCodeChanged(val code: String) : LoginUiIntent
    object ClickSendSms : LoginUiIntent
    object ClickLogin : LoginUiIntent
    object ClickTestLogin : LoginUiIntent
    data class TickCountdown(val secondsLeft: Int) : LoginUiIntent
    object FinishCountdown : LoginUiIntent
}

/**
 * Effect 副作用
 */
sealed interface LoginViewEffect : UiEffect {
    data class ShowToast(val message: String) : LoginViewEffect
    object NavigateToSelectionScreen : LoginViewEffect
}

