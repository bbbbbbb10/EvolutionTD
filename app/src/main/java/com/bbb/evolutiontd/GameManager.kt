package com.bbb.evolutiontd

import com.bbb.evolutiontd.model.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class GameState {
    MENU, PLAYING, PAUSED, GAMEOVER
}

class GameManager(private val screenWidth: Int, private val screenHeight: Int) {

    val enemies = CopyOnWriteArrayList<Enemy>()
    val towers = CopyOnWriteArrayList<Tower>()
    val projectiles = CopyOnWriteArrayList<Projectile>()
    val effects = CopyOnWriteArrayList<Effect>()

    var money = 450
    var lives = 20
    var wave = 0
    var state = GameState.MENU

    var gameSpeed = 1

    var enemiesToSpawn = 0
    private var spawnTimer = 0
    private val spawnInterval = 30
    var waveCooldown = 0
    val waveCooldownMax = 600

    val path = listOf(
        Point(0f, screenHeight * 0.15f),
        Point(screenWidth * 0.148f, screenHeight * 0.15f),
        Point(screenWidth * 0.148f, screenHeight * 0.74f),
        Point(screenWidth * 0.345f, screenHeight * 0.74f),
        Point(screenWidth * 0.345f, screenHeight * 0.24f),
        Point(screenWidth * 0.543f, screenHeight * 0.24f),
        Point(screenWidth * 0.543f, screenHeight * 0.83f),
        Point(screenWidth * 0.74f, screenHeight * 0.83f),
        Point(screenWidth * 0.74f, screenHeight * 0.38f),
        Point(screenWidth.toFloat(), screenHeight * 0.38f)
    )

    fun startGame() {
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

    fun update() {
        // Обновляем игру ТОЛЬКО если статус PLAYING
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
                if (lives <= 0) {
                    // ИСПРАВЛЕНИЕ: Ставим статус GAMEOVER и всё.
                    // Логика update() сама остановится, так как state != PLAYING
                    state = GameState.GAMEOVER
                }
            }
        }
    }

    private fun applyDamage(p: Projectile) {
        if (p.type == TowerType.ARTILLERY) {
            effects.add(Effect(p.x, p.y, EffectType.EXPLOSION))
            val splashRadius = 150f
            for (enemy in enemies) {
                val dist = Math.hypot((enemy.x - p.x).toDouble(), (enemy.y - p.y).toDouble())
                if (dist <= splashRadius) {
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
            if (spawnTimer >= spawnInterval) {
                spawnEnemy()
                spawnTimer = 0
            }
        }
    }

    fun startNextWave() {
        wave++
        enemiesToSpawn = 5 + (wave * 3)
        waveCooldown = 0
    }

    fun canSkipWave(): Boolean = enemiesToSpawn == 0 && (enemies.size <= 5 || waveCooldown > 0)

    fun skipWave() { if (canSkipWave()) startNextWave() }

    private fun spawnEnemy() {
        enemiesToSpawn--
        val isBossWave = (wave % 10 == 0)
        val isMiniBossWave = (wave % 5 == 0)
        val isFastWave = (wave % 3 == 0)

        val enemyType: EnemyType = when {
            isBossWave && enemiesToSpawn == 0 -> EnemyType.BOSS
            isMiniBossWave && enemiesToSpawn < 2 -> EnemyType.TANK
            isFastWave && !isBossWave -> EnemyType.FAST
            else -> EnemyType.NORMAL
        }

        val baseWaveHp = 20f * Math.pow(1.2, wave.toDouble()).toFloat()
        val finalHp = baseWaveHp * enemyType.hpMod
        val finalSpeed = 3.2f * enemyType.speedMod
        val finalReward = 10 * Math.pow(1.1, wave.toDouble()) * enemyType.rewardMod

        enemies.add(Enemy(path, finalHp, finalHp, finalSpeed, finalReward.toInt(), enemyType))
    }

    fun isValidPlacement(x: Float, y: Float): Boolean {
        val minDistanceX = 70f
        val minDistanceY = 100f
        val pathWidth = 60f

        for (t in towers) {
            val dx = abs(t.x - x)
            val dy = abs(t.y - y)
            if (dx < minDistanceX && dy < minDistanceY) return false
        }

        for (i in 0 until path.size - 1) {
            val p1 = path[i]
            val p2 = path[i+1]
            if (distToSegment(x, y, p1.x, p1.y, p2.x, p2.y) < pathWidth) return false
        }
        return true
    }

    private fun distToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val l2 = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)
        if (l2 == 0f) return Math.hypot((px-x1).toDouble(), (py-y1).toDouble()).toFloat()
        var t = ((px-x1)*(x2-x1) + (py-y1)*(y2-y1)) / l2
        t = max(0f, min(1f, t))
        val projX = x1 + t * (x2 - x1)
        val projY = y1 + t * (y2 - y1)
        return Math.hypot((px-projX).toDouble(), (py-projY).toDouble()).toFloat()
    }

    fun buyTower(x: Float, y: Float, type: TowerType): Boolean {
        if (money >= type.baseCost && isValidPlacement(x, y)) {
            money -= type.baseCost
            towers.add(Tower(x, y, type))
            return true
        }
        return false
    }
}