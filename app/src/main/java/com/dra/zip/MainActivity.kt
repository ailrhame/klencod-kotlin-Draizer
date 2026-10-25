package com.dra.zip

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : Activity() {

    // ============================================================
    // ===== DATA MODEL ===========================================
    // ============================================================
    data class Asset(
        val symbol: String,
        val name: String,
        var balance: Double,
        var price: Double,
        val change: Double,
        val bgColor: Int,
        val fgColor: Int,
        val iconChar: String
    )

    data class Currency(val symbol: String, val rate: Double, val name: String)

    private val assets = mutableListOf(
        Asset("BTC", "Bitcoin", 1.2384, 117300.0, 1.82,
            Color.parseColor("#20F7931A"), Color.parseColor("#F7931A"), "₿"),
        Asset("ETH", "Ethereum", 12.452, 3400.0, -0.54,
            Color.parseColor("#20627EEA"), Color.parseColor("#627EEA"), "Ξ"),
        Asset("SOL", "Solana", 156.32, 180.0, 4.15,
            Color.parseColor("#209945FF"), Color.parseColor("#9945FF"), "S"),
        Asset("USDC", "USD Coin", 25000.0, 1.0, 0.0,
            Color.parseColor("#202775CA"), Color.parseColor("#2775CA"), "$"),
        Asset("ADA", "Cardano", 18500.0, 0.5, 1.23,
            Color.parseColor("#200033AD"), Color.parseColor("#0033AD"), "₳"),
        Asset("DOGE", "Dogecoin", 42000.0, 0.1187, 0.87,
            Color.parseColor("#20C2A633"), Color.parseColor("#C2A633"), "Ð")
    )

    private val currencies = linkedMapOf(
        "USD" to Currency("$", 1.0, "الدولار الأمريكي"),
        "EUR" to Currency("€", 0.92, "اليورو"),
        "GBP" to Currency("£", 0.79, "الجنيه الإسترليني"),
        "JPY" to Currency("¥", 149.5, "الين الياباني"),
        "SAR" to Currency("﷼", 3.75, "الريال السعودي"),
        "AED" to Currency("د.إ", 3.67, "الدرهم الإماراتي"),
        "IQD" to Currency("ع.د", 1310.0, "الدينار العراقي"),
        "DZD" to Currency("د.ج", 134.0, "الدينار الجزائري"),
        "EGP" to Currency("ج.م", 47.9, "الجنيه المصري"),
        "MAD" to Currency("د.م.", 9.92, "الدرهم المغربي"),
        "TND" to Currency("د.ت", 3.10, "الدينار التونسي"),
        "LBP" to Currency("ل.ل", 89500.0, "الليرة اللبنانية"),
        "JOD" to Currency("د.أ", 0.709, "الدينار الأردني"),
        "KWD" to Currency("د.ك", 0.307, "الدينار الكويتي"),
        "BHD" to Currency("د.ب", 0.376, "الدينار البحريني"),
        "QAR" to Currency("ر.ق", 3.64, "الريال القطري"),
        "OMR" to Currency("ر.ع", 0.385, "الريال العماني"),
        "SYP" to Currency("ل.س", 13000.0, "الليرة السورية"),
        "YER" to Currency("ر.ي", 250.0, "الريال اليمني"),
        "SDG" to Currency("ج.س", 600.0, "الجنيه السوداني"),
        "LYD" to Currency("ل.د", 4.82, "الدينار الليبي")
    )

    private var activeCurrency = "USD"
    private var totalChangePercent = 2.34
    private var editMode = false

    // ============================================================
    // ===== COLORS ===============================================
    // ============================================================
    private val COLOR_BG = Color.parseColor("#0A0E27")
    private val COLOR_CARD = Color.parseColor("#141A3A")
    private val COLOR_BORDER = Color.parseColor("#1E2760")
    private val COLOR_PRIMARY = Color.parseColor("#0052FF")
    private val COLOR_TEXT_DIM = Color.parseColor("#8A8FB8")
    private val COLOR_GREEN = Color.parseColor("#0ECB81")
    private val COLOR_RED = Color.parseColor("#F6465D")

    // ============================================================
    // ===== UI REFS ==============================================
    // ============================================================
    private lateinit var rootFrame: FrameLayout
    private lateinit var mainColumn: LinearLayout
    private lateinit var editBar: LinearLayout
    private lateinit var balanceText: TextView
    private lateinit var currencyLabel: TextView
    private lateinit var changeText: TextView
    private lateinit var currencySymbolView: TextView
    private lateinit var currencyCodeView: TextView
    private lateinit var currencyBadge: LinearLayout
    private lateinit var assetsContainer: LinearLayout
    private lateinit var toastView: TextView
    private lateinit var modalOverlay: FrameLayout
    private lateinit var modalTitle: TextView
    private lateinit var modalInput: EditText
    private lateinit var ghostTrigger: View

    private var editTargetType = ""
    private var editTargetCoin = ""

    private val fadeOutRunnable = Runnable {
        toastView.animate().alpha(0f).setDuration(200).withEndAction {
            toastView.visibility = View.INVISIBLE
        }.start()
    }

    // ============================================================
    // ===== LIFECYCLE ============================================
    // ============================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = COLOR_BG
        window.navigationBarColor = COLOR_BG
        buildUI()
        refreshUI()
    }

    // ============================================================
    // ===== HELPERS ==============================================
    // ============================================================
    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).roundToInt()

    private fun tv(text: String, sizeSp: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(color)
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun roundedBg(bgColor: Int, radiusDp: Float, strokeColor: Int? = null, strokeDp: Float = 1f): GradientDrawable =
        GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeColor != null) setStroke(dp(strokeDp), strokeColor)
        }

    private fun lpWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun lpWeight(w: Float = 1f, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, h, w)

    // ============================================================
    // ===== BUILD UI =============================================
    // ============================================================
    private fun buildUI() {
        rootFrame = FrameLayout(this).apply {
            setBackgroundColor(COLOR_BG)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            clipChildren = false
        }
        setContentView(rootFrame)

        mainColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            clipChildren = false
        }
        rootFrame.addView(mainColumn)

        // --- Edit bar (initially hidden) ---
        editBar = buildEditBar().apply { visibility = View.GONE }
        mainColumn.addView(editBar)

        // --- Scrollable content ---
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(20))
        }
        scrollView.addView(scrollContent)
        mainColumn.addView(scrollView)

        scrollContent.addView(buildHeader())
        scrollContent.addView(buildWalletSection())

        assetsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, dp(18), 0)
        }
        scrollContent.addView(assetsContainer)

        // --- Bottom nav (fixed) ---
        mainColumn.addView(buildBottomNav())

        // --- Ghost trigger (top-right, invisible) ---
        ghostTrigger = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            alpha = 1f
            layoutParams = FrameLayout.LayoutParams(dp(80), dp(90)).apply {
                gravity = Gravity.TOP or Gravity.RIGHT
            }
        }
        rootFrame.addView(ghostTrigger)
        setupGhostTrigger()

        // --- Toast ---
        toastView = tv("", 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = roundedBg(COLOR_PRIMARY, 30f)
            alpha = 0f
            visibility = View.INVISIBLE
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(110)
            }
        }
        rootFrame.addView(toastView)

        // --- Edit modal ---
        modalOverlay = buildEditModal().apply { visibility = View.GONE }
        rootFrame.addView(modalOverlay)
    }

    // ============================================================
    // ===== HEADER ===============================================
    // ============================================================
    private fun buildHeader(): LinearLayout {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(18), dp(16), dp(18), dp(10))
            gravity = Gravity.CENTER_VERTICAL
        }
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = TextView(this).apply {
            text = "C"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(COLOR_PRIMARY)
                shape = GradientDrawable.OVAL
            }
            layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
        }
        val title = tv("Coinbase", 18f, Color.WHITE, true).apply {
            setPadding(dp(10), 0, 0, 0)
        }
        left.addView(logo)
        left.addView(title)

        val icons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        icons.addView(tv("🔔", 18f, Color.WHITE).apply { setPadding(dp(9), 0, dp(9), 0) })
        icons.addView(tv("⚙", 18f, Color.WHITE).apply { setPadding(dp(9), 0, dp(9), 0) })

        header.addView(left, lpWeight())
        header.addView(icons)
        return header
    }

    // ============================================================
    // ===== EDIT BAR =============================================
    // ============================================================
    private fun buildEditBar(): LinearLayout {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(COLOR_PRIMARY)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            gravity = Gravity.CENTER_VERTICAL
        }
        val label = tv("⚡ وضع التحرير الكامل", 12f, Color.WHITE, true)
        val exitBtn = TextView(this).apply {
            text = "إنهاء"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(COLOR_PRIMARY)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = roundedBg(Color.WHITE, 20f)
            setOnClickListener { toggleEditMode() }
        }
        bar.addView(label, lpWeight())
        bar.addView(exitBtn)
        return bar
    }

    // ============================================================
    // ===== WALLET SECTION =======================================
    // ============================================================
    private fun buildWalletSection(): LinearLayout {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, dp(18), 0)
        }

        // Portfolio label + currency badge
        val labelRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val portfolioLabel = tv("PORTFOLIO", 12f, COLOR_TEXT_DIM, true).apply {
            letterSpacing = 0.08f
        }
        currencyBadge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(4))
            background = roundedBg(COLOR_CARD, 20f)
            isClickable = true
            setOnClickListener { showCurrencyPopup() }
        }
        currencySymbolView = tv("$", 12f, Color.WHITE, true)
        currencyCodeView = tv("USD", 12f, Color.WHITE, true).apply { setPadding(dp(4), 0, 0, 0) }
        val chevron = tv("▾", 10f, Color.WHITE).apply { setPadding(dp(4), 0, 0, 0) }
        currencyBadge.addView(currencySymbolView)
        currencyBadge.addView(currencyCodeView)
        currencyBadge.addView(chevron)

        labelRow.addView(portfolioLabel)
        labelRow.addView(currencyBadge, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { marginStart = dp(8) })
        section.addView(labelRow)

        // Balance row
        val balanceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(0, dp(6), 0, 0)
        }
        balanceText = tv("$254,948.35", 40f, Color.WHITE, true).apply {
            setOnClickListener { if (editMode) openEdit("totalBalance", "") }
        }
        currencyLabel = tv("USD", 18f, COLOR_TEXT_DIM, true).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(6) }
        }
        balanceRow.addView(balanceText)
        balanceRow.addView(currencyLabel)
        section.addView(balanceRow)

        // Change pill
        changeText = tv("", 13f, COLOR_GREEN, true).apply {
            setPadding(dp(10), dp(3), dp(10), dp(3))
        }
        section.addView(changeText, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(6) })

        // Chart
        val chart = ChartView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(140)
            ).apply { topMargin = dp(16); bottomMargin = dp(16) }
        }
        section.addView(chart)

        // Quick actions
        val quickActions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        quickActions.addView(
            buildQuickBtn("⊕", "Buy", true),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) }
        )
        quickActions.addView(
            buildQuickBtn("⊖", "Sell", false),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6); marginEnd = dp(6) }
        )
        quickActions.addView(
            buildQuickBtn("⇄", "Convert", false),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) }
        )
        section.addView(quickActions, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10); bottomMargin = dp(20) })

        // Tabs
        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(10))
        }
        tabs.addView(buildTab("Assets", true, dp(24)))
        tabs.addView(buildTab("Trade", false, dp(24)))
        tabs.addView(buildTab("Earn", false, 0))
        section.addView(tabs)
        section.addView(View(this).apply {
            setBackgroundColor(COLOR_BORDER)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })

        return section
    }

    private fun buildQuickBtn(symbol: String, label: String, primary: Boolean): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(14))
            background = roundedBg(
                if (primary) COLOR_PRIMARY else COLOR_CARD,
                14f,
                if (primary) COLOR_PRIMARY else COLOR_BORDER
            )
        }
        container.addView(tv(symbol, 16f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        container.addView(tv(label, 13f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })
        return container
    }

    private fun buildTab(label: String, active: Boolean, marginEndPx: Int): LinearLayout {
        val tab = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { if (marginEndPx > 0) marginEnd = marginEndPx }
        }
        tab.addView(tv(label, 14f, if (active) Color.WHITE else COLOR_TEXT_DIM, true).apply {
            setPadding(0, 0, 0, dp(8))
        })
        tab.addView(View(this).apply {
            setBackgroundColor(if (active) COLOR_PRIMARY else Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2))
        })
        return tab
    }

    // ============================================================
    // ===== BOTTOM NAV ===========================================
    // ============================================================
    private fun buildBottomNav(): LinearLayout {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        wrapper.addView(View(this).apply {
            setBackgroundColor(COLOR_BORDER)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(COLOR_BG)
            setPadding(0, dp(10), 0, dp(10))
            gravity = Gravity.CENTER_VERTICAL
        }

        nav.addView(buildNavItem("🏠", "Home", true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(buildNavItem("📈", "Portfolio", false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(View(this), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(buildNavItem("💎", "Earn", false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(buildNavItem("👤", "Profile", false), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        wrapper.addView(nav)

        // Floating center button
        val fab = TextView(this).apply {
            text = "⇄"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(COLOR_PRIMARY)
                shape = GradientDrawable.OVAL
            }
            layoutParams = FrameLayout.LayoutParams(dp(52), dp(52)).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(48)
            }
            elevation = dp(8).toFloat()
        }
        rootFrame.addView(fab)

        return wrapper
    }

    private fun buildNavItem(icon: String, label: String, active: Boolean): LinearLayout {
        val color = if (active) COLOR_PRIMARY else COLOR_TEXT_DIM
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))
        }
        item.addView(tv(icon, 18f, color).apply { gravity = Gravity.CENTER })
        item.addView(tv(label, 10f, color, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        })
        return item
    }

    // ============================================================
    // ===== ASSETS ===============================================
    // ============================================================
    private fun renderAssets() {
        assetsContainer.removeAllViews()
        assets.forEach { assetsContainer.addView(buildAssetRow(it)) }
    }

    private fun buildAssetRow(asset: Asset): LinearLayout {
        val usdValue = asset.balance * asset.price
        val wrapper = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, dp(14))
        }

        // Left: icon + name + symbol
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val icon = TextView(this).apply {
            text = asset.iconChar
            setTextColor(asset.fgColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                setColor(asset.bgColor)
                shape = GradientDrawable.OVAL
            }
            layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
        }
        val names = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        val nameRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        nameRow.addView(tv(asset.name, 15f, Color.WHITE, true))
        if (editMode) {
            val pill = tv("✏", 9f, Color.WHITE, true).apply {
                setPadding(dp(8), dp(3), dp(8), dp(3))
                background = roundedBg(COLOR_PRIMARY, 12f)
            }
            pill.setOnClickListener { openEdit("coinBalance", asset.symbol) }
            nameRow.addView(pill, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(8) })
        }
        names.addView(nameRow)
        names.addView(tv(asset.symbol, 12f, COLOR_TEXT_DIM))
        left.addView(icon)
        left.addView(names)

        // Right: balance + usd + change
        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }
        val balanceStr = when (asset.symbol) {
            "BTC" -> String.format(Locale.US, "%,.4f", asset.balance)
            "ETH" -> String.format(Locale.US, "%,.3f", asset.balance)
            else -> String.format(Locale.US, "%,.2f", asset.balance)
        }
        val balView = tv(balanceStr, 15f, if (editMode) COLOR_PRIMARY else Color.WHITE, true).apply {
            gravity = Gravity.END
            if (editMode) setOnClickListener { openEdit("coinBalance", asset.symbol) }
        }
        right.addView(balView)
        right.addView(tv(formatCurrency(usdValue), 12f, COLOR_TEXT_DIM).apply { gravity = Gravity.END })
        val changeStr = (if (asset.change >= 0) "+" else "") + asset.change + "%"
        right.addView(tv(changeStr, 12f, if (asset.change >= 0) COLOR_GREEN else COLOR_RED, true).apply {
            gravity = Gravity.END
        })
        if (editMode) {
            val pricePill = tv("🏷", 9f, Color.WHITE, true).apply {
                setPadding(dp(8), dp(3), dp(8), dp(3))
                background = roundedBg(COLOR_PRIMARY, 12f)
            }
            pricePill.setOnClickListener { openEdit("coinPrice", asset.symbol) }
            right.addView(pricePill, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
                gravity = Gravity.END
            })
        }

        row.addView(left, lpWrap())
        row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        row.addView(right, lpWrap())

        wrapper.addView(row)
        wrapper.addView(View(this).apply {
            setBackgroundColor(COLOR_CARD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
        })
        return wrapper
    }

    // ============================================================
    // ===== FORMAT ===============================================
    // ============================================================
    private fun calcTotalUsd(): Double = assets.sumOf { it.balance * it.price }

    private fun convert(usd: Double): Double =
        usd * (currencies[activeCurrency]?.rate ?: 1.0)

    private fun formatCurrency(usd: Double): String {
        val sym = currencies[activeCurrency]?.symbol ?: "$"
        return sym + String.format(Locale.US, "%,.2f", convert(usd))
    }

    // ============================================================
    // ===== REFRESH ==============================================
    // ============================================================
    private fun refreshUI() {
        balanceText.text = formatCurrency(calcTotalUsd())
        currencyLabel.text = activeCurrency
        currencySymbolView.text = currencies[activeCurrency]?.symbol ?: "$"
        currencyCodeView.text = activeCurrency

        val sign = if (totalChangePercent >= 0) "+" else ""
        val arrow = if (totalChangePercent >= 0) "↑" else "↓"
        changeText.text = "$arrow $sign$totalChangePercent% All time"
        changeText.setTextColor(if (totalChangePercent >= 0) COLOR_GREEN else COLOR_RED)
        changeText.background = roundedBg(
            if (totalChangePercent >= 0) Color.parseColor("#200ECB81") else Color.parseColor("#20F6465D"),
            20f
        )

        renderAssets()
    }

    // ============================================================
    // ===== EDIT MODE ============================================
    // ============================================================
    private fun setupGhostTrigger() {
        var tapCount = 0
        var lastTapTime = 0L
        var downTime = 0L
        ghostTrigger.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - downTime
                    if (duration >= 2500) {
                        toggleEditMode()
                    } else {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime > 1500) tapCount = 0
                        lastTapTime = now
                        tapCount++
                        if (tapCount >= 5) {
                            toggleEditMode()
                            tapCount = 0
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleEditMode() {
        editMode = !editMode
        editBar.visibility = if (editMode) View.VISIBLE else View.GONE
        refreshUI()
        showToast(if (editMode) "🔓 وضع التحرير" else "🔒 تم الإغلاق")
    }

    // ============================================================
    // ===== EDIT MODAL ===========================================
    // ============================================================
    private fun buildEditModal(): FrameLayout {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = true
            setOnClickListener { hideModal() }
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            background = roundedBg(COLOR_CARD, 20f, Color.parseColor("#500052FF"))
            layoutParams = FrameLayout.LayoutParams(dp(320), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            isClickable = true
        }
        modalTitle = tv("تعديل", 16f, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        card.addView(modalTitle)

        modalInput = EditText(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            gravity = Gravity.CENTER
            setBackground(roundedBg(COLOR_BG, 14f, COLOR_BORDER))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(16); bottomMargin = dp(16) }
        }
        card.addView(modalInput)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val cancelBtn = tv("إلغاء", 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
            background = roundedBg(COLOR_BORDER, 30f)
            setOnClickListener { hideModal() }
        }
        val saveBtn = tv("حفظ", 14f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
            background = roundedBg(COLOR_PRIMARY, 30f)
            setOnClickListener { applyEditFromModal() }
        }
        btnRow.addView(cancelBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(5) })
        btnRow.addView(saveBtn, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(5) })
        card.addView(btnRow)

        overlay.addView(card)
        return overlay
    }

    private fun openEdit(type: String, coin: String) {
        if (!editMode) return
        editTargetType = type
        editTargetCoin = coin
        val (title, value) = when (type) {
            "totalBalance" -> "إجمالي المحفظة" to convert(calcTotalUsd())
            "coinBalance" -> "رصيد $coin" to (assets.find { it.symbol == coin }?.balance ?: 0.0)
            "coinPrice" -> "سعر $coin (USD)" to (assets.find { it.symbol == coin }?.price ?: 0.0)
            else -> "تعديل" to 0.0
        }
        modalTitle.text = title
        modalInput.setText(String.format(Locale.US, "%.6f", value).trimEnd('0').trimEnd('.'))
        modalOverlay.visibility = View.VISIBLE
        modalInput.requestFocus()
    }

    private fun hideModal() {
        modalOverlay.visibility = View.GONE
    }

    private fun applyEditFromModal() {
        val raw = modalInput.text.toString().replace(",", "")
        val newVal = raw.toDoubleOrNull() ?: return
        if (newVal < 0) return

        when (editTargetType) {
            "totalBalance" -> {
                val oldTotal = calcTotalUsd()
                if (oldTotal <= 0.0) return
                val newUsd = newVal / (currencies[activeCurrency]?.rate ?: 1.0)
                val ratio = newUsd / oldTotal
                val precision = mapOf("BTC" to 8, "ETH" to 6, "SOL" to 4, "USDC" to 2, "ADA" to 2, "DOGE" to 2)
                assets.forEach { a ->
                    val p = precision[a.symbol] ?: 2
                    val mult = Math.pow(10.0, p.toDouble())
                    a.balance = Math.round(a.balance * ratio * mult) / mult
                }
            }
            "coinBalance" -> {
                assets.find { it.symbol == editTargetCoin }?.balance = newVal
            }
            "coinPrice" -> {
                assets.find { it.symbol == editTargetCoin }?.price = newVal
            }
        }
        hideModal()
        refreshUI()
        showToast("✅ تم التعديل")
    }

    // ============================================================
    // ===== CURRENCY POPUP =======================================
    // ============================================================
    private fun showCurrencyPopup() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = roundedBg(COLOR_CARD, 16f, COLOR_PRIMARY)
            clipToOutline = true
            layoutParams = ViewGroup.LayoutParams(dp(240), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val searchInput = EditText(this).apply {
            hint = "ابحث عن عملة..."
            setHintTextColor(COLOR_TEXT_DIM)
            setTextColor(Color.WHITE)
            setBackgroundColor(COLOR_BORDER)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val listScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220))
        }
        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        listScroll.addView(listContainer)
        container.addView(searchInput)
        container.addView(listScroll)

        val popup = PopupWindow(
            container,
            dp(240),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = dp(20).toFloat()
        }

        fun buildList(filter: String = "") {
            listContainer.removeAllViews()
            currencies.entries
                .filter { (code, cur) ->
                    filter.isEmpty() ||
                        code.lowercase().contains(filter.lowercase()) ||
                        cur.name.contains(filter)
                }
                .forEach { (code, cur) ->
                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutDirection = View.LAYOUT_DIRECTION_RTL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(14), dp(10), dp(14), dp(10))
                        setOnClickListener {
                            activeCurrency = code
                            popup.dismiss()
                            refreshUI()
                            showToast("🔄 العملة: $code")
                        }
                    }
                    val leftView = tv("${cur.symbol} $code", 13f, Color.WHITE, true)
                    val rightView = tv(cur.name, 11f, COLOR_TEXT_DIM).apply { gravity = Gravity.END }
                    row.addView(leftView)
                    row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
                    row.addView(rightView)
                    listContainer.addView(row)
                }
        }

        buildList()
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { buildList(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        popup.showAsDropDown(currencyBadge, 0, dp(8))
    }

    // ============================================================
    // ===== TOAST ================================================
    // ============================================================
    private fun showToast(msg: String) {
        toastView.text = msg
        toastView.visibility = View.VISIBLE
        toastView.alpha = 0f
        toastView.animate().alpha(1f).setDuration(200).start()
        toastView.removeCallbacks(fadeOutRunnable)
        toastView.postDelayed(fadeOutRunnable, 2000)
    }
}

// ================================================================
// ===== CUSTOM CHART VIEW (SVG → Canvas) =========================
// ================================================================
class ChartView(context: Context) : View(context) {

    private val points = floatArrayOf(
        0f, 110f, 38f, 105f, 76f, 112f, 114f, 95f, 152f, 100f,
        190f, 88f, 228f, 92f, 266f, 78f, 304f, 82f, 342f, 70f, 380f, 65f
    )

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0052FF.toInt()
        strokeWidth = 2.5f * resources.displayMetrics.density
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val sx = width.toFloat() / 380f
        val sy = height.toFloat() / 140f

        val line = Path().apply {
            moveTo(points[0] * sx, points[1] * sy)
            var i = 2
            while (i < points.size) {
                lineTo(points[i] * sx, points[i + 1] * sy)
                i += 2
            }
        }

        val fill = Path(line).apply {
            lineTo(380f * sx, 140f * sy)
            lineTo(0f, 140f * sy)
            close()
        }

        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(0x590052FF, 0x050052FF),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fill, fillPaint)
        canvas.drawPath(line, linePaint)
    }
}