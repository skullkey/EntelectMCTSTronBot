package nl.unimaas.games.tron.player;

import java.awt.Color;

import org.w3c.dom.Node;

import nl.unimaas.games.tron.engine.Board;

public class DummyPlayer extends Player {
	private static final long serialVersionUID = 1L;

	public DummyPlayer(String name, int num, Color c) {
		super(name, num, c);
	}

	@Override
	protected int computeMove(Board b, long endTime) {
		return Board.MOVE_NONE;
	}

	public static Player fromXml(Node playerRoot, int num, Color color) {
		return new DummyPlayer(DummyPlayer.class.getSimpleName(), num, color);
	}
}
