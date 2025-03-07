package sg.model

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

data class Point(var x: Int, var y: Int)

class Snake(initialX: Int, initialY: Int) {
    //Сегменти змійки: перший елемент – голова
    val body: MutableList<Point> = mutableListOf(Point(initialX, initialY))
    var direction: Direction = Direction.RIGHT

    fun move() {
        val newHead = computeNewHead()
        body.add(0, newHead)
        body.removeAt(body.size - 1)
    }

    /**
     * Метод росту – додає новий сегмент у хвості.
     * Визначає напрямок хвоста за різницею між останніми двома сегментами.
     */
    fun grow() {
        if (body.size >= 2) {
            val tail = body.last()
            val beforeTail = body[body.size - 2]
            val dx = tail.x - beforeTail.x
            val dy = tail.y - beforeTail.y
            body.add(Point(tail.x + dx, tail.y + dy))
        } else {
            body.add(body.last().copy())
        }
    }

    private fun computeNewHead(): Point {
        val head = body.first()
        return when (direction) {
            Direction.UP -> Point(head.x, head.y - 1)
            Direction.DOWN -> Point(head.x, head.y + 1)
            Direction.LEFT -> Point(head.x - 1, head.y)
            Direction.RIGHT -> Point(head.x + 1, head.y)
        }
    }

    fun checkSelfCollision(): Boolean {
        val head = body.first()
        return body.drop(1).any { it.x == head.x && it.y == head.y }
    }
}

class Food(private val boardWidth: Int, private val boardHeight: Int, private val snake: Snake) {
    //Щоб їжа не з'являлась у стінах, генерується позиція у внутрішній області
    var position: Point = generateNewPosition()

    private fun generateNewPosition(): Point {
        var newPoint: Point
        do {
            val x = (1 until boardWidth - 1).random()
            val y = (1 until boardHeight - 1).random()
            newPoint = Point(x, y)
        } while (snake.body.any { it.x == newPoint.x && it.y == newPoint.y })
        return newPoint
    }

    fun respawn() {
        position = generateNewPosition()
    }
}