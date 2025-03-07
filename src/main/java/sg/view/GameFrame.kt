package sg.view

import javax.swing.JFrame

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