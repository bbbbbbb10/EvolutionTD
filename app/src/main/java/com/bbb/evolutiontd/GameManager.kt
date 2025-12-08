package com.bbb.evolutiontd

import com.bbb.evolutiontd.model.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class GameState {
    MENU, PLAYING, PAUSED, GAMEOVER
}

class GameManager(private var screenWidth: Int, private var screenHeight: Int) {

    // Список игровых обьектов
    val enemies = CopyOnWriteArrayList<Enemy>()
    val towers = CopyOnWriteArrayList<Tower>()
    val projectiles = CopyOnWriteArrayList<Projectile>()
    val effects = CopyOnWriteArrayList<Effect>()

    // состояние игры
    var money: Long = 450
    var lives = 20
    var wave = 0
    var state = GameState.MENU
    var gameSpeed = 1

    // настройка спавна
    var enemiesToSpawn = 0
    private var spawnTimer = 0

    // текущая задержка - динамическая
    private var currentSpawnInterval = 30

    // интервал спавна врагов на волнах
    private val spawnIntervalStandard = 30  // Обычная (0.5 сек)
    private val spawnIntervalBosses = 110   // Боссы (1.8 сек)

    var waveCooldown = 0
    val waveCooldownMax = 200 // Задержка между волнами

    // Очередь для Боссов
    private val currentWaveBosses = LinkedList<EnemyType>()

    // Координаты пути
    var path = ArrayList<Point>()

    init {
        recalculatePath()
    }

    fun setScreenSize(w: Int, h: Int) {
        this.screenWidth = w
        this.screenHeight = h
        recalculatePath()
    }
    //путь по которому идут враги (подстраивается под разные разрешения)
    private fun recalculatePath() {
        path.clear()
        path.add(Point(0f, screenHeight * 0.15f))
        path.add(Point(screenWidth * 0.148f, screenHeight * 0.15f))
        path.add(Point(screenWidth * 0.148f, screenHeight * 0.745f))
        path.add(Point(screenWidth * 0.345f, screenHeight * 0.745f))
        path.add(Point(screenWidth * 0.345f, screenHeight * 0.24f))
        path.add(Point(screenWidth * 0.543f, screenHeight * 0.24f))
        path.add(Point(screenWidth * 0.543f, screenHeight * 0.83f))
        path.add(Point(screenWidth * 0.74f, screenHeight * 0.83f))
        path.add(Point(screenWidth * 0.74f, screenHeight * 0.385f))
        path.add(Point(screenWidth.toFloat(), screenHeight * 0.385f))
    }
    // боссы на волнах в порядке их выхода на path
    private fun getBossesForWave(wave: Int): List<EnemyType> {
        return when (wave) {
            5 -> listOf(EnemyType.TANK, EnemyType.BOSS)
            10 -> listOf(EnemyType.TANK, EnemyType.TANK, EnemyType.BOSS, EnemyType.BOSS)
            15 -> listOf(EnemyType.TANK, EnemyType.TANK, EnemyType.TANK, EnemyType.TANK, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS)
            20 -> listOf(EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS)
            25 -> listOf(EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.GOKU)
            30 -> listOf(EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.GOKU, EnemyType.GOKU)
            40 -> listOf(EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.GOKU, EnemyType.GOKU, EnemyType.GOKU)
            50 -> listOf(EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.BOSS, EnemyType.GOKU, EnemyType.GOKU, EnemyType.GOKU, EnemyType.GOKU, EnemyType.GOKU)
            else -> emptyList()
        }
    }
    // враги на волны и их шансы
    private fun getRandomEnemyForWave(wave: Int): EnemyType {
        val pool = when{
            wave <= 2 -> mapOf(EnemyType.NORMAL to 100)
            wave <= 9 -> mapOf(EnemyType.NORMAL to 30, EnemyType.FAST to 40, EnemyType.DEMON to 30)
            wave <= 19 -> mapOf(EnemyType.DEMON to 40, EnemyType.NORMAL to 40, EnemyType.FAST to 20)
            wave <= 39 -> mapOf(EnemyType.DEMON to 30, EnemyType.NORMAL to 10, EnemyType.FAST to 20, EnemyType.TANK to 40)
            wave <= 59 -> mapOf(EnemyType.DEMON to 20, EnemyType.FAST to 20, EnemyType.TANK to 60)
            else -> mapOf(EnemyType.TANK to 30, EnemyType.GOKU to 20, EnemyType.BOSS to 50)
        }
        return getWeightedRandom(pool)
    }

    // запуск новой игры
    fun startGame() {
        // при старте игры отчищаем старое сохранение
        GameSaveManager.clearSave()

        enemies.clear()
        towers.clear()
        projectiles.clear()
        effects.clear()

        money = 150
        lives = 20
        wave = 0
        waveCooldown = 0
        gameSpeed = 1

        startNextWave()
        state = GameState.PLAYING
    }

    // продолжить игру из сохранения
    fun continueGame(data: SavedGameData) {
        enemies.clear(); towers.clear(); projectiles.clear(); effects.clear()

        money = data.money
        lives = data.lives
        wave = data.wave

        for (tData in data.towers) {
            val newTower = Tower(tData.x, tData.y, tData.type)
            for (i in 1 until tData.level) {
                newTower.upgrade()
            }
            towers.add(newTower)
        }

        waveCooldown = 0
        state = GameState.PLAYING

        // Откатываем волну на 1 назад чтобы запустить текущую с начала
        wave--
        startNextWave()
    }

    fun update() {
        if (state != GameState.PLAYING) return
        repeat(gameSpeed) { updateGameLogic() }
    }

    private fun updateGameLogic() {
        handleWaveLogic()
        towers.forEach { it.update(enemies, projectiles) }
        effects.forEach { it.update() }
        effects.removeAll { !it.active }
        projectiles.forEach { p ->
            p.update()
            if (!p.active && Math.hypot((p.x - p.target.x).toDouble(), (p.y - p.target.y).toDouble()) < 40) {
                applyDamage(p)
            }
        }
        projectiles.removeAll { !it.active }
        enemies.forEach { enemy ->
            enemy.update()
            if (!enemy.active && enemy.hp > 0) {
                lives--
                enemies.remove(enemy)
                if (lives <= 0) state = GameState.GAMEOVER
            }
        }
    }

    private fun applyDamage(p: Projectile) {
        if (p.type == TowerType.GOKU) {
            effects.add(Effect(p.x, p.y, EffectType.BLUE_EXPLOSION))
            val explosionRadius = 450f
            for (enemy in enemies) {
                if (Math.hypot((enemy.x - p.x).toDouble(), (enemy.y - p.y).toDouble()) <= explosionRadius) {
                    if (enemy.takeDamage(p.damage)) killEnemy(enemy)
                }
            }
        } else if (p.type == TowerType.ARTILLERY) {
            effects.add(Effect(p.x, p.y, EffectType.EXPLOSION))
            val splashRadius = 150f
            for (enemy in enemies) {
                if (Math.hypot((enemy.x - p.x).toDouble(), (enemy.y - p.y).toDouble()) <= splashRadius) {
                    if (enemy.takeDamage(p.damage)) killEnemy(enemy)
                }
            }
        } else {
            if (p.type == TowerType.FROST) p.target.freezeTimer = 120
            if (p.target.takeDamage(p.damage)) killEnemy(p.target)
        }
    }

    private fun killEnemy(e: Enemy) {
        if (enemies.contains(e)) {
            money += e.reward
            enemies.remove(e)
        }
    }

    private fun handleWaveLogic() {
        if (enemiesToSpawn <= 0 && enemies.isEmpty()) {
            waveCooldown++
            if (waveCooldown >= waveCooldownMax) startNextWave()
        } else if (enemiesToSpawn > 0) {
            spawnTimer++
            // Используем динамический интервал
            if (spawnTimer >= currentSpawnInterval) {
                spawnEnemy()
                spawnTimer = 0
            }
        }
    }

    fun startNextWave() {
        wave++

        // --- сохранение ----
        FirebaseHelper.saveScore(wave)
        GameSaveManager.saveGame(wave, money, lives, towers)
        // ------------------

        currentWaveBosses.clear()
        currentWaveBosses.addAll(getBossesForWave(wave))
        val randomEnemyCount = 8 + (wave * 2)
        enemiesToSpawn = randomEnemyCount + currentWaveBosses.size
        waveCooldown = 0
    }

    fun canSkipWave(): Boolean = enemiesToSpawn == 0 && (enemies.size <= 5 || waveCooldown > 0)
    fun skipWave() { if (canSkipWave()) startNextWave() }

    private fun spawnEnemy() {
        enemiesToSpawn--
        val typeToSpawn: EnemyType
        if (currentWaveBosses.isNotEmpty() && enemiesToSpawn < currentWaveBosses.size) {
            typeToSpawn = currentWaveBosses.poll() ?: EnemyType.BOSS
        } else {
            typeToSpawn = getRandomEnemyForWave(wave)
        }
        createEnemy(typeToSpawn)

        // задержка перед следующим врагом
        currentSpawnInterval = when(typeToSpawn) {
            EnemyType.BOSS, EnemyType.GOKU -> spawnIntervalBosses // Большая задержка
            else -> spawnIntervalStandard // Обычная задержка
        }
    }

    private fun createEnemy(type: EnemyType) {
        // расчет хп - скорость - награда за врага . На текущей волне
        val baseWaveHp = 30f * Math.pow(1.25, wave.toDouble()).toFloat()
        val finalHp = baseWaveHp * type.hpMod
        val finalSpeed = 3.0f * type.speedMod
        val finalReward = 15f * Math.pow(1.1, wave.toDouble()).toFloat() * type.rewardMod

        enemies.add(Enemy(path, finalHp, finalHp, finalSpeed, finalReward.toInt(), type))
    }

    private fun getWeightedRandom(weights: Map<EnemyType, Int>): EnemyType {
        val totalWeight = weights.values.sum()
        if (totalWeight == 0) return EnemyType.NORMAL
        var randomValue = (Math.random() * totalWeight).toInt()
        for ((enemy, weight) in weights) {
            randomValue -= weight
            if (randomValue < 0) return enemy
        }
        return EnemyType.NORMAL
    }

    fun isValidPlacement(x: Float, y: Float): Boolean {
        val minDistanceX = 70f; val minDistanceY = 100f; val pathWidth = 60f
        for (t in towers) {
            val dx = abs(t.x - x); val dy = abs(t.y - y)
            if (dx < minDistanceX && dy < minDistanceY) return false
        }
        for (i in 0 until path.size - 1) {
            val p1 = path[i]; val p2 = path[i+1]
            if (distToSegment(x, y, p1.x, p1.y, p2.x, p2.y) < pathWidth) return false
        }
        return true
    }

    private fun distToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val l2 = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)
        if (l2 == 0f) return Math.hypot((px-x1).toDouble(), (py-y1).toDouble()).toFloat()
        var t = ((px-x1)*(x2-x1) + (py-y1)*(y2-y1)) / l2
        t = max(0f, min(1f, t))
        val projX = x1 + t * (x2 - x1); val projY = y1 + t * (y2 - y1)
        return Math.hypot((px-projX).toDouble(), (py-projY).toDouble()).toFloat()
    }

    fun getTowerCount(type: TowerType): Int {
        var count = 0
        for (t in towers) {
            if (t.type == type) count++
        }
        return count
    }

    fun buyTower(x: Float, y: Float, type: TowerType): Boolean {
        // Проверяем:
        // 1. Хватает ли денег
        // 2. Не превышен ли лимит
        // 3. Можно ли тут строить

        if (money >= type.baseCost && getTowerCount(type) < type.maxLimit && isValidPlacement(x, y))
        {
            money -= type.baseCost
            towers.add(Tower(x, y, type))
            return true
        }
        return false
    }
}