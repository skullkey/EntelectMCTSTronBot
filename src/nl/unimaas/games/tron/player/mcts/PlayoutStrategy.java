package nl.unimaas.games.tron.player.mcts;

import java.io.Serializable;
import java.util.ArrayList;

import nl.unimaas.games.tron.engine.Board;
import nl.unimaas.games.tron.util.KnuthRandom;

@SuppressWarnings("serial")
public interface PlayoutStrategy extends Serializable {
	public int getMove(Board board, int x, int y, int playerNr);
	
	public final static PlayoutStrategy RANDOM = new PlayoutStrategy() {
		@Override
		public int getMove(Board board, int x, int y, int playerNr) {
			ArrayList<Integer> moves = board.getValidMovesFiltered(x, y);
			if (!moves.isEmpty())
				return moves.get(KnuthRandom.nextInt(moves.size()));
			else
				return Board.MOVE_NONE;
		}
		
		@Override
		public String toString() {
			return "random";
		}
	};
	
	public final static PlayoutStrategy SUICIDAL_RANDOM = new PlayoutStrategy() {
		@Override
		public int getMove(Board board, int x, int y, int playerNr) {
			ArrayList<Integer> moves = board.getValidMoves(x, y);
			if (!moves.isEmpty())
				return moves.get(KnuthRandom.nextInt(moves.size()));
			else {
				return Board.MOVE_NONE;
			}
		}
		
		@Override
		public String toString() {
			return "suicidal-random";
		}
	};
	
	public final static PlayoutStrategy WALLHUG = new PlayoutStrategy() {
		@Override
		public int getMove(Board board, int x, int y, int playerNr) {
			ArrayList<Integer> moves = new ArrayList<Integer>(3);
			if (x > 0 && board.isEmpty(x - 1, y) && board.getWallCount(x - 1, y) > 1 && !board.isSuicideMove(x, y, x - 1, y))
				 moves.add(Board.MOVE_LEFT);
			if (y > 0 && board.isEmpty(x, y - 1) && board.getWallCount(x, y - 1) > 1 && !board.isSuicideMove(x, y, x, y - 1))
				 moves.add(Board.MOVE_UP);
			if (x < board.getWidth() - 1 && board.isEmpty(x + 1, y) && board.getWallCount(x + 1, y) > 1 && !board.isSuicideMove(x, y, x + 1, y))
				 moves.add(Board.MOVE_RIGHT);
			if (y < board.getHeight() - 1 && board.isEmpty(x, y + 1) && board.getWallCount(x, y + 1) > 1 && !board.isSuicideMove(x, y, x, y + 1))
				 moves.add(Board.MOVE_DOWN);
			 
			if (!moves.isEmpty())
				return moves.get(KnuthRandom.nextInt(moves.size()));
			else
				return RANDOM.getMove(board, x, y, playerNr);
		}
		
		@Override
		public String toString() {
			return "wallhug";
		}
	};
	
	public final static PlayoutStrategy OFFENSIVE = new PlayoutStrategy() {
		@Override
		public int getMove(Board board, int x, int y, int playerNr) {
			//moves toward the enemy (no pathfinding)
			int pos = board.getPlayerPosition(1 - playerNr);
			int x2 = Board.posToX(pos), y2 = Board.posToY(pos);
			
			ArrayList<Integer> moves = new ArrayList<Integer>(3);
			if (x != x2) {
				if (x > x2 && board.isEmpty(x - 1, y))
					moves.add(Board.MOVE_LEFT);
				else if (x < x2 && board.isEmpty(x + 1, y))
					moves.add(Board.MOVE_RIGHT);
			}
			if (y != y2) {
				if (y > y2 && board.isEmpty(x, y - 1))
					moves.add(Board.MOVE_UP);
				else if (y < y2 && board.isEmpty(x, y + 1))
					moves.add(Board.MOVE_DOWN);
			}
			
			if (moves.isEmpty())
				return RANDOM.getMove(board, x, y, playerNr);
			else
				return moves.get(KnuthRandom.nextInt(moves.size()));
		}
		
		@Override
		public String toString() {
			return "offensive";
		}
	};
	
	public final static PlayoutStrategy DEFENSIVE = new PlayoutStrategy() {
		@Override
		public int getMove(Board board, int x, int y, int playerNr) {
			//moves toward the enemy (no pathfinding)
			int pos = board.getPlayerPosition(1 - playerNr);
			int x2 = Board.posToX(pos), y2 = Board.posToY(pos);
			
			ArrayList<Integer> moves = new ArrayList<Integer>(3);
			if (x != x2) {
				if (x > x2 && board.isEmpty(x + 1, y))
					moves.add(Board.MOVE_RIGHT);
				else if (x < x2 && board.isEmpty(x - 1, y))
					moves.add(Board.MOVE_LEFT);
			}
			if (y != y2) {
				if (y > y2 && board.isEmpty(x, y + 1))
					moves.add(Board.MOVE_DOWN);
				else if (y < y2 && board.isEmpty(x, y - 1))
					moves.add(Board.MOVE_UP);
			}
			
			if (moves.isEmpty())
				return RANDOM.getMove(board, x, y, playerNr);
			else
				return moves.get(KnuthRandom.nextInt(moves.size()));
		}
		
		@Override
		public String toString() {
			return "defensive";
		}
	};
	
	public final static PlayoutStrategy MIXED = new PlayoutStrategy() {
		private final PlayoutStrategy[] strategies = new PlayoutStrategy[] {RANDOM, WALLHUG, OFFENSIVE};
		//normalized weights
		private final double[] weights = new double[] {0.333, 0.333, 0.334};
		
		@Override
		public int getMove(Board board, int x, int y, int playerNr) {
			double cumprob = KnuthRandom.nextDouble();
			int i = -1;
			double sum = 0;
			do
				sum += weights[++i]; 
			while (sum < cumprob);
			return strategies[i].getMove(board, x, y, playerNr);
		}
		
		@Override
		public String toString() {
			return "mixed";
		}
	};
}
