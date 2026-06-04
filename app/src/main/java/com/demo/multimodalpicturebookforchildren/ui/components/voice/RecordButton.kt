package com.demo.multimodalpicturebookforchildren.ui.components.voice

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.demo.multimodalpicturebookforchildren.R
import java.io.File
import kotlin.math.min

/**
 *  代码来源：https://github.com/zhuguohui/WXSoundRecord
 *  感谢作者：朱国辉
 *  作者博客：https://blog.csdn.net/qq_22706515
 */
class RecordButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var mFileName = context.filesDir.absolutePath + "/" + "voice_" + System.currentTimeMillis() + ".wav"
    private var finishedListener: OnFinishedRecordListener? = null
    private val MIN_INTERVAL_TIME = 1000
    private val MAX_INTERVAL_TIME = 1000 * 60

    private var mStateTV: TextView? = null
    private var tv_voiceTip: TextView? = null

    @Volatile
    private var mRecorder: MediaRecorder? = null
    private var runningObtainDecibelThread = true
    private var mThread: ObtainDecibelThread? = null

    private var wxVoiceButton: WXVoiceButton? = null
    private var downY = 0f
    private var startTime: Long = 0
    private var recordDialog: Dialog? = null

    @Volatile
    private var firstNotice = true
    private var moveY = 0

    fun setOnFinishedRecordListener(listener: OnFinishedRecordListener) {
        finishedListener = listener
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        moveY = (event.y - downY).toInt()

        val stateTv = mStateTV
        val voiceBtn = wxVoiceButton
        if (stateTv != null && voiceBtn != null && moveY < 0 && moveY < -20) {
            stateTv.text = "松开手指,取消发送"
            voiceBtn.setCancel(true)
        } else if (stateTv != null && voiceBtn != null) {
            stateTv.text = "手指上滑,取消发送"
            voiceBtn.setCancel(false)
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                initDialogAndStartRecord()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (moveY < -20) {
                    cancelRecord()
                } else {
                    finishRecord()
                }
            }
        }
        return true
    }

    private fun initDialogAndStartRecord() {
        recordDialog = Dialog(context, R.style.like_toast_dialog_style)
        val view = inflate(context, R.layout.dialog_record, null)
        wxVoiceButton = view.findViewById(R.id.btn_wx_voice)
        mStateTV = view.findViewById(R.id.rc_audio_state_text)
        tv_voiceTip = view.findViewById(R.id.tv_voiceTip)

        @SuppressLint("UseCompatLoadingForDrawables")
        val drawable = resources.getDrawable(R.drawable.voice_tip, null)
        val width = 50
        val height = 50
        drawable.setBounds(0, 0, width, height)
        tv_voiceTip?.setCompoundDrawables(drawable, null, null, null)

        mStateTV?.visibility = VISIBLE
        mStateTV?.text = "手指上滑,取消发送"
        recordDialog?.setContentView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val window = recordDialog?.window
        if (window != null) {
            window.decorView.setPadding(0, 0, 0, 0)
            val layoutParams = window.attributes
            window.setBackgroundDrawableResource(android.R.color.transparent)
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            window.attributes = layoutParams
        }

        if (startRecording()) {
            recordDialog?.show()
        }
    }

    private fun finishRecord() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post { finishRecord() }
            return
        }

        val intervalTime = System.currentTimeMillis() - startTime
        firstNotice = true
        val wavFileName = mFileName
        val file = File(wavFileName)
        stopRecording()
        if (!file.exists()) {
            return
        }
        updateFileName()

        if (intervalTime < MIN_INTERVAL_TIME) {
            Toast.makeText(context, "录音时间太短", Toast.LENGTH_SHORT).show()
            mStateTV?.text = "录音时间太短"
            file.delete()
            return
        }

        val mediaPlayer = MediaPlayer()
        var duration = 0
        try {
            mediaPlayer.setDataSource(wavFileName)
            mediaPlayer.prepare()
            duration = mediaPlayer.duration
            mediaPlayer.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        finishedListener?.onFinishedRecord(wavFileName, duration / 1000)
        updateFileName()
    }

    private fun updateFileName() {
        mFileName = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/" + "voice_" + System.currentTimeMillis() + ".aac"
    }

    fun getMFileName(): String {
        return mFileName
    }

    fun cancelRecord() {
        stopRecording()
        val file = File(mFileName)
        file.delete()
        updateFileName()
    }

    private fun startRecording(): Boolean {
        mRecorder = if (mRecorder != null) {
            mRecorder?.reset()
            mRecorder
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context) // Android 12+ 传入 context
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()        // 低版本使用无参构造
            }
        }
        mRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
        mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mRecorder?.setOutputFile(mFileName)

        try {
            mRecorder?.prepare()
            mRecorder?.start()
            startTime = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
            mRecorder?.release()
            mRecorder = null
            toast("录音启动失败[${e.message}]")
            return false
        }
        runningObtainDecibelThread = true
        mThread = ObtainDecibelThread()
        mThread?.start()
        return true
    }

    private fun toast(content: String) {
        Toast.makeText(context, content, Toast.LENGTH_SHORT).show()
    }

    private fun stopRecording() {
        runningObtainDecibelThread = false
        mThread = null

        mRecorder?.let {
            try {
                it.stop()
                it.reset()
                it.release()
            } catch (e: RuntimeException) {
                e.printStackTrace()
            } finally {
                mRecorder = null
            }
        }
        recordDialog?.let {
            it.dismiss()
            recordDialog = null
        }
        wxVoiceButton?.let {
            it.quit()
            wxVoiceButton = null
        }
    }

    private inner class ObtainDecibelThread : Thread() {
        override fun run() {
            while (runningObtainDecibelThread) {
                val recorder = mRecorder ?: break
                if (!runningObtainDecibelThread) break

                var maxAmplitude = 0
                try {
                    maxAmplitude = recorder.maxAmplitude
                } catch (e: Exception) {
                    // Ignore if recorder state is invalid
                }
                var db = maxAmplitude / 35
                db = min(200, db)

                val now = System.currentTimeMillis()
                val useTime = now - startTime
                if (useTime > MAX_INTERVAL_TIME) {
                    finishRecord()
                    return
                }

                val lessTime = (MAX_INTERVAL_TIME - useTime) / 1000
                if (lessTime < 10) {
                    wxVoiceButton?.setContent("${lessTime}秒后将结束录音")
                    if (firstNotice) {
                        firstNotice = false
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            vibratorManager.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(500)
                        }
                    }
                } else {
                    wxVoiceButton?.addVoiceSize(db)
                }

                try {
                    sleep(500)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun interface OnFinishedRecordListener {
        fun onFinishedRecord(audioPath: String, time: Int)
    }
}

