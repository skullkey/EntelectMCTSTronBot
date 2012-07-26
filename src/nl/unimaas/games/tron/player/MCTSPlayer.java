package nl.unimaas.games.tron.player;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Stack;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.w3c.dom.NamedNodeMap;

import nl.unimaas.games.tron.engine.Board;
import nl.unimaas.games.tron.engine.Evaluator;
import nl.unimaas.games.tron.player.mcts.CutOffStrategy;
import nl.unimaas.games.tron.player.mcts.FinalNodeSelectionStrategy;
import nl.unimaas.games.tron.player.mcts.PlayoutStrategy;
import nl.unimaas.games.tron.player.mcts.SelectionStrategy;
import nl.unimaas.games.tron.util.GifSequenceWriter;

@SuppressWarnings("serial")
public class MCTSPlayer extends Player {
	private boolean DEBUG = false;
	private final static boolean GFX_DEBUG = false;
	private final static double SIM_WIN = 1, SIM_DRAW = 0, SIM_LOSS = -1;
	private static final int UNTESTED = -3;
	private long MAX_TURN_TIME = 1000;
	private int turn = 0;
	private ArrayList<BufferedImage> debugImgs;
	
	protected Node root;
	protected Board rootBoard;
	protected Board currentBoard;
	
	protected double[][] winsBoard;
	protected double[][] visitsBoard;

	protected SelectionStrategy select = SelectionStrategy.RANDOM;
	protected PlayoutStrategy playout = PlayoutStrategy.RANDOM;
	protected FinalNodeSelectionStrategy finalMove = FinalNodeSelectionStrategy.SECURE_CHILD;
	protected CutOffStrategy cutOff = null;
	protected boolean enableCutOffs = false; //(cutOff != null);
	
	public MCTSPlayer(String name, int num, Color c) {
		super(name, num, c);
		//DEBUG = (num == 0);
	}

	@Override
	protected int computeMove(Board b, long endTime) {
		turn++;
		try {
			long start = System.currentTimeMillis();
			if (DEBUG)
				System.out.println();
			if (root == null) {//first move of the game 
				if (GFX_DEBUG) {
					debugImgs = new ArrayList<BufferedImage>();
				}
				root = createNode(1 - getNumber(), null, Board.MOVE_NONE, b);
				onComputeFirstMove(root);
			}
			rootBoard = b;
			
			/*if (!root.board.equals(b)) {
				throw new IllegalStateException("root board does not match the current board");
			}*/
			
			if (endTime == 0)
				endTime = System.currentTimeMillis() + MAX_TURN_TIME;
			
			int move = MCTS(root, endTime);
			if (DEBUG) {
				int pos = b.getPlayerPosition(getNumber());
				System.out.println("PERFORM MOVE " + Board.moveToString(move) + " (in " + (System.currentTimeMillis() - start) + " ms) FROM (" + Board.posToX(pos) + ", " + Board.posToY(pos) + ")");
				System.out.println("Time " + getNumber() + ": " + (System.currentTimeMillis() - start));
			}
			return move;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("error in MCTSPlayer! " + e.getMessage());
			return Board.MOVE_NONE;
		}
	}
	
	@Override
	public synchronized void onPlayerMove(final Board b, int playerNr, int move) {
		if (playerNr == getNumber()) 
			return;
		
		if (root.children.isEmpty())
			throw new IllegalStateException("root has no children");
		for (int i = 0; i < root.children.size(); i++) {
			Node child = root.children.get(i);
			if (child.move == move) {
				child.parent.apply(rootBoard);
				child.parent = null;
				root = child;
				child.apply(rootBoard);
				if (!rootBoard.equals(b)) {
					rootBoard.print();
					b.print();
					throw new IllegalStateException("boards do not match");
				}
				return;
			}
		}
		throw new IllegalStateException("cannot move to the correct state");
	}

	protected int MCTS(Node root, long endTime) {
		if (endTime - System.currentTimeMillis() < 100)
			System.err.println("MCTS: I have less than 100 ms to compute a result!");
		Node current;
		currentBoard = rootBoard.deepClone(false);
		int its = 0, endIts = 0, cuts = 0;
		double r;
	
		if (enableCutOffs && root.prediction != UNTESTED && root.children.isEmpty()) {
			enableCutOffs = false;
		}
		
		while (System.currentTimeMillis() < endTime) {
			current = root;
			rootBoard.copyTo(currentBoard, false);
			
			while (!current.isLeaf() && (current.expanded || (enableCutOffs && current.prediction != UNTESTED))) {
				current = select(current);
			}
			
			//check for a possible game cut off
			r = 0;
			if (enableCutOffs) {
				current.connected = false;
				int gameEnd = CutOffStrategy.UNDECIDED;
				if (current.prediction == UNTESTED) {
					if (current.playerNr != getNumber())
						gameEnd = cutOff.getEnding(current, currentBoard);

					if (gameEnd == CutOffStrategy.UNDECIDED)
						expand(current, currentBoard);
					else {
						r = gameEnd;
						cuts++;
					}
					current.prediction = gameEnd;
				}
				else
					r = current.prediction;
			}
			else {
				expand(current, currentBoard);
			}
			
			if (current.expanded) {
				if (current.children.isEmpty()) {
					// terminal state //
					
					//the other player has no more moves
					int myPos = currentBoard.getPlayerPosition(current.playerNr);
					int otherPos = currentBoard.getPlayerPosition(1 - current.playerNr);
					if (myPos == otherPos)
						r = SIM_DRAW; //crash -> draw
					else
						//check if we have any moves left
						if (currentBoard.getValidMoveCount(Board.posToX(myPos), Board.posToY(myPos)) == 0)
							r = SIM_DRAW; //both players are stuck -> draw
						else
							r = SIM_WIN; //we can still move -> win
					endIts++;
				}
				else {
					current = select(current);
					//simulation starts from one of our moves, player 2 moves first
					r = simulate(current, currentBoard);
					its++;
				}
			}
			
			do {
				current.value += r;
				if (current.isMax()) {
					if (r == SIM_DRAW)
						current.draws++;
					else if (r > SIM_DRAW)
						current.wins++;
				}
				else {
					if (r == SIM_DRAW)
						current.draws++;
					else if (r < SIM_DRAW)
						current.wins++;
				}
				

				current.visits++;
				current = current.parent;
				r *= -1;
			} while (current.parent != null);
			root.visits++;
		}
		
		if (DEBUG) {
			System.out.println("Simulations: " + its + "  Terminal node passes: " + endIts + " Cuts: " + cuts);
			System.out.println("Root visits: " + root.visits);
			System.out.println("Root children: ");
			root.printChildren();
		}
		if (GFX_DEBUG) {
			int w = rootBoard.getWidth(), h = rootBoard.getHeight();
			int scale = 32;
			winsBoard = new double[w][h];
			visitsBoard = new double[w][h];
			Board b = rootBoard.deepClone(false);
			createDebugBoard(root, b);
			
			//String file = "output" + File.separator + "mcts" + getNumber() + " (" + turn + ").gif";
			
			BufferedImage debugImg = new BufferedImage(w * 32, h * 32, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = debugImg.createGraphics();
			//get max
			double[][] debugBoard = visitsBoard;
			double max = Double.NEGATIVE_INFINITY;
			for (int x = 0; x < w; x++)
				for (int y = 0; y < h; y++)
					if (debugBoard[x][y] > max)
						max = debugBoard[x][y];
			//max = 1;
			for (int x = 0; x < w; x++) {
				for (int y = 0; y < h; y++) {
					Color color;
					if (rootBoard.isWall(x, y))
						color = Color.BLACK;
					else {
						double factor = debugBoard[x][y] / max;
						if (factor > 0)
							color = blend(Color.RED, Color.GREEN, factor);
						else
							color = Color.GRAY;
					}
					g2.setColor(color);
				    g2.fillRect(x * scale, y * scale, (int) scale, (int) scale);
				}
			}
			//Board.printBoardArray(winsBoard);
			g2.dispose();
			debugImg.flush();
			debugImgs.add(debugImg);

			/*
			try {
			    File outputfile = new File(file);
			    ImageIO.write(debugImg, "gif", outputfile);
			} catch (Exception e) {
			    e.printStackTrace();
			}*/
		}
		
		root = finalMove.getNode(root);
		if (root == null)
			System.err.println("Got null from finalMove.getNode(root)!");
		this.root = root;
		onFinishComputation(root);
		//discard the upper part of the tree
		root.parent = null;
		if (DEBUG)
			System.out.println("Best node: " + root);
		return root.move;
	}
	
	private void createDebugBoard(Node n, Board b) {
		if (n.parent != null)
			n.apply(b);
		int mypos = b.getPlayerPosition(n.playerNr);
		double visits = visitsBoard[Board.posToX(mypos)][Board.posToY(mypos)];
		double wins = winsBoard[Board.posToX(mypos)][Board.posToY(mypos)];
		if (visits + n.visits > 0) {
			winsBoard[Board.posToX(mypos)][Board.posToY(mypos)] = (wins * visits + n.wins) / ((double) visits + n.visits);
		}
		visitsBoard[Board.posToX(mypos)][Board.posToY(mypos)] += n.visits;
		for (Node c : n.children) {
			createDebugBoard(c, b.deepClone(false));
		}
	}
	
	/** Selects a child node of the current node */
	protected Node select(Node node) {
		Node n = select.getNode(node);
		n.apply(currentBoard);
		return n;
	}
	
	protected void expand(Node node, Board board) {
		if (!node.expanded) {
			node.expanded = true;
			//the other player performs a move
			
			int myNr = 1 - node.playerNr; //player making the move
			//pre-create all nodes
			int myPos = board.getPlayerPosition(myNr);
			int myX = Board.posToX(myPos), myY = Board.posToY(myPos);

			ArrayList<Integer> moves = board.getValidMoves(myX, myY);
			for (int m : moves) {
				Node n = createNode(myNr, node, m, board);
				node.children.add(n);
			}
			
			//since we moved first, the current player (2) might be able turn the game into a draw
			if (myNr != getNumber()) {
				int otherNr = node.playerNr;
				int otherPos = board.getPlayerPosition(otherNr);
				int otherX = Board.posToX(otherPos), otherY = Board.posToY(otherPos);
				if (board.getManhattanDistance(myX, myY, otherX, otherY) == 1) {
					int m = board.getMoveFromPositions(myX, myY, otherX, otherY);
					Node n = createNode(myNr, node, m, board);
					n.expanded = true;
					node.children.add(n);
				}
			}
		}
	}
	
	/** Simulates a game from the viewpoint of the player of the given node */
	protected double simulate(Node node, Board simBoard) {	
		/*if (GFX_DEBUG) {
			int mypos = currentBoard.getPlayerPosition(node.playerNr);
			debugBoard[Board.posToX(mypos)][Board.posToY(mypos)]++;
		}*/
		int myMove, otherMove;
		
		int myNr = node.playerNr;
		int myPos = simBoard.getPlayerPosition(myNr);
		int myX = Board.posToX(myPos), myY = Board.posToY(myPos);
		//get position
		int otherNr = 1 - node.playerNr;
		int otherPos = simBoard.getPlayerPosition(otherNr);
		int otherX = Board.posToX(otherPos), otherY = Board.posToY(otherPos);

		if (myNr == getNumber()) {
			//player 2 still has to perform a move first
			otherMove = playout.getMove(simBoard, otherX, otherY, otherNr);
			
			if (otherMove == Board.MOVE_NONE) {
				//check if he could've moved to our square, as that would make it a tie since it's his only option
				if (simBoard.getManhattanDistance(myX, myY, otherX, otherY) == 1)
					return SIM_DRAW;
				else
					return SIM_WIN; //he's stuck
			}

			otherPos = simBoard.getPositionFromMove(otherX, otherY,otherMove);
			if (myPos == otherPos)
				return SIM_DRAW; //tie
			simBoard.performMove(otherX, otherY, otherMove, otherNr, false);
			otherX = Board.posToX(otherPos);
			otherY = Board.posToY(otherPos);
		}
		
		while (true) {
			myMove = playout.getMove(simBoard, myX, myY, myNr);
			otherMove = playout.getMove(simBoard, otherX, otherY, otherNr);
			
			if (myMove == Board.MOVE_NONE) {
				if (otherMove == Board.MOVE_NONE) 
					return SIM_DRAW; //both players did not move
				else
					return SIM_LOSS; //I did not move
			}
			else if (otherMove == Board.MOVE_NONE)
				return SIM_WIN; //other did not move
			
			myPos = simBoard.getPositionFromMove(myX, myY, myMove);
			otherPos = simBoard.getPositionFromMove(otherX, otherY, otherMove);
			
			if (myPos == otherPos)
				return SIM_DRAW;
			
			//perform moves
			simBoard.performMove(myX, myY, myMove, myNr, false);
			simBoard.performMove(otherX, otherY, otherMove, otherNr, false);
			
			//update positions
			myX = Board.posToX(myPos);
			myY = Board.posToY(myPos);
			otherX = Board.posToX(otherPos);
			otherY = Board.posToY(otherPos);
		}
	}
	
	protected void onComputeFirstMove(Node theRoot) {}
	
	protected void onFinishComputation(Node newRoot) {}
	
	protected Node createNode(int player, Node parent, int move, Board b) {
		Node n;
		boolean max = (player == getNumber());
		if (parent == null)
			n = new Node(player, max);
		else
			n = new Node(parent, player, move, max);
		return n;
	}
	
	@Override
	public void reset() {
		if (GFX_DEBUG && debugImgs != null && !debugImgs.isEmpty()) {
			ImageOutputStream output;
			try {
				output = new FileImageOutputStream(new File("output" + File.separator + "mcts" + getNumber() + ".gif"));
				GifSequenceWriter writer = new GifSequenceWriter(output, debugImgs.get(0).getType(), 1000, true);
				for (int i = 0; i < debugImgs.size(); i++) {
					BufferedImage nextImage = debugImgs.get(i);
					writer.writeToSequence(nextImage);
				}
				writer.close();
			    output.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			debugImgs = null;
		}
		
		turn = 0;
		root = null;
		//enableCutOffs = (cutOff != null);
	}
	
	public static class Node implements Serializable {
		public Node parent;
		public final ArrayList<Node> children = new ArrayList<Node>(3);
		//the move that was performed to get to this state
		public final int move;
		public final int playerNr;
		public final boolean max;
		
		public boolean expanded = false;
		public double value = 0;
		public int visits = 0;
		public int wins = 0, draws = 0;
		public long id = 0;
		public Evaluator tag;
		public double playerSpaceDiff = Double.NaN;
		public boolean connected = true;
		public int prediction = UNTESTED;
		public static long lastId = 0;
		
		public Node(int playerNr, boolean max) {
			this.parent = null;
			this.move = Board.MOVE_NONE;
			this.playerNr = playerNr;
			this.max = max;
			id = lastId;
			lastId++;
		}
		
		public Node(Node parent, int playerNr, int move, boolean max) {
			this.parent = parent;
			this.move = move;
			this.playerNr = playerNr;
			this.max = max;
			id = lastId;
			lastId++;
		}
		
		public boolean isRoot() {
			return (parent == null);
		}
		
		public boolean isLeaf() {
			return (children.size() == 0);
		}
		
		public boolean isMax() {
			return (max);
		}
		
		public boolean isMin() {
			return (!max);
		}
		
		public void apply(Board board) {
			board.performMove(playerNr, move, false);
		}
		
		public void revert(Board board) {
			int undoMove = move * -1;
			board.performMove(playerNr, undoMove, false);
		}
		
		/** Convenience method, for debugging purposes */
		public void applyFromRoot(Board board) {
			Stack<Node> path = new Stack<Node>();
			Node current = parent;
			while (current.parent != null) {
				path.push(current);
				current = current.parent;
			}
			while (!path.isEmpty()) {
				current = path.pop();
				current.apply(board);
			}
		}
		
		public void revertToRoot(Board board) {
			revert(board);
			if (parent != null)
				parent.revertToRoot(board);
		}
		
		@Override
		public String toString() {
			return String.format("node[id=%d, move=%s, player=%d, visits=%d, wins=%.1f%% (%d), draws=%.1f%% (%d), value=%.2f, children=%d]", id,  Board.moveToString(move), playerNr, visits, wins / (float) visits * 100, wins, draws / (float) visits * 100, draws, value, children.size());
		}
		
		public void printChildren() {
			for (int i = 0; i < children.size(); i++)
				System.out.println("	" + children.get(i) + ", ");
		}
	}
	
	public static Player fromXml(org.w3c.dom.Node playerRoot, int num, Color color) {
		return fromXml(playerRoot, new MCTSPlayer(MCTSPlayer.class.getSimpleName(), num, color));
	}
	public static Player fromXml(org.w3c.dom.Node playerRoot, MCTSPlayer player) {
		NamedNodeMap attrs = playerRoot.getAttributes();
		player.description = "";
		if (attrs.getNamedItem("finalMove") != null) {
			try {
				Field field = FinalNodeSelectionStrategy.class.getField(attrs.getNamedItem("finalMove").getNodeValue());
				player.finalMove = (FinalNodeSelectionStrategy) field.get(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (attrs.getNamedItem("selection") != null) {
			try {
				Field field = SelectionStrategy.class.getField(attrs.getNamedItem("selection").getNodeValue());
				player.select = (SelectionStrategy) field.get(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (attrs.getNamedItem("playout") != null) {
			try {
				Field field = PlayoutStrategy.class.getField(attrs.getNamedItem("playout").getNodeValue());
				player.playout = (PlayoutStrategy) field.get(null);
				player.description += String.format("playout=%s", field.getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (attrs.getNamedItem("cut") != null) {
			try {
				Field field = CutOffStrategy.class.getField(attrs.getNamedItem("cut").getNodeValue());
				player.cutOff = (CutOffStrategy) field.get(null);
				player.enableCutOffs = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (attrs.getNamedItem("maxTurnTime") != null) {
			try {
				player.MAX_TURN_TIME = Long.parseLong(attrs.getNamedItem("maxTurnTime").getNodeValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		player.description = String.format("select=%s playout=%s cut=%s final=%s", player.select, player.playout, player.cutOff, player.finalMove);
		return player;
	}
	
	@Override
	public MCTSPlayer clone() {
		MCTSPlayer player = new MCTSPlayer(getName(), getNumber(), getColor());
		player.select = select;
		player.finalMove = finalMove;
		player.cutOff = cutOff;
		player.playout = playout;
		player.description = description;
		return player;
	}
	
	 public static Color blend(Color c1, Color c2, double v) {
		    double v2 = 1 - v;
		    return c1 == null ? (c2 == null ? null : c2) :
		           c2 == null ? c1 :
		           new Color(Math.min(255, (int) (c1.getRed() * v2 + c2.getRed() * v)),
		                     Math.min(255, (int) (c1.getGreen() * v2 + c2.getGreen() * v)),
		                     Math.min(255, (int) (c1.getBlue() * v2 + c2.getBlue() * v)));
		  }
}
