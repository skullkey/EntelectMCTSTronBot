package nl.unimaas.games.tron.player.mcts;

import java.io.Serializable;

import nl.unimaas.games.tron.player.MCTSPlayer.Node;

@SuppressWarnings("serial")
public interface FinalNodeSelectionStrategy extends Serializable {
	public Node getNode(Node parent);
	
	public final static FinalNodeSelectionStrategy MAX_CHILD = new FinalNodeSelectionStrategy() {
		@Override
		public Node getNode(Node parent) {
			double bestValue = Double.NEGATIVE_INFINITY;
			Node bestNode = null;
			for (int i = 0; i < parent.children.size(); i++) {
				Node child = parent.children.get(i);
				if (child.value > bestValue) {
					bestValue = child.value / (child.visits + 1.0);
					bestNode = child;
				}
			}
			return bestNode;
		}
		
		@Override
		public String toString() {
			return "max";
		}
	};
	
	public final static FinalNodeSelectionStrategy SECURE_CHILD = new FinalNodeSelectionStrategy() {
		private double A = 4;
		
		@Override
		public Node getNode(Node parent) {
			float bestValue = Float.NEGATIVE_INFINITY;
			Node bestNode = null;
			for (int i = 0; i < parent.children.size(); i++) {
				Node child = parent.children.get(i);
				float value = (float) (child.value / (child.visits + 1.0) + A / Math.sqrt(child.visits));
				if (value > bestValue) {
					bestValue = value;
					bestNode = child;
				}
			}
			return bestNode;
		}
		
		@Override
		public String toString() {
			return "secure";
		}
	};
	
	public final static FinalNodeSelectionStrategy ROBUST_CHILD = new FinalNodeSelectionStrategy() {
		@Override
		public Node getNode(Node parent) {
			float bestValue = Float.NEGATIVE_INFINITY;
			Node bestNode = null;
			for (int i = 0; i < parent.children.size(); i++) {
				Node child = parent.children.get(i);
				if (child.visits > bestValue) {
					bestValue = child.visits;
					bestNode = child;
				}
			}
			return bestNode;
		}
		
		@Override
		public String toString() {
			return "robust";
		}
	};
	
	public final static FinalNodeSelectionStrategy ROBUST_MAX_CHILD = new FinalNodeSelectionStrategy() {
		@Override
		public Node getNode(Node parent) {
			double bestValue = Double.NEGATIVE_INFINITY;
			Node bestNode = null;
			for (int i = 0; i < parent.children.size(); i++) {
				Node child = parent.children.get(i);
				double value = child.value / (child.visits + 1.0) + child.visits;
				if (value > bestValue) {
					bestValue = value;
					bestNode = child;
				}
			}
			return bestNode;
		}
		
		@Override
		public String toString() {
			return "robust-max";
		}
	};
}
