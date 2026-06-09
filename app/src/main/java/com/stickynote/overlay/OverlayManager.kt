package com.stickynote.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager

object OverlayManager {

    private var overlayView: StickerNoteUI? = null
    private var windowManager: WindowManager? = null
    private var params: WindowManager.LayoutParams? = null

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
        lp.x = dm.widthPixels - defaultW - 20
        lp.y = (dm.heightPixels * 0.08).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        params = lp

        val sticker = StickerNoteUI(context)
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
