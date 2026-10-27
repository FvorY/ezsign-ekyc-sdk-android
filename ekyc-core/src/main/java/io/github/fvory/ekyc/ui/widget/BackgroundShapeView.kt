package io.github.fvory.ekyc.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

internal class BackgroundShapeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55C9E9")
        style = Paint.Style.FILL
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#55C9E9")
        alpha = (255 * 0.75).toInt()
        style = Paint.Style.FILL
    }

    private val pathPrimary = Path()
    private val pathShadow = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val density = resources.displayMetrics.density
        val h = 400f * density
        val lowerHeight = h + 30f * density

        // Shadow/lower curve
        pathShadow.reset()
        pathShadow.lineTo(0f, lowerHeight - 170f * density)
        pathShadow.quadTo(w / 2f, lowerHeight, w, lowerHeight - 170f * density)
        pathShadow.lineTo(w, 0f)
        pathShadow.close()
        canvas.drawPath(pathShadow, shadowPaint)

        // Main curve
        pathPrimary.reset()
        pathPrimary.lineTo(0f, h - 170f * density)
        pathPrimary.quadTo(w / 2f, h, w, h - 170f * density)
        pathPrimary.lineTo(w, 0f)
        pathPrimary.close()
        canvas.drawPath(pathPrimary, primaryPaint)
    }
}
