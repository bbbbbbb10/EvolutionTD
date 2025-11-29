package com.bbb.evolutiontd

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object SpriteManager {

    // --- НАСТРОЙКИ РАЗМЕРА (МАСШТАБ) ---
    private const val TOWER_WIDTH = 80

    const val ENEMY_NORMAL_WIDTH = 55
    const val ENEMY_FAST_WIDTH = 50
    const val ENEMY_TANK_WIDTH = 70
    const val ENEMY_BOSS_WIDTH = 90

    private const val PROJ_SCOUT_WIDTH = 25
    private const val PROJ_ARTILLERY_WIDTH = 40
    private const val PROJ_FROST_WIDTH = 35

    // Картинки
    lateinit var gameBackground: Bitmap // Фон

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
        // Загружаем фон (без ресайза, берем оригинал)
        try {
            val options = BitmapFactory.Options().apply { inScaled = false }
            gameBackground = BitmapFactory.decodeResource(context.resources, R.drawable.background, options)
        } catch (e: Exception) {
            // Если забыл файл, создаем черный квадрат
            gameBackground = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

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
            val options = BitmapFactory.Options().apply { inScaled = false }
            val original = BitmapFactory.decodeResource(context.resources, resId, options)
            val aspectRatio = original.height.toFloat() / original.width.toFloat()
            val targetHeight = (targetWidth * aspectRatio).toInt()
            return Bitmap.createScaledBitmap(original, targetWidth, targetHeight, false)
        } catch (e: Exception) {
            return Bitmap.createBitmap(targetWidth, targetWidth, Bitmap.Config.ARGB_8888)
        }
    }
}