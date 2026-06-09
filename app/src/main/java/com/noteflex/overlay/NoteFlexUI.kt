package com.noteflex.overlay

import android.app.AlertDialog
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
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NoteFlexUI(context: Context) : FrameLayout(context) {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private var saveJob: Job? = null

    private var expanded = true
    private var bulletMode = false
    private var tabs = mutableListOf<NoteTab>()
    private var activeTabIndex = 0

    private var dragStartX = 0f
    private var dragStartY = 0f
    private var isDragging = false

    private var resizeTouchX = 0f
    private var resizeTouchY = 0f

    private lateinit var editText: EditText
    private lateinit var cardBody: FrameLayout
    private lateinit var handleView: View
    private lateinit var tabContainer: LinearLayout
    private lateinit var tabScroll: HorizontalScrollView
    private lateinit var bulletBtn: ImageButton

    private var onMove: ((dx: Int, dy: Int) -> Unit)? = null
    private var onResize: ((dx: Int, dy: Int) -> Unit)? = null
    private var onFocus: ((Boolean) -> Unit)? = null
    private var onCollapse: ((Boolean) -> Unit)? = null

    private val P = ViewGroup.LayoutParams.MATCH_PARENT
    private val W = ViewGroup.LayoutParams.WRAP_CONTENT

    init {
        layoutParams = ViewGroup.LayoutParams(P, P)
        buildUI(context)
    }

    fun setOnMove(cb: (dx: Int, dy: Int) -> Unit) { onMove = cb }
    fun setOnResize(cb: (dx: Int, dy: Int) -> Unit) { onResize = cb }
    fun setOnFocus(cb: (Boolean) -> Unit) { onFocus = cb }
    fun setOnCollapse(cb: (Boolean) -> Unit) { onCollapse = cb }

    private fun buildUI(context: Context) {
        handleView = createHandle(context)
        addView(handleView)

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

        tabScroll = HorizontalScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(P, 46).apply {
                setMargins(0, 54, 0, 0)
            }
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
        }
        tabContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(W, P)
            orientation = LinearLayout.HORIZONTAL
        }
        tabScroll.addView(tabContainer)
        cardBody.addView(tabScroll)

        cardBody.addView(createToolbar(context))
        editText = createEditText(context)
        cardBody.addView(editText)
        cardBody.addView(createResizer(context))
        addView(cardBody)

        loadData(context)
    }

    private fun loadData(context: Context) {
        scope.launch {
            val data = NoteRepository.load(context)
            tabs = data.tabs.toMutableList()
            activeTabIndex = data.activeTabIndex
            rebuildTabs(context)
            showActiveTab(context)
            scheduleSave(context)
        }
    }

    private fun rebuildTabs(context: Context) {
        tabContainer.removeAllViews()

        for ((i, tab) in tabs.withIndex()) {
            val isActive = i == activeTabIndex

            val tabView = FrameLayout(context).apply {
                val lp = LinearLayout.LayoutParams(W, P)
                lp.setMargins(0, 0, 4, 0)
                layoutParams = lp

                val bg = GradientDrawable().apply {
                    if (isActive) {
                        setColor(Color.parseColor("#3DFFEB3B"))
                        setStroke(1, Color.parseColor("#66FFEB3B"))
                    } else {
                        setColor(Color.parseColor("#222222"))
                        setStroke(1, Color.parseColor("#444444"))
                    }
                    cornerRadius = 8f
                }
                setBackgroundDrawable(bg)
            }

            val titleLabel = TextView(context).apply {
                text = tab.title
                setTextColor(if (isActive) Color.parseColor("#FFEB3B") else Color.parseColor("#CCCCCC"))
                textSize = 13f
                gravity = Gravity.CENTER_VERTICAL
                val lp = FrameLayout.LayoutParams(W, P)
                lp.setMargins(12, 0, 32, 0)
                layoutParams = lp
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                maxWidth = 160
            }
            tabView.addView(titleLabel)

            if (tabs.size > 1) {
                val closeTab = TextView(context).apply {
                    text = "✕"
                    setTextColor(Color.parseColor("#88FFFFFF"))
                    textSize = 12f
                    gravity = Gravity.CENTER
                    val lp = FrameLayout.LayoutParams(28, 28)
                    lp.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    lp.setMargins(0, 0, 4, 0)
                    layoutParams = lp
                    setOnClickListener {
                        tabs.removeAt(i)
                        if (activeTabIndex >= tabs.size) activeTabIndex = tabs.size - 1
                        rebuildTabs(context)
                        showActiveTab(context)
                        scheduleSave(context)
                    }
                }
                tabView.addView(closeTab)
            }

            tabView.setOnClickListener {
                saveCurrentTab(context)
                activeTabIndex = i
                rebuildTabs(context)
                showActiveTab(context)
            }

            tabView.setOnLongClickListener {
                val input = EditText(context).apply {
                    setText(tab.title)
                    setSelection(tab.title.length)
                    setTextColor(Color.WHITE)
                    setHintTextColor(Color.GRAY)
                    setSingleLine(true)
                }
                AlertDialog.Builder(context)
                    .setTitle("Rename tab")
                    .setView(input)
                    .setPositiveButton("Rename") { _, _ ->
                        val newTitle = input.text?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: tab.title
                        tabs[i] = tab.copy(title = newTitle)
                        rebuildTabs(context)
                        scheduleSave(context)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }

            tabContainer.addView(tabView)
        }

        val addBtn = FrameLayout(context).apply {
            val lp = LinearLayout.LayoutParams(46, P)
            lp.setMargins(0, 0, 0, 0)
            layoutParams = lp

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#333333"))
                cornerRadius = 8f
            }
            setBackgroundDrawable(bg)
        }

        val plus = TextView(context).apply {
            text = "+"
            setTextColor(Color.parseColor("#88FFFFFF"))
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(P, P)
        }
        addBtn.addView(plus)

        addBtn.setOnClickListener {
            saveCurrentTab(context)
            val newTab = NoteTab(
                id = java.util.UUID.randomUUID().toString(),
                title = "Note ${tabs.size + 1}",
                content = ""
            )
            tabs.add(newTab)
            activeTabIndex = tabs.size - 1
            rebuildTabs(context)
            showActiveTab(context)
            scheduleSave(context)

            tabScroll.post { tabScroll.fullScroll(View.FOCUS_RIGHT) }
        }

        tabContainer.addView(addBtn)
    }

    private fun showActiveTab(context: Context) {
        if (tabs.isEmpty()) return
        val tab = tabs[activeTabIndex]
        editText.removeTextChangedListener(textWatcher)
        editText.setText(tab.content)
        if (tab.content.isNotEmpty()) {
            editText.setSelection(tab.content.length)
        } else {
            editText.setSelection(0)
        }
        editText.addTextChangedListener(textWatcher)
    }

    private fun saveCurrentTab(context: Context) {
        if (tabs.isNotEmpty() && activeTabIndex < tabs.size) {
            val content = editText.text?.toString() ?: ""
            tabs[activeTabIndex] = tabs[activeTabIndex].copy(content = content)
        }
    }

    private val textWatcher = object : TextWatcher {
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
                    editText.setText(newTxt)
                    editText.setSelection(start + 4)
                    updating = false
                    return
                }
            }
            if (tabs.isNotEmpty() && activeTabIndex < tabs.size) {
                tabs[activeTabIndex] = tabs[activeTabIndex].copy(content = s?.toString() ?: "")
            }
            scheduleSave()
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun createDragBar(context: Context): View {
        val bar = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(P, 52)
            setBackgroundColor(Color.parseColor("#1AFFFFFF"))
        }
        val line = View(context).apply {
            val lp = FrameLayout.LayoutParams(100, 4)
            lp.gravity = Gravity.CENTER
            layoutParams = lp
            setBackgroundColor(Color.parseColor("#44FFFFFF"))
        }
        bar.addView(line)

        bar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX; dragStartY = event.rawY; isDragging = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - dragStartX; val dy = event.rawY - dragStartY
                    if (!isDragging && (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6)) isDragging = true
                    if (isDragging) {
                        onMove?.invoke(dx.toInt(), dy.toInt())
                        dragStartX = event.rawX; dragStartY = event.rawY
                    }
                    true
                }
                else -> false
            }
        }
        return bar
    }

    private fun createToolbar(context: Context): FrameLayout {
        val toolbar = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(P, 44).apply {
                setMargins(12, 102, 12, 0)
            }
        }

        bulletBtn = ImageButton(context).apply {
            val lp = FrameLayout.LayoutParams(40, 40)
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
            setOnClickListener {
                bulletMode = !bulletMode
                val c = if (bulletMode) Color.parseColor("#FFEB3B") else Color.parseColor("#88FFFFFF")
                setColorFilter(c)
                if (bulletMode) insertBullet()
            }
        }
        toolbar.addView(bulletBtn)

        val tabBtn = ImageButton(context).apply {
            val lp = FrameLayout.LayoutParams(40, 40)
            lp.gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = lp
            val bg = GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(2, Color.parseColor("#88FFFFFF"))
                cornerRadius = 8f
            }
            setBackgroundDrawable(bg)
            setImageDrawable(createTabIcon(context))
            setColorFilter(Color.parseColor("#88FFFFFF"))
            scaleType = android.widget.ImageView.ScaleType.CENTER
            contentDescription = "Tab"
            setOnClickListener { insertAtCursor("    ") }
        }
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
            lp.setMargins(16, 150, 16, 44)
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
        }
    }

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
        val bmp = android.graphics.Bitmap.createBitmap(40, 40, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint().apply {
            color = Color.WHITE; textSize = 26f; isAntiAlias = true
        }
        c.drawText("•", 10f, 28f, paint)
        return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
    }

    private fun createTabIcon(context: Context): android.graphics.drawable.Drawable {
        val bmp = android.graphics.Bitmap.createBitmap(40, 40, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val paint = android.graphics.Paint().apply {
            color = Color.WHITE; textSize = 22f; isAntiAlias = true
        }
        c.drawText("↹", 7f, 27f, paint)
        return android.graphics.drawable.BitmapDrawable(context.resources, bmp)
    }

    private fun createHandle(context: Context): View {
        val container = FrameLayout(context).apply {
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
                        dragStartX = event.rawX; dragStartY = event.rawY; isDragging = false; true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - dragStartX; val dy = event.rawY - dragStartY
                        if (!isDragging && (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6)) isDragging = true
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
                            if (!expanded) {
                                onFocus?.invoke(false)
                            }
                            onCollapse?.invoke(expanded)
                        }
                        isDragging = false; true
                    }
                    else -> false
                }
            }
        }
        return container
    }

    private fun createResizer(context: Context): View {
        return View(context).apply {
            val lp = FrameLayout.LayoutParams(36, 36)
            lp.gravity = Gravity.BOTTOM or Gravity.END
            layoutParams = lp
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#55FFFFFF"))
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

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            val ctx = context
            NoteRepository.save(ctx, tabs.toList(), activeTabIndex)
        }
    }

    private fun scheduleSave(context: Context) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            NoteRepository.save(context, tabs.toList(), activeTabIndex)
        }
    }
}
