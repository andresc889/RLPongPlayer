
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.JFrame;
import javax.swing.Timer;

/**
 *
 * @author Andres
 */
public class PongUI extends JFrame {

    public final GameBoard board;
    public final GameBoardPanel pnlBoard;

    public PongUI(GameBoard board, int width, int height) {
        super();
        this.board = board;

        setTitle("Pong");
        setSize(width, height);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Get screen size: http://stackoverflow.com/a/1936586
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Center window
        setLocation((int) (screenSize.getWidth() - width) / 2, (int) (screenSize.getHeight() - height) / 2);

        // Do not allow resizing
        setResizable(false);

        // Do not use a layout manager: https://docs.oracle.com/javase/tutorial/uiswing/layout/none.html
        getContentPane().setLayout(null);

        // Set a background color
        getContentPane().setBackground(Color.BLACK);

        // Add a game board container
        pnlBoard = new GameBoardPanel(board);
        add(pnlBoard);

        setVisible(true);

        // Position the board container
        int xPnlBoard = (getContentPane().getWidth() - (int) board.getWidth()) / 2;
        int yPnlBoard = (getContentPane().getHeight() - (int) board.getHeight()) / 2;

        pnlBoard.setBounds(xPnlBoard, yPnlBoard, xPnlBoard + (int) board.getWidth(), yPnlBoard + (int) board.getHeight());
        pnlBoard.repaint();
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 3) {
            System.out.println("Usage: PongUI <train | play> <readNNFile | none> <writeNNFile>");
            System.exit(1);
        }

        if (args[0].equals("train")) {
            GameBoard board = new GameBoard(300, 200, true, false);

            // Initialize training parameters
            int maxNumEpisodes = 40000;
            int episodesPerBatch = 10;
            double maxEpsilon = 0.9;
            double minEpsilon = 0;
            int epsilonEpisodes = 10000;
            double gamma = 0.9;
            int maxNumHitsPerEpisode = 10;
            int nnIterations = 1;
            int batchNNBackupFrequency = 1000;
            
            // Create controllers
            QPaddleController ai = new QPaddleController(board, PaddleController.LEFT, maxEpsilon, gamma, nnIterations, (args[1].equals("none") ? null : args[1]), true);
            ai.setMaxNumHits(maxNumHitsPerEpisode);
            ai.startTrainingEpisode();
            board.addGameBoardListener(ai);

            // Save the neural network before training
            ai.commitNNToFile(args[2] + "_0");

            /* System.out.printf("%-7s %-7s %-8s %-11s %-9s %-8s %-8s %-10s %-18s\n", "Batch", "Epsilon", "NN Error", "Median Hits", "Mean Hits", "Min Hits", "Max Hits", "% Above 25", "Mean Miss Distance"); */
            System.out.printf("%-7s %-7s %-8s %-18s\n", "Batch", "Epsilon", "NN Error", "Mean Miss Distance");

            // Keep track of the best batch
            int bestBatch;
            double bestMissDistance = Double.POSITIVE_INFINITY;
            
            // Test before training
            double[] testResults = testQPaddleController(board, args[2] + "_0", 75);
            System.out.printf("%-7d %-7.3f %-8.3f %-18.3f\n", 0, ai.getEpsilon(), Double.NaN, testResults[5]);

            // Save best network found so far
            ai.commitNNToFile(args[2] + "_best");
            bestBatch = 0;
            bestMissDistance = testResults[5];
            
            int batch = 1;
            
            for (int curEpisode = 1; curEpisode <= maxNumEpisodes; curEpisode++) {
                while (true) {
                    board.update();

                    if (board.isDone()) {
                        // Prepare board and controller for next episode
                        board.reset();
                        ai.startTrainingEpisode();

                        // Commit batch to neural network if necessary
                        if (curEpisode % episodesPerBatch == 0) {
                            double nnError = ai.commitSamplesToNN();
                            ai.commitNNToFile(args[2]);

                            testResults = testQPaddleController(board, args[2], 75);
                            System.out.printf("%-7d %-7.3f %-8.3f %-18.3f\n", batch, ai.getEpsilon(), nnError, testResults[5]);
                            
                            if (testResults[5] < bestMissDistance) {
                                // Save best network found so far
                                ai.commitNNToFile(args[2] + "_best");
                                bestBatch = batch;
                                bestMissDistance = testResults[5];
                            }
                            
                            if (batch % batchNNBackupFrequency == 0) {
                                ai.commitNNToFile(args[2] + "_batch_" + batch);
                            }

                            batch++;
                        }

                        // Update epsilon
                        if ((curEpisode + 1) < epsilonEpisodes) {
                            ai.setEpsilon(((minEpsilon - maxEpsilon) / (epsilonEpisodes - 1)) * curEpisode + maxEpsilon);
                        } else {
                            ai.setEpsilon(minEpsilon);
                        }

                        break;
                    }
                }
            }
            
            System.out.println();
            System.out.println("Best neural network found in batch " + bestBatch + " (saved to " + args[2] + "_best)");
            System.out.println();
            
            System.exit(0);
        } else if (args[0].equals("play")) {
            // Acceleration: http://stackoverflow.com/a/13832805
            System.setProperty("sun.java2d.opengl", "true");

            GameBoard board = new GameBoard(300, 200, true, true);
            PongUI ui = new PongUI(board, 350, 270);

            // Create controllers
            QPaddleController ai = new QPaddleController(board, PaddleController.LEFT, 0, 0, 0, (args[1].equals("none") ? null : args[1]), false);
            ai.setMaxNumHits(0);
            board.addGameBoardListener(ai);

            HumanPaddleController human = new HumanPaddleController(board, PaddleController.RIGHT);
            ui.addKeyListener(human);

            // Start an animation loop
            Timer animator = new Timer(10, (ActionEvent ae) -> {
                board.update();
                ui.revalidate();
                ui.repaint();

                if (board.isDone()) {
                    ((Timer) ae.getSource()).stop();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }

                    board.reset();
                    ai.startTrainingEpisode();

                    ((Timer) ae.getSource()).start();
                }
            });

            animator.start();

        } else {
            System.out.println("Usage: PongUI <train | play> <readNNFile | none> <writeNNFile>");
            System.exit(1);
        }
    }

    public static double[] testQPaddleController(GameBoard board, String nnFileName, int numEpisodes) {
        // Index Description
        // 0     Median # of hits
        // 1     Mean # of hits
        // 2     Minimum # of hits
        // 3     Maximum # of hits
        // 4     % of episodes where # hits > 25
        // 5     Average missing distance
        //
        double[] result = {0, 0, 0, 0, 0, 0};

        GameBoard boardCopy = new GameBoard(board.getWidth(), board.getHeight(), true, false);

        // Create controller
        QPaddleController ai = new QPaddleController(boardCopy, PaddleController.LEFT, 0, 0, 0, nnFileName, false);
        ai.setMaxNumHits(30);
        boardCopy.addGameBoardListener(ai);

        // Keep track of number of hits
        ArrayList<Double> hits = new ArrayList<>();

        // Keep track of missing distances
        ArrayList<Double> missDistances = new ArrayList<>();

        for (int i = 0; i < numEpisodes; i++) {
            while (true) {
                boardCopy.update();

                if (boardCopy.isDone()) {
                    hits.add(new Double(ai.getNumHits()));

                    if ((ai.getType() == PaddleController.LEFT && boardCopy.getWhoLost() == GameBoard.LEFT_LOST) || (ai.getType() == PaddleController.RIGHT && boardCopy.getWhoLost() == GameBoard.RIGHT_LOST)) {
                        double mDist = 0;

                        if (boardCopy.getBallPosition().y + GameBoard.BALL_DIAMETER <= ai.getPaddleY(boardCopy)) {
                            mDist = Math.abs(ai.getPaddleY(boardCopy) - boardCopy.getBallPosition().y - GameBoard.BALL_DIAMETER);
                        } else {
                            mDist = Math.abs(boardCopy.getBallPosition().y - ai.getPaddleY(boardCopy) - GameBoard.PADDLE_HEIGHT);
                        }

                        missDistances.add(mDist);
                    }

                    for (int j = 0; j < ai.getNumHits(); j++) {
                        missDistances.add(0.0);
                    }

                    boardCopy.reset();
                    ai.startTrainingEpisode();
                    break;
                }
            }
        }

        // Sort hits: http://beginnersbook.com/2013/12/how-to-sort-arraylist-in-java/
        Collections.sort(hits);

        // Get the median of the number of hits
        if (hits.size() % 2 != 0) {
            result[0] = hits.get(hits.size() / 2);
        } else {
            result[0] = hits.get((hits.size() - 1) / 2) + hits.get((hits.size() - 1) / 2 + 1);
            result[0] /= 2;
        }

        // Get the mean of the number of hits
        double sum = 0;

        for (Double s : hits) {
            sum += s;
        }

        result[1] = sum / hits.size();

        // Get the minimum and maximum number of hits
        result[2] = hits.get(0);
        result[3] = hits.get(hits.size() - 1);

        // Get the percentage of episodes where hits > 25
        for (Double s : hits) {
            if (s > 25) {
                result[4]++;
            }
        }

        result[4] = result[4] * 100.0 / hits.size();

        // Get the average missing distance
        sum = 0;

        for (Double s : missDistances) {
            sum += s;
        }

        result[5] = sum / missDistances.size();

        return result;
    }
}
