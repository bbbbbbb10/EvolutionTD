package com.bbb.evolutiontd

class GameLoop(private val view: GameView, private val gameManager: GameManager) : Thread() {
    private var running = false
    private val targetFPS = 60L

    fun setRunning(run: Boolean) {
        running = run
    }

    override fun run() {
        var startTime: Long
        var timeMillis: Long
        var waitTime: Long
        val targetTime = 1000 / targetFPS

        while (running) {
            startTime = System.nanoTime()

            // 1. Обновляем логику
            gameManager.update()

            // 2. Рисуем
            val canvas = view.holder.lockCanvas()
            if (canvas != null) {
                synchronized(view.holder) {
                    view.draw(canvas)
                }
                view.holder.unlockCanvasAndPost(canvas)
            }

            // 3. Ждем для стабилизации FPS
            timeMillis = (System.nanoTime() - startTime) / 1000000
            waitTime = targetTime - timeMillis

            try {
                if (waitTime > 0) sleep(waitTime)
            } catch (e: Exception) {}
        }
    }
}