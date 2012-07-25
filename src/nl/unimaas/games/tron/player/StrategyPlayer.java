package nl.unimaas.games.tron.player;

import java.awt.Color;
import java.lang.reflect.Field;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import nl.unimaas.games.tron.engine.Board;
import nl.unimaas.games.tron.player.mcts.PlayoutStrategy;

/**
 * Plays according to a given PlayoutStrategy
 */
@SuppressWarnings("serial")
public class StrategyPlayer extends Player {
	private PlayoutStrategy strategy = PlayoutStrategy.DEFENSIVE;

	public StrategyPlayer(String name, int num, Color c) {
		super(name, num, c);
	}

	@Override
	protected int computeMove(Board b, long endTime) {
		int pos = b.getPlayerPosition(this);
		int x = Board.posToX(pos), y = Board.posToY(pos);
		int m = strategy.getMove(b, x, y, getNumber());
		if (m == Board.MOVE_NONE)
			return PlayoutStrategy.SUICIDAL_RANDOM.getMove(b, x, y, getNumber());
		else
			return m;
	}

	public void setStrategy(PlayoutStrategy strategy) {
		this.strategy = strategy;
	}
	
	public PlayoutStrategy getStrategy() {
		return strategy;
	}
	
	public static Player fromXml(Node playerRoot, int num, Color color) {
		StrategyPlayer player = new StrategyPlayer(StrategyPlayer.class.getSimpleName(), num, color);
		NamedNodeMap attrs = playerRoot.getAttributes();
		if (attrs.getNamedItem("strategy") != null) {
			try {
				Field field = PlayoutStrategy.class.getField(attrs.getNamedItem("strategy").getNodeValue());
				player.description = String.format("strategy=%s", field.getName());
				player.strategy = (PlayoutStrategy) field.get(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return player;
	}
	
	@Override
	public StrategyPlayer clone() {
		StrategyPlayer player = new StrategyPlayer(getName(), getNumber(), getColor());
		player.setStrategy(strategy);
		return player;
	}
}
