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
        const val RENDER_DELAY = 16 //~60 FPS
        const val BOARD_WIDTH = 20
        const val BOARD_HEIGHT = 20
        const val CELL_SIZE = 30
        const val MIN_SNAKE_LENGTH = 3
        const val MIN_GAME_DELAY = 50f //мінімальний час між кроками логіки
        const val DEFAULT_GAME_DELAY = 190f //початковий час між кроками логіки
        const val SPEED_INCREASE_STEP = 10f //крок збільшення швидкості
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

        //Частинки поверх усього
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

        val stats = listOf(
            "Рахунок: ${state.score}",
            "Їжі з'їдено: ${state.foodEatenCount}",
            "Швидкість: ${String.format("%.1f", 1000.0 / state.currentDelay)} кроків/сек"
        )

        stats.forEachIndexed { index, text ->
            g.drawString(text, 10, 20 + index * 20)
        }
    }

    private fun drawCenteredText(g: Graphics, text: String, y: Int, fontSize: Int = 40) {
        g.font = Font("Arial", Font.BOLD, fontSize)
        val metrics = g.getFontMetrics(g.font)
        val x = (width - metrics.stringWidth(text)) / 2
        g.drawString(text, x, y)
    }

    private fun drawPauseMenu(g: Graphics) {
        g.color = Color.WHITE
        drawCenteredText(g, "Пауза", height / 2)
    }

    private fun drawGameOver(g: Graphics) {
        g.color = Color.WHITE

        drawCenteredText(g, "Гру закінчено", height / 2)

        val y2 = height / 2 + 40
        drawCenteredText(g, "'R' щоб почати заново", y2, 20)

        drawCenteredText(g, "Фінальний рахунок: ${state.score}", y2 + 30, 20)
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
        val head = snake.body.firstOrNull() ?: return

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
        food.position.let { foodPos ->
            ParticleSystem.spawnParticles(
                foodPos.x,
                foodPos.y,
                EffectType.FOOD,
                CELL_SIZE
            )
        }

        //Звичайний рух і ріст змійки
        snake.move()
        snake.grow()

        //Запускає ефект росту
        snake.body.lastOrNull()?.let { tail ->
            ParticleSystem.spawnParticles(
                tail.x,
                tail.y,
                EffectType.GROWTH,
                CELL_SIZE
            )
        }

        //Оновлює стан гри
        state.score += 10
        state.foodEatenCount++

        //Збільшує швидкість гри кожні 5 з'їденої їжі
        if (state.foodEatenCount % 5 == 0) {
            state.currentDelay = max(MIN_GAME_DELAY, state.currentDelay - SPEED_INCREASE_STEP)
        }

        food.respawn()
    }

    private fun checkCollisions() {
        val head = snake.body.firstOrNull() ?: return

        //Перевіряє колізії зі стінами
        val hitBoundary = head.x <= 0 || head.x >= BOARD_WIDTH - 1 ||
                head.y <= 0 || head.y >= BOARD_HEIGHT - 1

        //Перевіряє колізії з власним тілом
        val hitSelf = snake.checkSelfCollision()

        if (hitBoundary || hitSelf) {
            stopGame()
        }
    }

    override fun keyPressed(e: KeyEvent) {
        //Якщо гра закінчена, дозволяється лише перезапуск
        if (state.isGameOver) {
            if (e.keyCode == KeyEvent.VK_R) startGame()
            return
        }

        when (e.keyCode) {
            KeyEvent.VK_LEFT -> if (!directionChanged && snake.direction != Direction.RIGHT) {
                snake.direction = Direction.LEFT
                directionChanged = true
            }
            KeyEvent.VK_RIGHT -> if (!directionChanged && snake.direction != Direction.LEFT) {
                snake.direction = Direction.RIGHT
                directionChanged = true
            }
            KeyEvent.VK_UP -> if (!directionChanged && snake.direction != Direction.DOWN) {
                snake.direction = Direction.UP
                directionChanged = true
            }
            KeyEvent.VK_DOWN -> if (!directionChanged && snake.direction != Direction.UP) {
                snake.direction = Direction.DOWN
                directionChanged = true
            }
            KeyEvent.VK_SPACE -> state.isPaused = !state.isPaused
        }
    }

    override fun keyReleased(e: KeyEvent?) {}
    override fun keyTyped(e: KeyEvent?) {}
}