package nl.unimaas.games.tron.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.unimaas.games.tron.engine.GameReplay.GameEnd;
import nl.unimaas.games.tron.player.Player;

public class Game {
	public final static int PLAYER_COUNT = 2;
	public final static long EXTRA_TIME = 1000;
	private Thread thread;
	public final Board board;
	public final Player[] players;
	private int playersAliveCount = 0;
	private volatile boolean gameOver = false;
	private volatile boolean paused = false;
	private volatile boolean stop = false;
	private boolean setup = false;
	private boolean scrambleStartPositions = false;
	private short turn = 0, turnLimit = 0;
	private long turnTimeLimit = 0;
	/** Pause time between turns, in ms */
	public long turnDelay = 0;
	private Logger logger;
	
	private final static ExecutorService movePool = Executors.newFixedThreadPool(PLAYER_COUNT);
	private static final int PLAYERMOVECOUNT = 1; // only move first player
	private Random random = new Random();
	private boolean parallelMoves = false;
	private ArrayList<GameListener> listeners = new ArrayList<GameListener>();
	
	private boolean recordReplay = false;
	private GameReplay replay;
	private Player winner = null;
	
	public Game(Player[] players, Board board) {
		if (players == null || board == null)
			throw new IllegalArgumentException("no null values allowed");
		
		this.players = players;
		this.board = board;
		board.setPlayerCount(players.length);
	}
	
	/** Set the player positions */
	public void setup() {
		setup = true;
		
		if (recordReplay)
			replay = new GameReplay(players, board);
		
		playersAliveCount = players.length;
		
		Player[] starters = players.clone();
		int[] startPositions = board.getStartPositions(starters.length);
		if (scrambleStartPositions) {
			//shuffle
			for (int i = 0; i < startPositions.length; i++) {
			    int randomIndex = random.nextInt(startPositions.length);
			    int tmp = startPositions[i];
			    startPositions[i] = startPositions[randomIndex];
			    startPositions[randomIndex] = tmp;
			}
		}
		
		for (int i = 0; i < players.length; i++) {
			players[i].setAlive(true);
			players[i].setNumber(i);
			players[i].reset();
			int start = startPositions[i];
			board.placePlayerWall(Board.posToX(start), Board.posToY(start), players[i]);
		}
		
		if (recordReplay)
			replay.setStartPositions(startPositions);
		
		thread = new Thread() {
			@Override
			public void run() {
				runGame();
			}
		};
	}
	
	/** Start the game 
	 * The game runs in a separate thread */
	public void start() {
		getLogger().finer("Starting the game");
		if (thread == null)
			throw new IllegalStateException("no game thread");
		if (thread.isInterrupted())
			throw new IllegalStateException("game thread interupted");
		
		gameOver = false;
		paused = false;
		thread.start();
		for (int i = 0; i < listeners.size(); i++)
			listeners.get(i).onStart(this);
	} 	
	
	public void stop() {
		if (isRunning()) {
			stop = true;
			try {
				thread.join();
			} catch (InterruptedException e) {}
		}
	}
	
	/** Pause the game */
	public void pause() {
		getLogger().fine("Game paused");
		paused = true;
		for (int i = 0; i < listeners.size(); i++)
			listeners.get(i).onPause(this);
	}
	
	public void resume() {
		paused = false;
		for (int i = 0; i < listeners.size(); i++)
			listeners.get(i).onResume(this);
	}
	
	private void runGame() {
		do {
			if (paused || stop) {
				return;
			}
			nextTurn();
			if (turnDelay > 0) {
				try {
					Thread.sleep(turnDelay);
				} catch (InterruptedException e) {}
			}
		} while (!gameOver);
		
		gameOver();
	}
	
	private synchronized void nextTurn() {
		turn++;
		getLogger().finest(String.format("- Turn %d -", turn));
		for (int i = 0; i < listeners.size(); i++)
			listeners.get(i).onNextTurn(this);
		for (int i = 0; i < listeners.size(); i++)
			listeners.get(i).onMoveBegin(this);
		
		ArrayList<Player> movingPlayersList = new ArrayList<Player>(players.length);
		ArrayList<Callable<Integer>> moveCalls = new ArrayList<Callable<Integer>>(players.length);
		
		//see who's alive
		for (int i = 0; i < players.length; i++) {
			final Player p = players[i];
			if (p.isAlive()) {
				final Board cloneBoard = board.deepClone(true);
				movingPlayersList.add(p);
				moveCalls.add(new Callable<Integer>() {
					@Override
					public Integer call() throws Exception {
						return p.getMove(cloneBoard, turnTimeLimit);
					}
				});
			}
		}
		int movingPlayersCount = movingPlayersList.size();
		int[] playerMoves = new int[movingPlayersCount];
		
		if (parallelMoves) {
			System.gc();
			List<Future<Integer>> moveResultsList;
			try {
				if (turnTimeLimit <= 0)
					moveResultsList = movePool.invokeAll(moveCalls);
				else
					moveResultsList = movePool.invokeAll(moveCalls, turnTimeLimit + EXTRA_TIME, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				throw new IllegalStateException("invoke fail");
			}
			@SuppressWarnings("unchecked")
			Future<Integer>[] movesResults = moveResultsList.toArray(new Future[moveResultsList.size()]);
			//wait for the moves
			for (int i = 0; i < movesResults.length; i++) {
				try {
					playerMoves[i] = movesResults[i].get();
				} catch (Exception e) {
					getLogger().warning("Execution error in player " + i + ": " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
		else {
			List<Future<Integer>> moveResult;
			for (int i = 0; i < movingPlayersList.size(); i++) {
				System.gc();
				Collection<Callable<Integer>> moveCall = new ArrayList<Callable<Integer>>(1);
				moveCall.add(moveCalls.get(i));
				try {
					if (turnTimeLimit <= 0)
						moveResult = movePool.invokeAll(moveCall);
					else
						moveResult = movePool.invokeAll(moveCall, turnTimeLimit + EXTRA_TIME, TimeUnit.MILLISECONDS);
					playerMoves[i] = moveResult.get(0).get();
				} catch (Exception e) {
					System.out.println(String.format("Error in player %d's getMove function: %s", i, e.getMessage()));
					e.printStackTrace();
				}
			}
		}
		
		final Player[] movingPlayers = movingPlayersList.toArray(new Player[movingPlayersCount]);
		int[] posMoves = new int[movingPlayersCount];
		boolean[] crashes = new boolean[movingPlayersCount];
		
		int[] replayMoves = new int[players.length];
		
		//gather the moves and target positions and check for crashes
		for (int i = 0; i < movingPlayersCount; i++) {
			int m = playerMoves[i];
			
			//check for collisions
			if (playerMoves[i] != Board.MOVE_NONE) {
				posMoves[i] = board.getPositionFromPlayerMove(movingPlayers[i], m);
				for (int j = 0; j < i; j++) {
					if (playerMoves[j] != Board.MOVE_NONE && posMoves[i] == posMoves[j]) {
						crashes[i] = true;
						crashes[j] = true;
					}
				}
				
				if (recordReplay)
					replayMoves[movingPlayers[i].getNumber()] = m;
			}
			else {
				//no move -> crash
				crashes[i] = true;
			}
		}
		
		if (recordReplay)
			replay.appendTurn(replayMoves);
	
		
		//perform the moves, kill the crash dudes
		for (int i = 0; i < playerMoves.length; i++) {
			if (!crashes[i]) {
				int m = playerMoves[i];
				board.performMove(movingPlayers[i], m);
				getLogger().finest(String.format("%s goes %s", movingPlayers[i].getName(), m));
			}
		}
			
		for (int i = 0; i < playerMoves.length; i++) {
			if (!crashes[i]) {
				//check if he's stuck
				crashes[i] = (board.getValidPlayerMoves(movingPlayers[i]).isEmpty());
				if (crashes[i])
					getLogger().fine(String.format("%s got stuck", movingPlayers[i].getName()));
			}
			//check again..
			if (crashes[i]) {
				if (playerMoves[i] == Board.MOVE_NONE) {
					board.print();
					getLogger().fine(String.format("%s didn't move", movingPlayers[i].getName()));
				}
				else
					getLogger().fine(String.format("%s crashed", movingPlayers[i].getName()));
				movingPlayers[i].setAlive(false);
				playersAliveCount--;
				for (int j = 0; j < listeners.size(); j++)
					listeners.get(j).onPlayerDied(this, movingPlayers[i]);
			}
		}
		
		Board cloneBoard = board.deepClone(true);
		//notify moving players of the move performed by the others
		for (int i = 0; i < movingPlayers.length; i++) {
			if (!movingPlayers[i].isAlive())
				continue;
			int nr = movingPlayers[i].getNumber();
			int m = playerMoves[i];

			for (int j = 0; j < movingPlayers.length; j++) 
				if (movingPlayers[j].isAlive()) //don't notify crashed players
					movingPlayers[j].onPlayerMove(cloneBoard, nr, m);
			
			//notify listeners
			for (int j = 0; j < listeners.size(); j++)
				listeners.get(j).onPlayerMove(this, cloneBoard, movingPlayers[i], m);
		}
		
		//notify the listeners
		for (int i = 0; i < listeners.size(); i++)
			listeners.get(i).onMoveEnd(this);
		
		switch (playersAliveCount) {
		case 1:
			//someone won
			Player[] alives = getAlivePlayers();
			getLogger().fine(String.format("%s won the game!", alives[0].getName()));
			gameOver = true;
			for (int i = 0; i < listeners.size(); i++)
				listeners.get(i).onWin(this, alives[0]);
			if (recordReplay)
				replay.setGameEnding(GameEnd.WIN, alives);
			winner = alives[0];
			break;
		case 0:
			//everyone died/got stuck: draw
			getLogger().fine(String.format("Game draw."));
			gameOver = true;
			for (int i = 0; i < listeners.size(); i++)
				listeners.get(i).onDraw(this, movingPlayers);
			if (recordReplay)
				replay.setGameEnding(GameEnd.DRAW, getAlivePlayers());
			winner = null;
			break;
		}
	}
	
	private void gameOver() {
		getLogger().finer("Game over!");
		for (int i = 0; i < listeners.size(); i++)
			listeners.get(i).onGameOver(this);
		players[0].reset();
		players[1].reset();
	}
	
// #region Game Listener
	public void addGameListener(GameListener gl) {
		if (!listeners.contains(gl))
			listeners.add(gl);
	}
	
	public void removeGameListener(GameListener gl) {
		listeners.remove(gl);
	}
// #end
	
// #region Getters and setters
	public boolean isRunning() {
		return (thread != null && thread.isAlive());
	}
	
	/** Set this before the game starts */
	public void setGameReplayRecordingEnabled(boolean enable) {
		if (setup)
			throw new IllegalStateException();
		recordReplay = enable;
	}
	
	public Player getPlayer(int index) {
		return players[index];
	}
	
	public Player[] getAlivePlayers() {
		ArrayList<Player> alive = new ArrayList<Player>(players.length);
		for (int i = 0; i < players.length; i++)
			if (players[i].isAlive())
				alive.add(players[i]);
		return alive.toArray(new Player[alive.size()]);
	}
	
	public int getPlayerCount() {
		return players.length;
	}
	
	public int getNumberOfPlayersAlive() {
		int count = 0;
		for (int i = 0; i < players.length; i++)
			if (players[i].isAlive())
				count++;
		return count;
	}
	
	public short getTurn() {
		return turn;
	}
	
	public void setStartPositionsScrambled(boolean scrambled) {
		this.scrambleStartPositions = scrambled;
	}
	
	public void setTurnRules(short maxTurns, long turnTimeLimit) {
		this.turnLimit = maxTurns;
		this.turnTimeLimit = turnTimeLimit;
	}
	
	public void setTurnDelay(long delay) {
		this.turnDelay = delay;
	}
	
	public GameReplay getReplay() {
		return replay;
	}
	
	public Logger getLogger() {
		if (logger == null)
			logger = Logger.getLogger("Game");
		return logger;
	}
	
	public void setLogger(Logger l) {
		this.logger = l;
	}
	
	public Player getWinner() {
		return winner;
	}
	
	public boolean isGameOver() {
		return gameOver;
	}
	
	public void setComputeMovesInParallel(boolean yes) {
		if (isRunning())
			throw new IllegalStateException("game is running");
		parallelMoves = yes;
	}
	
	public String toLongString() {
		String playerNames = "[" + players[0].getName();
		for (int i = 1; i < players.length; i++)
			playerNames += ", " + players[i].getName();
		playerNames += "]";
		return String.format("Game{board=%s, players=%s, turnLimit=%d, turnTimeLimit=%d, scramble=%b}", board.getName(), playerNames, turnLimit, turnTimeLimit, scrambleStartPositions);
	}
// #end
	
	public void runOnce(){
			turn++;
			getLogger().finest(String.format("- Turn %d -", turn));
			for (int i = 0; i < listeners.size(); i++)
				listeners.get(i).onNextTurn(this);
			for (int i = 0; i < listeners.size(); i++)
				listeners.get(i).onMoveBegin(this);
			
			ArrayList<Player> movingPlayersList = new ArrayList<Player>(players.length);
			ArrayList<Callable<Integer>> moveCalls = new ArrayList<Callable<Integer>>(players.length);
			
			//see who's alive
			for (int i = 0; i < players.length; i++) {
				final Player p = players[i];
				if (p.isAlive()) {
					final Board cloneBoard = board.deepClone(true);
					movingPlayersList.add(p);
					moveCalls.add(new Callable<Integer>() {
						@Override
						public Integer call() throws Exception {
							return p.getMove(cloneBoard, turnTimeLimit);
						}
					});
				}
			}
			int movingPlayersCount = movingPlayersList.size();
			int[] playerMoves = new int[movingPlayersCount];
			
			if (parallelMoves) {
				System.gc();
				List<Future<Integer>> moveResultsList;
				try {
					if (turnTimeLimit <= 0)
						moveResultsList = movePool.invokeAll(moveCalls);
					else
						moveResultsList = movePool.invokeAll(moveCalls, turnTimeLimit + EXTRA_TIME, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					throw new IllegalStateException("invoke fail");
				}
				@SuppressWarnings("unchecked")
				Future<Integer>[] movesResults = moveResultsList.toArray(new Future[moveResultsList.size()]);
				//wait for the moves
				for (int i = 0; i < movesResults.length; i++) {
					try {
						playerMoves[i] = movesResults[i].get();
					} catch (Exception e) {
						getLogger().warning("Execution error in player " + i + ": " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
			else {
				List<Future<Integer>> moveResult;
				for (int i = 0; i < movingPlayersList.size(); i++) {
					System.gc();
					Collection<Callable<Integer>> moveCall = new ArrayList<Callable<Integer>>(1);
					moveCall.add(moveCalls.get(i));
					try {
						if (turnTimeLimit <= 0)
							moveResult = movePool.invokeAll(moveCall);
						else
							moveResult = movePool.invokeAll(moveCall, turnTimeLimit + EXTRA_TIME, TimeUnit.MILLISECONDS);
						playerMoves[i] = moveResult.get(0).get();
					} catch (Exception e) {
						System.out.println(String.format("Error in player %d's getMove function: %s", i, e.getMessage()));
						e.printStackTrace();
					}
				}
			}
			
			final Player[] movingPlayers = movingPlayersList.toArray(new Player[movingPlayersCount]);
			int[] posMoves = new int[movingPlayersCount];
			boolean[] crashes = new boolean[movingPlayersCount];
			
			int[] replayMoves = new int[players.length];
			
			//gather the moves and target positions and check for crashes
			for (int i = 0; i < movingPlayersCount; i++) {
				int m = playerMoves[i];
				
				//check for collisions
				if (playerMoves[i] != Board.MOVE_NONE) {
					posMoves[i] = board.getPositionFromPlayerMove(movingPlayers[i], m);
					for (int j = 0; j < i; j++) {
						if (playerMoves[j] != Board.MOVE_NONE && posMoves[i] == posMoves[j]) {
							crashes[i] = true;
							crashes[j] = true;
						}
					}
					
					if (recordReplay)
						replayMoves[movingPlayers[i].getNumber()] = m;
				}
				else {
					//no move -> crash
					crashes[i] = true;
				}
			}
			
			if (recordReplay)
				replay.appendTurn(replayMoves);
		
			
			//perform the moves, kill the crash dudes
			for (int i = 0; i < PLAYERMOVECOUNT; i++) {
				if (!crashes[i]) {
					int m = playerMoves[i];
					board.performMove(movingPlayers[i], m);
					getLogger().finest(String.format("%s goes %s", movingPlayers[i].getName(), m));
				}
			}

	}
	
}
