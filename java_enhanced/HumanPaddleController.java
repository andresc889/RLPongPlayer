
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 *
 * @author Andres
 */
public class HumanPaddleController extends PaddleController implements KeyListener {

    private static double ACCELERATION_MAGNITUDE = 0.25;

    public HumanPaddleController(GameBoard board, int type) {
        super(board, type);
    }

    @Override
    public void keyTyped(KeyEvent ke) {
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_UP) {
            if (type == PaddleController.LEFT) {
                board.setLeftPaddleAccelerationY(-ACCELERATION_MAGNITUDE);
            } else {
                board.setRightPaddleAccelerationY(-ACCELERATION_MAGNITUDE);
            }
        }

        if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
            if (type == PaddleController.LEFT) {
                board.setLeftPaddleAccelerationY(ACCELERATION_MAGNITUDE);
            } else {
                board.setRightPaddleAccelerationY(ACCELERATION_MAGNITUDE);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_UP || ke.getKeyCode() == KeyEvent.VK_DOWN) {
            if (type == PaddleController.LEFT) {
                board.setLeftPaddleAccelerationY(0);
            } else {
                board.setRightPaddleAccelerationY(0);
            }
        }
    }

}
