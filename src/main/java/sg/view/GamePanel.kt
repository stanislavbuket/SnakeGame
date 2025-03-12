package sg.view

import sg.model.Direction
import sg.model.Food
import sg.model.Point
import sg.model.Snake
import sg.fx.ParticleSystem
import sg.fx.ParticleSystem.EffectType
import sg.util.TextureManager
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.math.max

class GamePanel : JPanel(), ActionListener, KeyListener {
    //Константи для налаштувань гри
    private companion object {
        const val RENDER_DELAY = 16 // ~60 FPS
        const val BOARD_WIDTH = 20
        const val BOARD_HEIGHT = 20
        const val CELL_SIZE = 30
        const val MIN_SNAKE_LENGTH = 3
        const val MIN_GAME_DELAY = 50f //мінімальний час між кроками логіки
        const val DEFAULT_GAME_DELAY = 190f //початковий час між кроками логіки
    }

    private inner class GameState {
        var isRunning = false
        var isGameOver = false
        var isPaused = false
        var score = 0
        var foodEatenCount = 0
        var currentDelay = DEFAULT_GAME_DELAY

        fun reset() {
            isRunning = true
            isGameOver = false
            isPaused = false
            score = 0
            foodEatenCount = 0
            currentDelay = DEFAULT_GAME_DELAY
        }
    }

    private val timer = Timer(RENDER_DELAY, this)
    private val state = GameState()
    private val snake = Snake(BOARD_WIDTH / 2, BOARD_HEIGHT / 2)
    private val food = Food(BOARD_WIDTH, BOARD_HEIGHT, snake)
    private val exitButton = JButton("Вийти з гри")

    //Рендерер змійки
    private val snakeRenderer = SnakeRenderer(
        CELL_SIZE,
        TextureManager.snakeHead,
        TextureManager.snakeBody,
        TextureManager.snakeTail
    )

    //Анімація: контроль часу та інтерполяції
    private var accumulator = 0f
    private var lastUpdateTime = System.currentTimeMillis()
    private var interpolation = 0f

    //Прапорець, який обмежує зміну напрямку за один логічний крок, щоб уникнути "випадкових смертей"
    private var directionChanged = false

    //Передзавантажені зображення в цілях оптимізації
    private val tileImage: Image = TextureManager.tile
    private val wallImage: Image = TextureManager.wall
    private val foodImage: Image = TextureManager.food

    //Двовимірний масив для зберігання типів клітинок поля
    private val grid = Array(BOARD_WIDTH) { x ->
        Array(BOARD_HEIGHT) { y ->
            if (x == 0 || x == BOARD_WIDTH - 1 || y == 0 || y == BOARD_HEIGHT - 1)
                CellType.WALL
            else
                CellType.EMPTY
        }
    }

    //Типи клітинок поля
    private enum class CellType { EMPTY, WALL }

    init {
        preferredSize = Dimension(BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE)
        layout = null
        isFocusable = true
        addKeyListener(this)
        exitButton.addActionListener { System.exit(0) }
        exitButton.isVisible = false
        add(exitButton)
        startGame()
    }

    override fun doLayout() {
        super.doLayout()
        if (exitButton.isVisible) {
            val btnWidth = 100
            val btnHeight = 30
            val x = (width - btnWidth) / 2
            val y = (height / 2) + 100
            exitButton.setBounds(x, y, btnWidth, btnHeight)
        }
    }

    private fun startGame() {
        state.reset()
        accumulator = 0f
        lastUpdateTime = System.currentTimeMillis()
        directionChanged = false
        exitButton.isVisible = false

        //Ініціалізує змійку з мінімальною довжиною
        snake.body.clear()
        val startX = BOARD_WIDTH / 2
        val startY = BOARD_HEIGHT / 2

        for (i in 0 until MIN_SNAKE_LENGTH) {
            snake.body.add(Point(startX - i, startY))
        }

        snake.direction = Direction.RIGHT
        snakeRenderer.setPrevState(snake.body)
        food.respawn()
        timer.restart()
    }

    private fun stopGame() {
        state.isRunning = false
        state.isGameOver = true

        ParticleSystem.spawnParticles(
            snake.body.first().x,
            snake.body.first().y,
            EffectType.COLLISION,
            CELL_SIZE
        )
        exitButton.isVisible = true
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        //Використовує буферизацію рендерингу
        val bufferImage = createImage(width, height)
        val bufferGraphics = bufferImage.graphics
        drawGame(bufferGraphics)
        //Малювання результату буферизації на екран
        g.drawImage(bufferImage, 0, 0, this)
    }

    private fun drawGame(g: Graphics) {
        //Малює поле
        drawGrid(g)

        if (state.isRunning) {
            //Малюємо ігрові елементи
            drawFood(g)
            snakeRenderer.render(g, snake, interpolation)
            drawStats(g)

            if (state.isPaused) {
                drawPauseMenu(g)
                exitButton.isVisible = true
            } else {
                exitButton.isVisible = false
            }
        } else {
            drawGameOver(g)
        }

        //Малюємо частинки поверх усього
        ParticleSystem.draw(g, CELL_SIZE)
    }

    //Оптимізоване малювання поля з кешуванням
    private fun drawGrid(g: Graphics) {
        for (x in 0 until BOARD_WIDTH) {
            for (y in 0 until BOARD_HEIGHT) {
                val drawX = x * CELL_SIZE
                val drawY = y * CELL_SIZE
                //Використовує попередньо обчислений тип клітинки
                val image = if (grid[x][y] == CellType.WALL) wallImage else tileImage
                g.drawImage(image, drawX, drawY, CELL_SIZE, CELL_SIZE, this)
            }
        }
    }

    private fun drawFood(g: Graphics) {
        g.drawImage(
            foodImage,
            food.position.x * CELL_SIZE,
            food.position.y * CELL_SIZE,
            CELL_SIZE,
            CELL_SIZE,
            this
        )
    }

    private fun drawStats(g: Graphics) {
        g.color = Color.WHITE
        g.font = Font("Arial", Font.BOLD, 14)
        g.drawString("Рахунок: ${state.score}", 10, 20)
        g.drawString("Їжі з'їдено: ${state.foodEatenCount}", 10, 40)
        val movesPerSecond = String.format("%.1f", 1000.0 / state.currentDelay)
        g.drawString("Швидкість: $movesPerSecond кроків/сек", 10, 60)
    }

    private fun drawPauseMenu(g: Graphics) {
        g.font = Font("Arial", Font.BOLD, 40)
        val metrics = g.getFontMetrics(g.font)
        val text = "Пауза"
        val x = (width - metrics.stringWidth(text)) / 2
        val y = height / 2
        g.drawString(text, x, y)
    }

    private fun drawGameOver(g: Graphics) {
        g.color = Color.WHITE
        g.font = Font("Arial", Font.BOLD, 40)
        val metrics = g.getFontMetrics(g.font)
        val text = "Гру закінчено"
        val x = (width - metrics.stringWidth(text)) / 2
        val y = height / 2
        g.drawString(text, x, y)

        g.font = Font("Arial", Font.BOLD, 20)
        val restartText = "'R' щоб почати заново"
        val metrics2 = g.getFontMetrics(g.font)
        val x2 = (width - metrics2.stringWidth(restartText)) / 2
        val y2 = y + 40
        g.drawString(restartText, x2, y2)

        val scoreText = "Фінальний рахунок: ${state.score}"
        val x3 = (width - metrics2.stringWidth(scoreText)) / 2
        val y3 = y2 + 30
        g.drawString(scoreText, x3, y3)
        exitButton.isVisible = true
    }

    override fun actionPerformed(e: ActionEvent?) {
        val now = System.currentTimeMillis()
        val dt = (now - lastUpdateTime).toFloat()
        lastUpdateTime = now

        if (!state.isPaused && state.isRunning) {
            accumulator += dt

            //Синхронізація логіки гри з поточним часом
            while (accumulator >= state.currentDelay) {
                snakeRenderer.setPrevState(snake.body)
                updateGameLogic()
                accumulator -= state.currentDelay
            }

            //Обчислює коефіцієнт інтерполяції для рендерингу
            interpolation = accumulator / state.currentDelay
        }

        //Оновлює частинки незалежно від стану гри
        ParticleSystem.update(dt)
        repaint()
    }

    private fun updateGameLogic() {
        val head = snake.body.first()

        //Перевіряє, чи змійка з'їла їжу
        if (head.x == food.position.x && head.y == food.position.y) {
            handleFoodEaten()
        } else {
            snake.move()
        }

        //Перевіряє колізії зі стінами та власним тілом
        checkCollisions()

        directionChanged = false
    }

    private fun handleFoodEaten() {
        ParticleSystem.spawnParticles(
            food.position.x,
            food.position.y,
            EffectType.FOOD,
            CELL_SIZE
        )
        //Звичайний рух
        snake.move()
        //Додає новий сегмент у хвості
        snake.grow()
        //Запускає ефект росту
        ParticleSystem.spawnParticles(
            snake.body.last().x,
            snake.body.last().y,
            EffectType.GROWTH,
            CELL_SIZE
        )
        //Оновлює стан гри
        state.score += 10
        state.foodEatenCount++

        //Збільшує швидкість гри кожні 5 з'їденої їжі
        if (state.foodEatenCount % 5 == 0) {
            state.currentDelay = max(MIN_GAME_DELAY, state.currentDelay - 10)
        }

        food.respawn()
    }

    private fun checkCollisions() {
        val newHead = snake.body.first()

        //Перевіряє колізії зі стінами
        val hitBoundary = newHead.x <= 0 || newHead.x >= BOARD_WIDTH - 1 ||
                newHead.y <= 0 || newHead.y >= BOARD_HEIGHT - 1

        //Перевіряє колізії з власним тілом
        val hitSelf = snake.checkSelfCollision()

        if (hitBoundary || hitSelf) {
            stopGame()
        }
    }

    override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_LEFT -> if (snake.direction != Direction.RIGHT) snake.direction = Direction.LEFT
            KeyEvent.VK_RIGHT -> if (snake.direction != Direction.LEFT) snake.direction = Direction.RIGHT
            KeyEvent.VK_UP -> if (snake.direction != Direction.DOWN) snake.direction = Direction.UP
            KeyEvent.VK_DOWN -> if (snake.direction != Direction.UP) snake.direction = Direction.DOWN
            KeyEvent.VK_SPACE -> state.isPaused = !state.isPaused
            KeyEvent.VK_R -> if (state.isGameOver) startGame()
        }
    }

    override fun keyReleased(e: KeyEvent?) {}
    override fun keyTyped(e: KeyEvent?) {}
}