package com.bbb.evolutiontd.model

import com.bbb.evolutiontd.SpriteManager

data class Point(var x: Float, var y: Float)

enum class TargetStrategy { FIRST, CLOSEST, STRONGEST, WEAKEST }

enum class TowerType(val displayName: String, val baseCost: Int, val baseRange: Float, val baseDmg: Float, val baseCooldown: Int) {
    SCOUT("Scout", 50, 250f, 10f, 20),
    ARTILLERY("Artillery", 150, 350f, 40f, 120),
    FROST("Frost", 200, 200f, 2f, 10)
}

enum class EnemyType(val speedMod: Float, val hpMod: Float, val rewardMod: Float) {
    NORMAL(1.0f, 1.0f, 1.0f),
    FAST(1.7f, 0.6f, 1.5f),
    TANK(0.6f, 4.0f, 3.0f),
    BOSS(0.4f, 15.0f, 10.0f)
}

class Effect(val x: Float, val y: Float, val type: EffectType) {
    var age = 0
    var maxAge = 20
    var active = true
    fun update() { age++; if (age >= maxAge) active = false }
}
enum class EffectType { EXPLOSION }

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

    fun update() {
        if (freezeTimer > 0) {
            freezeTimer--
            isFrozen = true
        } else {
            isFrozen = false
        }
        val currentSpeed = if (isFrozen) baseSpeed * 0.5f else baseSpeed

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

    fun update(enemies: List<Enemy>, projectiles: MutableList<Projectile>) {
        if (cooldownCurrent > 0) cooldownCurrent--
        val target = findTarget(enemies)

        if (cooldownCurrent <= 0 && target != null) {
            projectiles.add(Projectile(x, y, target, damage, type))
            cooldownCurrent = cooldownMax
        }
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

    fun upgradeCost(): Int = (type.baseCost * Math.pow(1.5, level.toDouble())).toInt()
    fun sellCost(): Int = (upgradeCost() / 2)

    // --- МЕТОДЫ ПРЕДСКАЗАНИЯ СТАТОВ (ДЛЯ UI) ---
    fun getNextDamage(): Float = damage * 1.2f
    fun getNextRange(): Float = range * 1.05f

    // Возвращает скорострельность в секундах (60 тиков = 1 сек)
    fun getFireRateSec(): String = String.format("%.1fs", cooldownMax / 60f)
    fun getNextFireRateSec(): String {
        val nextCd = if(type == TowerType.FROST) (cooldownMax * 0.9).toInt() else cooldownMax
        return String.format("%.1fs", nextCd / 60f)
    }

    fun upgrade() {
        level++
        damage *= 1.2f
        range *= 1.05f
        if(type == TowerType.FROST) cooldownMax = (cooldownMax * 0.9).toInt()
    }
}

class Projectile(var x: Float, var y: Float, val target: Enemy, val damage: Float, val type: TowerType) {
    val speed = 25f; var active = true; var angle: Float = 0f

    fun update() {
        if (!target.active) { active = false; return }
        val dx = target.x - x; val dy = target.y - y
        val dist = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()

        angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()

        if (dist <= speed) { x = target.x; y = target.y; active = false } else { x += (dx / dist) * speed; y += (dy / dist) * speed }
    }
}