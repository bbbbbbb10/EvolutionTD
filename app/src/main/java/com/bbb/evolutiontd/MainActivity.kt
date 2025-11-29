package com.bbb.evolutiontd

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bbb.evolutiontd.model.TargetStrategy
import com.bbb.evolutiontd.model.Tower
import com.bbb.evolutiontd.model.TowerType

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var gameManager: GameManager

    // МЕНЮ
    private lateinit var menuMain: RelativeLayout
    private lateinit var btnLeaderboard: Button
    private lateinit var overlayLeaderboard: FrameLayout
    private lateinit var recyclerLeaderboard: RecyclerView
    private lateinit var btnCloseLeaderboard: Button

    // HUD
    private lateinit var gameHud: RelativeLayout
    private lateinit var menuPause: LinearLayout
    private lateinit var btnResume: Button

    // БОКОВОЕ МЕНЮ
    private lateinit var sideMenuContainer: LinearLayout
    private lateinit var menuContent: LinearLayout
    private lateinit var btnToggleMenu: Button
    private lateinit var shopLayout: LinearLayout
    private lateinit var upgradeLayout: LinearLayout

    // ТЕКСТЫ
    private lateinit var txtWave: TextView
    private lateinit var txtMoney: TextView
    private lateinit var txtLives: TextView

    // КНОПКИ УПРАВЛЕНИЯ
    private lateinit var btnPause: Button
    private lateinit var btnSpeed: Button
    private lateinit var btnSkipWave: Button

    // МАГАЗИН
    private lateinit var btnBuyScout: Button
    private lateinit var btnBuyArtillery: Button
    private lateinit var btnBuyFrost: Button

    // УЛУЧШЕНИЯ
    private lateinit var txtTowerStats: TextView
    private lateinit var btnUpgrade: Button
    private lateinit var btnSell: Button
    private lateinit var btnTargetStrategy: Button
    private lateinit var btnCloseShop: Button

    private var selectedTower: Tower? = null
    private var isMenuVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Скрываем системные панели СРАЗУ при создании
        hideSystemUI()

        setContentView(R.layout.activity_main)

        SpriteManager.init(this)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        // Используем реальные размеры экрана (включая области под камерой и кнопками)
        val realWidth = displayMetrics.widthPixels
        val realHeight = displayMetrics.heightPixels

        gameManager = GameManager(realWidth, realHeight)
        gameView = GameView(this, gameManager)

        findViewById<FrameLayout>(R.id.gameContainer).addView(gameView)

        bindViews()
        setupMenuListeners()
        setupGameListeners()
        setupShopTouchListeners()
        setupMenuToggle()

        Toast.makeText(this, "Welcome, ${FirebaseHelper.currentUserName}!", Toast.LENGTH_LONG).show()

        startUiUpdater()
    }

    // 2. Гарантируем скрытие панелей, если фокус вернулся (например, закрыли шторку уведомлений)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    // --- ФУНКЦИЯ ДЛЯ ПОЛНОЭКРАННОГО РЕЖИМА ---
    private fun hideSystemUI() {
        // Заставляем контент рисоваться под системными барами
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        // Скрываем и навигацию (снизу), и статус бар (сверху)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        // Бары появятся, если свайпнуть от края, и потом снова исчезнут
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // ВАЖНО ДЛЯ POCO / XIAOMI / SAMSUNG:
        // Разрешаем рисовать в области выреза камеры (Notch)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val attrib = window.attributes
            attrib.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = attrib
        }
    }
    // -----------------------------------------

    override fun onPause() {
        super.onPause()
        if (::gameManager.isInitialized && gameManager.state == GameState.PLAYING) {
            gameManager.state = GameState.PAUSED
            runOnUiThread {
                menuPause.visibility = View.VISIBLE
                btnResume.visibility = View.VISIBLE
                val title = menuPause.findViewWithTag<TextView>("title")
                title?.text = "PAUSED"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::gameManager.isInitialized && (gameManager.state == GameState.PLAYING || gameManager.state == GameState.PAUSED)) {
            FirebaseHelper.saveScore(gameManager.wave)
        }
    }

    private fun bindViews() {
        menuMain = findViewById(R.id.menuMain)
        gameHud = findViewById(R.id.gameHud)
        menuPause = findViewById(R.id.menuPause)
        btnResume = findViewById(R.id.btnResume)

        btnLeaderboard = findViewById(R.id.btnLeaderboard)
        overlayLeaderboard = findViewById(R.id.overlayLeaderboard)
        recyclerLeaderboard = findViewById(R.id.recyclerLeaderboard)
        btnCloseLeaderboard = findViewById(R.id.btnCloseLeaderboard)
        recyclerLeaderboard.layoutManager = LinearLayoutManager(this)

        sideMenuContainer = findViewById(R.id.sideMenuContainer)
        menuContent = findViewById(R.id.menuContent)
        btnToggleMenu = findViewById(R.id.btnToggleMenu)

        shopLayout = findViewById(R.id.shopLayout)
        upgradeLayout = findViewById(R.id.upgradeLayout)

        txtWave = findViewById(R.id.txtWave)
        txtMoney = findViewById(R.id.txtMoney)
        txtLives = findViewById(R.id.txtLives)
        btnPause = findViewById(R.id.btnPause)
        btnSpeed = findViewById(R.id.btnSpeed)
        btnSkipWave = findViewById(R.id.btnSkipWave)

        btnBuyScout = findViewById(R.id.btnBuyScout)
        btnBuyArtillery = findViewById(R.id.btnBuyArtillery)
        btnBuyFrost = findViewById(R.id.btnBuyFrost)

        txtTowerStats = findViewById(R.id.txtTowerStats)
        btnUpgrade = findViewById(R.id.btnUpgrade)
        btnSell = findViewById(R.id.btnSell)
        btnTargetStrategy = findViewById(R.id.btnTargetStrategy)
        btnCloseShop = findViewById(R.id.btnCloseShop)
    }

    private fun setupMenuToggle() {
        btnToggleMenu.setOnClickListener {
            if (isMenuVisible) {
                val width = menuContent.width.toFloat()
                menuContent.animate().translationX(width).setDuration(300).start()
                btnToggleMenu.text = "<"; isMenuVisible = false
            } else {
                menuContent.animate().translationX(0f).setDuration(300).start()
                btnToggleMenu.text = ">"; isMenuVisible = true
            }
        }
    }

    private fun setupMenuListeners() {
        findViewById<Button>(R.id.btnEndlessMode).setOnClickListener {
            gameManager.startGame()
            menuMain.visibility = View.GONE
            gameHud.visibility = View.VISIBLE
            menuPause.visibility = View.GONE
            btnResume.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btnCreator).setOnClickListener {
            Toast.makeText(this, "Made by: buwu", Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.btnExitGame).setOnClickListener {
            finish()
        }

        btnLeaderboard.setOnClickListener {
            overlayLeaderboard.visibility = View.VISIBLE
            FirebaseHelper.getLeaderboard { list ->
                recyclerLeaderboard.adapter = LeaderboardAdapter(list)
            }
        }

        btnCloseLeaderboard.setOnClickListener {
            overlayLeaderboard.visibility = View.GONE
        }

        btnResume.setOnClickListener {
            gameManager.state = GameState.PLAYING
            menuPause.visibility = View.GONE
            // При возврате в игру снова скрываем UI на всякий случай
            hideSystemUI()
        }

        findViewById<Button>(R.id.btnRestart).setOnClickListener {
            if (gameManager.state != GameState.GAMEOVER && gameManager.state != GameState.MENU) {
                FirebaseHelper.saveScore(gameManager.wave)
            }
            gameManager.startGame()
            menuPause.visibility = View.GONE
            btnResume.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btnMenu).setOnClickListener {
            if (gameManager.state != GameState.GAMEOVER && gameManager.state != GameState.MENU) {
                FirebaseHelper.saveScore(gameManager.wave)
            }
            gameManager.state = GameState.MENU
            menuPause.visibility = View.GONE
            gameHud.visibility = View.GONE
            menuMain.visibility = View.VISIBLE
            btnResume.visibility = View.VISIBLE
        }
    }

    private fun setupGameListeners() {
        btnPause.setOnClickListener {
            gameManager.state = GameState.PAUSED
            menuPause.visibility = View.VISIBLE
            btnResume.visibility = View.VISIBLE
            val title = menuPause.findViewWithTag<TextView>("title")
            title?.text = "PAUSED"
        }

        btnSpeed.setOnClickListener {
            if (gameManager.gameSpeed == 1) { gameManager.gameSpeed = 2; btnSpeed.text = "2x" }
            else { gameManager.gameSpeed = 1; btnSpeed.text = "1x" }
        }
        btnSkipWave.setOnClickListener { gameManager.skipWave() }

        gameView.onTowerSelected = { tower ->
            runOnUiThread {
                selectedTower = tower
                if (tower != null) {
                    if (!isMenuVisible) btnToggleMenu.performClick()
                    shopLayout.visibility = View.GONE
                    upgradeLayout.visibility = View.VISIBLE
                    updateTowerPanel(tower)
                } else {
                    shopLayout.visibility = View.VISIBLE
                    upgradeLayout.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupShopTouchListeners() {
        val touchListener = View.OnTouchListener { v, event ->
            if (gameManager.state != GameState.PLAYING) return@OnTouchListener false

            val type = when(v.id) {
                R.id.btnBuyScout -> TowerType.SCOUT
                R.id.btnBuyArtillery -> TowerType.ARTILLERY
                R.id.btnBuyFrost -> TowerType.FROST
                else -> TowerType.SCOUT
            }

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    gameView.startDrag(type)
                    updateDragPosition(event)
                    return@OnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    updateDragPosition(event)
                    return@OnTouchListener true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    gameView.finishDrag()
                    return@OnTouchListener true
                }
            }
            false
        }

        btnBuyScout.setOnTouchListener(touchListener)
        btnBuyArtillery.setOnTouchListener(touchListener)
        btnBuyFrost.setOnTouchListener(touchListener)

        btnCloseShop.setOnClickListener {
            selectedTower?.isSelected = false
            selectedTower = null
            shopLayout.visibility = View.VISIBLE
            upgradeLayout.visibility = View.GONE
        }
        btnUpgrade.setOnClickListener {
            selectedTower?.let { t ->
                val cost = t.upgradeCost()
                if (gameManager.money >= cost) {
                    gameManager.money -= cost; t.upgrade(); updateTowerPanel(t)
                }
            }
        }
        btnSell.setOnClickListener {
            selectedTower?.let { t ->
                gameManager.money += t.sellCost(); gameManager.towers.remove(t); selectedTower = null; shopLayout.visibility = View.VISIBLE; upgradeLayout.visibility = View.GONE
            }
        }
        btnTargetStrategy.setOnClickListener {
            selectedTower?.let { t ->
                val nextOrdinal = (t.strategy.ordinal + 1) % TargetStrategy.values().size
                t.strategy = TargetStrategy.values()[nextOrdinal]
                updateTowerPanel(t)
            }
        }
    }

    private fun updateDragPosition(event: MotionEvent) {
        val location = IntArray(2)
        gameView.getLocationOnScreen(location)
        val x = event.rawX - location[0]
        val y = event.rawY - location[1]
        gameView.updateDrag(x, y)
    }

    private fun updateTowerPanel(t: Tower) {
        val statsText = """
            ${t.type.displayName} (Lvl ${t.level})
            
            DMG: ${t.damage.toInt()} -> ${t.getNextDamage().toInt()}
            RANGE: ${t.range.toInt()} -> ${t.getNextRange().toInt()}
            COOLDOWN: ${t.getFireRateSec()} -> ${t.getNextFireRateSec()}
        """.trimIndent()

        txtTowerStats.text = statsText
        btnUpgrade.text = "UPGRADE ($${t.upgradeCost()})"
        btnSell.text = "SELL ($${t.sellCost()})"
        btnTargetStrategy.text = "Target: ${t.strategy.name}"
    }

    private fun startUiUpdater() {
        Thread {
            while (!isDestroyed) {
                try {
                    Thread.sleep(100)
                    runOnUiThread {
                        if (::gameManager.isInitialized) {
                            if (gameManager.state == GameState.PLAYING || gameManager.state == GameState.PAUSED) {
                                txtWave.text = "Wave: ${gameManager.wave}"
                                txtMoney.text = "$${gameManager.money}"
                                txtLives.text = "♥ ${gameManager.lives}"
                                btnSkipWave.isEnabled = gameManager.canSkipWave()
                            }
                            if (gameManager.state == GameState.GAMEOVER) {
                                if (menuPause.visibility != View.VISIBLE) {
                                    FirebaseHelper.saveScore(gameManager.wave)
                                    btnResume.visibility = View.GONE
                                    menuPause.visibility = View.VISIBLE
                                    val title = menuPause.findViewWithTag<TextView>("title")
                                    title?.text = "GAME OVER (Wave ${gameManager.wave})"
                                    txtWave.text = "GAME OVER"
                                }
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        }.start()
    }
}