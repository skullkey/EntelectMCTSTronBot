package nl.unimaas.games.tron.player;

import java.awt.Color;
import java.util.LinkedList;
import java.util.ListIterator;

import nl.unimaas.games.tron.engine.Board;

/** Player class used for replays */
public class ReplayPlayer extends Player {
	private static final long serialVersionUID = 1L;
	
	private LinkedList<Integer> moves;
	private ListIterator<Integer> moveIterator;
	
	public ReplayPlayer(String name, int num, Color c, LinkedList<Integer> moves) {
		super(name, num, c);
		this.moves = moves;
		resetMoves();
	}

	@Override
	protected int computeMove(Board b, long endTime) {
		if (moveIterator != null && moveIterator.hasNext())
			return moveIterator.next();
		else
			return Board.MOVE_NONE;
	}
	
	/** Resets the move iterator */
	public void resetMoves() {
		moveIterator = moves.listIterator();
	}
	
	public void assignMoves(LinkedList<Integer> moves) {
		this.moves = moves;
		resetMoves();
	}
}
