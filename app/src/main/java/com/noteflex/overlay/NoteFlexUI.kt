package com.noteflex.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NoteFlexUI(context: Context) : FrameLayout(context) {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private var saveJob: Job? = null

    private var expanded = true
    private var bulletMode = false
    private var checkboxMode = false
    private var tabs = mutableListOf<NoteTab>()
    private var activeTabIndex = 0
    private var undoStack = mutableListOf<String>()

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
    private lateinit var checkboxBtn: ImageButton
    private lateinit var sizeBtn: ImageButton
    private lateinit var statusBar: TextView

    private var overlayPanel: FrameLayout? = null

    private var onMove: ((dx: Int, dy: Int) -> Unit)? = null
    private var onResize: ((dx: Int, dy: Int) -> Unit)? = null
    private var onFocus: ((Boolean) -> Unit)? = null
    private var onCollapse: ((Boolean) -> Unit)? = null
    private var onClose: (() -> Unit)? = null
    private var onScale: ((Float) -> Unit)? = null

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
    fun setOnClose(cb: () -> Unit) { onClose = cb }
    fun setOnScale(cb: (Float) -> Unit) { onScale = cb }

    private fun buildUI(context: Context) {
        handleView = createHandle(context)
        addView(handleView)

        cardBody = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(P, P).apply { leftMargin = 52 }
            visibility = VISIBLE
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F51E1E1E"))
                cornerRadius = 16f
            }
            background = bg
            clipChildren = false
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

        statusBar = createStatusBar(context)
        cardBody.addView(statusBar)
        cardBody.addView(createResizer(context))
        addView(cardBody)

        loadData(context)
    }

    private fun showOverlayPanel(content: View, width: Int = 280, height: Int = W) {
        hideOverlayPanel()
        overlayPanel = FrameLayout(context).apply {
            val lp = FrameLayout.LayoutParams(width, height)
            lp.gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#F51E1E1E"))
                cornerRadius = 16f
                setStroke(2, Color.parseColor("#88FFFFFF"))
            }
            background = bg
            elevation = 8f
            addView(content)
            bringToFront()
        }
        cardBody.addView(overlayPanel)
    }

    private fun hideOverlayPanel() {
        overlayPanel?.let { cardBody.removeView(it) }
        overlayPanel = null
    }

    private fun showDialog(title: String, body: View, onDone: (() -> Unit)? = null) {
        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val titleTv = TextView(context).apply {
            text = title
            setTextColor(Color.parseColor("#FFEB3B"))
            textSize = 16f
            gravity = Gravity.CENTER
        }
        ll.addView(titleTv)
        ll.addView(body)

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(P, W).apply { topMargin = 16 }
        }
        val doneBtn = TextView(context).apply {
            text = "Done"
            setTextColor(Color.parseColor("#FFEB3B"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(24, 8, 24, 8)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#33FFEB3B"))
                cornerRadius = 8f
            }
            setBackgroundDrawable(bg)
            setOnClickListener {
                onDone?.invoke()
                hideOverlayPanel()
            }
        }
        btnRow.addView(doneBtn)

        val cancelBtn = TextView(context).apply {
            text = "Cancel"
            setTextColor(Color.parseColor("#88FFFFFF"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(24, 8, 24, 8)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#33FFFFFF"))
                cornerRadius = 8f
            }
            setBackgroundDrawable(bg)
            setOnClickListener { hideOverlayPanel() }
            layoutParams = LinearLayout.LayoutParams(W, W).apply { rightMargin = 8 }
        }
        btnRow.addView(cancelBtn, 0)

        ll.addView(btnRow)
        showOverlayPanel(ll)
    }

    private fun showMenu(items: List<String>, title: String, onItem: (Int) -> Unit) {
        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }
        if (title.isNotEmpty()) {
            val titleTv = TextView(context).apply {
                text = title
                setTextColor(Color.parseColor("#FFEB3B"))
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 12)
            }
            ll.addView(titleTv)
        }
        for ((i, item) in items.withIndex()) {
            val btn = TextView(context).apply {
                text = item
                setTextColor(Color.WHITE)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(16, 12, 16, 12)
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#222222"))
                    cornerRadius = 8f
                }
                setBackgroundDrawable(bg)
                setOnClickListener {
                    hideOverlayPanel()
                    onItem(i)
                }
                layoutParams = LinearLayout.LayoutParams(P, W).apply {
                    if (i > 0) topMargin = 4
                }
            }
            ll.addView(btn)
        }
        val cancelBtn = TextView(context).apply {
            text = "Cancel"
            setTextColor(Color.parseColor("#88FFFFFF"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(16, 12, 16, 12)
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#333333"))
                cornerRadius = 8f
            }
            setBackgroundDrawable(bg)
            setOnClickListener { hideOverlayPanel() }
            layoutParams = LinearLayout.LayoutParams(P, W).apply { topMargin = 8 }
        }
        ll.addView(cancelBtn)
        showOverlayPanel(ll, 260, W)
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
                val bg = android.graphics.drawable.GradientDrawable().apply {
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
                text = if (tab.locked && !AuthState.isUnlocked(tab.id)) "🔒 ${tab.title}" else tab.title
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
                        if (tab.locked) AuthState.lock(tab.id)
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
                if (tab.locked && !AuthState.isUnlocked(tab.id)) {
                    saveCurrentTab(context)
                    val intent = Intent(context, AuthActivity::class.java).apply {
                        putExtra("tabId", tab.id)
                        putExtra("tabTitle", tab.title)
                        putExtra("passwordHash", tab.passwordHash ?: "")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    scope.launch {
                        var attempts = 0
                        while (attempts < 50) {
                            delay(200)
                            if (AuthState.isUnlocked(tab.id)) {
                                withContext(Dispatchers.Main) {
                                    activeTabIndex = i
                                    rebuildTabs(context)
                                    showActiveTab(context)
                                }
                                break
                            }
                            attempts++
                        }
                    }
                } else {
                    saveCurrentTab(context)
                    activeTabIndex = i
                    rebuildTabs(context)
                    showActiveTab(context)
                }
            }

            tabView.setOnLongClickListener {
                showMenu(
                    items = mutableListOf<String>().apply {
                        add("Rename")
                        add(if (tab.locked) { if (AuthState.isUnlocked(tab.id)) "Remove password" else "Unlock" } else "Lock with password")
                    },
                    title = tab.title,
                    onItem = { which ->
                        when (which) {
                            0 -> showRenameDialog(context, i, tab)
                            1 -> handleLockAction(context, i, tab)
                        }
                    }
                )
                true
            }

            tabContainer.addView(tabView)
        }

        val addBtn = FrameLayout(context).apply {
            val lp = LinearLayout.LayoutParams(46, P)
            lp.setMargins(0, 0, 0, 0)
            layoutParams = lp
            val bg = android.graphics.drawable.GradientDrawable().apply {
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

    private fun showRenameDialog(context: Context, index: Int, tab: NoteTab) {
        val input = EditText(context).apply {
            setText(tab.title)
            setSelection(tab.title.length)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(P, W)
        }
        showDialog("Rename tab", input) {
            val newTitle = input.text?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: tab.title
            tabs[index] = tab.copy(title = newTitle)
            rebuildTabs(context)
            scheduleSave(context)
        }
    }

    private fun handleLockAction(context: Context, index: Int, tab: NoteTab) {
        if (tab.locked && AuthState.isUnlocked(tab.id)) {
            val msg = TextView(context).apply {
                text = "Remove the password from this note?"
                setTextColor(Color.WHITE)
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(P, W)
            }
            showDialog("Remove password", msg) {
                tabs[index] = tab.copy(locked = false, passwordHash = null)
                AuthState.lock(tab.id)
                rebuildTabs(context)
                scheduleSave(context)
            }
        } else if (tab.locked && !AuthState.isUnlocked(tab.id)) {
            val intent = Intent(context, AuthActivity::class.java).apply {
                putExtra("tabId", tab.id)
                putExtra("tabTitle", tab.title)
                putExtra("passwordHash", tab.passwordHash ?: "")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            scope.launch {
                var attempts = 0
                while (attempts < 50) {
                    delay(200)
                    if (AuthState.isUnlocked(tab.id)) {
                        withContext(Dispatchers.Main) {
                            activeTabIndex = index
                            rebuildTabs(context)
                            showActiveTab(context)
                        }
                        break
                    }
                    attempts++
                }
            }
        } else {
            val input = EditText(context).apply {
                hint = "Enter password"
                setSingleLine(true)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
                layoutParams = LinearLayout.LayoutParams(P, W)
            }
            val confirm = EditText(context).apply {
                hint = "Confirm password"
                setSingleLine(true)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setTextColor(Color.WHITE)
                setHintTextColor(Color.GRAY)
                layoutParams = LinearLayout.LayoutParams(P, W)
            }
            val ll = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(input)
                val spacer = View(context)
                spacer.layoutParams = LinearLayout.LayoutParams(P, 12)
                addView(spacer)
                addView(confirm)
            }
            showDialog("Set password", ll) {
                val pw = input.text?.toString() ?: ""
                val pw2 = confirm.text?.toString() ?: ""
                if (pw.isEmpty() || pw != pw2) {
                    val errorMsg = TextView(context).apply {
                        text = if (pw.isEmpty()) "Password cannot be empty" else "Passwords do not match"
                        setTextColor(Color.parseColor("#FF4444"))
                        textSize = 14f
                    }
                    showDialog("Error", errorMsg)
                } else {
                    val hash = AuthActivity.hashPassword(pw)
                    tabs[index] = tab.copy(locked = true, passwordHash = hash)
                    AuthState.unlock(tab.id)
                    rebuildTabs(context)
                    scheduleSave(context)
                }
            }
        }
    }

    private fun showActiveTab(context: Context) {
        if (tabs.isEmpty()) return
        val tab = tabs[activeTabIndex]
        if (tab.locked && !AuthState.isUnlocked(tab.id)) {
            editText.removeTextChangedListener(textWatcher)
            editText.setText("")
            editText.hint = "This note is locked"
            editText.isEnabled = false
        } else {
            editText.isEnabled = true
            editText.hint = "Start typing..."
            editText.removeTextChangedListener(textWatcher)
            editText.setText(tab.content)
            if (tab.content.isNotEmpty()) {
                editText.setSelection(tab.content.length)
            } else {
                editText.setSelection(0)
            }
            editText.addTextChangedListener(textWatcher)
        }
        updateStatus()
    }

    private fun saveCurrentTab(context: Context) {
        if (tabs.isNotEmpty() && activeTabIndex < tabs.size) {
            val tab = tabs[activeTabIndex]
            if (!tab.locked || AuthState.isUnlocked(tab.id)) {
                tab.content = editText.text?.toString() ?: ""
                tabs[activeTabIndex] = tab
            }
        }
    }

    private val textWatcher = object : TextWatcher {
        private var updating = false
        override fun beforeTextChanged(s: CharSequence?, s1: Int, s2: Int, s3: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (updating) return
            if (count == 1 && before == 0) {
                val c = s?.getOrNull(start) ?: return
                if (c == '\n') {
                    if (bulletMode || checkboxMode) {
                        updating = true
                        val txt = s.toString()
                        val prefix = if (bulletMode) "•  " else "[ ] "
                        val newTxt = txt.substring(0, start + 1) + prefix + txt.substring(start + 1)
                        editText.setText(newTxt)
                        editText.setSelection(start + 1 + prefix.length)
                        updating = false
                        return
                    }
                }
            }
            if (tabs.isNotEmpty() && activeTabIndex < tabs.size) {
                val tab = tabs[activeTabIndex]
                if (!tab.locked || AuthState.isUnlocked(tab.id)) {
                    tab.content = s?.toString() ?: ""
                    tabs[activeTabIndex] = tab
                }
            }
            scheduleSave()
        }
        override fun afterTextChanged(s: Editable?) {
            if (!updating) updateStatus()
        }
    }

    private fun createDragBar(context: Context): View {
        val bar = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(P, 52)
            setBackgroundColor(Color.parseColor("#1AFFFFFF"))
        }
        val line = View(context).apply {
            val lp = FrameLayout.LayoutParams(48, 4)
            lp.gravity = Gravity.CENTER
            layoutParams = lp
            setBackgroundColor(Color.parseColor("#66FFFFFF"))
        }
        bar.addView(line)

        val titleText = TextView(context).apply {
            text = "NoteFlex"
            setTextColor(Color.parseColor("#66FFFFFF"))
            textSize = 10f
            gravity = Gravity.CENTER
            val lp = FrameLayout.LayoutParams(W, W)
            lp.gravity = Gravity.CENTER
            lp.topMargin = 34
            layoutParams = lp
        }
        bar.addView(titleText)

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
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        expanded = !expanded
                        cardBody.visibility = if (expanded) VISIBLE else GONE
                        if (!expanded) onFocus?.invoke(false)
                        onCollapse?.invoke(expanded)
                    }
                    isDragging = false; true
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

        bulletBtn = createToolbarBtn(context).apply {
            setImageDrawable(makeIcon { cv, p -> p.textSize = 26f; cv.drawText("•", 10f, 28f, p) })
            contentDescription = "Bullets"
            setOnClickListener {
                bulletMode = !bulletMode; checkboxMode = false; updateBtnColors()
                if (bulletMode) insertBullet()
            }
        }
        toolbar.addView(bulletBtn)

        checkboxBtn = createToolbarBtn(context).apply {
            setImageDrawable(makeIcon { cv, p ->
                p.style = android.graphics.Paint.Style.STROKE; p.strokeWidth = 2.5f
                cv.drawRect(8f, 8f, 32f, 32f, p)
                p.strokeWidth = 3f; cv.drawLine(11f, 18f, 17f, 27f, p); cv.drawLine(17f, 27f, 29f, 14f, p)
            })
            contentDescription = "Checkbox"
            setOnClickListener {
                checkboxMode = !checkboxMode; bulletMode = false; updateBtnColors()
                if (checkboxMode) insertAtCursor("[ ] ")
            }
            (layoutParams as? FrameLayout.LayoutParams)?.apply { setMargins(46, 0, 0, 0) }
        }
        toolbar.addView(checkboxBtn)

        val undoBtn = createToolbarBtn(context).apply {
            setImageDrawable(makeIcon { cv, p ->
                p.style = android.graphics.Paint.Style.STROKE; p.strokeWidth = 2.5f
                cv.drawArc(8f, 8f, 32f, 32f, -90f, -270f, false, p)
                p.strokeWidth = 3f; cv.drawLine(24f, 14f, 8f, 20f, p); cv.drawLine(8f, 20f, 24f, 26f, p)
            })
            contentDescription = "Undo"
            setOnClickListener { undo() }
            (layoutParams as? FrameLayout.LayoutParams)?.apply { setMargins(92, 0, 0, 0) }
        }
        toolbar.addView(undoBtn)

        val searchBtn = createToolbarBtn(context).apply {
            setImageDrawable(makeIcon { cv, p ->
                p.style = android.graphics.Paint.Style.STROKE; p.strokeWidth = 2.5f
                cv.drawCircle(16f, 16f, 10f, p)
                p.strokeWidth = 3f; cv.drawLine(24f, 24f, 34f, 34f, p)
            })
            contentDescription = "Search"
            setOnClickListener { showSearchPanel() }
            (layoutParams as? FrameLayout.LayoutParams)?.apply { setMargins(138, 0, 0, 0) }
        }
        toolbar.addView(searchBtn)

        sizeBtn = createToolbarBtn(context).apply {
            setImageDrawable(makeIcon { cv, p ->
                p.style = android.graphics.Paint.Style.STROKE; p.strokeWidth = 2.5f
                val path = android.graphics.Path()
                path.moveTo(10f, 30f); path.lineTo(18f, 10f)
                path.moveTo(30f, 10f); path.lineTo(22f, 30f)
                path.moveTo(18f, 10f); path.lineTo(14f, 18f)
                path.moveTo(22f, 30f); path.lineTo(26f, 22f)
                cv.drawPath(path, p)
            })
            contentDescription = "Size"
            setOnClickListener { showSizePanel() }
            (layoutParams as? FrameLayout.LayoutParams)?.apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL; setMargins(0, 0, 92, 0) }
        }
        toolbar.addView(sizeBtn)

        val minimizeBtn = createToolbarBtn(context).apply {
            setImageDrawable(makeIcon { cv, p ->
                p.style = android.graphics.Paint.Style.STROKE; p.strokeWidth = 3f
                cv.drawLine(8f, 20f, 32f, 20f, p)
            })
            contentDescription = "Minimize"
            setOnClickListener {
                expanded = false; cardBody.visibility = GONE
                onFocus?.invoke(false); onCollapse?.invoke(false)
            }
            (layoutParams as? FrameLayout.LayoutParams)?.apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL; setMargins(0, 0, 46, 0) }
        }
        toolbar.addView(minimizeBtn)

        createToolbarBtn(context).apply {
            setImageDrawable(makeIcon { cv, p ->
                p.style = android.graphics.Paint.Style.STROKE; p.strokeWidth = 3f
                cv.drawLine(12f, 12f, 28f, 28f, p); cv.drawLine(28f, 12f, 12f, 28f, p)
            })
            contentDescription = "Close"
            setOnClickListener { onClose?.invoke() }
            layoutParams = FrameLayout.LayoutParams(40, 40).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        }.also { toolbar.addView(it) }

        return toolbar
    }

    private fun createToolbarBtn(context: Context): ImageButton {
        return ImageButton(context).apply {
            layoutParams = FrameLayout.LayoutParams(40, 40).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL }
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.TRANSPARENT); setStroke(2, Color.parseColor("#88FFFFFF")); cornerRadius = 8f
            }
            setBackgroundDrawable(bg); setColorFilter(Color.parseColor("#88FFFFFF")); scaleType = android.widget.ImageView.ScaleType.CENTER
        }
    }

    private fun updateBtnColors() {
        bulletBtn.setColorFilter(if (bulletMode) Color.parseColor("#FFEB3B") else Color.parseColor("#88FFFFFF"))
        checkboxBtn.setColorFilter(if (checkboxMode) Color.parseColor("#FFEB3B") else Color.parseColor("#88FFFFFF"))
    }

    private fun makeIcon(draw: (android.graphics.Canvas, android.graphics.Paint) -> Unit): android.graphics.drawable.Drawable {
        val bmp = android.graphics.Bitmap.createBitmap(40, 40, android.graphics.Bitmap.Config.ARGB_8888)
        val cv = android.graphics.Canvas(bmp)
        draw(cv, android.graphics.Paint().apply { isAntiAlias = true; color = Color.WHITE })
        return android.graphics.drawable.BitmapDrawable(resources, bmp)
    }

    private fun showSizePanel() {
        val slider = SeekBar(context).apply {
            max = 70; progress = 70
            layoutParams = LinearLayout.LayoutParams(200, W)
        }
        val label = TextView(context).apply {
            text = "100%"; textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(P, W)
        }
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, prg: Int, fromUser: Boolean) {
                val pct = 30 + prg; label.text = "$pct%"
                onScale?.invoke(pct / 100f)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                onScale?.invoke((30 + slider.progress) / 100f)
            }
        })
        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(label); addView(slider)
        }
        showDialog("Overlay size", ll)
    }

    private fun showSearchPanel() {
        val input = EditText(context).apply {
            hint = "Search..."; setSingleLine(true)
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            layoutParams = LinearLayout.LayoutParams(200, W)
        }
        showDialog("Find in note", input) {
            val query = input.text?.toString() ?: ""
            if (query.isNotEmpty()) {
                val body = editText.text?.toString() ?: ""
                val idx = body.indexOf(query, ignoreCase = true)
                if (idx >= 0) {
                    editText.requestFocus(); editText.setSelection(idx, idx + query.length)
                } else {
                    val err = TextView(context).apply {
                        this.text = "No matches for \"$query\""; setTextColor(Color.parseColor("#FF4444")); textSize = 14f
                    }
                    showDialog("Not found", err)
                }
            }
        }
    }

    private fun createEditText(context: Context): EditText {
        return EditText(context).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#88FFFFFF"))
            hint = "Start typing..."
            textSize = 15f
            background = null
            val lp = FrameLayout.LayoutParams(P, P)
            lp.setMargins(16, 150, 16, 70)
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
                    setText(newText); setSelection(pos + 4); true
                } else false
            }
            setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val et = v as EditText
                    val layout = et.layout ?: return@setOnTouchListener false
                    val x = event.x; val y = event.y
                    val line = layout.getLineForVertical(y.toInt())
                    val offset = layout.getOffsetForHorizontal(line, x)
                    val txt = et.text?.toString() ?: ""
                    if (toggleCheckboxAt(txt, offset)) {
                        return@setOnTouchListener true
                    }
                }
                false
            }
        }
    }

    private fun toggleCheckboxAt(text: String, offset: Int): Boolean {
        for (i in maxOf(0, offset - 2) until minOf(text.length, offset + 2)) {
            if (i + 2 < text.length && text[i] == '[' && text[i + 2] == ']') {
                val c = text[i + 1]
                if (c == ' ' || c == 'x' || c == 'X') {
                    val sb = StringBuilder(text)
                    sb[i + 1] = if (c == ' ') 'x' else ' '
                    pushUndo(text)
                    editText.setText(sb.toString())
                    editText.setSelection(offset.coerceAtMost(sb.length))
                    return true
                }
            }
        }
        return false
    }

    private fun insertBullet() {
        val s = editText.text?.toString() ?: ""
        val pos = editText.selectionStart
        val lineStart = s.lastIndexOf('\n', pos - 1) + 1
        pushUndo(s)
        val newText = s.substring(0, lineStart) + "•  " + s.substring(lineStart)
        editText.setText(newText); editText.setSelection(lineStart + 3)
    }

    private fun insertAtCursor(text: String) {
        val s = editText.text?.toString() ?: ""
        val pos = editText.selectionStart
        pushUndo(s)
        val newText = s.substring(0, pos) + text + s.substring(pos)
        editText.setText(newText); editText.setSelection(pos + text.length)
    }

    private var undoHistory = mutableListOf<String>()
    private fun pushUndo(text: String) {
        if (undoHistory.size > 100) undoHistory.removeAt(0)
        undoHistory.add(text)
    }

    private fun undo() {
        val current = editText.text?.toString() ?: ""
        if (undoHistory.isNotEmpty()) {
            val prev = undoHistory.removeAt(undoHistory.lastIndex)
            editText.setText(prev)
            editText.setSelection(prev.length.coerceAtMost(current.length))
        }
    }

    private fun createStatusBar(context: Context): TextView {
        return TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(P, W).apply { setMargins(16, 0, 16, 44); gravity = Gravity.BOTTOM }
            textSize = 11f; setTextColor(Color.parseColor("#66FFFFFF")); gravity = Gravity.START
        }
    }

    private fun updateStatus() {
        val text = editText.text?.toString() ?: ""
        val chars = text.length
        val words = if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
        val lines = text.count { it == '\n' } + if (text.isNotEmpty()) 1 else 0
        statusBar.text = "$words words  ·  $chars chars  ·  $lines lines"
    }

    private fun createHandle(context: Context): View {
        val container = FrameLayout(context).apply {
            val lp = FrameLayout.LayoutParams(52, 400)
            lp.gravity = Gravity.CENTER_VERTICAL
            layoutParams = lp
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#991E1E1E"))
                cornerRadii = floatArrayOf(12f, 12f, 0f, 0f, 0f, 0f, 12f, 12f)
            }
            setBackgroundDrawable(bg)
            val dots = TextView(context).apply {
                text = "⋮"
                setTextColor(Color.parseColor("#DDFFFFFF")); textSize = 32f; gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(W, W).apply { gravity = Gravity.CENTER }
            }
            addView(dots)
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { dragStartX = event.rawX; dragStartY = event.rawY; isDragging = false; true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - dragStartX; val dy = event.rawY - dragStartY
                        if (!isDragging && (kotlin.math.abs(dx) > 6 || kotlin.math.abs(dy) > 6)) isDragging = true
                        if (isDragging) { onMove?.invoke(dx.toInt(), dy.toInt()); dragStartX = event.rawX; dragStartY = event.rawY }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            expanded = !expanded; cardBody.visibility = if (expanded) VISIBLE else GONE
                            if (!expanded) onFocus?.invoke(false)
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
            val lp = FrameLayout.LayoutParams(30, 30)
            lp.gravity = Gravity.BOTTOM or Gravity.END
            layoutParams = lp
            val bg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#55FFFFFF"))
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 16f, 0f)
            }
            setBackgroundDrawable(bg)
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { resizeTouchX = event.rawX; resizeTouchY = event.rawY; true }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - resizeTouchX).toInt(); val dy = (event.rawY - resizeTouchY).toInt()
                        if (dx != 0 || dy != 0) { onResize?.invoke(dx, dy); resizeTouchX = event.rawX; resizeTouchY = event.rawY }
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
            NoteRepository.save(context, tabs.toList(), activeTabIndex)
        }
    }

    private fun scheduleSave(context: Context) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            NoteRepository.save(context, tabs.toList(), activeTabIndex)
        }
    }

    fun getCurrentScale(): Float = 1.0f
}