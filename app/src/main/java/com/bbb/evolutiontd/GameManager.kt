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

class GameManager(private val screenWidth: Int, private val screenHeight: Int) {

    // --- СПИСКИ ИГРОВЫХ ОБЪЕКТОВ ---
    val enemies = CopyOnWriteArrayList<Enemy>()
    val towers = CopyOnWriteArrayList<Tower>()
    val projectiles = CopyOnWriteArrayList<Projectile>()
    val effects = CopyOnWriteArrayList<Effect>()

    // --- СОСТОЯНИЕ ИГРЫ ---
    var money = 450
    var lives = 20
    var wave = 0
    var state = GameState.MENU
    var gameSpeed = 1

    // --- НАСТРОЙКИ СПАВНА ---
    var enemiesToSpawn = 0
    private var spawnTimer = 0
    private val spawnInterval = 30 // Задержка между врагами (30 кадров = 0.5 сек)
    var waveCooldown = 0
    val waveCooldownMax = 200 // Задержка между волнами (200 кадров = 3сек примерно я хз)

    // Очередь для Боссов (они выходят в конце волны)
    private val currentWaveBosses = LinkedList<EnemyType>()

    // Координаты пути (Если враги идут мимо дороги на картинке — меняй цифры здесь!)
    val path = listOf(
        Point(0f, screenHeight * 0.15f),
        Point(screenWidth * 0.15f, screenHeight * 0.15f),
        Point(screenWidth * 0.15f, screenHeight * 0.75f),
        Point(screenWidth * 0.35f, screenHeight * 0.75f),
        Point(screenWidth * 0.35f, screenHeight * 0.25f),
        Point(screenWidth * 0.55f, screenHeight * 0.25f),
        Point(screenWidth * 0.55f, screenHeight * 0.85f),
        Point(screenWidth * 0.75f, screenHeight * 0.85f),
        Point(screenWidth * 0.75f, screenHeight * 0.40f),
        Point(screenWidth.toFloat(), screenHeight * 0.40f)
    )

    // ========================================================================
    //  БЛОК НАСТРОЙКИ БАЛАНСА (РЕДАКТИРУЙ ЗДЕСЬ!)
    // ========================================================================

    /**
     * 1. СЦЕНАРИИ БОССОВ
     * Здесь ты решаешь, на какой волне кто выйдет в самом конце.
     * Можно перечислять сколько угодно врагов.
     */
    private fun getBossesForWave(wave: Int): List<EnemyType> {
        return when (wave) {
            5 -> listOf(EnemyType.TANK) // Волна 5: Один Танк
            10 -> listOf(EnemyType.BOSS) // Волна 10: Один Босс
            15 -> listOf(EnemyType.TANK, EnemyType.TANK) // Волна 15: Два Танка
            20 -> listOf(EnemyType.BOSS, EnemyType.GOKU) // Волна 20: Босс и Мега-Босс
            30 -> listOf(EnemyType.GOKU, EnemyType.GOKU, EnemyType.GOKU)
            // Если волны нет в списке — боссов не будет
            else -> emptyList()
        }
    }

    /**
     * 2. ВЕСА ОБЫЧНЫХ ВРАГОВ (РАНДОМ)
     * Здесь настраивается, кто спавнится в основной части волны.
     * Цифра — это "шанс". Чем она больше по сравнению с другими, тем чаще враг.
     * Например: Normal=90, Fast=10 -> В 90% случаев будет Обычный.
     */
    private fun getRandomEnemyForWave(wave: Int): EnemyType {
        val pool = when{
            // -- ЛЕГКО (1-2 волна) --
            wave <= 2 -> mapOf(
                EnemyType.NORMAL to 100,
            )
            // -- РАЗГОН (3-8 волна) --
            wave <= 8 -> mapOf(
                EnemyType.NORMAL to 40,
                EnemyType.FAST to 30,
                EnemyType.DEMON to 30
            )
            // -- СРЕДНЕ (9-19 волна) --
            wave <= 19 -> mapOf(
                EnemyType.DEMON to 40,
                EnemyType.NORMAL to 30,
                EnemyType.FAST to 10,
                EnemyType.TANK to 20    // Появляются танки в толпе!
            )
            // -- ХАРДКОР (20+ волна) --
            else -> mapOf(
                EnemyType.NORMAL to 20,
                EnemyType.FAST to 10,
                EnemyType.TANK to 40,   // Танков больше всего
                EnemyType.DEMON to 30
            )
        }
        return getWeightedRandom(pool)
    }

    // ========================================================================
    //  КОНЕЦ НАСТРОЕК (ДАЛЬШЕ ЛОГИКА ДВИЖКА)
    // ========================================================================

    fun startGame() {
        enemies.clear()
        towers.clear()
        projectiles.clear()
        effects.clear()

        money = 5000
        lives = 20
        wave = 0
        waveCooldown = 0
        gameSpeed = 1

        startNextWave()
        state = GameState.PLAYING
    }

    fun update() {
        if (state != GameState.PLAYING) return
        repeat(gameSpeed) { updateGameLogic() }
    }

    private fun updateGameLogic() {
        handleWaveLogic()

        // Обновление сущностей
        towers.forEach { it.update(enemies, projectiles) }

        effects.forEach { it.update() }
        effects.removeAll { !it.active }

        projectiles.forEach { p ->
            p.update()
            // Проверка попадания (дистанция < 40)
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
        // --- ГОКУ: ЯДЕРНЫЙ ВЗРЫВ ---
        if (p.type == TowerType.GOKU) {
            // Синий взрыв
            effects.add(Effect(p.x, p.y, EffectType.BLUE_EXPLOSION))

            // НАСТРОЙКИ ВЗРЫВА
            val explosionRadius = 500f // Огромный радиус

            for (enemy in enemies) {
                val dist = Math.hypot((enemy.x - p.x).toDouble(), (enemy.y - p.y).toDouble())
                if (dist <= explosionRadius) {
                    if (enemy.takeDamage(p.damage)) killEnemy(enemy)
                }
            }
        }
        // --- АРТИЛЛЕРИЯ ---
        else if (p.type == TowerType.ARTILLERY) {
            effects.add(Effect(p.x, p.y, EffectType.EXPLOSION))
            val splashRadius = 150f
            for (enemy in enemies) {
                val dist = Math.hypot((enemy.x - p.x).toDouble(), (enemy.y - p.y).toDouble())
                if (dist <= splashRadius) {
                    if (enemy.takeDamage(p.damage)) killEnemy(enemy)
                }
            }
        }
        // --- ОБЫЧНЫЕ ---
        else {
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

    // --- ЗАПУСК НОВОЙ ВОЛНЫ ---
    fun startNextWave() {
        wave++

        // 1. Получаем список боссов для этой волны и кладем в очередь
        currentWaveBosses.clear()
        currentWaveBosses.addAll(getBossesForWave(wave))

        // 2. Рассчитываем количество случайных врагов (обычного "мяса")
        // Формула: 5 + (3 за каждую волну). Можно менять!
        val randomEnemyCount = 5 + (wave * 3)

        // 3. Общее число врагов = случайные + боссы
        enemiesToSpawn = randomEnemyCount + currentWaveBosses.size

        waveCooldown = 0
    }

    fun canSkipWave(): Boolean = enemiesToSpawn == 0 && (enemies.size <= 5 || waveCooldown > 0)
    fun skipWave() { if (canSkipWave()) startNextWave() }

    // --- СОЗДАНИЕ ВРАГА ---
    private fun spawnEnemy() {
        enemiesToSpawn--

        val typeToSpawn: EnemyType

        // Логика очереди: Боссы выходят последними
        // Если оставшихся врагов меньше или равно, чем боссов в очереди -> достаем босса
        if (currentWaveBosses.isNotEmpty() && enemiesToSpawn < currentWaveBosses.size) {
            // .poll() достает первого из очереди и удаляет его оттуда
            typeToSpawn = currentWaveBosses.poll() ?: EnemyType.BOSS
        } else {
            // Иначе берем случайного из настроек волны
            typeToSpawn = getRandomEnemyForWave(wave)
        }

        createEnemy(typeToSpawn)
    }

    // Применение статов (ХП, Скорость) к врагу
    private fun createEnemy(type: EnemyType) {
        // Базовое ХП растет на 30% (1.3) каждую волну.
        // Если слишком сложно, поменяй 1.3 на 1.15 или 1.2
        val baseWaveHp = 20f * Math.pow(1.3, wave.toDouble()).toFloat()

        val finalHp = baseWaveHp * type.hpMod
        val finalSpeed = 3.2f * type.speedMod
        val finalReward = 10f * Math.pow(1.1, wave.toDouble()).toFloat() * type.rewardMod

        enemies.add(Enemy(path, finalHp, finalHp, finalSpeed, finalReward.toInt(), type))
    }

    // --- МАТЕМАТИКА ВЕРОЯТНОСТЕЙ ---
    private fun getWeightedRandom(weights: Map<EnemyType, Int>): EnemyType {
        val totalWeight = weights.values.sum()
        if (totalWeight == 0) return EnemyType.NORMAL // Защита от ошибок

        var randomValue = (Math.random() * totalWeight).toInt()

        for ((enemy, weight) in weights) {
            randomValue -= weight
            if (randomValue < 0) {
                return enemy
            }
        }
        return EnemyType.NORMAL
    }

    // --- ПРОВЕРКА МЕСТА ДЛЯ ПОСТРОЙКИ ---
    fun isValidPlacement(x: Float, y: Float): Boolean {
        // Минимальное расстояние между центрами башен (Ширина башни ~110)
        val minDistanceX = 70f  // Можно ставить плотно сбоку
        val minDistanceY = 100f // Но нельзя плотно сверху/снизу

        val pathWidth = 60f // Запас от дороги

        // 1. Проверяем другие башни (Прямоугольная проверка)
        for (t in towers) {
            val dx = abs(t.x - x)
            val dy = abs(t.y - y)
            if (dx < minDistanceX && dy < minDistanceY) return false
        }

        // 2. Проверяем дорогу
        for (i in 0 until path.size - 1) {
            val p1 = path[i]
            val p2 = path[i+1]
            if (distToSegment(x, y, p1.x, p1.y, p2.x, p2.y) < pathWidth) return false
        }
        return true
    }

    // Математика расстояния до отрезка
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