package com.bbb.evolutiontd.model

import com.bbb.evolutiontd.SoundManager
import com.bbb.evolutiontd.SpriteManager
import kotlin.math.sin

data class Point(var x: Float, var y: Float)

enum class TargetStrategy { FIRST, CLOSEST, STRONGEST, WEAKEST }

enum class TowerType(val displayName: String, val baseCost: Int, val baseRange: Float, val baseDmg: Float, val baseCooldown: Int, val maxLimit: Int) {
    SCOUT("Archer", 50, 250f, 10f, 20, 10),
    ARTILLERY("Artillery", 150, 350f, 40f, 120,10),
    FROST("Frost", 200, 200f, 2f, 10,10),
    GOKU("Goku", 5000, 550f, 3000f, 600,3)
}
// Враги
enum class EnemyType(val speedMod: Float, val hpMod: Float, val rewardMod: Float) {
    NORMAL(1.0f, 1.0f, 1.0f),
    FAST(1.7f, 0.8f, 1.5f),
    DEMON(1.1f, 1.6f, 2f),
    TANK(0.6f, 7.0f, 3.0f),
    BOSS(0.4f, 23.0f, 8.0f),
    GOKU(0.5f, 28.0f, 14.0f)
}

// Добавил BLUE_EXPLOSION для синего взрыва
enum class EffectType { EXPLOSION, BLUE_EXPLOSION }

class Effect(val x: Float, val y: Float, val type: EffectType) {
    var age = 0
    var maxAge = 20
    var active = true
    fun update() { age++; if (age >= maxAge) active = false }
}

class Enemy(
    val path: List<Point>,
    var hp: Float,
    val maxHp: Float,
    private val baseSpeed: Float,
    val reward: Int,
    val type: EnemyType
) {
    var x: Float = path[0].x
    var y: Float = path[0].y
    private var currentWaypointIndex = 0
    var active = true

    var freezeTimer = 0
    var isFrozen = false

    private var animTick = 0f

    fun update() {
        if (freezeTimer > 0) {
            freezeTimer--
            isFrozen = true
        } else {
            isFrozen = false
        }
        val currentSpeed = if (isFrozen) baseSpeed * 0.5f else baseSpeed

        animTick += 0.15f * (currentSpeed / 4f)

        if (currentWaypointIndex >= path.size - 1) {
            active = false
            return
        }

        val target = path[currentWaypointIndex + 1]
        val dx = target.x - x
        val dy = target.y - y
        val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

        if (dist <= currentSpeed) {
            x = target.x
            y = target.y
            currentWaypointIndex++
        } else {
            x += (dx / dist) * currentSpeed
            y += (dy / dist) * currentSpeed
        }
    }

    fun getSwayAngle(): Float {
        return (sin(animTick.toDouble()) * 10.0).toFloat()
    }

    fun getDistanceToEnd(): Float = (path.size - currentWaypointIndex) * 1000f
    fun takeDamage(amount: Float): Boolean {
        hp -= amount
        return hp <= 0
    }
}

class Tower(var x: Float, var y: Float, val type: TowerType) {
    var level = 1
    var range = type.baseRange
    var damage = type.baseDmg
    var cooldownMax = type.baseCooldown
    var cooldownCurrent = 0
    var strategy = TargetStrategy.FIRST
    var isSelected = false

    // --- ЛОГИКА ГОКУ (Зарядка и Левитация) ---
    var isCharging = false
    var chargeTimer = 0
    val chargeDuration = 170   // время накапливания заряда
    private var floatTick = 0f  // Для левитации

    fun update(enemies: List<Enemy>, projectiles: MutableList<Projectile>) {
        // Левитация работает всегда
        floatTick += 0.05f

        if (cooldownCurrent > 0) cooldownCurrent--

        // Если заряжаемся
        if (isCharging) {
            chargeTimer++
            // Если зарядились -> Стреляем
            if (chargeTimer >= chargeDuration) {
                val target = findTarget(enemies)
                if (target != null) {
                    projectiles.add(Projectile(x, y, target, damage, type))
                    cooldownCurrent = cooldownMax
                    isCharging = false
                    chargeTimer = 0
                } else {
                    // Цель ушла
                    isCharging = false
                    chargeTimer = 0
                }
            }
            return
        }

        // Обычное состояние
        if (cooldownCurrent <= 0) {
            val target = findTarget(enemies)
            if (target != null) {
                if (type == TowerType.GOKU) {
                    // Гоку сначала заряжается
                    isCharging = true
                    chargeTimer = 0
                    SoundManager.playGokuCharge() // Звук!
                } else {
                    // Остальные стреляют сразу
                    projectiles.add(Projectile(x, y, target, damage, type))
                    cooldownCurrent = cooldownMax
                }
            }
        }
    }

    // Метод для получения смещения (вверх-вниз)
    fun getLevitationOffset(): Float {
        return (sin(floatTick.toDouble()) * 10f).toFloat()
    }

    private fun findTarget(enemies: List<Enemy>): Enemy? {
        val inRange = enemies.filter { Math.hypot((it.x - x).toDouble(), (it.y - y).toDouble()) <= range }
        if (inRange.isEmpty()) return null
        return when (strategy) {
            TargetStrategy.FIRST -> inRange.maxByOrNull { -it.getDistanceToEnd() }
            TargetStrategy.CLOSEST -> inRange.minByOrNull { Math.hypot((it.x - x).toDouble(), (it.y - y).toDouble()) }
            TargetStrategy.STRONGEST -> inRange.maxByOrNull { it.hp }
            TargetStrategy.WEAKEST -> inRange.minByOrNull { it.hp }
        }
    }

    //цена апгрейда растет на 35 проц
    fun upgradeCost(): Int = (type.baseCost * Math.pow(1.35, level.toDouble())).toInt()
    fun sellCost(): Int = (upgradeCost() / 2)
    fun getNextDamage(): Float = damage * 1.25f
    fun getNextRange(): Float = range * 1.05f
    fun getFireRateSec(): String = String.format("%.1fs", cooldownMax / 60f)
    fun getNextFireRateSec(): String {
        var futureCooldown = cooldownMax

        if (futureCooldown > 5) {
            futureCooldown = (futureCooldown * 0.95).toInt()
        }
        return String.format("%.1fs", futureCooldown / 60f)
    }


    fun upgrade() {
        level++
        damage *= 1.25f
        range *= 1.05f

        if (cooldownMax > 10) {
            cooldownMax = (cooldownMax * 0.95).toInt()
        }
    }
}

class Projectile(var x: Float, var y: Float, val target: Enemy, val damage: Float, val type: TowerType) {
    // Настройка скорости: Гоку медленный, Артиллерия средняя, остальные быстрые
    val speed = when(type) {
        TowerType.GOKU -> 12f
        TowerType.ARTILLERY -> 15f
        else -> 25f
    }

    var active = true
    var angle: Float = 0f

    fun update() {
        if (!target.active) { active = false; return }

        val dx = target.x - x
        val dy = target.y - y
        val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

        angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()

        if (dist <= speed) {
            x = target.x
            y = target.y
            active = false
        } else {
            x += (dx / dist) * speed
            y += (dy / dist) * speed
        }
    }
}