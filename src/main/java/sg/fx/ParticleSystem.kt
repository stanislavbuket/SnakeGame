package sg.fx

import java.awt.Color
import java.awt.Graphics
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object ParticleSystem {
    private val particles = CopyOnWriteArrayList<Particle>()

    //Об'єднує частинки в "партії" в цілях оптимізації
    fun update(dt: Float) {
        //Видалення мертвих частинок
        particles.removeIf { !it.isAlive }

        //Масове оновлення "живих" частинок
        particles.forEach { it.update(dt) }
    }

    fun draw(g: Graphics, cellSize: Int) {
        //Групує частинки за кольором для зменшення перемикань
        val colorGroups = particles.groupBy { it.color }

        colorGroups.forEach { (color, particleGroup) ->
            g.color = color
            particleGroup.forEach { p ->
                g.fillOval(p.x.toInt(), p.y.toInt(), p.size, p.size)
            }
        }
    }

    enum class EffectType {
        FOOD, GROWTH, COLLISION
    }

    fun spawnParticles(x: Int, y: Int, effect: EffectType, cellSize: Int) {
        val centerX = x * cellSize + cellSize / 2
        val centerY = y * cellSize + cellSize / 2

        val params = when (effect) {
            EffectType.FOOD -> ParticleParams(10, Color(241, 84, 84), 0.002f, 300f)
            EffectType.GROWTH -> ParticleParams(6, Color(153, 174, 199), 0.001f, 200f)
            EffectType.COLLISION -> ParticleParams(12, Color(255, 0, 0), 0.002f, 300f)
        }

        //Сворення партії частинок
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

    //Клас для зберігання параметрів частинок
    private data class ParticleParams(
        val count: Int,
        val color: Color,
        val speed: Float,
        val life: Float
    )
}