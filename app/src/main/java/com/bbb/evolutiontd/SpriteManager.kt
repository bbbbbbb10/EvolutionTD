package com.bbb.evolutiontd

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object SpriteManager {

    // --- настройка размеров ---
    private const val TOWER_WIDTH = 80
    private const val GOKU_TOWER_WIDTH = 175
    private const val GOKU_TOWER_CHARGE_WIDTH = 130


    const val ENEMY_NORMAL_WIDTH = 55
    const val ENEMY_FAST_WIDTH = 50
    const val ENEMY_DEMON_WIDTH = 60
    const val ENEMY_TANK_WIDTH = 70
    const val ENEMY_BOSS_WIDTH = 100
    const val ENEMY_GOKU_WIDTH = 170
    // размеры снарядов
    private const val PROJ_SCOUT_WIDTH = 25
    private const val PROJ_ARTILLERY_WIDTH = 40
    private const val PROJ_FROST_WIDTH = 35
    private const val PROJ_GOKU_WIDTH = 120

    // Задний фон
    lateinit var gameBackground: Bitmap // Фон
    // Башни
    lateinit var towerScout: Bitmap
    lateinit var towerArtillery: Bitmap
    lateinit var towerFrost: Bitmap
    //Враги
    lateinit var enemyNormal: Bitmap
    lateinit var enemyFast: Bitmap
    lateinit var enemyTank: Bitmap
    lateinit var enemyBoss: Bitmap
    lateinit var enemyDemon: Bitmap
    lateinit var gokuBlack: Bitmap
    // гоку башня
    lateinit var towerGokuIdle: Bitmap
    lateinit var towerGokuCharge: Bitmap
    // снаряды
    lateinit var projScout: Bitmap
    lateinit var projArtillery: Bitmap
    lateinit var projFrost: Bitmap
    lateinit var projGoku: Bitmap

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
        towerGokuIdle = loadAndResize(context, R.drawable.tower_goku_idle, GOKU_TOWER_WIDTH)
        towerGokuCharge = loadAndResize(context, R.drawable.tower_goku_charge, GOKU_TOWER_CHARGE_WIDTH)

        // Враги
        enemyNormal = loadAndResize(context, R.drawable.enemy_weak, ENEMY_NORMAL_WIDTH)
        enemyFast = loadAndResize(context, R.drawable.enemy_fast, ENEMY_FAST_WIDTH)
        enemyTank = loadAndResize(context, R.drawable.enemy_tank, ENEMY_TANK_WIDTH)
        enemyBoss = loadAndResize(context, R.drawable.enemy_boss, ENEMY_BOSS_WIDTH)
        enemyDemon = loadAndResize(context, R.drawable.enemy_demon, ENEMY_DEMON_WIDTH)
        gokuBlack = loadAndResize(context, R.drawable.enemy_goku, ENEMY_GOKU_WIDTH)

        // Снаряды
        projScout = loadAndResize(context, R.drawable.projectile_scout, PROJ_SCOUT_WIDTH)
        projArtillery = loadAndResize(context, R.drawable.projectile_artillery, PROJ_ARTILLERY_WIDTH)
        projFrost = loadAndResize(context, R.drawable.projectile_frost, PROJ_FROST_WIDTH)
        // Снаряд Гоку
        projGoku = loadAndResize(context, R.drawable.projectile_goku, PROJ_GOKU_WIDTH)
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