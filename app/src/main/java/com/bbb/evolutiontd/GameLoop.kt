package com.bbb.evolutiontd

import android.graphics.Canvas

class GameLoop(private val view: GameView, private val gameManager: GameManager) : Thread() {
    private var running = false

    // Настройки FPS (60 кадров в секунду)
    private val targetFPS = 60L

    // Сколько миллисекунд должен длиться один кадр (1000 мс / 60 кадров ≈ 16.6 мс)
    private val targetTime = 1000L / targetFPS

    fun setRunning(run: Boolean) {
        running = run
    }

    override fun run() {
        var startTime: Long
        var timeMillis: Long
        var waitTime: Long

        while (running) {
            startTime = System.nanoTime()

            try {
                // 1. Обновляем логику
                // (Делаем это ДО блокировки канваса, чтобы не задерживать отрисовку)
                gameManager.update()

                // 2. Рисуем (В блоке try, чтобы не вылетало при сворачивании)
                val canvas = view.holder.lockCanvas()
                if (canvas != null) {
                    synchronized(view.holder) {
                        view.draw(canvas)
                    }
                    view.holder.unlockCanvasAndPost(canvas)
                }
            } catch (e: Exception) {
                // Игнорируем ошибки отрисовки при выходе/сворачивании
                e.printStackTrace()
            }

            // 3. Считаем время и отдыхаем
            timeMillis = (System.nanoTime() - startTime) / 1000000
            waitTime = targetTime - timeMillis

            try {
                if (waitTime > 0) sleep(waitTime)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
}