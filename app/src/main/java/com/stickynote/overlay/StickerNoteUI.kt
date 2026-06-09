package com.stickynote.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun dpToPx(context: Context, dp: Int): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
}

class StickerNoteUI(context: Context) : FrameLayout(context) {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private var saveJob: Job? = null

    private var expanded = true
    private var bulletMode = false

    private var dragStartX = 0f
    private var dragStartY = 0f
    private var isDragging = false

    private var resizeTouchX = 0f
    private var resizeTouchY = 0f

    private lateinit var editText: EditText
    private lateinit var cardBody: FrameLayout
    private lateinit var handleView: View

    private var onMove: ((dx: Int, dy: Int) -> Unit)? = null
    private var onResize: ((dx: Int, dy: Int) -> Unit)? = null
    private var onFocus: ((Boolean) -> Unit)? = null

    private val P = ViewGroup.LayoutParams.MATCH_PARENT
    private val W = ViewGroup.LayoutParams.WRAP_CONTENT

    init {
        layoutParams = ViewGroup.LayoutParams(P, P)
        buildUI(context)
        loadText(context)
    }

    fun setOnMove(cb: (dx: Int, dy: Int) -> Unit) { onMove = cb }
    fun setOnResize(cb: (dx: Int, dy: Int) -> Unit) { onResize = cb }
    fun setOnFocus(cb: (Boolean) -> Unit) { onFocus = cb }

    private fun buildUI(context: Context) {
        handleView = createHandle(context).also { addView(it) }

        cardBody = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(P, P)
            visibility = VISIBLE

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#F51E1E1E"))
                cornerRadius = 16f
            }
            background = bg
        }

        cardBody.addView(createDragBar(context))
        cardBody.addView(createCloseBtn(context))
        cardBody.addView(createToolbar(context))
        editText = createEditText(context)
        cardBody.addView(editText)
        cardBody.addView(createResizer(context))
        addView(cardBody)
    }

    private fun createDragBar(context: Context): View {
        return FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(P, 52)

            setBackgroundColor(Color.parseColor("#1AFFFFFF"))

            val line = View(context)
            val lp = FrameLayout.LayoutParams(100, 4)
            lp.gravity = Gravity.CENTER
            line.layoutParams = lp
            line.setBackgroundColor(Color.parseColor("#44FFFFFF"))
            addView(line)

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartX = event.rawX; dragStartY = event.rawY
                        isDragging = false; true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - dragStartX
                        val dy = event.rawY - dragStartY
                        if (!isDragging && (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8)) isDragging = true
                        if (isDragging) {
                            onMove?.invoke(dx.toInt(), dy.toInt())
                            dragStartX = event.rawX; dragStartY = event.rawY
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun createCloseBtn(context: Context): TextView {
        return TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#FF999999"))
            textSize = 18f
            gravity = Gravity.CENTER
            val lp = FrameLayout.LayoutParams(48, 48)
            lp.gravity = Gravity.END or Gravity.TOP
            lp.setMargins(0, 2, 6, 0)
            layoutParams = lp
            setOnClickListener {
                expanded = false
                cardBody.visibility = GONE
                handleView.visibility = VISIBLE
                onFocus?.invoke(false)
            }
        }
    }

    private fun createToolbar(context: Context): FrameLayout {
        val toolbar = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(P, 48).apply {
                setMargins(12, 54, 12, 0)
                gravity = Gravity.TOP
            }
        }

        bulletBtn = ImageButton(context).apply {
            setOnClickListener {
                bulletMode = !bulletMode
                val c = if (bulletMode) Color.parseColor("#FFEB3B") else Color.parseColor("#88FFFFFF")
                setColorFilter(c)
                if (bulletMode) insertBullet()
            }
            val lp = FrameLayout.LayoutParams(44, 44)
            lp.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            layoutParams = lp

            val bg = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(2, Color.parseColor("#88FFFFFF"))
                cornerRadius = 8f
            }
            setBackgroundDrawable(bg)
            setImageDrawable(createBulletIcon(context))
            setColorFilter(Color.parseColor("#88FFFFFF"))
            scaleType = android.widget.ImageView.ScaleType.CENTER
            contentDescription = "Bullets"
        }
        toolbar.addView(bulletBtn)

        val tabBtn = ImageButton(context).apply {
            val lp = FrameLayout.LayoutParams(44, 44)
            lp.gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = lp

            val bg = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(2, Color.parseColor("#88FFFFFF"))
                cornerRadius = 8f
            }
            setBackgroundDrawable(bg)
            setOnClickListener { insertAtCursor("    ") }
            contentDescription = "Tab"
        }
        tabBtn.setImageDrawable(createTabIcon(context))
        tabBtn.setColorFilter(Color.parseColor("#88FFFFFF"))
        tabBtn.scaleType = android.widget.ImageView.ScaleType.CENTER
        toolbar.addView(tabBtn)

        return toolbar
    }

    private fun createEditText(context: Context): EditText {
        return EditText(context).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#88FFFFFF"))
            hint = "Start typing..."
            textSize = 15f
            background = null
            val lp = FrameLayout.LayoutParams(P, P)
            lp.setMargins(16, 106, 16, 44)
            layoutParams = lp
            gravity = Gravity.TOP
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION

            setOnFocusChangeListener { _, hasFocus -> onFocus?.invoke(hasFocus) }

            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_TAB) {
                    val s = text?.toString() ?: ""
                    val pos = selectionStart
                    val newText = s.substring(0, pos) + "    " + s.substring(pos)
                    setText(newText)
                    setSelection(pos + 4)
                    true
                } else false
            }

            addTextChangedListener(object : TextWatcher {
                private var updating = false
                override fun beforeTextChanged(s: CharSequence?, s1: Int, s2: Int, s3: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (updating) return
                    if (bulletMode && count == 1 && before == 0) {
                        val c = s?.getOrNull(start) ?: return
                        if (c == '\n') {
                            updating = true
                            val txt = s.toString()
                            val newTxt = txt.substring(0, start + 1) + "•  " + txt.substring(start + 1)
                            setText(newTxt)
                            setSelection(start + 4)
                            updating = false
                            return
                        }
                    }
                    scheduleSave(context, s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private var bulletBtn: ImageButton? = null

    private fun insertBullet() {
        val s = editText.text?.toString() ?: ""
        val pos = editText.selectionStart
        val lineStart = s.lastIndexOf('\n', pos - 1) + 1
        val prefix = "•  "
        val newText = s.substring(0, lineStart) + prefix + s.substring(lineStart)
        editText.setText(newText)
        editText.setSelection(lineStart + prefix.length)
    }

    private fun insertAtCursor(text: String) {
        val s = editText.text?.toString() ?: ""
        val pos = editText.selectionStart
        val newText = s.substring(0, pos) + text + s.substring(pos)
        editText.setText(newText)
        editText.setSelection(pos + text.length)
    }

    private fun createBulletIcon(context: Context): android.graphics.drawable.Drawable {
        val bmp = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint().apply {
            color = Color.WHITE
            textSize = 30f
            isAntiAlias = true
        }
        c.drawText("•", 14f, 34f, paint)
        return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
    }

    private fun createTabIcon(context: Context): android.graphics.drawable.Drawable {
        val bmp = android.graphics.Bitmap.createBitmap(48, 48, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint().apply {
            color = Color.WHITE
            textSize = 24f
            isAntiAlias = true
        }
        c.drawText("↹", 10f, 32f, paint)
        return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
    }

    private fun createHandle(context: Context): View {
        return FrameLayout(context).apply {
            val lp = FrameLayout.LayoutParams(52, 400)
            lp.gravity = Gravity.CENTER_VERTICAL
            layoutParams = lp

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#991E1E1E"))
                cornerRadii = floatArrayOf(12f, 12f, 0f, 0f, 0f, 0f, 12f, 12f)
            }
            setBackgroundDrawable(bg)

            val dots = TextView(context).apply {
                text = "⋮"
                setTextColor(Color.parseColor("#DDFFFFFF"))
                textSize = 32f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(W, W).apply { gravity = Gravity.CENTER }
            }
            addView(dots)

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        dragStartX = event.rawX; dragStartY = event.rawY
                        isDragging = false; true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - dragStartX
                        val dy = event.rawY - dragStartY
                        if (!isDragging && (kotlin.math.abs(dx) > 8 || kotlin.math.abs(dy) > 8)) isDragging = true
                        if (isDragging) {
                            onMove?.invoke(dx.toInt(), dy.toInt())
                            dragStartX = event.rawX; dragStartY = event.rawY
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            expanded = !expanded
                            cardBody.visibility = if (expanded) VISIBLE else GONE
                            if (!expanded) onFocus?.invoke(false)
                        }
                        isDragging = false; true
                    }
                    else -> false
                }
            }
        }
    }

    private fun createResizer(context: Context): View {
        return View(context).apply {
            val lp = FrameLayout.LayoutParams(40, 40)
            lp.gravity = Gravity.BOTTOM or Gravity.END
            layoutParams = lp

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#66FFFFFF"))
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 16f, 0f)
            }
            setBackgroundDrawable(bg)

            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        resizeTouchX = event.rawX; resizeTouchY = event.rawY; true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - resizeTouchX).toInt()
                        val dy = (event.rawY - resizeTouchY).toInt()
                        if (dx != 0 || dy != 0) {
                            onResize?.invoke(dx, dy)
                            resizeTouchX = event.rawX; resizeTouchY = event.rawY
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun scheduleSave(context: Context, text: String) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            NoteRepository.saveNote(context, text)
        }
    }

    private fun loadText(context: Context) {
        scope.launch {
            val t = NoteRepository.loadNote(context)
            editText.setText(t)
            if (t.isNotEmpty()) editText.setSelection(t.length)
        }
    }
}
