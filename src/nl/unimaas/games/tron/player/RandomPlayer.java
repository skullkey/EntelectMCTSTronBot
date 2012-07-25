package nl.unimaas.games.tron.player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import org.w3c.dom.Node;

import nl.unimaas.games.tron.engine.Board;

public class RandomPlayer extends Player {
	private static final long serialVersionUID = 1L;
	private final Random random;
	
	public RandomPlayer(String name, int num, Color c) {
		super(name, num, c);
		this.random = new Random();
	}
	
	public RandomPlayer(String name, int num, Color c, int seed) {
		super(name, num, c);
		this.random = new Random(seed);
	}

	@Override
	protected int computeMove(final Board b, long endTime) {
		final int pos = b.getPlayerPosition(this);
		ArrayList<Integer> moves = b.getValidMoves(Board.posToX(pos), Board.posToY(pos));
		return moves.get(random.nextInt(moves.size()));
	}

	public static Player fromXml(Node playerRoot, int num, Color color) {
		if (playerRoot.getAttributes().getNamedItem("seed") == null) {
			return new RandomPlayer(RandomPlayer.class.getSimpleName(), num, color);
		}
		String seed = playerRoot.getAttributes().getNamedItem("seed").getNodeValue();
		if (seed == null || seed.equals("random"))
			return new RandomPlayer(RandomPlayer.class.getSimpleName(), num, color);
		else 
			return new RandomPlayer(RandomPlayer.class.getSimpleName(), num, color, Integer.parseInt(seed));
	}
	
	@Override
	public RandomPlayer clone() {
		return new RandomPlayer(getName(), getNumber(), getColor());
	}
}
