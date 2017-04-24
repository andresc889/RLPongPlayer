
import java.io.File;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import org.encog.engine.network.activation.ActivationLOG;
import org.encog.engine.network.activation.ActivationLinear;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.train.MLTrain;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.manhattan.ManhattanPropagation;
import org.encog.neural.networks.training.propagation.quick.QuickPropagation;
import static org.encog.neural.networks.training.propagation.resilient.RPROPType.iRPROPp;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.neural.networks.training.propagation.scg.ScaledConjugateGradient;
import org.encog.persist.EncogDirectoryPersistence;

/**
 *
 * @author Andres
 */
public class QPaddleController extends PaddleController implements GameBoardListener {

    private static final double[] VALID_ACTIONS = {-0.25, 0, 0.25};

    private GameBoard lastBoard;
    private double lastAction;

    private final BasicNetwork nn;
    private MLDataSet samples;

    private double epsilon;
    private final double gamma;
    private final int nnIterations;

    // Keep count of hits for the current episode
    private int numHits;
    private int maxNumHits;

    // Whether we want to train the neural network or not
    private final boolean train;

    public QPaddleController(GameBoard board, int type, double epsilon, double gamma, int nnIterations, String readNNFileName, boolean train) {
        super(board, type);

        // Make a copy of the board
        lastBoard = new GameBoard(board);
        lastAction = getPaddleAccelerationY(board);

        // Initialize the samples dataset
        samples = new BasicMLDataSet();

        // Check if we're reading from a neural network file: Encog user guide
        if (readNNFileName != null) {
            nn = (BasicNetwork) EncogDirectoryPersistence.loadObject(new File(readNNFileName));
        } else {
            nn = new BasicNetwork();

            nn.addLayer(new BasicLayer(null, true, 8));
            nn.addLayer(new BasicLayer(new ActivationLOG(), true, 21));
            nn.addLayer(new BasicLayer(new ActivationLOG(), true, 14));
            nn.addLayer(new BasicLayer(new ActivationLOG(), true, 7));
            nn.addLayer(new BasicLayer(new ActivationLOG(), true, 5));
            nn.addLayer(new BasicLayer(new ActivationLOG(), true, 4));
            nn.addLayer(new BasicLayer(new ActivationLOG(), true, 3));
            nn.addLayer(new BasicLayer(new ActivationLinear(), false, 1));

            nn.getStructure().finalizeStructure();
            nn.reset();
        }

        // Initialize parameters
        this.epsilon = epsilon;
        this.gamma = gamma;
        this.nnIterations = nnIterations;

        // Initialize the number of hits
        numHits = 0;
        maxNumHits = 2;

        // Whether or not we want to train the neural network
        this.train = train;
    }

    @Override
    public void gameBoardUpdated(GameBoard board, boolean leftHit, boolean rightHit) {
        // Make a copy of the new board
        GameBoard newBoard = new GameBoard(board);

        // Figure out the reward for the state transition
        double reward = 0;

        if (board.isDone()) {
            if (type == PaddleController.LEFT && board.getWhoLost() == GameBoard.LEFT_LOST) {
                reward = -0.5 * Math.abs(getPaddleY(board) - board.getBallPosition().y + GameBoard.PADDLE_HEIGHT / 2 - GameBoard.BALL_DIAMETER / 2);
            } else if (type == PaddleController.RIGHT && board.getWhoLost() == GameBoard.RIGHT_LOST) {
                reward = -0.5 * Math.abs(getPaddleY(board) - board.getBallPosition().y + GameBoard.PADDLE_HEIGHT / 2 - GameBoard.BALL_DIAMETER / 2);
            }
        } else if (type == PaddleController.LEFT && leftHit) {
            numHits++;
            reward = 10.0;
        } else if (type == PaddleController.RIGHT && rightHit) {
            numHits++;
            reward = 10.0;
        } else if (Math.abs(getPaddleY(board) + GameBoard.PADDLE_HEIGHT / 2 - board.getBallPosition().y) - GameBoard.BALL_DIAMETER / 2 < 5) {
            // reward = 1.0;
        }

        if (maxNumHits > 0 && numHits >= maxNumHits) {
            board.setDone(true);
        }

        // Choose an action according to an epsilon-greedy policy
        double chosenAction = -1;
        double nextQ = Double.NEGATIVE_INFINITY;

        if (train && board.isDone()) {
            nextQ = 0;

            // Add this observation to the samples dataset
            if (lastAction != Double.NEGATIVE_INFINITY) {
                double[] target = {reward + gamma * nextQ};
                samples.add(getNNInput(lastBoard, lastAction), new BasicMLData(target));
            }
        } else {
            if (Math.random() < epsilon) {
                // Generate random integer: http://stackoverflow.com/a/363692
                chosenAction = VALID_ACTIONS[ThreadLocalRandom.current().nextInt(0, VALID_ACTIONS.length)];
                nextQ = nn.compute(getNNInput(board, chosenAction)).getData(0);

                // Find nextQ (maximum among the next actions)
                /*for (int i = 0; i < VALID_ACTIONS.length; i++) {
                    double q = nn.compute(getNNInput(board, VALID_ACTIONS[i])).getData(0);

                    if (q > nextQ) {
                        nextQ = q;
                    }
                }*/
            } else {
                // Choose the best action
                shuffleArray(VALID_ACTIONS);

                for (int i = 0; i < VALID_ACTIONS.length; i++) {
                    double q = nn.compute(getNNInput(board, VALID_ACTIONS[i])).getData(0);

                    if (q > nextQ) {
                        chosenAction = VALID_ACTIONS[i];
                        nextQ = q;
                    }
                }
            }

            if (train) {
                // Add this observation to the samples dataset
                if (lastAction != Double.NaN) {
                    double[] target = {reward + gamma * nextQ};
                    samples.add(getNNInput(lastBoard, lastAction), new BasicMLData(target));
                }

                // Update lastBoard and lastAction
                lastBoard = newBoard;
                lastAction = chosenAction;
            }

            // Execute the chosen action
            setPaddleAccelerationY(board, chosenAction);
        }
    }

    public double getPaddleY(GameBoard board) {
        if (type == PaddleController.LEFT) {
            return board.getLeftPaddleY();
        } else {
            return board.getRightPaddleY();
        }
    }

    private double getPaddleVelocityY(GameBoard board) {
        if (type == PaddleController.LEFT) {
            return board.getLeftPaddleVelocityY();
        } else {
            return board.getRightPaddleVelocityY();
        }
    }

    private double getPaddleAccelerationY(GameBoard board) {
        if (type == PaddleController.LEFT) {
            return board.getLeftPaddleAccelerationY();
        } else {
            return board.getRightPaddleAccelerationY();
        }
    }

    private void setPaddleAccelerationY(GameBoard board, double acceleration) {
        if (type == PaddleController.LEFT) {
            board.setLeftPaddleAccelerationY(acceleration);
        } else {
            board.setRightPaddleAccelerationY(acceleration);
        }
    }

    private MLData getNNInput(GameBoard board, double action) {
        double input[] = {getPaddleY(board),
            getPaddleVelocityY(board),
            getPaddleAccelerationY(board),
            board.getBallPosition().x,
            board.getBallPosition().y,
            board.getBallVelocity().x,
            board.getBallVelocity().y,
            action};
        
        return new BasicMLData(input);
    }

    public void startTrainingEpisode() {
        lastBoard = new GameBoard(board);
        lastAction = getPaddleAccelerationY(board);
        numHits = 0;
    }

    public double commitSamplesToNN() {
        // Train the neural network: Encog user guide
        final MLTrain trainingStrategy = new ResilientPropagation(nn, samples);

        for (int i = 0; i < nnIterations; i++) {
            trainingStrategy.iteration();
        }

        trainingStrategy.finishTraining();

        // Clear the samples
        samples.close();
        samples = new BasicMLDataSet();

        return trainingStrategy.getError();
    }

    public void commitNNToFile(String filename) {
        // Save neural network to file: Encog user guide
        EncogDirectoryPersistence.saveObject(new File(filename), nn);
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public int getNumHits() {
        return numHits;
    }

    public void setMaxNumHits(int maxNumHits) {
        this.maxNumHits = maxNumHits;
    }

    private static void shuffleArray(double[] ar) {
        // This code was taken and adapted from http://stackoverflow.com/a/1520212
        Random rnd = ThreadLocalRandom.current();

        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            double a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }
}
