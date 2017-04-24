
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 *
 * @author Andres
 */
public class GameBoardPanel extends JPanel {
    private final GameBoard board;
    
    public GameBoardPanel(GameBoard board) {
        super();
        this.board = board;
        setBackground(Color.BLACK);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        // Based on 
        super.paintComponent(g);
        
        Graphics2D g2D = (Graphics2D)g.create();
        
        // Draw critical lines
        g2D.setColor(Color.DARK_GRAY);
        g2D.drawLine((int)GameBoard.PADDLE_WIDTH - 1, 0, (int)GameBoard.PADDLE_WIDTH - 1, (int)board.getHeight());
        g2D.drawLine((int)board.getWidth() - (int)GameBoard.PADDLE_WIDTH + 1, 0, (int)board.getWidth() - (int)GameBoard.PADDLE_WIDTH + 1, (int)board.getHeight());
        
        // Draw border
        g2D.setColor(Color.GRAY);
        g2D.drawRect(0, 0, (int)board.getWidth(), (int)board.getHeight());
        
        // Draw paddles
        if (board.hasLeftPaddle()) {
            g2D.setColor(Color.WHITE);
            g2D.fillRect(0, (int)board.getLeftPaddleY(), (int)GameBoard.PADDLE_WIDTH, (int)GameBoard.PADDLE_HEIGHT);
            
            if (board.getLeftPaddleAccelerationY() < 0) {
                g2D.setColor(Color.BLUE);
                g2D.fillPolygon(new int[] {0,
                                           (int)GameBoard.PADDLE_WIDTH / 2,
                                           (int)GameBoard.PADDLE_WIDTH},
                                new int[] {(int)board.getLeftPaddleY() + 10,
                                           (int)board.getLeftPaddleY(),
                                           (int)board.getLeftPaddleY() + 10},
                                3);
            } else if (board.getLeftPaddleAccelerationY() > 0) {
                g2D.setColor(Color.BLUE);
                g2D.fillPolygon(new int[] {0,
                                           (int)GameBoard.PADDLE_WIDTH / 2,
                                           (int)GameBoard.PADDLE_WIDTH},
                                new int[] {(int)board.getLeftPaddleY() + (int)GameBoard.PADDLE_HEIGHT - 10,
                                           (int)board.getLeftPaddleY() + (int)GameBoard.PADDLE_HEIGHT,
                                           (int)board.getLeftPaddleY() + (int)GameBoard.PADDLE_HEIGHT - 10},
                                3);
            }
        }
        
        if (board.hasRightPaddle()) {
            g2D.setColor(Color.WHITE);
            g2D.fillRect((int)board.getWidth() - (int)GameBoard.PADDLE_WIDTH + 1, (int)board.getRightPaddleY(), (int)GameBoard.PADDLE_WIDTH, (int)GameBoard.PADDLE_HEIGHT);
            
            if (board.getRightPaddleAccelerationY() < 0) {
                g2D.setColor(Color.BLUE);
                g2D.fillPolygon(new int[] {(int)board.getWidth() - (int)GameBoard.PADDLE_WIDTH + 1,
                                           (int)board.getWidth() - (int)GameBoard.PADDLE_WIDTH / 2 + 1,
                                           (int)board.getWidth()},
                                new int[] {(int)board.getRightPaddleY() + 10,
                                           (int)board.getRightPaddleY(),
                                           (int)board.getRightPaddleY() + 10},
                                3);
            } else if (board.getRightPaddleAccelerationY() > 0) {
                g2D.setColor(Color.BLUE);
                g2D.fillPolygon(new int[] {(int)board.getWidth() - (int)GameBoard.PADDLE_WIDTH + 1,
                                           (int)board.getWidth() - (int)GameBoard.PADDLE_WIDTH / 2 + 1,
                                           (int)board.getWidth()},
                                new int[] {(int)board.getRightPaddleY() + (int)GameBoard.PADDLE_HEIGHT - 10,
                                           (int)board.getRightPaddleY() + (int)GameBoard.PADDLE_HEIGHT,
                                           (int)board.getRightPaddleY() + (int)GameBoard.PADDLE_HEIGHT - 10},
                                3);
            }
        }
        
        // Draw score. Font: http://stackoverflow.com/a/18249860
        g2D.setColor(Color.GRAY);
        
        g2D.setFont(new Font("Courier", Font.PLAIN, 24));
        g2D.drawString(Integer.toString(board.getLeftScore()), (int)GameBoard.PADDLE_WIDTH + 10, 30);
        
        if (board.getRightScore() > 9) {
            g2D.drawString(Integer.toString(board.getRightScore()), (int)board.getWidth() - (int)GameBoard.PADDLE_WIDTH - 37, 30);
        } else {
            g2D.drawString(Integer.toString(board.getRightScore()), (int)board.getWidth() - (int)GameBoard.PADDLE_WIDTH - 23, 30);
        }
        
        // Draw ball
        g2D.setColor(Color.WHITE);
        g2D.fillRect((int)board.getBallPosition().x, (int)board.getBallPosition().y, (int)GameBoard.BALL_DIAMETER, (int)GameBoard.BALL_DIAMETER);
        
        g2D.dispose();
    }
}
