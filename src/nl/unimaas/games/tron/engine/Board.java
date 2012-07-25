package nl.unimaas.games.tron.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.unimaas.games.tron.player.Player;

public class Board implements Cloneable, Serializable {
	private static final long serialVersionUID = 1159491603141557778L;

	public final static byte EMPTY_FIELD = -1;
	public final static byte FILLED_FIELD = 11;
	public final static short MAX_BOARD_SIZE = 255;

	public final byte[][] board;
	private int[] playerPositions = new int[0];
	private HashMap<Integer, int[]> startConfigurations = new HashMap<Integer, int[]>();
	/** Name of this board */
	private String name = null;
	private final ArrayList<Integer> moves = new ArrayList<Integer>(4);

	public final static int MOVE_NONE = 0;
	public final static int MOVE_LEFT = -1;
	public final static int MOVE_RIGHT = 1;
	public final static int MOVE_UP = -2;
	public final static int MOVE_DOWN = 2;

	private static final int PLAYERCOUNT = 2;

	public Board(int w, int h) {
		board = new byte[w][h];
		setPlayerCount(2);
	}

	public Board(int w, int h, int playerCount) {
		board = new byte[w][h];
		setPlayerCount(playerCount);
	}

	public Board(byte[][] board) {
		this.board = board;
	}

	public Board(byte[][] board, int[] playerPositions) {
		this.board = board;
		this.playerPositions = playerPositions;
	}

	protected Board(Board board) {
		this.board = board.board;
		this.playerPositions = board.playerPositions;
	}

	public byte[][] getBoard() {
		return board;
	}

	public int getPlayerCount() {
		return playerPositions.length;
	}

	public int getPlayerPosition(Player p) {
		return playerPositions[p.getNumber()];
	}

	public int getPlayerPosition(int nr) {
		return playerPositions[nr];
	}

	public boolean isValidPosition(int x, int y) {
		return (x >= 0 && y >= 0 && x <= getWidth() && y <= getHeight());
	}

	public boolean isValidPlayerMove(int x, int y, Player p) {
		int pos = getPlayerPosition(p);
		return (getManhattanDistance(x, y, posToX(pos), posToY(pos)) == 1
				&& isValidPosition(x, y) && isEmpty(x, y));
	}

	public void performMove(Player p, int m) {
		int pos = getPlayerPosition(p);
		performMove(posToX(pos), posToY(pos), m, p.getNumber());
	}

	public void performMove(int playerNr, int m) {
		int pos = getPlayerPosition(playerNr);
		performMove(posToX(pos), posToY(pos), m, playerNr, true);
	}

	public void performMove(int playerNr, int m, boolean check) {
		int pos = getPlayerPosition(playerNr);
		performMove(posToX(pos), posToY(pos), m, playerNr, check);
	}

	public int getPositionFromPlayerMove(Player p, int m) {
		int pos = getPlayerPosition(p);
		return getPositionFromMove(posToX(pos), posToY(pos), m);
	}

	public void performMove(int x, int y, int m, int playerNr) {
		performMove(x, y, m, playerNr, true);
	}

	public void performMove(int x, int y, int m, int playerNr, boolean check) {
		switch (m) {
		case MOVE_LEFT:
			x = moveLeft(x);
			break;
		case MOVE_RIGHT:
			x = moveRight(x);
			break;
		case MOVE_UP:
			y = moveUp(y);
			break;
		case MOVE_DOWN:
			y = moveDown(y);
			break;
		}

		if (check) {
			if (isValidPosition(x, y))
				if (isEmpty(x, y))
					placePlayerWall(x, y, playerNr);
				else
					throw new IllegalStateException(String.format(
							"move position of P%d contains a wall (%d, %d) %s",
							playerNr, x, y, m));
			else
				throw new IllegalStateException(String.format(
						"invalid position after move to (%d, %d) %s", x, y, m));
		} else
			placePlayerWall(x, y, playerNr);
	}

	public void placeWall(int x, int y) {
		board[x][y] = FILLED_FIELD;
	}

	public void placePlayerWall(int x, int y, int playerNr) {
		board[x][y] = (byte) playerNr;
		playerPositions[playerNr] = xyToPos(x, y);
	}

	public void placePlayerWall(int x, int y, Player p) {
		placePlayerWall(x, y, p.getNumber());
	}

	public void removeWall(int x, int y) {
		board[x][y] = EMPTY_FIELD;
	}

	public boolean isEmpty(int x, int y) {
		return (board[x][y] == EMPTY_FIELD);
	}

	public boolean isWall(int x, int y) {
		return (board[x][y] != EMPTY_FIELD);
	}

	public boolean isPlayerWall(int x, int y) {
		return (board[x][y] != EMPTY_FIELD && board[x][y] != FILLED_FIELD);
	}

	public boolean isWallOfPlayer(int x, int y, int playerNr) {
		return (board[x][y] == playerNr);
	}

	public int getField(int x, int y) {
		return board[x][y];
	}

	public boolean isSuicideMove(int sx, int sy, int tx, int ty) {
		if (board[tx][ty] != EMPTY_FIELD)
			return true;
		if (sx != moveLeft(tx) && board[moveLeft(tx)][ty] == EMPTY_FIELD)
			return false;
		if (sx != moveRight(tx) && board[moveRight(tx)][ty] == EMPTY_FIELD)
			return false;
		if (sy != moveUp(ty) && board[tx][moveUp(ty)] == EMPTY_FIELD)
			return false;
		if (sy != moveDown(ty) && board[tx][moveDown(ty)] == EMPTY_FIELD)
			return false;
		return true;
	}

	public ArrayList<Integer> getValidPlayerMoves(Player p) {
		int pos = getPlayerPosition(p);
		return getValidMoves(posToX(pos), posToY(pos));
	}

	public ArrayList<Integer> getValidMoves(int x, int y) {
		moves.clear();

		if (board[moveLeft(x)][y] == EMPTY_FIELD)
			moves.add(MOVE_LEFT);
		if (board[x][moveUp(y)] == EMPTY_FIELD)
			moves.add(MOVE_UP);
		if (board[moveRight(x)][y] == EMPTY_FIELD)
			moves.add(MOVE_RIGHT);
		if (board[x][moveDown(y)] == EMPTY_FIELD)
			moves.add(MOVE_DOWN);
		return moves;
	}

	/** Excludes suicidial moves */
	public ArrayList<Integer> getValidMovesFiltered(int x, int y) {
		moves.clear();
		if (board[moveLeft(x)][y] == EMPTY_FIELD
				&& !isSuicideMove(x, y, moveLeft(x), y))
			moves.add(MOVE_LEFT);
		if (board[x][moveUp(y)] == EMPTY_FIELD
				&& !isSuicideMove(x, y, x, moveUp(y)))
			moves.add(MOVE_UP);
		if (board[moveRight(x)][y] == EMPTY_FIELD
				&& !isSuicideMove(x, y, moveRight(x), y))
			moves.add(MOVE_RIGHT);
		if (board[x][moveDown(y)] == EMPTY_FIELD
				&& !isSuicideMove(x, y, x, moveDown(y)))
			moves.add(MOVE_DOWN);
		return moves;
	}

	public ArrayList<Integer> getValidPlayerMovePositions(Player p) {
		int pos = getPlayerPosition(p);
		return getValidMovePositions(posToX(pos), posToY(pos));
	}

	public ArrayList<Integer> getValidMovePositions(int x, int y) {
		ArrayList<Integer> pos = moves;
		pos.clear();
		if (isEmpty(moveLeft(x), y))
			pos.add(xyToPos(moveLeft(x), y));
		if (isEmpty(x, moveUp(y)))
			pos.add(xyToPos(x, moveUp(y)));
		if (isEmpty(moveRight(x), y))
			pos.add(xyToPos(moveRight(x), y));
		if (isEmpty(x, moveDown(y)))
			pos.add(xyToPos(x, moveDown(y)));
		return pos;
	}

	public ArrayList<int[]> getValidMovePositionsXY(int x, int y) {
		ArrayList<int[]> pos = new ArrayList<int[]>(4);

		if (isEmpty(moveLeft(x), y))
			pos.add(new int[] { moveLeft(x), y });
		if (isEmpty(x, moveUp(y)))
			pos.add(new int[] { x, moveUp(y) });
		if (isEmpty(moveRight(x), y))
			pos.add(new int[] { moveRight(x), y });
		if (isEmpty(x, moveDown(y)))
			pos.add(new int[] { x, moveDown(y) });
		return pos;
	}

	public int getValidMoveCount(int x, int y) {
		int count = 0;
		if (isEmpty(moveLeft(x), y))
			count++;
		if (isEmpty(x, moveUp(y)))
			count++;
		if (isEmpty(moveRight(x), y))
			count++;
		if (isEmpty(x, moveDown(y)))
			count++;
		return count;
	}

	public ArrayList<Integer> getValidMovePositions(int x, int y,
			boolean filterSuicide) {
		ArrayList<Integer> pos = moves;
		pos.clear();
		if (isEmpty(moveLeft(x), y)
				&& (!filterSuicide || !isSuicideMove(x, y, moveLeft(x), y)))
			pos.add(xyToPos(moveLeft(x), y));
		if (isEmpty(x, moveUp(y))
				&& (!filterSuicide || !isSuicideMove(x, y, x, moveUp(y))))
			pos.add(xyToPos(x, moveUp(y)));
		if (isEmpty(moveRight(x), y)
				&& (!filterSuicide || !isSuicideMove(x, y, moveRight(x), y)))
			pos.add(xyToPos(moveRight(x), y));
		if (isEmpty(x, moveDown(y))
				&& (!filterSuicide || !isSuicideMove(x, y, x, moveDown(y))))
			pos.add(xyToPos(x, moveDown(y)));
		return pos;
	}

	/** Returns the number of walls adjacent to this cell */
	public int getWallCount(int x, int y) {
		int count = 0;
		if (!isEmpty(moveLeft(x), y))
			count++;
		if (!isEmpty(x, moveUp(y)))
			count++;
		if (!isEmpty(moveRight(x), y))
			count++;
		if (!isEmpty(x, moveDown(y)))
			count++;
		return count;
	}

	public int getFreeCount(int x, int y) {
		return 4 - getWallCount(x, y);
	}

	/** Prepares the board to handle n number of players */
	public void setPlayerCount(int count) {
		playerPositions = new int[count];
	}

	public void setStartPositions(HashMap<Integer, int[]> configurations) {
		startConfigurations = configurations;
	}

	public void setStartPositions(int playerCount, int[] positions) {
		startConfigurations.put(playerCount, positions);
	}

	public int[] getStartPositions(int playerCount) {
		return startConfigurations.get(playerCount);
	}

	public Integer[] getStartPositionConfigurations() {
		return startConfigurations.keySet().toArray(
				new Integer[startConfigurations.keySet().size()]);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * ..X .OX .XX etc.
	 * */
	public boolean isWallhugPosition(int x, int y) {
		if (getFreeCount(x, y) != 2)
			return false;
		else if (isEmpty(moveLeft(x), y) && isEmpty(moveRight(x), y))
			return false;
		else if (isEmpty(x, moveUp(y)) && isEmpty(x, moveDown(y)))
			return false;
		else
			return true;
	}

	/**
	 * Returns whether we are moving into a position which cuts us off from
	 * other move positions
	 */
	public boolean isIsolatingMove(int ox, int oy, int tx, int ty) {
		if (getFreeCount(ox, oy) == 1) // we are already isolated
			return false;
		// o-pos contains more than one free space

		if (getFreeCount(tx, ty) == 1) {
			// check for turns. turns are good for you
			if (ox != tx) {
				if (!isEmpty(tx, moveUp(ty)) && !isEmpty(tx, moveDown(ty)))
					return true;
			} else {
				if (!isEmpty(moveLeft(tx), ty) && !isEmpty(moveRight(tx), ty))
					return true;
			}
		}

		if (ox != tx) {
			if (!isEmpty(tx, moveUp(ty)) && isEmpty(ox, moveUp(oy)))
				return true;
			if (!isEmpty(tx, moveDown(ty)) && isEmpty(ox, moveDown(oy)))
				return true;
			return false;
		} else {
			if (!isEmpty(moveLeft(tx), ty) && isEmpty(moveLeft(ox), oy))
				return true;
			if (!isEmpty(moveRight(tx), ty) && isEmpty(moveRight(ox), oy))
				return true;
			return false;
		}
	}

	public int getFreeSpace(int x, int y) {
		byte[][] shadowBoard = Board.cloneArray(board);
		return spaceStep(shadowBoard, x, y);
	}

	public int getFreeSpace(int ox, int oy, int tx, int ty) {
		byte[][] shadowBoard = Board.cloneArray(board);
		shadowBoard[ox][oy] = FILLED_FIELD;
		return spaceStep(shadowBoard, tx, ty);
	}

	private int spaceStep(byte[][] shadowBoard, int x, int y) {
		shadowBoard[x][y] = FILLED_FIELD;
		int space = 1;
		if (board[moveLeft(x)][y] == EMPTY_FIELD)
			space += spaceStep(board, moveLeft(x), y);
		if (board[moveRight(x)][y] == EMPTY_FIELD)
			space += spaceStep(board, moveRight(x), y);
		if (board[x][y - 1] == EMPTY_FIELD)
			space += spaceStep(board, x, moveUp(y));
		if (board[x][y + 1] == EMPTY_FIELD)
			space += spaceStep(board, x, moveDown(y));
		return space;
	}

	public int getWidth() {
		return board.length;
	}

	public int getHeight() {
		return board[0].length;
	}

	/** Returns a new board with this board's byte array */
	public Board shallowClone() {
		return new Board(board);
	}

	public void print() {
		int x1 = Board.posToX(playerPositions[0]), x2 = Board
				.posToX(playerPositions[1]);
		int y1 = Board.posToY(playerPositions[0]), y2 = Board
				.posToY(playerPositions[1]);

		int w = board.length, h = board[0].length;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (x == x1 && y == y1)
					System.out.printf("P0 ");
				else if (x == x2 && y == y2)
					System.out.printf("P1 ");
				else
					System.out.printf("%2d ", board[x][y]);
			}
			System.out.println();
		}
	}

	/** Returns a new board with this board's byte array and properties */
	@Override
	public Board clone() {
		Board b = new Board(board);
		b.setStartPositions(startConfigurations);
		b.setName(name);
		b.playerPositions = playerPositions;
		return b;
	}

	/** Returns a new board with a new byte array based on the old one */
	public Board deepClone(boolean inheritProperties) {
		int w = board.length, h = board[0].length;
		byte[][] newBoard = new byte[w][];
		byte[] y;
		for (int x = 0; x < w; x++) {
			y = new byte[h];
			System.arraycopy(board[x], 0, y, 0, h);
			newBoard[x] = y;
		}
		int[] playerPositions = this.playerPositions.clone();
		Board b = new Board(newBoard, playerPositions);
		if (inheritProperties) {
			b.setStartPositions(startConfigurations);
			b.setName(name);
		}
		return b;
	}

	public void copyTo(Board b, boolean inheritProperties) {
		int w = board.length, h = board[0].length;
		if (b.getWidth() != w || b.getHeight() != h)
			throw new IllegalStateException("boards sizes do not match");

		byte[] y;
		for (int x = 0; x < w; x++) {
			y = b.board[x];
			System.arraycopy(board[x], 0, y, 0, h);
		}
		System.arraycopy(playerPositions, 0, b.playerPositions, 0, PLAYERCOUNT);

		if (inheritProperties) {
			b.setStartPositions(startConfigurations);
			b.setName(name);
		}
	}

	public byte[][] getClonedBoardArray() {
		int w = board.length;
		int h = board[0].length;
		byte[][] newBoard = new byte[w][];
		byte[] y;
		for (int x = 0; x < w; x++) {
			y = new byte[h];
			System.arraycopy(board[x], 0, y, 0, h);
			newBoard[x] = y;
		}
		return newBoard;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Board))
			return false;
		Board other = (Board) obj;
		if (other.getWidth() != getWidth() || other.getHeight() != getHeight())
			return false;
		for (int x = 0; x < getWidth(); x++)
			for (int y = 0; y < getHeight(); y++)
				if (board[x][y] != other.board[x][y])
					return false;
		return true;
	}

	public static Board fromFile(String file) throws FileNotFoundException {
		// file = file.toLowerCase();
		if (file.endsWith(".txt")) {
			// Google AI Tron map
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			try {
				// get map size
				String line = br.readLine();
				Pattern numPattern = Pattern.compile("\\d+");
				Matcher numMatcher = numPattern.matcher(line);
				numMatcher.find();
				int w = Integer.parseInt(numMatcher.group());
				numMatcher.find();
				int h = Integer.parseInt(numMatcher.group());
				final byte[][] board = new byte[w][h];
				int[] playerPositions = new int[2];
				// parse level
				for (int y = 0; y < h; y++) {
					line = br.readLine();
					if (line != null) {
						for (int x = 0; x < line.length(); x++) {
							char c = line.charAt(x);
							switch (c) {
							case ' ':
								board[x][y] = EMPTY_FIELD;
								break;
							case '#':
								board[x][y] = FILLED_FIELD;
								break;
							case '1':
								board[x][y] = EMPTY_FIELD;
								playerPositions[0] = Board.xyToPos(x, y);
								break;
							case '2':
								board[x][y] = EMPTY_FIELD;
								playerPositions[1] = Board.xyToPos(x, y);
								break;
							default:
								throw new IllegalStateException();
							}
						}
					} else
						break;
				}
				Board b;
				b = new Board(board);
				b.setPlayerCount(PLAYERCOUNT);
				b.setStartPositions(PLAYERCOUNT, playerPositions);
				b.setName(file);
				return b;
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println(ex);
				return null;
			} finally {
				try {
					fr.close();
				} catch (IOException e) {
				}
			}
		} else
			throw new IllegalArgumentException();
	}

	public int getMoveFromPositions(int ox, int oy, int tx, int ty) {
		if (ox != tx) {
			if (moveRight(ox) == tx) {
				return MOVE_RIGHT;
			} else {
				return MOVE_LEFT;
			}
		} else {
			if (moveDown(oy) == ty)
				return MOVE_DOWN;
			else
				return MOVE_UP;
		}
	}

	public int getPositionFromMove(int x, int y, int m) {
		if (m < 0) {
			if (m == Board.MOVE_LEFT) {
				return xyToPos(moveLeft(x), y);
			}

			else
				return xyToPos(x, moveUp(y));
		} else {
			if (m == Board.MOVE_RIGHT) {
				return xyToPos(moveRight(x), y);
			}

			else
				return xyToPos(x, moveDown(y));
		}
	}

	public int getManhattanDistance(int x1, int y1, int x2, int y2) {
		return getManhattanDistance(x1, y1, x2, y2, getWidth(), getHeight());
	}

	public static int getManhattanDistance(int x1, int y1, int x2, int y2,
			int w, int h) {
		if ((x1 == 0 && x2 == (w - 1)) || (x2 == 0 && x1 == (w - 1))) {
			return (1 + Math.abs(y1 - y2));
			/*
			 * } else if((y1 == 0 && y2 == (h-1)) || (y2 == 0 && y1 == (h-1))){
			 * return (Math.abs(x1 - x2) + 1);
			 */
		} else {
			return (Math.abs(x1 - x2) + Math.abs(y1 - y2));
		}
	}

	public static int posToX(int boardpos) {
		return boardpos / MAX_BOARD_SIZE;
	}

	public static int posToY(int boardpos) {
		return boardpos % MAX_BOARD_SIZE;
	}

	public static int xyToPos(int x, int y) {
		return x * MAX_BOARD_SIZE + y;
	}

	public static boolean hasAdjacentField(byte[][] board, int x, int y,
			byte field) {
		return (board[moveLeft(x, board.length)][y] == field
				|| board[moveRight(x, board.length)][y] == field
				|| board[x][moveUp(y, board[0].length)] == field || board[x][moveDown(
					y, board[0].length)] == field);
	}

	public static boolean hasAdjacentFieldSmallerThan(byte[][] board, int x,
			int y, byte field) {
		return (board[moveLeft(x, board.length)][y] < field
				|| board[moveRight(x, board.length)][y] < field
				|| board[x][moveUp(y, board[0].length)] < field || board[x][moveDown(
				y, board[0].length)] < field);
	}

	public static void printBoardArray(byte[][] board) {
		int w = board.length, h = board[0].length;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				System.out.printf("%2d ", board[x][y]);
			}
			System.out.println();
		}
	}

	public static void printBoardArray(short[][] board) {
		int w = board.length, h = board[0].length;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				System.out.printf("%5d ", board[x][y]);
			}
			System.out.println();
		}
	}

	public static void printBoardArray(long[][] board) {
		int w = board.length, h = board[0].length;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				System.out.printf("%5d ", board[x][y]);
			}
			System.out.println();
		}
	}

	public static void printBoardArray(double[][] board) {
		int w = board.length, h = board[0].length;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				System.out.printf("%5f ", board[x][y]);
			}
			System.out.println();
		}
	}

	public static byte[][] cloneArray(byte[][] source) {
		int w = source.length;
		int h = source[0].length;
		byte[][] newBoard = new byte[w][];
		byte[] y;
		for (int x = 0; x < w; x++) {
			y = new byte[h];
			System.arraycopy(source[x], 0, y, 0, h);
			newBoard[x] = y;
		}
		return newBoard;
	}

	public static int[][] cloneArray(int[][] source) {
		int w = source.length;
		int h = source[0].length;
		int[][] newBoard = new int[w][];
		int[] y;
		for (int x = 0; x < w; x++) {
			y = new int[h];
			System.arraycopy(source[x], 0, y, 0, h);
			newBoard[x] = y;
		}
		return newBoard;
	}

	public static String moveToString(int move) {
		switch (move) {
		case MOVE_NONE:
			return "NONE";
		case MOVE_LEFT:
			return "LEFT";
		case MOVE_RIGHT:
			return "RIGHT";
		case MOVE_UP:
			return "UP";
		case MOVE_DOWN:
			return "DOWN";
		default:
			return "???";
		}
	}

	public int moveLeft(int x) {
		return moveLeft(x, getWidth());
	}

	public static int moveLeft(int x, int w) {
		if (x == 0) {
			return w - 1;
		} else {
			return x - 1;
		}

	}

	public int moveRight(int x) {
		return moveRight(x, getWidth());
	}

	public static int moveRight(int x, int w) {
		if (x == w - 1) {
			return 0;
		} else {
			return x + 1;
		}
	}

	public int moveUp(int y) {
		return moveUp(y, getHeight());
	}

	public static int moveUp(int y, int h) {
		if (y == 0) {
			return 0; // h-1;
		} else {
			return y - 1;
		}

	}

	public int moveDown(int y) {
		return moveDown(y, getHeight());
	}

	public static int moveDown(int y, int h) {
		if (y == h - 1) {
			return h - 1; // 0
		} else {
			return y + 1;
		}

	}

	public static Board fromEntelectFile(String file)
			throws FileNotFoundException {
		Board result = null;
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		try {
			final byte[][] board = new byte[30][30];
			int[] playerPositions = new int[2];
			String line = br.readLine();
			while (line != null) {
				String[] parts = line.split(" ");
				if (parts.length != 3) {
					throw new Exception("Could not parse:" + line);
				}
				int x = Integer.parseInt(parts[0]);
				int y = Integer.parseInt(parts[1]);
				if (parts[2].equalsIgnoreCase("clear")) {
					board[x][y] = EMPTY_FIELD;
				} else if (parts[2].equalsIgnoreCase("you")) {
					playerPositions[0] = Board.xyToPos(x, y);
					board[x][y] = EMPTY_FIELD;
				} else if (parts[2].equalsIgnoreCase("opponent")) {
					playerPositions[1] = Board.xyToPos(x, y);
					board[x][y] = EMPTY_FIELD;
				} else if (parts[2].equalsIgnoreCase("yourwall")) {
					board[x][y] = 0;
				} else if (parts[2].equalsIgnoreCase("opponentwall")) {
					board[x][y] = 1;
				} else {
					board[x][y] = FILLED_FIELD;
				}
				line = br.readLine();
			}
			result = new Board(board);
			result.setPlayerCount(PLAYERCOUNT);
			result.setStartPositions(PLAYERCOUNT, playerPositions);
			result.setName(file);

		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println(ex);
			return null;
		}
		return result;

	}

	public void toEntelectFile(String file, boolean crossover) throws IOException {
		Writer output = new BufferedWriter(new FileWriter(file));
		try {
			for (int x = 0; x < getWidth(); x++) {
				for (int y = 0; y < getHeight(); y++) {
					int f = getField(x, y);
					String s = x + " " + y + " ";
					switch (f) {
					case EMPTY_FIELD:
						s += "Clear";
						break;
					case 0:
						
						if (getPlayerPosition(0) == Board.xyToPos(x, y)) {
							
							s += (crossover ? "Opponent" : "You"); // cross over so we don't need game
												// server
						} else {
							s += (crossover ? "OpponentWall" : "YourWall");
						}
						break;
					case 1:
						if (getPlayerPosition(1) == Board.xyToPos(x, y)) {
							s += (crossover ? "You" : "Opponent"); // cross over so we don't need game
										// server
						} else {
							s += (crossover ? "YourWall" : "OpponentWall");
						}
						break;
					default:
						break;
					}
					output.write(s + "\r\n");
				}

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			output.close();
		}
	}

}
