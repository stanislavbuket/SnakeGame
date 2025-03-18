package view

import javax.swing.JFrame

/**
 * Головне вікно гри; налаштовує параметри вікна та додає панель гри.
 */
class GameFrame : JFrame() {
    init {
        add(GamePanel())
        pack()
        setLocationRelativeTo(null)
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true
        title = "Гра в Змійку"
    }
}