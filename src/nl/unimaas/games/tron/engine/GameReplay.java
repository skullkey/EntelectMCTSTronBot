package nl.unimaas.games.tron.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.swing.filechooser.FileFilter;

import nl.unimaas.games.tron.player.Player;
import nl.unimaas.games.tron.player.ReplayPlayer;

public class GameReplay implements Serializable {
	private static final long serialVersionUID = 2364460197634021671L;
	private static final long TURN_DELAY = 1000;
	
	public final Player[] players;
	public final Board startBoard;
	private final LinkedList<int[]> moves;
	private GameEnd ending = GameEnd.UNDECIDED;
	private Player[] winningPlayers = new Player[0], losingPlayers = new Player[0];
	private int[] startPositions = null;
	
	private Board currentBoard;
	private transient ListIterator<int[]> moveIterator;
	private int turn = 0;
	
	public enum GameEnd {
		DRAW,
		WIN,
		UNDECIDED
	}
	
	/** Creates a GameReplay of a game that is being played */
	public GameReplay(Player[] players, Board start) {
		this.players = players.clone();
		this.startBoard = start.deepClone(true);
		this.moves = new LinkedList<int[]>();
		reset();
	}
	
	public GameReplay(Player[] players, Board start, LinkedList<int[]> moves, GameEnd ending, Player[] winningPlayers) {
		this.players = players.clone();
		this.startBoard = start.deepClone(true);
		this.moves = moves;
		setGameEnding(ending, winningPlayers);
		reset();
	}
	
	/** Sets the game state back to 0 */
	public void reset() {
		moveIterator = moves.listIterator();
		currentBoard = startBoard;
		turn = 0;
	}
	
	public boolean hasNext() {
		if (moveIterator == null)
			return false;
		else
			return (moveIterator.hasNext());
	}
	
	public Board next() {
		turn++;
		if (moveIterator.hasNext()) {
			int[] playerMoves = moveIterator.next();
			for (int i = 0; i < players.length; i++) {
				int m = playerMoves[i];
				if (m != Board.MOVE_NONE)
					currentBoard.performMove(players[i], m);
			}
			return currentBoard;
		}
		else
			return null;
	}
	
	public void appendTurn(int[] moves) {
		this.moves.add(moves);
	}
	
	public LinkedList<Integer> getPlayerMoves(Player p) {
		for (int i = 0; i < players.length; i++)
			if (players[i] == p)
				return getPlayerMoves(i);
		return null;
	}
	
	public LinkedList<Integer> getPlayerMoves(int index) {
		LinkedList<Integer> myMoves = new LinkedList<Integer>();
		ListIterator<int[]> iter = moves.listIterator();
		while (iter.hasNext()) 
			myMoves.add(iter.next()[index]);
		return myMoves;
	}
	
	public int getTurn() {
		return turn;
	}
	
	public int getNumberOfTurns() {
		return moves.size();
	}
	
	public GameEnd getGameEnding() {
		return ending;
	}
	
	public Player[] getWinners() {
		return winningPlayers;
	}
	
	public Player[] getLosers() {
		return losingPlayers;
	}
	
	public void setGameEnding(GameEnd ending, Player[] winners) {
		this.ending = ending;
		winningPlayers = winners;
		ArrayList<Player> losers = new ArrayList<Player>();
		for (int i = 0; i < players.length; i++) {
			Player p = players[i];
			boolean winner = false;
			for (int j = 0; j < winners.length; j++)
				if (winners[j] == p) {
					winner = true;
					break;
				}
			if (!winner)
				losers.add(p);
		}
		losingPlayers = losers.toArray(new Player[losers.size()]);
	}
	
	public void setStartPositions(int[] pos) {
		startPositions = pos;
		startBoard.setStartPositions(players.length, pos);
	}
	
	public int[] getStartPositions() {
		return startPositions;
	}
	
	public void save(String file) throws IOException {
		ObjectOutput ObjOut = new ObjectOutputStream(new FileOutputStream(file));
		ObjOut.writeObject(this);
		ObjOut.close();
	}
	
	/** Loads a replay from a file and returns the game that will play it */
	public static Game loadReplay(String file) throws IOException {
		GameReplay replay = fromFile(file);

		//create the ReplayPlayers
		Player[] players = new Player[replay.players.length];
		for (int i = 0; i < players.length; i++) {
			Player me = replay.players[i];
			LinkedList<Integer> moves = replay.getPlayerMoves(me);
			players[i] = new ReplayPlayer(me.getName(), me.getNumber(), me.getColor(), moves);
		}
		Game game = new Game(players, replay.startBoard.clone());
		game.setStartPositionsScrambled(false);
		game.setTurnRules((short) replay.getNumberOfTurns(), 0);
		game.setGameReplayRecordingEnabled(false);
		game.setTurnDelay(TURN_DELAY);
		return game;
	}
	
	public static GameReplay fromFile(String file) throws IOException {
		ObjectInput ObjInput = new ObjectInputStream(new FileInputStream(file));
		GameReplay replay = null;
		try {
			replay = (GameReplay) ObjInput.readObject();
			//replay.startBoard.setStartPositions(replay.players.length, replay.startPositions);
		} catch (ClassNotFoundException e) {}
		finally {
			ObjInput.close();
		}
		return replay;
	}
	
	public static final String FILE_EXT = "rp";
	public static FileFilter fileFilter = new FileFilter() {
		@Override
		public boolean accept(File f) {
			if (f.isDirectory())
				return true;
			
			String ext = "";
	        String name = f.getName();
	        int i = name.lastIndexOf('.');
	        if (i > 0 && i < name.length() - 1)
	            ext = name.substring(i + 1).toLowerCase();

	        if (ext.equals(FILE_EXT))
	        	return true;
	        else
	        	return false;
		}

		@Override
		public String getDescription() {
			return "Replay files (*." + FILE_EXT + ")";
		}
	};
}
