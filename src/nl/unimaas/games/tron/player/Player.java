package nl.unimaas.games.tron.player;

import java.awt.Color;
import java.io.Serializable;
import java.lang.reflect.Constructor;

import nl.unimaas.games.tron.engine.Board;

public abstract class Player implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final String[] types = {"HumanPlayer", "MCTSPlayer", "UCTPlayer", "MCTSSolverPlayer", "BiasedUCTPlayer", "RandomPlayer", "MonteCarloPlayer", "StrategyPlayer", "FillPlayer", "ContestPlayer"};
	private final String name;
	public String description;
	private int number;
	private Color color;
	private boolean alive = true;
	
	public Player(String name, int num, Color c) {
		this.name = name;
		this.number = num;
		this.color = c;
	}
	
	public synchronized int getMove(final Board b, long time) {
		if (time <= 0)
			return computeMove(b, 0);
		else
			return computeMove(b, System.currentTimeMillis() + time - 1);
	}
	
	/** Called when another player makes a move */
	public synchronized void onPlayerMove(final Board b, int playerNr, int move) {}
	
	protected abstract int computeMove(Board b, long endTime);
	
	public String getName() {
		return name;
	}
	
	public int getNumber() {
		return number;
	}
	
	public void setNumber(int num) {
		this.number = num;
	}
	
	public Color getColor() {
		return color;
	}
	
	public boolean isAlive() {
		return alive;
	}
	
	public void setAlive(boolean alive) {
		this.alive = alive;
	}
	
	public void reset() {}
	
	@Override
	public String toString() {
		return String.format("Player{type=%s, name=%s, #=%d, alive=%s}", getClass().getSimpleName(), name, number, alive);
	}
	
	public String toDescriptiveString() {
		return String.format("%s(%s)", getType(), description);
	}
	
	public String getType() {
		return getClass().getSimpleName();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Player fromType(String type, String name, int num, Color color) {
		try {
			  String className = "nl.unimaas.games.tron.player." + type;
			  Class cl = Class.forName(className);
	          // get the constructor with one parameter
	          Constructor constructor = cl.getConstructor(new Class[] {String.class, int.class, Color.class});
	          // create the instance
	         return (Player) constructor.newInstance(new Object[]{name, num, color});
		}
		catch (Exception ex) {
			return null;
		}
	}
	
	@Override
	public Player clone() {
		throw new UnsupportedOperationException();
	}
}
