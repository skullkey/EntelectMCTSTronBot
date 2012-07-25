package nl.unimaas.games.tron.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


/** Evaluates a position. Gives a positive score if the position is in favor of player 1 */
@SuppressWarnings("serial")
public class Evaluator implements Serializable {
	private static final long serialVersionUID = -8217975131443220665L;
	
	private final Board board;
	private final EvaluationFunction[] evals;
	private final double[] scores;
	private final double[] weights;
	private final Object[] states;
	private double score = 0;

	public Evaluator(Board board, EvaluationFunction[] evals, double[] weights) {
		this.board = board;
		this.evals = evals;
		this.states = new Object[evals.length];
		this.scores = new double[evals.length];
		this.weights = weights;
		compute();
	}
	
	private Evaluator(Board board, EvaluationFunction[] evals, double[] weights, double[] scores, double score, Object[] states) {
		this.board = board;
		this.evals = evals;
		this.states = states;
		this.scores = scores;
		this.weights = weights;
	}
	
	/** Re-computes the evaluation value from the start */
	public void compute() {
		score = 0;
		for (int i = 0; i < evals.length; i++) {
			double s = evals[i].compute(board);
			scores[i] = s;
			score += s * weights[i];
		}
	}
	
	public double getScore() {
		return score;
	}
	
	public static interface EvaluationFunction extends Serializable {
		public double compute(Board board);
	}
	
	public static interface ConnectionEvaluationFunction extends EvaluationFunction {
		public boolean isConnected();
	}
	
	public final static EvaluationFunction FREE_SPACE = new EvaluationFunction() {
		@Override
		public double compute(Board board) {
			int freespace = 0;
			for (int x = 0; x < board.getWidth(); x++) {
				for (int y = 0; y < board.getHeight(); y++) {
					if (board.isEmpty(x, y))
						freespace++;
				}
			}
			return freespace;
		}
	};
	
	public final static ConnectionEvaluationFunction COLOR_SPACE = new ConnectionEvaluationFunction() {
		private final static byte COLOR1 = -2, COLOR2 = -3;
		private final static byte EMPTY = Board.EMPTY_FIELD;
		private boolean connected;
		
		@Override
		public double compute(Board board) {
			byte[][] colorBoard1 = board.getClonedBoardArray();
			byte[][] colorBoard2 = board.getClonedBoardArray();
			
			int pos = board.getPlayerPosition(0);
			int x1 = Board.posToX(pos);
			int y1 = Board.posToY(pos);
			int pos2 = board.getPlayerPosition(1);
			int x2 = Board.posToX(pos2);
			int y2 = Board.posToY(pos2);
			int[][] playerPos = new int[][] {{x1, y1},{x2, y2}};
			connected = false;

			int[] colorCounts1 = new int[2], colorCounts2 = new int[2];
			//players start on COLOR1
			step(colorBoard1, playerPos[0][0], playerPos[0][1], COLOR1, colorCounts1);
			//Board.printBoardArray(colorBoard1);
			if (Board.hasAdjacentFieldSmallerThan(colorBoard1, x2, y2, EMPTY)) {
				//connected!
				connected = true;
				colorBoard2 = Board.cloneArray(colorBoard1);
				colorCounts2[0] = colorCounts1[0];
				colorCounts2[1] = colorCounts1[1];
			}
			step(colorBoard2, playerPos[1][0], playerPos[1][1], COLOR1, colorCounts2);
			//Board.printBoardArray(colorBoard1);
			int total1 = colorCounts1[0] + colorCounts1[1] - 1;
			int total2 = colorCounts2[0] + colorCounts2[1] - 1;
			int moves1 = total1 - Math.abs(colorCounts1[0] - colorCounts1[1]);
			int moves2 = total2 - Math.abs(colorCounts2[0] - colorCounts2[1]);
			return moves1 - moves2;
		}
		
		private void step(byte[][] board, int x, int y, byte color, int[] colorCount) {
			if (board[x][y] == EMPTY)
				board[x][y] = color;
			
			if (color == COLOR1) {
				colorCount[0]++;
				color = COLOR2;
			}
			else {
				colorCount[1]++;
				color = COLOR1;
			}

			if (board[x - 1][y] == EMPTY)
				step(board, x - 1, y, color, colorCount);
			if (board[x + 1][y] == EMPTY)
				step(board, x + 1, y, color, colorCount);
			if (board[x][y - 1] == EMPTY)
				step(board, x, y - 1, color, colorCount);
			if (board[x][y + 1] == EMPTY)
				step(board, x, y + 1, color, colorCount);
		}

		/** of the last state */
		public boolean isConnected() {
			return connected;
		}
	};
	
	public final static ConnectionEvaluationFunction CHAMBER_SPACE = new TreeOfChambersEvaluator();
	
	private static class TreeOfChambersEvaluator implements ConnectionEvaluationFunction {
		private final static byte EMPTY = Board.EMPTY_FIELD;
		
		private boolean connected = false;
		private HashMap<Integer, Chamber> chambers = new HashMap<Integer, Chamber>();
		
		public TreeOfChambersEvaluator() {}
		
		@Override
		public double compute(Board b) {
			chambers.clear();
			byte[][] board = b.getClonedBoardArray();
			int pos1 = b.getPlayerPosition(0);
			step(board, Board.posToX(pos1), Board.posToY(pos1));
			return 0;
		}
		
		/** We're at a point, but we don't know what it is */
		private void step(byte[][] board, int x, int y) {
			/*
			if (board[x - 1][y] == EMPTY)
				step(board, x - 1, y, color, colorCount);
			if (board[x + 1][y] == EMPTY)
				step(board, x + 1, y, color, colorCount);
			if (board[x][y - 1] == EMPTY)
				step(board, x, y - 1, color, colorCount);
			if (board[x][y + 1] == EMPTY)
				step(board, x, y + 1, color, colorCount);
				*/
		}
		
		/** We're in ur chamber, traversin'*/
		private void chamberStep() {
			
		}
		
		/** We're in a path, traveling to the next chamber */
		private void pathStep() {
			
		}

		@Override
		public boolean isConnected() {
			return connected;
		}
		
		private static class Chamber {
			public ArrayList<Path> paths;
			public int size, moves;
			public int id;
			
			public Chamber() {}
		}
		
		private static class Path {
			public int length;
			public Chamber start, end;
			
			public Path() {}
		}
	};
	
	@Override
	public Evaluator clone() {
		return new Evaluator(board, evals, weights.clone(), scores.clone(), score, states);
	}
	
	public Evaluator clone(Board newBoard) {
		return new Evaluator(newBoard, evals, weights.clone(), scores.clone(), score, states);
	}
}
