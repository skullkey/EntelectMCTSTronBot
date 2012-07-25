package nl.unimaas.games.tron.player.mcts;

import java.io.Serializable;

import nl.unimaas.games.tron.player.MCTSPlayer.Node;
import nl.unimaas.games.tron.util.KnuthRandom;

@SuppressWarnings("serial")
public interface SelectionStrategy extends Serializable {
	public Node getNode(Node node);
	
	public final static SelectionStrategy RANDOM = new SelectionStrategy() {
		@Override
		public Node getNode(Node node) {
			if (node.children.size() > 0)
				return node.children.get(KnuthRandom.nextInt(node.children.size()));
			else
				return null;
		}

		@Override
		public String toString() {
			return "random";
		}
	};
	
	public class UCT implements SelectionStrategy {
		protected double C = Math.sqrt(2);
		protected int T = 30;
		protected double eps = 1E-10;
		
		public UCT(double C, int T) {
			this.C = C;
			this.T = T;
		}
		
		@Override
		public Node getNode(Node node) {
			Node bestNode = null;
			double bestValue = Double.NEGATIVE_INFINITY;
			for (Node n : node.children) {
				double uct = n.value / (n.visits + 1.0) + C * Math.sqrt(Math.log(node.visits + 1.0) / (n.visits + 1.0));

	            if (uct > bestValue) {
	            	bestNode = n;
	                bestValue = uct;
	            }
	        }
	        return bestNode;
		}
		
		@Override
		public String toString() {
			return "UCT";
		}
		
		public double getNodeValue(Node n) {
			return n.value * 1.0 / n.visits + C * Math.sqrt(Math.log(n.parent.visits) / n.visits);
		}
	}
}
