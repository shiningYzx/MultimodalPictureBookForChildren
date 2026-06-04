package com.demo.multimodalpicturebookforchildren.ui.components.voice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.Interpolator
import java.util.ArrayList
import java.util.Random
import androidx.core.graphics.withTranslation
import androidx.core.graphics.toColorInt

class WXVoiceButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ratios = floatArrayOf(
        0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.5f, 0.3f, 0.5f, 0.8f, 1.0f, 0.8f, 0.5f, 0.3f, 0.5f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f
    )
    private val MIN_VOICE_SIZE = 20
    private val linePaint: Paint = Paint().apply {
        color = "#000000".toColorInt()
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val bgPaint: Paint = Paint().apply {
        color = normalBgColor.toColorInt()
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    private val txtPaint: Paint = Paint().apply {
        color = "#000000".toColorInt()
        style = Paint.Style.FILL
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        textSize = dip2px(context, 14f).toFloat()
        isAntiAlias = true
    }

    private val drawLines = ArrayList<DrawLine>()
    private val LINE_WIDTH = 10 //音量条的宽度
    private val LINE_SPACE = 10

    private var lineHandler: LineHandler? = null

    private val DURATION = 400
    private var mInterpolator: Interpolator = BounceInterpolator()

    private val initWidthRotas = 0.42f //最开始显示的宽度占控件整个宽度的比例
    private val maxWidthRotas = 0.8f //最大显示宽度
    private var widthRotas = initWidthRotas //当前显示的宽度

    private var bgRound = 15 //背景圆角的大小,单位dp
    private var showWidth = 0 //显示的宽度
    private val textRect = Rect()
    @Volatile
    private var quit = false

    init {
        bgRound = dip2px(context, bgRound.toFloat())
        buildDrawLines()
        Thread {
            Looper.prepare()
            lineHandler = LineHandler(Looper.myLooper()!!)
            // Trigger size change once thread is up if size changed was already called
            if (width > 0) {
                lineHandler?.sendEmptyMessage(WHAT_ANIMATION)
            }
            Looper.loop()
        }.start()
    }

    private class DrawLine {
        var rectF: RectF = RectF()
        var maxSize: Int = 0
        var lineSize: Int = 0
        var small: Boolean = true //是否缩小模式
        var rotas: Float = 1.0f
        var timeCompletion: Float = 0f //时间完成度，返回在0-1
        var duration: Int = 0
    }

    fun setCancel(cancel: Boolean) {
        if (cancel) {
            bgPaint.color = cancelBgColor.toColorInt()
        } else {
            bgPaint.color = normalBgColor.toColorInt()
        }
        invalidate()
    }



    private fun buildDrawLines() {
        drawLines.clear()
        val random = Random()
        for (ratio in ratios) {
            val maxSize = (MIN_VOICE_SIZE * ratio).toInt()
            val rect = RectF(-LINE_WIDTH.toFloat() / 2, -maxSize.toFloat() / 2, LINE_WIDTH.toFloat() / 2, maxSize.toFloat() / 2)
            val drawLine = DrawLine().apply {
                this.maxSize = maxSize
                this.rectF = rect
                this.lineSize = random.nextInt(maxSize.coerceAtLeast(1))
                this.rotas = ratio
                this.duration = (DURATION * (1.0f / ratio)).toInt()
            }
            drawLines.add(drawLine)
        }
    }

    private fun dip2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    private var showText = false
    private var content: String? = null

    fun setContent(content: String) {
        showText = true
        this.content = content
        txtPaint.getTextBounds(content, 0, content.length, textRect)
        invalidate()
    }

    private var toBig = true

    fun addVoiceSize(voiceSize: Int) {
        val handler = lineHandler ?: return
        val message = Message.obtain().apply {
            obj = voiceSize
            what = WHAT_CHANGE_VOICE_SIZE
        }
        handler.sendMessage(message)
        if (toBig) {
            //开始变大
            handler.removeMessages(WHAT_BIG)
            handler.sendEmptyMessage(WHAT_BIG)
            toBig = false
        }
    }

    fun setInterpolator(mInterpolator: Interpolator) {
        this.mInterpolator = mInterpolator
    }

    private var showVoiceSize = 40

    companion object {
        private const val WHAT_ANIMATION = 1 //驱动动画的事件
        private const val WHAT_BIG = 2 //驱动变宽的事件
        private const val WHAT_CHANGE_VOICE_SIZE = 3 //驱动音量条高低变化的事件
        private const val WHAT_CHECK_VOICE = 4 //驱动进入巡检模式的事件

        private val checkModeItemSize = intArrayOf(15, 20, 25, 30, 25, 20, 15)

        private const val cancelBgColor = "#F85050"
        private const val normalBgColor = "#EFEFEF"
    }

    private var checkStarIndex = 0
    private var isCheckMode = false

    private inner class LineHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                WHAT_ANIMATION -> {
                    // 遍历所有声波线条，计算每一根声波柱当前的最新高度和坐标矩形
                    for (drawLine in drawLines) {
                        // 1. 计算时间进度步长。以 60 帧动画（每帧 16ms）为基准，计算这 16ms 占该线条整个动画周期（duration）的百分比
                        val timeStep = 16.0f / drawLine.duration
                        // 获取该声波线条当前的动画时间完成度（0.0f - 1.0f 之间）
                        var timeCompletion = drawLine.timeCompletion
                        timeCompletion += timeStep // 累加时间进度
                        
                        // 2. 使用弹性插值器（BounceInterpolator）将线性时间进度映射为带有弹跳震动感的物理空间动画进度
                        val animationCompletion = mInterpolator.getInterpolation(timeCompletion)
                        
                        var lineSize = 0
                        // 3. 根据当前的运动模式（变大或变小），计算声波柱在当前帧的实际高度像素值 (lineSize)
                        lineSize = if (drawLine.small) {
                            // 变小模式：高度从 maxSize 缩回到 0
                            ((1 - animationCompletion) * drawLine.maxSize).toInt()
                        } else {
                            // 变大模式：高度从 0 伸展到 maxSize
                            (animationCompletion * drawLine.maxSize).toInt()
                        }
                        
                        // 4. 时序控制：如果时间进度达到或超过 100% (>= 1.0f)，说明单向动画（变长或变短）执行完毕
                        if (timeCompletion >= 1f) {
                            // 切换运动方向（刚才在变大就转为变小，刚才在变小就转为变大）
                            drawLine.small = !drawLine.small
                            // 重置当前单向动画进度为 0f，准备开始下一次反向跳动
                            drawLine.timeCompletion = 0f
                        } else {
                            // 动画尚未完成，保存当前的进度百分比供下一帧计算使用
                            drawLine.timeCompletion = timeCompletion
                        }

                        // 5. 对算出来的高度进行安全过滤：限制最小高度为 10px，避免音量极低时声波柱完全收缩消失
                        lineSize = Math.max(lineSize, 10)
                        
                        // 6. 根据新高度，以几何中心线 (y=0) 对称更新该声波柱在画布上的 top 和 bottom 坐标
                        val rectF = drawLine.rectF
                        rectF.top = -lineSize.toFloat() / 2
                        rectF.bottom = lineSize.toFloat() / 2
                        drawLine.lineSize = lineSize
                    }
                    postInvalidate()
                    removeMessages(WHAT_ANIMATION)
                    sendEmptyMessageDelayed(WHAT_ANIMATION, 16)
                }
                WHAT_BIG -> {
                    //改变宽度
                    if (widthRotas < maxWidthRotas) {
                        widthRotas += 0.005f
                        showWidth = (width * widthRotas).toInt()
                        postInvalidate()
                        sendEmptyMessageDelayed(WHAT_BIG, 500)
                    }
                }
                WHAT_CHANGE_VOICE_SIZE -> {
                    val voiceSize = msg.obj as Int
                    if (voiceSize < 30) {
                        if (!isCheckMode) {
                            //声音太小进入监听模式
                            isCheckMode = true
                            checkStarIndex = drawLines.size - 1
                            sendEmptyMessage(WHAT_CHECK_VOICE)
                        }
                        return
                    } else {
                        //退出监听模式
                        isCheckMode = false
                        removeMessages(WHAT_CHECK_VOICE)
                    }

                    for (drawLine in drawLines) {
                        drawLine.timeCompletion = 0f
                        drawLine.small = false
                        drawLine.maxSize = (drawLine.rotas * voiceSize).toInt()
                    }
                    sendEmptyMessage(WHAT_ANIMATION)
                }
                WHAT_CHECK_VOICE -> {
                    if (!isCheckMode) {
                        //判断是否是监听模式
                        return
                    }

                    removeMessages(WHAT_ANIMATION) //移除之前的上下的动画模式
                    //由于声音太小，显示波浪动画表示正在检查声音
                    // 遍历所有声波线条，将预设的静态波形 checkModeItemSize 映射并滚动赋值到 21 根声波柱中，形成横向滚动的流水波浪
                    for (i in drawLines.indices) {
                        val drawLine = drawLines[i]
                        val index = i - checkStarIndex
                        if (index >= 0 && index < checkModeItemSize.size) {
                            drawLine.lineSize = checkModeItemSize[index]
                        } else {
                            drawLine.lineSize = 10
                        }
                        drawLine.rectF.top = -drawLine.lineSize.toFloat() / 2
                        drawLine.rectF.bottom = drawLine.lineSize.toFloat() / 2
                    }
                    checkStarIndex--
                    if (checkStarIndex == -checkModeItemSize.size) {
                        checkStarIndex = drawLines.size - 1
                    }

                    postInvalidate()
                    removeMessages(WHAT_CHECK_VOICE)
                    sendEmptyMessageDelayed(WHAT_CHECK_VOICE, 100)
                }
            }
        }
    }

    fun quit() {
        lineHandler?.looper?.quit()
        quit = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        showWidth = (widthRotas * w).toInt()
        lineHandler?.sendEmptyMessage(WHAT_ANIMATION)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.withTranslation(width.toFloat() / 2, height.toFloat() / 2) {
            //画背景
            drawRoundRect(
                -showWidth.toFloat() / 2,
                -height.toFloat() / 2,
                showWidth.toFloat() / 2,
                height.toFloat() / 2,
                bgRound.toFloat(),
                bgRound.toFloat(),
                bgPaint
            )
            if (showText) {
                val txtHeight = textRect.height() / 2
                val txtWidth = textRect.width() / 2
                content?.let {
                    drawText(it, -txtWidth.toFloat(), txtHeight.toFloat(), txtPaint)
                }
            } else {
                val offsetX = (drawLines.size - 1).toFloat() / 2 * (LINE_WIDTH + LINE_SPACE)
                translate(-offsetX, 0f)
                for (drawLine in drawLines) {
                    drawRoundRect(drawLine.rectF, 5f, 5f, linePaint)
                    translate((LINE_WIDTH + LINE_SPACE).toFloat(), 0f)
                }
            }
        }
    }
}

