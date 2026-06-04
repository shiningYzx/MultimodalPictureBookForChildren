package com.demo.multimodalpicturebookforchildren.ui.components.voice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Window
import com.demo.multimodalpicturebookforchildren.ui.voice.VoiceQAActivity
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat

/**
 * 全局 Window 触摸回调拦截代理，用于实现全局长按屏幕唤起无障碍语音问答
 */
class GlobalTouchWindowCallback(
    private val activity: Activity,
    private val delegate: Window.Callback
) : Window.Callback by delegate {

    private var tempRecordButton: RecordButton? = null
    private var isTempRecording = false
    private var tempStartRawX = -1f
    private var tempStartRawY = -1f

    private val gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            // 检查并申请录音权限
            val hasPermission = ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), 200)
                return
            }

            // 触发物理震动反馈
            triggerVibration()

            if (activity is VoiceQAActivity) {
                val btn = activity.recordButton
                if (btn != null) {
                    val location = IntArray(2)
                    btn.getLocationOnScreen(location)
                    val x = e.rawX
                    val y = e.rawY
                    if (x >= location[0] && x <= location[0] + btn.width &&
                        y >= location[1] && y <= location[1] + btn.height) {
                        // 长按在录音按钮自身，由其自身的onTouchEvent正常处理，不二次触发
                        return
                    }
                }
                // 如果当前已经是问答页面，且长按在按钮外部，无需跳转，直接就地触发自动录音
                activity.startAutoRecording()
            } else {
                if (isTempRecording) return

                isTempRecording = true
                tempStartRawX = e.rawX
                tempStartRawY = e.rawY

                val btn = RecordButton(activity).apply {
                    setOnFinishedRecordListener { audioPath, _ ->
                        val intent = Intent(activity, VoiceQAActivity::class.java).apply {
                            putExtra("AUDIO_PATH", audioPath)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        activity.startActivity(intent)
                    }
                }
                tempRecordButton = btn

                // 模拟派发 ACTION_DOWN
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
        }
    })

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            gestureDetector.onTouchEvent(event)

            if (isTempRecording) {
                val btn = tempRecordButton
                if (btn != null) {
                    val action = event.actionMasked

                    val localX = 0f + (event.rawX - tempStartRawX)
                    val localY = 0f + (event.rawY - tempStartRawY)

                    val mappedEvent = MotionEvent.obtain(
                        event.downTime,
                        event.eventTime,
                        event.action,
                        localX,
                        localY,
                        event.metaState
                    )
                    btn.onTouchEvent(mappedEvent)
                    mappedEvent.recycle()

                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        isTempRecording = false
                        tempRecordButton = null
                        tempStartRawX = -1f
                        tempStartRawY = -1f
                    }
                    return true
                }
            }

            val activeQA = VoiceQAActivity.activeInstance
            if (activeQA != null && activeQA.isAutoRecordingMode) {
                activeQA.handleRedirectedTouchEvent(event)
                return true
            }
        }
        return delegate.dispatchTouchEvent(event)
    }

    private fun triggerVibration() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }
}
