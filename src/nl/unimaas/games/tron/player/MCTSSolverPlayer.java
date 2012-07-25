package nl.unimaas.games.tron.player;

import java.awt.Color;
import java.util.Random;

import nl.unimaas.games.tron.engine.Board;
import nl.unimaas.games.tron.player.mcts.CutOffStrategy;
import nl.unimaas.games.tron.player.mcts.FinalNodeSelectionStrategy;
import nl.unimaas.games.tron.player.mcts.PlayoutStrategy;
import nl.unimaas.games.tron.player.mcts.SolverSelectionStrategy;

/** 2-player game */
@SuppressWarnings("serial")
public class MCTSSolverPlayer extends Player {
	private final static boolean DEBUG = true;
	private final static double SIM_WIN = 1, SIM_DRAW = 0, SIM_LOSS = -1;
	private static final int UNTESTED = -3;
	private long MAX_TURN_TIME = 1000;
	
	protected static final Random random = new Random(1000);
	
	//protected SolverNode root;
	protected SolverSelectionStrategy select; //SolverSelectionStrategy.RANDOM;
	protected PlayoutStrategy playout = PlayoutStrategy.RANDOM;
	protected FinalNodeSelectionStrategy finalMove = FinalNodeSelectionStrategy.SECURE_CHILD;
	protected CutOffStrategy cutOff = CutOffStrategy.SPACE;
	protected boolean enableCutOffs = false; //(cutOff != null);
	
	public MCTSSolverPlayer(String name, int num, Color c) {
		super(name, num, c);
	}

	@Override
	protected int computeMove(Board b, long endTime) {
		return 0;
		/*try {
			long start = System.currentTimeMillis();
			if (DEBUG)
				System.out.println();
			System.out.println();
			if (root == null) {//first move of the game 
				root = createNode(b, 1 - getNumber(), null, Board.MOVE_NONE);
				onComputeFirstMove(root);
			}
			
			if (!root.board.equals(b))
				throw new IllegalStateException("root board does not match the current board");
			
			if (endTime == 0)
				endTime = System.currentTimeMillis() + MAX_TURN_TIME;
			
			int move = MCTSSolver(root, Math.min(endTime - 5, System.currentTimeMillis() + MAX_TURN_TIME));
			if (DEBUG)
				System.out.println("PERFORM MOVE " + Board.moveToString(move) + " (in " + (System.currentTimeMillis() - start) + " ms)");
			System.out.println("Time " + getNumber() + ": " + (System.currentTimeMillis() - start));
			return move;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("error in MCTSSolverPlayer! " + e.getMessage());
			return Board.MOVE_NONE;
		}*/
	}
	
	@Override
	public synchronized void onPlayerMove(final Board b, int playerNr, int move) {
		/*if (playerNr == getNumber()) 
			return;
		
		if (root.children.size() == 0)
			throw new IllegalStateException("root has no children");
		for (int i = 0; i < root.children.size(); i++) {
			SolverNode child = (SolverNode) root.children.get(i);
			if (child.move == move) {
				child.parent = null;
				root = child;
				if (!root.board.equals(b)) {
					root.board.print();
					b.print();
					throw new IllegalStateException("boards do not match");
				}
				return;
			}
		}
		throw new IllegalStateException("cannot move to the correct state");*/
	}
	
//	protected int MCTSSolver(SolverNode root, long endTime) {
//		//System.out.println("I have " + (endTime - System.currentTimeMillis()) + " ms to compute a result");
//		if (endTime - System.currentTimeMillis() < 100)
//			System.err.println("MCTS: I have less than 100 ms to compute a result!");
//		SolverNode current;
//		int its = 0, proofIts = 0, cuts = 0;
//	
//		/*if (enableCutOffs && root.prediction != UNTESTED && root.children.isEmpty()) {
//			enableCutOffs = false;
//		}*/
//		
//		while (System.currentTimeMillis() < endTime) {
//			current = root;
//			
//			if (root.proven) {
//				System.out.println("Not going to bother with this proven root");
//				break;
//			}
//			
//			while (!current.isLeaf() && current.expanded) {
//				SolverNode prev = current;
//				
//				current = select(current);
//				if (current == null)
//					current = select(prev);
//			}
//			
//			//check for a possible game cut off
//			double r = 0;
//			expand(current);
//
//			
//			if (current.expanded) { //always true (for now)
//				if (current.children.isEmpty()) {
//					// terminal state //
//					
//					//the other player has no more moves
//					int myPos = current.board.getPlayerPosition(current.playerNr);
//					int otherPos = current.board.getPlayerPosition(1 - current.playerNr);
//					if (myPos == otherPos) {
//						r = 0; //crash -> draw
//					}
//					else {
//						//check if we have any moves left
//						if (current.board.getValidMoveCount(Board.posToX(myPos), Board.posToY(myPos)) == 0)
//							r = 0; //both players are stuck -> draw
//						else
//							r = 1; //we can still move -> win
//					}
//					
//					if (current.isMin())
//						r *= -1;
//					
//					current.pess = r;
//					current.opti = r;
//					current.proven = true;
//					propagatePessimisticBound(current);
//					propagateOptimisticBound(current);
//					//update the 'proven' field
//					SolverNode cur = current;
//					do {
//						if (!cur.checkIfProven())
//							break;
//						cur = (SolverNode) cur.parent;
//					} while (cur != null);
//					proofIts++;
//				}
//				else {
//					current = select(current);
//					//simulation starts from one of our moves, player 2 moves first
//					r = simulate(current);
//					its++;
//				}
//			}
//
//			/* Backpropagation phase*/
//			do {
//				current.value += r;
//				if (r == 0)
//					current.draws++;
//				else if (r > 0)
//					current.wins++;
//
//				r *= -1;
//				current.visits++;
//				current = (SolverNode) current.parent;
//			} while (current != null);
//		}
//		
//		if (DEBUG) {
//			System.out.println("Simulations: " + its + "  End-node passes: " + proofIts + " Cuts: " + cuts);
//			System.out.println("Root visits: " + root.visits);
//			System.out.println("Root children: ");
//			root.printChildren();
//		}
//		root = (SolverNode) finalMove.getNode(root);
//		if (root == null)
//			System.err.println("Got null from finalMove.getNode(root)!");
//		this.root = root;
//		if (root.proven)
//			System.err.println("Root proven!");
//		onFinishComputation(root);
//		//discard the upper part of the tree
//		root.parent = null;
//		if (DEBUG)
//			System.out.println("Best node: " + root);
//		return root.move;
//	}
//	
//	/** Selects a child node of the current node */
//	protected SolverNode select(SolverNode node) {
//		return (SolverNode) select.getNode(node);
//	}
//	
//	protected void expand(SolverNode node) {
//		if (!node.expanded) {
//			//the other player performs a move
//			int myNr = 1 - node.playerNr;
//			node.expanded = true;
//			//pre-create all nodes
//			int myPos = node.board.getPlayerPosition(myNr);
//			int myX = Board.posToX(myPos), myY = Board.posToY(myPos);
//
//			ArrayList<Integer> moves = node.board.getValidMoves(myX, myY);
//			for (int m : moves) {
//				Board newBoard = node.board.deepClone(false);
//				newBoard.performMove(myNr, m, false);
//				SolverNode n = createNode(newBoard, myNr, node, m);
//				node.children.add(n);
//			}
//			
//			//since we moved first, player 2 might be able turn the game into a draw
//			if (myNr != getNumber()) {
//				int otherPos = node.board.getPlayerPosition(1 - myNr);
//				int otherX = Board.posToX(otherPos), otherY = Board.posToY(otherPos);
//				if (Board.getManhattanDistance(myX, myY, otherX, otherY) == 1) {
//					int m = Board.getMoveFromPositions(myX, myY, otherX, otherY);
//					Board newBoard = node.board.deepClone(false);
//					newBoard.performMove(myNr, m, false);
//					SolverNode n = createNode(newBoard, myNr, node, m);
//					n.expanded = true; //game end
//					node.children.add(n);
//				}
//			}
//		}
//	}
//	
//	/** Simulates a game from the viewpoint of the player of the given node */
//	protected int simulate(final SolverNode node) {
//		//Board board = node.board.deepClone(false);
//		Board board = node.board.deepClone(false);
//		
//		int myMove, otherMove;
//		
//		int myNr = node.playerNr;
//		int myPos = board.getPlayerPosition(myNr);
//		int myX = Board.posToX(myPos), myY = Board.posToY(myPos);
//		//get position
//		int otherNr = 1 - node.playerNr;
//		int otherPos = board.getPlayerPosition(otherNr);
//		int otherX = Board.posToX(otherPos), otherY = Board.posToY(otherPos);
//
//		if (myNr == getNumber()) {
//			//player 2 still has to perform a move first
//			otherMove = playout.getMove(board, otherX, otherY, otherNr);
//			
//			if (otherMove == Board.MOVE_NONE) {
//				//check if he could've moved to our square, as that would make it a tie since it's his only option
//				if (Board.getManhattanDistance(myX, myY, otherX, otherY) == 1)
//					return 0;
//				else
//					return 1; //he's stuck
//			}
//
//			otherPos = board.getPositionFromMove(otherX, otherY, otherMove);
//			if (myPos == otherPos)
//				return 0; //tie
//			board.performMove(otherX, otherY, otherMove, otherNr, false);
//			otherX = Board.posToX(otherPos);
//			otherY = Board.posToY(otherPos);
//		}
//		
//		while (true) {
//			myMove = playout.getMove(board, myX, myY, myNr);
//			otherMove = playout.getMove(board, otherX, otherY, otherNr);
//			
//			if (myMove == Board.MOVE_NONE) {
//				if (otherMove == Board.MOVE_NONE) 
//					return 0; //both players did not move
//				else
//					return -1; //I did not move
//			}
//			else if (otherMove == Board.MOVE_NONE)
//				return 1; //other did not move
//			
//			myPos = board.getPositionFromMove(myX, myY, myMove);
//			otherPos = board.getPositionFromMove(otherX, otherY, otherMove);
//			
//			if (myPos == otherPos)
//				return 0;
//			
//			//perform moves
//			board.performMove(myX, myY, myMove, myNr, false);
//			board.performMove(otherX, otherY, otherMove, otherNr, false);
//			
//			//update positions
//			myX = Board.posToX(myPos);
//			myY = Board.posToY(myPos);
//			otherX = Board.posToX(otherPos);
//			otherY = Board.posToY(otherPos);
//		}
//	}
//	
//	private void propagatePessimisticBound(SolverNode s) {
//		if (!s.isRoot()) {
//			SolverNode n = (SolverNode) s.parent;
//			double oldPess = n.pess;
//			if (oldPess < s.pess) {
//				if (n.isMax()) {
//					//n.pess = s.pess;
//					n.setPess(s.pess);
//					propagatePessimisticBound(n);
//				}
//				else {
//					double minPess = Double.POSITIVE_INFINITY;
//					for (int i = 0; i < n.children.size(); i++) {
//						SolverNode c = (SolverNode) n.children.get(i);
//						if (c.pess < minPess)
//							minPess = c.pess;
//					}
//					//n.pess = minPess;
//					n.setPess(minPess);
//					if (oldPess > n.pess) 
//						propagatePessimisticBound(n);
//				}
//			}
//		}
//	}
//	
//	private void propagateOptimisticBound(SolverNode s) {
//		if (!s.isRoot()) {
//			SolverNode n = (SolverNode) s.parent;
//			double oldOpti = n.opti;
//			if (oldOpti > s.opti) {
//				if (n.isMax()) {
//					double maxOpti = Double.NEGATIVE_INFINITY;
//					for (int i = 0; i < n.children.size(); i++) {
//						SolverNode c = (SolverNode) n.children.get(i);
//						if (c.opti > maxOpti)
//							maxOpti = c.opti;
//					}
//					//n.opti = maxOpti;
//					n.setOpti(maxOpti);
//					if (oldOpti > n.opti)
//						propagateOptimisticBound(n);
//				}
//				else {
//					//n.opti = s.opti;
//					n.setOpti(s.opti);
//					propagateOptimisticBound(n);
//				}
//			}
//		}
//	}
//	
//	protected void onComputeFirstMove(SolverNode theRoot) {}
//	
//	protected void onFinishComputation(SolverNode newRoot) {}
//	
//	protected SolverNode createNode(Board b, int player, SolverNode parent, int move) {
//		SolverNode n;
//		boolean max = (player == getNumber());
//		if (parent == null)
//			n = new SolverNode(b, player, max);
//		else
//			n = new SolverNode(b, parent, player, move, max);
//		return n;
//	}
//	
//	@Override
//	public void reset() {
//		root = null;
//		//enableCutOffs = (cutOff != null);
//	}
//	
//	public static class SolverNode extends Node {
//		public double pess, opti;
//		public boolean proven = false;
//		
//		public boolean isProvenWin() {
//			if (max)
//				return (proven && pess >= 1.0);
//			else
//				return (proven && pess <= -1.0);
//		}
//		
//		public boolean isProvenDraw() {
//			return (proven && pess == 0.0);
//		}
//		
//		public boolean isProvenLoss() {
//			if (max)
//				return (proven && pess <= -1.0);
//			else
//				return (proven && pess >= 1.0);
//		}
//		
//		public SolverNode(final Board b, int playerNr, boolean max) {
//			super(b, playerNr, max);
//			init();
//		}
//		
//		public SolverNode(final Board b, SolverNode parent, int playerNr, int move, boolean max) {
//			super(b, parent, playerNr, move, max);
//			init();
//		}
//		
//		private void init() {
//			pess = -1;
//			opti = 1;
////			if (max) {
////				pess = -1;
////				opti = 1;
////			}
////			else {
////				pess = 1;
////				opti = -1;
////			}
//		}
//		
//		public boolean checkIfProven() {
//			proven = (pess == opti);
//			if (!proven) {
//				for (int i = 0; i < children.size(); i++)
//					if (!((SolverNode) children.get(i)).proven)
//						return false;
//			}
//			proven = true;
//			return proven;
//		}
//		
//		public void setPess(double p) {
//			pess = p;
//			if (Double.isInfinite(p)) {
//				System.out.println(p);
//				throw new IllegalStateException();
//			}
//		}
//		
//		public void setOpti(double p) {
//			opti = p;
//			if (Double.isInfinite(p))
//				throw new IllegalStateException();
//		}
//		
//		@Override
//		public String toString() {
//			return String.format("node[id=%d, move=%s, player=%d, visits=%d, wins=%.1f%% (%d), draws=%.1f%% (%d), value=%.2f, children=%d, proven=%s (pess=%.0f, opti=%.0f)]", id,  Board.moveToString(move), playerNr, visits, wins / (float) visits * 100, wins, draws / (float) visits * 100, draws, value, children.size(), Boolean.toString(proven), pess, opti);
//		}
	//}
}