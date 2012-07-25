package nl.unimaas.games.tron.player.mcts;

import java.io.Serializable;

import nl.unimaas.games.tron.engine.Board;
import nl.unimaas.games.tron.engine.Evaluator;
import nl.unimaas.games.tron.engine.Evaluator.ConnectionEvaluationFunction;
import nl.unimaas.games.tron.player.MCTSPlayer.Node;

@SuppressWarnings("serial")
public interface CutOffStrategy extends Serializable {
	public final static int UNDECIDED = -2, CURRENT_PLAYER_WON = 1, OTHER_PLAYER_WON = -1, DRAW = 0;
	
	public int getEnding(Node node, Board board);
	
	public final static CutOffStrategy SPACE = new CutOffStrategy() {
		public final ConnectionEvaluationFunction estimator = Evaluator.COLOR_SPACE;
		public final static int MIN_DELTA = 5;
		
		@Override
		public int getEnding(Node node, Board board) {
			double deltaSpace = estimator.compute(board);
			if (node.playerNr == 1)
				deltaSpace *= -1;
			
			if (estimator.isConnected())
				return UNDECIDED;
			else {
				if (deltaSpace > MIN_DELTA)
					return CURRENT_PLAYER_WON;
				else if (deltaSpace < -MIN_DELTA)
					return OTHER_PLAYER_WON;
				else
					return DRAW;
			}
		}
		
		@Override
		public String toString() {
			return "space";
		}
	};
}
