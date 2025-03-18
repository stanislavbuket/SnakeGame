import view.GameFrame
import javax.swing.SwingUtilities

/**
 * Точка входу програми; створює головне вікно гри у окремому потоці.
 */
fun main() {
    SwingUtilities.invokeLater { GameFrame() }
}