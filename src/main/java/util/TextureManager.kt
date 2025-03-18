package util

import java.awt.Image
import javax.swing.ImageIcon

/**
 * Менеджер текстур; завантажує зображення для ігрових об'єктів з ресурсів.
 */
object TextureManager {
    /**
     * Завантажує зображення за вказаним шляхом; генерує помилку, якщо ресурс не знайдено.
     */
    private fun loadImage(path: String): Image {
        val resource = TextureManager::class.java.getResource(path)
            ?: throw RuntimeException("Ресурс не знайдено: $path")
        return ImageIcon(resource).image
    }

    val tile: Image = loadImage("/sprites/tile.png")
    val wall: Image = loadImage("/sprites/wall.png")
    val snakeHead: Image = loadImage("/sprites/snake_head.png")
    val snakeBody: Image = loadImage("/sprites/snake_body.png")
    val snakeTail: Image = loadImage("/sprites/snake_tail.png")
    val food: Image = loadImage("/sprites/food.png")
}