package com.bbb.evolutiontd

import android.content.Context
import android.content.SharedPreferences
import com.bbb.evolutiontd.model.Tower
import com.bbb.evolutiontd.model.TowerType

object GameSaveManager {
    private const val PREFS_NAME = "EvolutionTD_SaveGame"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Проверка: есть ли сохранение?
    fun hasSave(): Boolean {
        // Если волна > 0, значит сохранение есть
        return prefs.getInt("wave", 0) > 0
    }

    // СОХРАНЕНИЕ
    fun saveGame(wave: Int, money: Long, lives: Int, towers: List<Tower>) {
        val editor = prefs.edit()
        editor.putInt("wave", wave)
        editor.putLong("money", money)
        editor.putInt("lives", lives)

        // Сериализуем башни в строку: "Type,Level,X,Y;Type,Level,X,Y"
        val towersString = StringBuilder()
        for (t in towers) {
            towersString.append("${t.type.name},${t.level},${t.x},${t.y};")
        }
        editor.putString("towers", towersString.toString())

        editor.apply() // Сохраняем асинхронно
    }

    // ЗАГРУЗКА (Возвращает объект с данными или null)
    fun loadGame(): SavedGameData? {
        if (!hasSave()) return null

        val wave = prefs.getInt("wave", 1)
        val money = prefs.getLong("money", 600)
        val lives = prefs.getInt("lives", 20)
        val towersStr = prefs.getString("towers", "") ?: ""

        val towersList = ArrayList<SavedTowerData>()

        if (towersStr.isNotEmpty()) {
            val splitTowers = towersStr.split(";")
            for (s in splitTowers) {
                if (s.isNotBlank()) {
                    val parts = s.split(",")
                    if (parts.size == 4) {
                        try {
                            val type = TowerType.valueOf(parts[0])
                            val lvl = parts[1].toInt()
                            val x = parts[2].toFloat()
                            val y = parts[3].toFloat()
                            towersList.add(SavedTowerData(type, lvl, x, y))
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
        }

        return SavedGameData(wave, money, lives, towersList)
    }

    // УДАЛЕНИЕ СОХРАНЕНИЯ (при Game Over)
    fun clearSave() {
        prefs.edit().clear().apply()
    }
}

// Простые классы для передачи данных
data class SavedGameData(
    val wave: Int,
    val money: Long,
    val lives: Int,
    val towers: List<SavedTowerData>
)

data class SavedTowerData(
    val type: TowerType,
    val level: Int,
    val x: Float,
    val y: Float
)