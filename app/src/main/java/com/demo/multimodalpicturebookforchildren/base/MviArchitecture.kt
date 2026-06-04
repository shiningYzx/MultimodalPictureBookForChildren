package com.demo.multimodalpicturebookforchildren.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 界面唯一不可变状态的标识接口
 */
interface UiState

/**
 * 用户业务意图的标识接口
 */
interface UiIntent

/**
 * 界面单次副作用的标识接口
 */
interface UiEffect

/**
 * MVI 架构通用的 ViewModel 基类
 */
abstract class BaseMviViewModel<State : UiState, Intent : UiIntent, Effect : UiEffect> : ViewModel() {

    private val initialState: State by lazy { createInitialState() }
    protected abstract fun createInitialState(): State

    private val _viewState = MutableStateFlow(initialState)
    val viewState: StateFlow<State> = _viewState.asStateFlow()

    private val _viewEffect = MutableSharedFlow<Effect>()
    val viewEffect: SharedFlow<Effect> = _viewEffect.asSharedFlow()

    val currentState: State
        get() = _viewState.value

    /**
     * 视图层分发用户意图
     */
    fun dispatchIntent(intent: Intent) {
        viewModelScope.launch {
            handleIntent(intent)
        }
    }

    /**
     * 业务实现类需要覆写此方法，用于处理意图并转换为 State 的更新或 Effect 派发
     */
    protected abstract suspend fun handleIntent(intent: Intent)

    /**
     * 更新当前界面的 ViewState
     */
    protected fun updateState(reducer: State.() -> State) {
        _viewState.value = currentState.reducer()
    }

    /**
     * 发送一次性副作用 (例如弹出 Toast，跳转导航等)
     */
    protected fun emitEffect(effect: Effect) {
        viewModelScope.launch {
            _viewEffect.emit(effect)
        }
    }
}

