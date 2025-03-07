package sg.view

import sg.model.Direction
import sg.model.Point
import sg.model.Snake
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.geom.AffineTransform
import kotlin.math.atan2

class SnakeRenderer(
    private val cellSize: Int,
    private val headImage: Image,
    private val bodyImage: Image
) {
    //Зберігає попередній стан для інтерполяції
    private var prevSegments = emptyList<Point>()

    //Метод для встановлення попереднього стану
    fun setPrevState(segments: List<Point>) {
        prevSegments = segments.map { it.copy() }
    }

    fun render(g: Graphics, snake: Snake, interpolation: Float) {
        val g2d = g as Graphics2D
        val segments = snake.body

        //Зберігає оригінальну трансформацію для відновлення після малювання
        val originalTransform = g2d.transform

        segments.forEachIndexed { index, point ->
            val drawX: Float
            val drawY: Float

            //Застосовує інтерполяцію для плавності руху
            if (index < prevSegments.size) {
                val prev = prevSegments[index]
                drawX = lerp(prev.x.toFloat(), point.x.toFloat(), interpolation)
                drawY = lerp(prev.y.toFloat(), point.y.toFloat(), interpolation)
            } else {
                drawX = point.x.toFloat()
                drawY = point.y.toFloat()
            }

            val pixelX = drawX * cellSize
            val pixelY = drawY * cellSize

            //Обчислює кут повороту для сегмента на основі сусідніх сегментів
            val rotationAngle = when (index) {
                0 -> getHeadRotationAngle(snake.direction) //Голова
                else -> getBodyRotationAngle(index, segments, interpolation, prevSegments) //Тіло
            }

            //Застосовує обертання та положення
            g2d.transform = AffineTransform().apply {
                translate((pixelX + cellSize / 2).toDouble(), (pixelY + cellSize / 2).toDouble())
                rotate(rotationAngle)
                translate(-cellSize / 2.0, -cellSize / 2.0)
            }

            //Малює відповідне зображення
            val image = if (index == 0) headImage else bodyImage
            g2d.drawImage(image, 0, 0, cellSize, cellSize, null)

            //Відновлює початкову трансформацію для наступного сегмента
            g2d.transform = originalTransform
        }
    }

    //Отримує кут повороту для голови змійки
    private fun getHeadRotationAngle(direction: Direction): Double {
        return when (direction) {
            Direction.RIGHT -> 0.0
            Direction.DOWN -> Math.PI / 2
            Direction.LEFT -> Math.PI
            Direction.UP -> 3 * Math.PI / 2
        }
    }

    //Обчислює кут повороту для сегментів тіла змійки з урахуванням сусідніх сегментів
    private fun getBodyRotationAngle(
        index: Int,
        segments: List<Point>,
        interpolation: Float,
        prevSegments: List<Point>
    ): Double {
        //Якщо це останній сегмент, використовує напрям від попереднього до нього
        if (index == segments.size - 1) {
            val current = segments[index]
            val prev = segments[index - 1]
            return atan2((current.y - prev.y).toDouble(), (current.x - prev.x).toDouble())
        }

        //Для внутрішніх сегментів обчислює середнє значення напрямків
        val prev = segments[index - 1]
        val curr = segments[index]
        val next = segments[index + 1]

        //Обчислює вектори напрямку
        val dx1 = curr.x - prev.x
        val dy1 = curr.y - prev.y
        val dx2 = next.x - curr.x
        val dy2 = next.y - curr.y

        //Вираховує середній кут для плавного повороту
        val angle1 = atan2(dy1.toDouble(), dx1.toDouble())
        val angle2 = atan2(dy2.toDouble(), dx2.toDouble())

        //Корегує кут, щоб уникнути різких стрибків між -PI і PI
        val angleDiff = normalizeAngle(angle2 - angle1)
        return angle1 + angleDiff * interpolation
    }

    //Нормалізує кут до діапазону [-PI, PI]
    private fun normalizeAngle(angle: Double): Double {
        var result = angle
        while (result > Math.PI) result -= 2 * Math.PI
        while (result < -Math.PI) result += 2 * Math.PI
        return result
    }

    //Лінійна інтерполяція для плавності руху
    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }
}