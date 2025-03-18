package fx

import java.awt.Color

/**
 * Містить дані частинки: координати, швидкість, час життя (в мс), колір та розмір.
 */
data class Particle(
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    var life: Float, //в мс
    val color: Color,
    val size: Int
) {
    /**
     * Оновлює позицію частинки та зменшує її час життя.
     */
    fun update(dt: Float) {
        x += dx * dt
        y += dy * dt
        life -= dt
    }

    /**Повертає true, якщо час життя частинки більше нуля.*/
    val isAlive: Boolean
        get() = life > 0
}