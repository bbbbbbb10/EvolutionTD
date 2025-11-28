package com.bbb.evolutiontd

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object SpriteManager {

    // --- НАСТРОЙКИ РАЗМЕРА (МАСШТАБ) ---
    // Меняй эти цифры, чтобы увеличить или уменьшить объекты.
    // Высота подстроится сама!

    private const val TOWER_WIDTH = 90      // Размер башен

    const val ENEMY_NORMAL_WIDTH = 65        // Обычные враги
    const val ENEMY_FAST_WIDTH = 60          // Мелкие быстрые
    const val ENEMY_TANK_WIDTH = 80         // Большие танки
    const val ENEMY_BOSS_WIDTH = 120        // Огромный босс

    private const val PROJ_SCOUT_WIDTH = 30  // Пули
    private const val PROJ_ARTILLERY_WIDTH = 45
    private const val PROJ_FROST_WIDTH = 35

    lateinit var towerScout: Bitmap
    lateinit var towerArtillery: Bitmap
    lateinit var towerFrost: Bitmap

    lateinit var enemyNormal: Bitmap
    lateinit var enemyFast: Bitmap
    lateinit var enemyTank: Bitmap
    lateinit var enemyBoss: Bitmap

    lateinit var projScout: Bitmap
    lateinit var projArtillery: Bitmap
    lateinit var projFrost: Bitmap

    fun init(context: Context) {
        // Башни
        towerScout = loadAndResize(context, R.drawable.tower_scout, TOWER_WIDTH)
        towerArtillery = loadAndResize(context, R.drawable.tower_artillery, TOWER_WIDTH)
        towerFrost = loadAndResize(context, R.drawable.tower_frost, TOWER_WIDTH)

        // Враги
        enemyNormal = loadAndResize(context, R.drawable.enemy_weak, ENEMY_NORMAL_WIDTH)
        enemyFast = loadAndResize(context, R.drawable.enemy_fast, ENEMY_FAST_WIDTH)
        enemyTank = loadAndResize(context, R.drawable.enemy_tank, ENEMY_TANK_WIDTH)
        enemyBoss = loadAndResize(context, R.drawable.enemy_boss, ENEMY_BOSS_WIDTH)

        // Снаряды
        projScout = loadAndResize(context, R.drawable.projectile_scout, PROJ_SCOUT_WIDTH)
        projArtillery = loadAndResize(context, R.drawable.projectile_artillery, PROJ_ARTILLERY_WIDTH)
        projFrost = loadAndResize(context, R.drawable.projectile_frost, PROJ_FROST_WIDTH)
    }

    private fun loadAndResize(context: Context, resId: Int, targetWidth: Int): Bitmap {
        try {
            // 1. Грузим оригинал без обработки Android'ом (чтобы не мылило)
            val options = BitmapFactory.Options().apply { inScaled = false }
            val original = BitmapFactory.decodeResource(context.resources, resId, options)

            // 2. Считаем пропорции (Aspect Ratio)
            val aspectRatio = original.height.toFloat() / original.width.toFloat()

            // 3. Вычисляем высоту на основе ширины
            val targetHeight = (targetWidth * aspectRatio).toInt()

            // 4. Создаем картинку нужного размера БЕЗ фильтрации (false) - для пиксель-арта
            return Bitmap.createScaledBitmap(original, targetWidth, targetHeight, false)
        } catch (e: Exception) {
            // Заглушка, если файла нет
            return Bitmap.createBitmap(targetWidth, targetWidth, Bitmap.Config.ARGB_8888)
        }
    }
}