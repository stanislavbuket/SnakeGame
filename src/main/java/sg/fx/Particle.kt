package sg.fx

import java.awt.Color

data class Particle(
    var x: Float,
    var y: Float,
    var dx: Float,
    var dy: Float,
    var life: Float, // в мс
    val color: Color,
    val size: Int
) {
    fun update(dt: Float) {
        x += dx * dt
        y += dy * dt
        life -= dt
    }

    val isAlive: Boolean
        get() = life > 0
}