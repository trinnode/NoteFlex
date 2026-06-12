package com.noteflex.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager

object OverlayManager {

    private var overlayView: NoteFlexUI? = null
    private var windowManager: WindowManager? = null
    private var params: WindowManager.LayoutParams? = null
    private var onStopRequested: (() -> Unit)? = null

    private var expandedWidth = 0
    private var expandedHeight = 0
    private var expandedX = 0
    private var expandedY = 0

    private var baseWidth = 0
    private var baseHeight = 0
    private var currentScale = 1.0f

    fun setOnStopRequested(cb: () -> Unit) {
        onStopRequested = cb
    }

    fun showOverlay(context: Context) {
        if (overlayView != null) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val dm = context.resources.displayMetrics
        val defaultW = (dm.widthPixels * 0.65).toInt()
        val defaultH = (dm.heightPixels * 0.55).toInt()

        @Suppress("DEPRECATION")
        val lp = WindowManager.LayoutParams(
            defaultW,
            defaultH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = (dm.widthPixels - defaultW) / 3
        lp.y = (dm.heightPixels * 0.06).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        params = lp
        expandedWidth = defaultW
        expandedHeight = defaultH
        expandedX = lp.x
        expandedY = lp.y
        baseWidth = defaultW
        baseHeight = defaultH
        currentScale = 1.0f

        val sticker = NoteFlexUI(context)
        overlayView = sticker

        sticker.setOnMove { dx, dy ->
            lp.x += dx
            lp.y += dy
            try {
                windowManager?.updateViewLayout(sticker, lp)
            } catch (_: Exception) {}
        }

        sticker.setOnResize { dx, dy ->
            val newW = (lp.width + dx).coerceIn(260, 1600)
            val newH = (lp.height + dy).coerceIn(260, 2000)
            lp.width = newW
            lp.height = newH
            baseWidth = newW
            baseHeight = newH
            currentScale = 1.0f
            try {
                windowManager?.updateViewLayout(sticker, lp)
            } catch (_: Exception) {}
        }

        sticker.setOnFocus { hasFocus ->
            if (hasFocus) {
                lp.flags = lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            try {
                windowManager?.updateViewLayout(sticker, lp)
            } catch (_: Exception) {}
        }

        sticker.setOnCollapse { expanded ->
            if (expanded) {
                lp.width = expandedWidth
                lp.height = expandedHeight
                lp.x = expandedX
                lp.y = expandedY
            } else {
                expandedWidth = lp.width
                expandedHeight = lp.height
                expandedX = lp.x
                expandedY = lp.y

                val handleW = 52
                val handleH = 400
                val handleOffset = (expandedHeight - handleH) / 2
                lp.x = expandedX
                lp.y = expandedY + handleOffset
                lp.width = handleW
                lp.height = handleH
            }
            try {
                windowManager?.updateViewLayout(sticker, lp)
            } catch (_: Exception) {}
        }

        sticker.setOnScale { scale ->
            currentScale = scale.coerceIn(0.3f, 1.0f)
            lp.width = (baseWidth * currentScale).toInt().coerceIn(100, 1600)
            lp.height = (baseHeight * currentScale).toInt().coerceIn(100, 2000)
            try {
                windowManager?.updateViewLayout(sticker, lp)
            } catch (_: Exception) {}
        }

        sticker.setOnClose {
            removeOverlay()
            onStopRequested?.invoke()
        }

        windowManager?.addView(sticker, lp)
    }

    fun removeOverlay() {
        try {
            overlayView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        overlayView = null
        windowManager = null
        params = null
    }
}
