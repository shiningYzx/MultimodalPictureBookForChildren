package com.demo.multimodalpicturebookforchildren.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/**
 * 简单的圆形提示 View
 */
class CircleHintView(context: Context) : View(context) {

    private val paint: Paint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#800000FF") // 半透明蓝色
    }
    private val radius = 100 // 半径，可根据需求调整

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2
        val centerY = height / 2
        canvas.drawCircle(centerX.toFloat(), centerY.toFloat(), radius.toFloat(), paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 设置 View 的大小为直径
        val size = radius * 2
        setMeasuredDimension(size, size)
    }
}

