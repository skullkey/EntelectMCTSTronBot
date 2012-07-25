package nl.unimaas.games.tron.player;

import java.awt.Color;

import org.w3c.dom.Node;

import nl.unimaas.games.tron.engine.Board;

public class HumanPlayer extends Player {
	private static final long serialVersionUID = 1L;
	
	private volatile int nextMove;
	
	public HumanPlayer(String name, int num, Color c) {
		super(name, num, c);
	}

	@Override
	protected synchronized int computeMove(Board b, long endTime) {
		try {
			wait();
		} catch (InterruptedException e) {}
		return nextMove;
	}
	
	public synchronized void setMove(int m) {
		nextMove = m;
		notify();
	}

	public static Player fromXml(Node playerRoot) {
		throw new IllegalStateException();
	}
}
