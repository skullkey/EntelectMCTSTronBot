package nl.unimaas.games.tron.player;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;

import org.w3c.dom.NamedNodeMap;

import nl.unimaas.games.tron.engine.Board;
import nl.unimaas.games.tron.player.MCTSPlayer.Node;
import nl.unimaas.games.tron.player.mcts.FinalNodeSelectionStrategy;
import nl.unimaas.games.tron.player.mcts.PlayoutStrategy;
import nl.unimaas.games.tron.util.KnuthRandom;

@SuppressWarnings("serial")
public class MonteCarloPlayer extends Player {
	private final static boolean DEBUG = false;
	private long MAX_TURN_TIME = 500;
	private FinalNodeSelectionStrategy finalNode = FinalNodeSelectionStrategy.SECURE_CHILD;
	private PlayoutStrategy playout = PlayoutStrategy.RANDOM;
	
	public MonteCarloPlayer(String name, int num, Color c) {
		super(name, num, c);
	}

	@Override
	protected int computeMove(Board rootBoard, long endTime) {
		if (endTime == 0)
			endTime = System.currentTimeMillis() + MAX_TURN_TIME;
		long sims = 0;
		Node root = new Node(1 - getNumber(), false);
		Board currentBoard = rootBoard.deepClone(false);
		int pos = rootBoard.getPlayerPosition(this);
		int x = Board.posToX(pos), y = Board.posToY(pos);
		ArrayList<Integer> moves = rootBoard.getValidMoves(x, y);
		
		//expand
		for (int i = moves.size(); --i >= 0;)
			root.children.add(new Node(root, getNumber(), moves.get(i), true));
		
		int childrenCount = root.children.size();
		
		while (System.currentTimeMillis() < endTime) {
			//select a random node
			rootBoard.copyTo(currentBoard, false);
			Node node = root.children.get(KnuthRandom.nextInt(childrenCount));
			node.apply(currentBoard);
			sims++;
			int r = simulate(node, currentBoard);
			node.value += r;
			node.visits++;
			if (r == 1)
				node.wins++;
			else if (r == 0)
				node.draws++;
		}
		
		Node moveNode = finalNode.getNode(root);
		
		if (DEBUG) {
			System.out.println("Simulations: " + sims);
			//root.printChildren();
			System.out.println("Perform move " + Board.moveToString(moveNode.move));
		}
		
		return moveNode.move;
	}
	
	@Override
	public void reset() {
		
	}
	
	protected int simulate(Node node, Board simBoard) {
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
					return 0;
				else
					return 1; //he's stuck
			}

			otherPos = simBoard.getPositionFromMove(otherX, otherY, otherMove);
			if (myPos == otherPos)
				return 0; //tie
			simBoard.performMove(otherX, otherY, otherMove, otherNr, false);
			otherX = Board.posToX(otherPos);
			otherY = Board.posToY(otherPos);
		}
		
		while (true) {
			myMove = playout.getMove(simBoard, myX, myY, myNr);
			otherMove = playout.getMove(simBoard, otherX, otherY, otherNr);
			
			if (myMove == Board.MOVE_NONE) {
				if (otherMove == Board.MOVE_NONE) 
					return 0; //both players did not move
				else
					return -1; //I did not move
			}
			else if (otherMove == Board.MOVE_NONE)
				return 1; //other did not move
			
			myPos = simBoard.getPositionFromMove(myX, myY, myMove);
			otherPos = simBoard.getPositionFromMove(otherX, otherY, otherMove);
			
			if (myPos == otherPos)
				return 0;
			
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
	
	public static Player fromXml(org.w3c.dom.Node playerRoot, int num, Color color) {
		return fromXml(playerRoot, new MonteCarloPlayer(MonteCarloPlayer.class.getSimpleName(), num, color));
	}
	public static Player fromXml(org.w3c.dom.Node playerRoot, MonteCarloPlayer player) {
		NamedNodeMap attrs = playerRoot.getAttributes();
		player.description = "";

		if (attrs.getNamedItem("playout") != null) {
			try {
				Field field = PlayoutStrategy.class.getField(attrs.getNamedItem("playout").getNodeValue());
				player.playout = (PlayoutStrategy) field.get(null);
				player.description += String.format("playout=%s", field.getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (attrs.getNamedItem("finalMove") != null) {
			try {
				Field field = FinalNodeSelectionStrategy.class.getField(attrs.getNamedItem("finalMove").getNodeValue());
				player.finalNode = (FinalNodeSelectionStrategy) field.get(null);
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
		player.description = String.format("playout=%s final=%s", player.playout, player.finalNode);
		return player;
	}
	
	@Override
	public MonteCarloPlayer clone() {
		MonteCarloPlayer player = new MonteCarloPlayer(getName(), getNumber(), getColor());
		player.playout = playout;
		player.finalNode = finalNode;
		player.description = description;
		return player;
	}
}
