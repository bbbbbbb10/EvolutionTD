package com.bbb.evolutiontd

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.bbb.evolutiontd.model.EffectType
import com.bbb.evolutiontd.model.EnemyType
import com.bbb.evolutiontd.model.Tower
import com.bbb.evolutiontd.model.TowerType

class GameView(context: Context, private val gameManager: GameManager) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameLoop: GameLoop? = null
    var onTowerSelected: ((Tower?) -> Unit)? = null

    // Кисть для СПРАЙТОВ (без сглаживания)
    private val paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }

    // Кисть для UI (сглаживание включено)
    private val uiPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    // Текст уровня (Системный шрифт)
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val textStrokePaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    // Текст ХП Врагов
    private val hpTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val hpStrokePaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    // ФИЛЬТРЫ
    private val frozenFilter = LightingColorFilter(0xFF8888FF.toInt(), 0x00000033)
    // Красный фильтр для выделения
    private val selectedFilter = LightingColorFilter(0xFFFF8888.toInt(), 0x440000)

    private val backgroundRect = Rect()

    // Drag state
    var isDragging = false
    var dragTowerType: TowerType? = null
    var dragX = 0f
    var dragY = 0f
    private var isDragValid = false

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    fun startDrag(type: TowerType) {
        isDragging = true
        dragTowerType = type
    }

    fun updateDrag(x: Float, y: Float) {
        dragX = x
        dragY = y
        val locationValid = gameManager.isValidPlacement(x, y)
        val moneyValid = gameManager.money >= (dragTowerType?.baseCost ?: 9999)
        isDragValid = locationValid && moneyValid
    }

    fun finishDrag(): Boolean {
        isDragging = false
        if (isDragValid && dragTowerType != null) {
            val success = gameManager.buyTower(dragX, dragY, dragTowerType!!)
            dragTowerType = null
            return success
        }
        dragTowerType = null
        return false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameLoop = GameLoop(this, gameManager)
        gameLoop?.setRunning(true)
        gameLoop?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        backgroundRect.set(0, 0, width, height)
        // подстраивается под телефон размеры для пути врага это
        gameManager.setScreenSize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        gameLoop?.setRunning(false)
        while (retry) {
            try { gameLoop?.join(); retry = false } catch (e: InterruptedException) { e.printStackTrace() }
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (canvas == null) return

        // 1. ФОН
        canvas.drawBitmap(SpriteManager.gameBackground, null, backgroundRect, null)

        // 2. БАШНИ
        for (tower in gameManager.towers) {
            // Выбор спрайта
            val bitmap = when(tower.type) {
                TowerType.SCOUT -> SpriteManager.towerScout
                TowerType.ARTILLERY -> SpriteManager.towerArtillery
                TowerType.FROST -> SpriteManager.towerFrost
                // Гоку меняет спрайт при зарядке
                TowerType.GOKU -> if (tower.isCharging) SpriteManager.towerGokuCharge else SpriteManager.towerGokuIdle
            }

            canvas.save()

            // --- АНИМАЦИЯ БАШНИ ---
            var drawX = tower.x
            var drawY = tower.y

            if (tower.type == TowerType.GOKU) {
                if (tower.isCharging) {
                    // Дрожание при атаке
                    val shake = 5f
                    drawX += (Math.random().toFloat() - 0.5f) * shake
                    drawY += (Math.random().toFloat() - 0.5f) * shake
                } else {
                    // Левитация в покое
                    drawY += tower.getLevitationOffset()
                }
            }

            canvas.translate(drawX, drawY)

            // Подсветка
            if (tower.isSelected) paint.colorFilter = selectedFilter else paint.colorFilter = null

            canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, paint)
            paint.colorFilter = null
            canvas.restore()

            // Текст уровня
            val levelText = "Lvl ${tower.level}"
            val textY = tower.y + bitmap.height/2 - 10f
            canvas.drawText(levelText, tower.x, textY, textStrokePaint)
            canvas.drawText(levelText, tower.x, textY, textPaint)

            if (tower.isSelected) {
                drawRadius(canvas, tower.x, tower.y, tower.range, isValid = true, isPreview = false)
            }
        }

        // 3. ВРАГИ
        for (enemy in gameManager.enemies) {
            val bitmap = when (enemy.type) {
                EnemyType.BOSS -> SpriteManager.enemyBoss
                EnemyType.TANK -> SpriteManager.enemyTank
                EnemyType.FAST -> SpriteManager.enemyFast
                EnemyType.NORMAL -> SpriteManager.enemyNormal
                EnemyType.DEMON -> SpriteManager.enemyDemon
                EnemyType.GOKU -> SpriteManager.gokuBlack
            }

            canvas.save()
            canvas.translate(enemy.x, enemy.y)

            // Качание врага
            canvas.rotate(enemy.getSwayAngle())

            if (enemy.isFrozen) paint.colorFilter = frozenFilter else paint.colorFilter = null

            canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, paint)

            paint.colorFilter = null
            canvas.restore()

            // HP Bar
            val hpYOffset = (bitmap.height / 2f) + 10f
            val hpWidth = bitmap.width.toFloat()
            paint.color = Color.RED; canvas.drawRect(enemy.x - hpWidth/2, enemy.y - hpYOffset - 10, enemy.x + hpWidth/2, enemy.y - hpYOffset, paint)
            paint.color = Color.GREEN; val hpPercent = enemy.hp / enemy.maxHp; canvas.drawRect(enemy.x - hpWidth/2, enemy.y - hpYOffset - 10, (enemy.x - hpWidth/2) + (hpWidth * hpPercent), enemy.y - hpYOffset, paint)

            // Текст ХП
            val hpText = "${enemy.hp.toInt()}/${enemy.maxHp.toInt()}"
            val textY2 = enemy.y - hpYOffset - 20f
            canvas.drawText(hpText, enemy.x, textY2, hpStrokePaint)
            canvas.drawText(hpText, enemy.x, textY2, hpTextPaint)
        }

        // 4. СНАРЯДЫ
        for (p in gameManager.projectiles) {
            val projBitmap = when (p.type) {
                TowerType.SCOUT -> SpriteManager.projScout
                TowerType.ARTILLERY -> SpriteManager.projArtillery
                TowerType.FROST -> SpriteManager.projFrost
                TowerType.GOKU -> SpriteManager.projGoku // Шар Гоку
            }
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.angle)
            canvas.drawBitmap(projBitmap, -projBitmap.width / 2f, -projBitmap.height / 2f, paint)
            canvas.restore()
        }

        // 5. ЭФФЕКТЫ
        for (effect in gameManager.effects) {
            // Обычный взрыв (оранжевый)
            if (effect.type == EffectType.EXPLOSION) {
                uiPaint.style = Paint.Style.FILL
                uiPaint.color = Color.argb(150, 255, 100, 0)
                val radius = 20f + (effect.age * 5f)
                canvas.drawCircle(effect.x, effect.y, radius, uiPaint)

                uiPaint.style = Paint.Style.STROKE; uiPaint.strokeWidth=2f; uiPaint.color = Color.WHITE
                canvas.drawCircle(effect.x, effect.y, radius * 0.7f, uiPaint)
            }
            // Синий взрыв (Гоку)
            else if (effect.type == EffectType.BLUE_EXPLOSION) {
                uiPaint.style = Paint.Style.FILL
                uiPaint.color = Color.argb(180, 0, 200, 255) // Голубой
                // Очень быстрый рост радиуса (* 20f)
                val radius = 80f + (effect.age * 18f)
                canvas.drawCircle(effect.x, effect.y, radius, uiPaint)

                uiPaint.style = Paint.Style.STROKE; uiPaint.strokeWidth=4f; uiPaint.color = Color.WHITE
                canvas.drawCircle(effect.x, effect.y, radius, uiPaint)
            }
        }

        // 6. Drag Preview
        if (isDragging && dragTowerType != null) {
            val type = dragTowerType!!
            val bitmap = when(type) {
                TowerType.SCOUT -> SpriteManager.towerScout
                TowerType.ARTILLERY -> SpriteManager.towerArtillery
                TowerType.FROST -> SpriteManager.towerFrost
                TowerType.GOKU -> SpriteManager.towerGokuIdle
            }
            drawRadius(canvas, dragX, dragY, type.baseRange, isDragValid, isPreview = true)
            paint.alpha = 150
            canvas.save()
            canvas.translate(dragX, dragY)
            canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, paint)
            canvas.restore()
            paint.alpha = 255
        }
    }

    private fun drawRadius(canvas: Canvas, x: Float, y: Float, range: Float, isValid: Boolean, isPreview: Boolean) {
        uiPaint.style = Paint.Style.FILL
        if (isValid) uiPaint.color = Color.argb(40, 50, 255, 50) else uiPaint.color = Color.argb(40, 255, 50, 50)
        canvas.drawCircle(x, y, range, uiPaint)
        uiPaint.style = Paint.Style.STROKE
        uiPaint.strokeWidth = 3f
        if (isValid) uiPaint.color = if(isPreview) Color.GREEN else Color.WHITE else uiPaint.color = Color.RED
        canvas.drawCircle(x, y, range, uiPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameManager.state != GameState.PLAYING) return false
        if (isDragging) return true

        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y
            var clickedTower: Tower? = null
            val clickRadius = 60f
            for (tower in gameManager.towers) {
                if (x >= tower.x - clickRadius && x <= tower.x + clickRadius &&
                    y >= tower.y - clickRadius && y <= tower.y + clickRadius) {
                    clickedTower = tower
                    break
                }
            }
            if (clickedTower != null) {
                gameManager.towers.forEach { it.isSelected = false }
                clickedTower.isSelected = true
                onTowerSelected?.invoke(clickedTower)
            } else {
                gameManager.towers.forEach { it.isSelected = false }
                onTowerSelected?.invoke(null)
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}