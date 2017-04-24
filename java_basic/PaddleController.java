
/**
 *
 * @author Andres
 */
public abstract class PaddleController {

    public static int LEFT = -1;
    public static int RIGHT = 1;

    protected final GameBoard board;
    protected final int type;

    public PaddleController(GameBoard board, int type) {
        this.board = board;
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
