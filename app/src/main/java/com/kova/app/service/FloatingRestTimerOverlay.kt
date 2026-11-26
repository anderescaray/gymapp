package com.kova.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.kova.app.domain.logic.WeightFormatter
import com.kova.app.ui.state.RestTimerState
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Floating Dynamic-Island-style countdown pill rendered via WindowManager over other apps
 * while Kova is in the background. Supports dragging anywhere on screen, quick ±15s tweaks,
 * dismissing, and tapping to jump straight back into Kova.
 */
class FloatingRestTimerOverlay(
    private val context: Context,
    private val onAdjust: (Int) -> Unit,
    private val onDismiss: () -> Unit,
    private val onOpenApp: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = context.resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var rootView: View? = null
    private var pillContainer: LinearLayout? = null
    private var pillBgDrawable: GradientDrawable? = null
    private var ringView: OverlayProgressRingView? = null
    private var timeTextView: TextView? = null
    private var subtitleTextView: TextView? = null
    private var minusBtn: TextView? = null
    private var plusBtn: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isAttached = false

    companion object {
        private var savedX: Int? = null
        private var savedY: Int? = null
    }

    fun showOrUpdate(state: RestTimerState) {
        if (!state.isVisible || !Settings.canDrawOverlays(context)) {
            hide()
            return
        }

        if (!isAttached) {
            attach(state)
        }
        if (isAttached) {
            bindState(state)
        }
    }

    fun hide() {
        if (!isAttached) return
        val view = rootView
        isAttached = false
        rootView = null
        pillContainer = null
        pillBgDrawable = null
        ringView = null
        timeTextView = null
        subtitleTextView = null
        minusBtn = null
        plusBtn = null
        layoutParams = null
        if (view != null) {
            runCatching { windowManager.removeView(view) }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attach(initialState: RestTimerState) {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val statusBarHeight = getStatusBarHeight()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX ?: dp(16)
            y = savedY ?: (statusBarHeight + dp(10))
        }

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(100).toFloat()
            setColor(Color.parseColor("#F2121419"))
            setStroke(dp(1), Color.parseColor("#323846"))
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = bg
            elevation = dp(10).toFloat()
            setPadding(dp(12), dp(9), dp(10), dp(9))
        }

        val ring = OverlayProgressRingView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                marginEnd = dp(10)
            }
        }
        container.addView(ring)

        val textColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(10)
            }
        }

        val timeText = TextView(context).apply {
            setTextColor(Color.parseColor("#F5F6F8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            fontFeatureSettings = "tnum"
            maxLines = 1
        }
        val subtitleText = TextView(context).apply {
            setTextColor(Color.parseColor("#9AA0AA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            maxWidth = dp(112)
        }
        textColumn.addView(timeText)
        textColumn.addView(subtitleText)
        container.addView(textColumn)

        val minus = createMiniChip("-15s") { onAdjust(-15) }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(6)
            }
        }
        val plus = createMiniChip("+15s") { onAdjust(15) }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(6)
            }
        }
        val close = createCloseButton { onDismiss() }

        container.addView(minus)
        container.addView(plus)
        container.addView(close)

        // Wrap in FrameLayout so elevation shadow isn't clipped
        val wrapper = FrameLayout(context).apply {
            setPadding(dp(4), dp(4), dp(4), dp(8))
            addView(container)
        }

        // Drag the pill anywhere on screen; tap the timer body to return to Kova
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var dragging = false

        val dragListener = View.OnTouchListener { _, event ->
            val lp = layoutParams ?: return@OnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).roundToInt()
                    val dy = (event.rawY - initialTouchY).roundToInt()
                    if (!dragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        dragging = true
                    }
                    if (dragging) {
                        val metrics = context.resources.displayMetrics
                        val maxX = (metrics.widthPixels - wrapper.width).coerceAtLeast(0)
                        val maxY = (metrics.heightPixels - wrapper.height - dp(24)).coerceAtLeast(statusBarHeight)
                        lp.x = (initialX + dx).coerceIn(0, maxX)
                        lp.y = (initialY + dy).coerceIn(statusBarHeight / 2, maxY)
                        savedX = lp.x
                        savedY = lp.y
                        runCatching { windowManager.updateViewLayout(wrapper, lp) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) {
                        onOpenApp()
                    }
                    true
                }
                else -> false
            }
        }
        container.setOnTouchListener(dragListener)

        runCatching {
            windowManager.addView(wrapper, params)
            rootView = wrapper
            pillContainer = container
            pillBgDrawable = bg
            ringView = ring
            timeTextView = timeText
            subtitleTextView = subtitleText
            minusBtn = minus
            plusBtn = plus
            layoutParams = params
            isAttached = true

            // Center horizontally on first ever placement once measured
            if (savedX == null) {
                wrapper.post {
                    if (isAttached && wrapper.width > 0) {
                        val centeredX = ((screenWidth - wrapper.width) / 2).coerceAtLeast(dp(8))
                        params.x = centeredX
                        savedX = centeredX
                        runCatching { windowManager.updateViewLayout(wrapper, params) }
                    }
                }
            }
            bindState(initialState)
        }
    }

    private fun bindState(state: RestTimerState) {
        val finishedGreen = Color.parseColor("#34D399")
        val ringColor = if (state.isFinished) finishedGreen else state.accentColorInt

        pillBgDrawable?.setStroke(
            dp(1),
            if (state.isFinished) Color.parseColor("#A634D399") else Color.parseColor("#323846")
        )

        ringView?.update(
            progress = if (state.isFinished) 1f else state.progress,
            ringColor = ringColor,
            isFinished = state.isFinished
        )

        if (state.isFinished) {
            timeTextView?.text = "¡Descanso listo!"
            subtitleTextView?.apply {
                if (state.exerciseName.isNotBlank()) {
                    text = state.exerciseName
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }
            minusBtn?.visibility = View.GONE
            plusBtn?.visibility = View.GONE
        } else {
            val formatted = WeightFormatter.formatDuration(state.remainingSeconds.toLong())
            timeTextView?.text = if (state.isRunning) formatted else "$formatted · Pausa"
            subtitleTextView?.apply {
                if (state.exerciseName.isNotBlank()) {
                    text = state.exerciseName
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }
            minusBtn?.visibility = View.VISIBLE
            plusBtn?.visibility = View.VISIBLE
        }
    }

    private fun createMiniChip(label: String, onClick: () -> Unit): TextView {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(100).toFloat()
            color = ColorStateList.valueOf(Color.parseColor("#232731"))
        }
        return TextView(context).apply {
            text = label
            setTextColor(Color.parseColor("#F5F6F8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            fontFeatureSettings = "tnum"
            background = bg
            gravity = Gravity.CENTER
            setPadding(dp(9), dp(6), dp(9), dp(6))
            setOnClickListener { onClick() }
        }
    }

    private fun createCloseButton(onClick: () -> Unit): TextView {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            color = ColorStateList.valueOf(Color.parseColor("#232731"))
        }
        return TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#A0A6B1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            background = bg
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
            setOnClickListener { onClick() }
        }
    }

    private fun getStatusBarHeight(): Int {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else dp(28)
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private class OverlayProgressRingView(context: Context) : View(context) {
        private val density = context.resources.displayMetrics.density
        private var progress: Float = 1f
        private var ringColor: Int = Color.parseColor("#C6F635")
        private var isFinished: Boolean = false

        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#232731")
            strokeWidth = 3.2f * density
        }
        private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 3.2f * density
        }
        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 2.4f * density
        }
        private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        private val arcBounds = RectF()

        fun update(progress: Float, ringColor: Int, isFinished: Boolean) {
            this.progress = progress.coerceIn(0f, 1f)
            this.ringColor = ringColor
            this.isFinished = isFinished
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = w / 2f
            val cy = h / 2f

            if (isFinished) {
                fillPaint.color = Color.argb(52, Color.red(ringColor), Color.green(ringColor), Color.blue(ringColor))
                canvas.drawCircle(cx, cy, minOf(w, h) / 2f, fillPaint)
                checkPaint.color = ringColor
                canvas.drawLine(w * 0.30f, h * 0.52f, w * 0.44f, h * 0.66f, checkPaint)
                canvas.drawLine(w * 0.44f, h * 0.66f, w * 0.72f, h * 0.36f, checkPaint)
                return
            }

            val inset = 2.5f * density
            arcBounds.set(inset, inset, w - inset, h - inset)
            canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint)
            arcPaint.color = ringColor
            canvas.drawArc(arcBounds, -90f, 360f * progress, false, arcPaint)
            dotPaint.color = ringColor
            canvas.drawCircle(cx, cy, 3f * density, dotPaint)
        }
    }
}
