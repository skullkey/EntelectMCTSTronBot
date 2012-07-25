package nl.unimaas.games.tron.player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;

import nl.unimaas.games.tron.engine.Board;

@SuppressWarnings("serial")
public class FillPlayer extends Player {
	private int turns = 0;
	
	public FillPlayer(String name, int num, Color c) {
		super(name, num, c);
	}

	@Override
	protected int computeMove(Board b, long endTime) {
		long start = System.currentTimeMillis();
		turns++;
		System.out.println();
		int pos = b.getPlayerPosition(this);
		int x = Board.posToX(pos), y = Board.posToY(pos);
		ArrayList<int[]> positions = b.getValidMovePositionsXY(x, y);
		ArrayList<Integer> moves = b.getValidMoves(x, y);
		if (positions.isEmpty()) {
			return Board.MOVE_NONE;
		}
		else {
			LinkedList<int[]> wallCounts = new LinkedList<int[]>();
			int[] p;
			boolean hasIsoMove = false;
			for (int i = 0; i < positions.size(); i++) {
				p = positions.get(i);
				if (b.isIsolatingMove(x, y, p[0], p[1]))
					hasIsoMove = true;
				int count = b.getWallCount(p[0], p[1]);
				if (!wallCounts.isEmpty()) {
					int j = 0;
					while (wallCounts.size() > j && wallCounts.get(j)[0] > count)
						j++;
					wallCounts.add(j, new int[] {count, i, -1});
				}
				else {
					wallCounts.add(new int[] {count, i, -1});
				}
			}
			
			if (hasIsoMove) {
				LinkedList<int[]> newList = new LinkedList<int[]>();
				for (int i = 0; i < wallCounts.size(); i++) {
					//get the space
					int[] wall = wallCounts.get(i);
					int ind = wall[1];
					p = positions.get(ind);
					int space = b.getFreeSpace(x, y, p[0], p[1]);
					wall[2] = space;
					
					if (!newList.isEmpty()) {
						int j = 0;
						while (newList.size() > j && newList.get(j)[2] > space)
							j++;
						newList.add(j, wall);
					}
					else {
						newList.add(wall);
					}
				}
				int bestIndex = newList.getFirst()[1];
				//System.out.println("Took " + (System.currentTimeMillis() - start) + " ms to get a fill result");
				return moves.get(bestIndex);
			}
			
			int bestIndex = wallCounts.getFirst()[1];
			//System.out.println("Took " + (System.currentTimeMillis() - start) + " ms to get a fill result");
			return moves.get(bestIndex);
		}
	}
	
}
