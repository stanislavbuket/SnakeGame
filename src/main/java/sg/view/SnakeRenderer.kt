package sg.view

import sg.model.Point
import sg.model.Snake
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.geom.AffineTransform
import kotlin.math.atan2

//М'яку анімацію змійки реалізовано з використанням Catmull-Rom spline інтерполяції.
class SnakeRenderer(
    private val cellSize: Int,
    private val headImage: Image,
    private val bodyImage: Image,
    private val tailImage: Image
) {
    //Збереження попереднього стану для плавної інтерполяції.
    private var prevSegments = emptyList<Point>()

    //Константа для регулювання плавності кривої
    private val samplesPerSegment = 5

    fun setPrevState(segments: List<Point>) {
        prevSegments = segments.map { it.copy() }
    }

    fun render(g: Graphics, snake: Snake, interpolation: Float) {
        val g2d = g as Graphics2D
        val segments = snake.body

        //Обчислює гладку криву через сегменти змійки
        val curvePoints = computeSmoothCurve(segments, interpolation)

        if (curvePoints.isEmpty()) return

        //Рендер голови, тіла та хвоста вздовж кривої
        drawSegment(g2d, headImage, curvePoints.first(), getAngle(curvePoints, 0))

        for (i in 1 until curvePoints.size - 1) {
            drawSegment(g2d, bodyImage, curvePoints[i], getAngle(curvePoints, i))
        }

        if (curvePoints.size > 1) {
            drawSegment(g2d, tailImage, curvePoints.last(), getAngle(curvePoints, curvePoints.size - 1))
        }
    }

    //Малювання окремого сегмента з урахуванням позиції та кута повороту
    private fun drawSegment(g2d: Graphics2D, image: Image, pt: PointF, angle: Double) {
        val originalTransform = g2d.transform
        val transform = AffineTransform().apply {
            translate(pt.x.toDouble(), pt.y.toDouble())
            rotate(angle)
            translate(-cellSize / 2.0, -cellSize / 2.0)
        }

        g2d.transform = transform
        g2d.drawImage(image, 0, 0, cellSize, cellSize, null)
        g2d.transform = originalTransform
    }

    //Обчислення кута на основі напрямку між сусідніми точками
    private fun getAngle(curve: List<PointF>, index: Int): Double {
        if (curve.size < 2) return 0.0

        val pCurrent = curve[index]
        val (dx, dy) = when (index) {
            0 -> {
                val next = curve[1]
                Pair(next.x - pCurrent.x, next.y - pCurrent.y)
            }
            curve.lastIndex -> {
                val prev = curve[index - 1]
                Pair(pCurrent.x - prev.x, pCurrent.y - prev.y)
            }
            else -> {
                //Вирівнює напрям між попередньою та наступною точками
                val prev = curve[index - 1]
                val next = curve[index + 1]
                Pair((next.x - prev.x) / 2f, (next.y - prev.y) / 2f)
            }
        }
        return atan2(dy.toDouble(), dx.toDouble())
    }

    //Обчислення згладженої кривої за допомогою Catmull-Rom spline інтерполяції
    private fun computeSmoothCurve(segments: List<Point>, interpolation: Float): List<PointF> {
        if (segments.isEmpty()) return emptyList()

        //Перетворення координат з логічних у пікселі
        val points = segments.mapIndexed { index, pt ->
            val (interpX, interpY) = if (index < prevSegments.size) {
                val prev = prevSegments[index]
                Pair(
                    lerp(prev.x.toFloat(), pt.x.toFloat(), interpolation),
                    lerp(prev.y.toFloat(), pt.y.toFloat(), interpolation)
                )
            } else {
                Pair(pt.x.toFloat(), pt.y.toFloat())
            }
            PointF(interpX * cellSize + cellSize / 2.0f, interpY * cellSize + cellSize / 2.0f)
        }

        if (points.size < 2) return points

        //Побудова кривої
        val curve = mutableListOf<PointF>()
        for (i in 0 until points.size - 1) {
            val p0 = if (i > 0) points[i - 1] else points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i + 2 < points.size) points[i + 2] else p2

            curve.add(p1)

            //Додає проміжні точки, лише якщо це не зовсім маленька змійка
            if (points.size > 2 && samplesPerSegment > 1) {
                for (j in 1 until samplesPerSegment) {
                    val t = j / samplesPerSegment.toFloat()
                    curve.add(catmullRomInterpolate(p0, p1, p2, p3, t))
                }
            }
        }

        curve.add(points.last()) // Додаємо останню точку
        return curve
    }

    //Реалізація Catmull-Rom інтерполяції
    private fun catmullRomInterpolate(p0: PointF, p1: PointF, p2: PointF, p3: PointF, t: Float): PointF {
        val t2 = t * t
        val t3 = t2 * t
        val x = 0.5f * ((2 * p1.x) +
                (-p0.x + p2.x) * t +
                (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 +
                (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3)
        val y = 0.5f * ((2 * p1.y) +
                (-p0.y + p2.y) * t +
                (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
                (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3)
        return PointF(x, y)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }
}

//Допоміжний клас для роботи з позицією що плаває
data class PointF(val x: Float, val y: Float)