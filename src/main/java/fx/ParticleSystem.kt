package fx

import java.awt.Color
import java.awt.Graphics
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Система частинок для візуальних ефектів; керує створенням, оновленням та рендерингом частинок.
 */
object ParticleSystem {
    private val particles = CopyOnWriteArrayList<Particle>()

    /**
     * Оновлює позицію всіх частинок, видаляє мертві частинки.
     */
    fun update(dt: Float) {
        //Видаляє мертві частинки
        particles.removeIf { !it.isAlive }
        //Масово оновлює живі частинки
        particles.forEach { it.update(dt) }
    }

    /**
     * Рендерить частинки, групуючи їх за кольором для зменшення перемикань.
     */
    fun draw(g: Graphics, cellSize: Int) {
        val colorGroups = particles.groupBy { it.color }

        colorGroups.forEach { (color, particleGroup) ->
            g.color = color
            particleGroup.forEach { p ->
                g.fillOval(p.x.toInt(), p.y.toInt(), p.size, p.size)
            }
        }
    }

    /**Перераховує типи ефектів частинок.*/
    enum class EffectType {
        FOOD, GROWTH, COLLISION
    }

    /**
     * Створює групу частинок на заданій позиції для визначеного ефекту.
     */
    fun spawnParticles(x: Int, y: Int, effect: EffectType, cellSize: Int) {
        val centerX = x * cellSize + cellSize / 2
        val centerY = y * cellSize + cellSize / 2

        val params = when (effect) {
            EffectType.FOOD -> ParticleParams(10, Color(241, 84, 84), 0.002f, 300f)
            EffectType.GROWTH -> ParticleParams(6, Color(153, 174, 199), 0.001f, 200f)
            EffectType.COLLISION -> ParticleParams(12, Color(255, 0, 0), 0.002f, 300f)
        }

        //Створює партію частинок
        repeat(params.count) {
            val angle = Random.nextFloat() * (2 * Math.PI).toFloat()
            val speed = Random.nextFloat() * params.speed + params.speed
            val dx = (cos(angle.toDouble()) * speed).toFloat() * cellSize
            val dy = (sin(angle.toDouble()) * speed).toFloat() * cellSize
            val life = Random.nextFloat() * params.life + params.life
            val size = Random.nextInt(8, 14)
            particles.add(Particle(centerX.toFloat(), centerY.toFloat(), dx, dy, life, params.color, size))
        }
    }

    /**Зберігає параметри для створення частинок: кількість, колір, швидкість та час життя.*/
    private data class ParticleParams(
        val count: Int,
        val color: Color,
        val speed: Float,
        val life: Float
    )
}