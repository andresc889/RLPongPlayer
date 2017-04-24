
import java.awt.geom.Point2D;
import java.util.Vector;

/**
 *
 * @author Andres
 */
public final class GameBoard {

    public static final double PADDLE_WIDTH = 10.0;
    public static final double PADDLE_HEIGHT = 50.0;
    public static final double BALL_DIAMETER = 8.0;

    public static final double PADDLE_MAX_SPEED = 5.0;
    public static final double PADDLE_WALL_SPEED_LOSS = 1.0;

    public static final double BALL_SPEED = 5.0;

    public static final double DT = 1.0;

    public static final int LEFT_LOST = -1;
    public static final int NONE_LOST = 0;
    public static final int RIGHT_LOST = 1;

    private final double width;
    private final double height;

    private final boolean leftPaddle;
    private double leftPaddleY;
    private double leftPaddleVelocityY;
    private double leftPaddleAccelerationY;

    private final boolean rightPaddle;
    private double rightPaddleY;
    private double rightPaddleVelocityY;
    private double rightPaddleAccelerationY;

    private Point2D.Double ballPosition;
    private Point2D.Double ballVelocity;
    private Point2D.Double ballAcceleration;

    private Vector<GameBoardListener> gameBoardListeners;

    private boolean done;
    private int whoLost;
    private int leftScore;
    private int rightScore;

    public GameBoard(double width, double height, boolean leftPaddle, boolean rightPaddle) {
        this.width = width;
        this.height = height;

        this.leftPaddle = leftPaddle;
        leftPaddleY = (height - PADDLE_HEIGHT) / 2;
        leftPaddleVelocityY = 0;
        leftPaddleAccelerationY = 0;

        this.rightPaddle = rightPaddle;
        rightPaddleY = (height - PADDLE_HEIGHT) / 2;
        rightPaddleVelocityY = 0;
        rightPaddleAccelerationY = 0;

        randomizeBall();

        gameBoardListeners = new Vector<>();

        done = false;
        whoLost = NONE_LOST;
        leftScore = 0;
        rightScore = 0;
    }

    public GameBoard(GameBoard original) {
        width = original.width;
        height = original.height;

        leftPaddle = original.leftPaddle;
        leftPaddleY = original.leftPaddleY;
        leftPaddleVelocityY = original.leftPaddleVelocityY;
        leftPaddleAccelerationY = original.leftPaddleAccelerationY;

        rightPaddle = original.rightPaddle;
        rightPaddleY = original.rightPaddleY;
        rightPaddleVelocityY = original.rightPaddleVelocityY;
        rightPaddleAccelerationY = original.rightPaddleAccelerationY;

        ballPosition = new Point2D.Double(original.ballPosition.x, original.ballPosition.y);
        ballVelocity = new Point2D.Double(original.ballVelocity.x, original.ballVelocity.y);
        ballAcceleration = new Point2D.Double(original.ballAcceleration.x, original.ballAcceleration.y);

        // Don't copy the listeners
        gameBoardListeners = new Vector<>();

        done = original.done;
        whoLost = original.whoLost;
        leftScore = original.leftScore;
        rightScore = original.rightScore;
    }

    public void randomizeBall() {
        double randomX = width / 2 + Math.random() * (width / 2 - BALL_DIAMETER - PADDLE_WIDTH);
        double randomY = Math.random() * (height - BALL_DIAMETER);

        ballPosition = new Point2D.Double(randomX, randomY);

        double velocityAngle = 2 * Math.PI / 3 + Math.random() * (4 * Math.PI / 3 - 2 * Math.PI / 3);

        ballVelocity = new Point2D.Double(BALL_SPEED * Math.cos(velocityAngle), BALL_SPEED * Math.sin(velocityAngle));
        ballAcceleration = new Point2D.Double(0, 0);
    }

    public void update() {
        if (done) {
            return;
        }

        boolean leftHit = false;
        boolean rightHit = false;

        // Check if left paddle achieved maximum speed
        if (leftPaddleVelocityY >= PADDLE_MAX_SPEED) {
            leftPaddleVelocityY = PADDLE_MAX_SPEED;
        } else if (leftPaddleVelocityY <= -PADDLE_MAX_SPEED) {
            leftPaddleVelocityY = -PADDLE_MAX_SPEED;
        }

        // Check if left paddle reached a top/bottom wall
        if (leftPaddleY < 0) {
            leftPaddleY = 0;
            leftPaddleVelocityY *= -(1 - PADDLE_WALL_SPEED_LOSS);
        }

        if (leftPaddleY > height - PADDLE_HEIGHT + 1) {
            leftPaddleY = height - PADDLE_HEIGHT + 1;
            leftPaddleVelocityY *= -(1 - PADDLE_WALL_SPEED_LOSS);
        }

        // Update the left paddle velocity and position
        leftPaddleVelocityY += leftPaddleAccelerationY * DT;
        leftPaddleY += leftPaddleVelocityY * DT;

        // Check if right paddle achieved maximum speed
        if (rightPaddleVelocityY >= PADDLE_MAX_SPEED) {
            rightPaddleVelocityY = PADDLE_MAX_SPEED;
        } else if (rightPaddleVelocityY <= -PADDLE_MAX_SPEED) {
            rightPaddleVelocityY = -PADDLE_MAX_SPEED;
        }

        // Check if right paddle reached a top/bottom wall
        if (rightPaddleY < 0) {
            rightPaddleY = 0;
            rightPaddleVelocityY *= -(1 - PADDLE_WALL_SPEED_LOSS);
        }

        if (rightPaddleY > height - PADDLE_HEIGHT + 1) {
            rightPaddleY = height - PADDLE_HEIGHT + 1;
            rightPaddleVelocityY *= -(1 - PADDLE_WALL_SPEED_LOSS);
        }

        // Update the right paddle velocity and position
        rightPaddleVelocityY += rightPaddleAccelerationY * DT;
        rightPaddleY += rightPaddleVelocityY * DT;

        // Check if the ball hit the left paddle
        if (ballPosition.x < PADDLE_WIDTH) {
            if (!leftPaddle || (ballPosition.y + BALL_DIAMETER >= leftPaddleY && ballPosition.y <= leftPaddleY + PADDLE_HEIGHT)) {
                ballVelocity.x = -ballVelocity.x;
                leftHit = true;
            } else {
                done = true;
                whoLost = LEFT_LOST;

                if (rightPaddle) {
                    rightScore++;
                }

                // Notify the listeners of updates to the board
                for (GameBoardListener listener : gameBoardListeners) {
                    listener.gameBoardUpdated(this, leftHit, rightHit);
                }

                return;
            }
        }

        // Check if the ball hit the right paddle
        if (ballPosition.x + BALL_DIAMETER > width - PADDLE_WIDTH) {
            if (!rightPaddle || (ballPosition.y + BALL_DIAMETER >= rightPaddleY && ballPosition.y <= rightPaddleY + PADDLE_HEIGHT)) {
                ballVelocity.x = -ballVelocity.x;
                rightHit = true;
            } else {
                done = true;
                whoLost = RIGHT_LOST;

                if (leftPaddle) {
                    leftScore++;
                }

                // Notify the listeners of updates to the board
                for (GameBoardListener listener : gameBoardListeners) {
                    listener.gameBoardUpdated(this, leftHit, rightHit);
                }

                return;
            }
        }

        // Check if the ball hit a top/bottom wall
        if (ballPosition.y <= 0 || ballPosition.y >= height - BALL_DIAMETER) {
            ballVelocity.y = -ballVelocity.y;
        }

        // Check if the ball hit a left/right wall
        if (ballPosition.x <= 0 || ballPosition.x >= width - BALL_DIAMETER) {
            ballVelocity.x = -ballVelocity.x;
        }

        // Update the ball velocity and position
        ballVelocity.x += ballAcceleration.x * DT;
        ballVelocity.y += ballAcceleration.y * DT;
        ballPosition.x += ballVelocity.x * DT;
        ballPosition.y += ballVelocity.y * DT;

        // Notify the listeners of updates to the board
        for (GameBoardListener listener : gameBoardListeners) {
            listener.gameBoardUpdated(this, leftHit, rightHit);
        }
    }

    public void reset() {
        leftPaddleY = (height - PADDLE_HEIGHT) / 2;
        leftPaddleVelocityY = 0;
        leftPaddleAccelerationY = 0;

        rightPaddleY = (height - PADDLE_HEIGHT) / 2;
        rightPaddleVelocityY = 0;
        rightPaddleAccelerationY = 0;

        randomizeBall();

        done = false;
        whoLost = NONE_LOST;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public boolean hasLeftPaddle() {
        return leftPaddle;
    }

    public double getLeftPaddleY() {
        return leftPaddleY;
    }

    public boolean hasRightPaddle() {
        return rightPaddle;
    }

    public double getRightPaddleY() {
        return rightPaddleY;
    }

    public double getLeftPaddleVelocityY() {
        return leftPaddleVelocityY;
    }

    public double getRightPaddleVelocityY() {
        return rightPaddleVelocityY;
    }

    public double getLeftPaddleAccelerationY() {
        return leftPaddleAccelerationY;
    }

    public double getRightPaddleAccelerationY() {
        return rightPaddleAccelerationY;
    }

    public void setLeftPaddleAccelerationY(double leftPaddleAccelerationY) {
        this.leftPaddleAccelerationY = leftPaddleAccelerationY;
    }

    public Point2D.Double getBallPosition() {
        return ballPosition;
    }

    public Point2D.Double getBallVelocity() {
        return ballVelocity;
    }

    public double getBallAngle() {
        if (ballVelocity.x == 0) {
            if (ballVelocity.y > 0) {
                return Math.PI / 2;
            } else if (ballVelocity.y < 0) {
                return -Math.PI / 2;
            } else {
                return Double.NaN;
            }
        }

        if (ballVelocity.y == 0) {
            if (ballVelocity.x > 0) {
                return 0;
            } else if (ballVelocity.x < 0) {
                return -Math.PI;
            } else {
                return Double.NaN;
            }
        }

        double rawAngle = Math.atan(ballVelocity.y / ballVelocity.x);

        if (ballVelocity.x > 0 && ballVelocity.y > 0) {
            return rawAngle;
        } else if (ballVelocity.x > 0 && ballVelocity.y < 0) {
            return rawAngle;
        } else if (ballVelocity.x < 0 && ballVelocity.y > 0) {
            return rawAngle + Math.PI;
        } else if (ballVelocity.x < 0 && ballVelocity.y < 0) {
            return rawAngle + Math.PI;
        }

        return Double.NaN;
    }

    public void setRightPaddleAccelerationY(double rightPaddleAccelerationY) {
        this.rightPaddleAccelerationY = rightPaddleAccelerationY;
    }

    public void addGameBoardListener(GameBoardListener listener) {
        gameBoardListeners.add(listener);
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getWhoLost() {
        return whoLost;
    }

    public int getLeftScore() {
        return leftScore;
    }

    public int getRightScore() {
        return rightScore;
    }

    public void resetScores() {
        leftScore = 0;
        rightScore = 0;
    }
}
