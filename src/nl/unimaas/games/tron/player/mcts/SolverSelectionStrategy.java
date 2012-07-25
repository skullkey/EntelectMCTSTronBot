package nl.unimaas.games.tron.player.mcts;

public interface SolverSelectionStrategy {
	//public SolverNode getNode(SolverNode node);
	
	/*public final static SolverSelectionStrategy RANDOM = new SolverSelectionStrategy() {
		@Override
		public String toString() {
			return "random";
		}

		@Override
		public SolverNode getNode(SolverNode node) {
			ArrayList<SolverNode> candidates = new ArrayList<SolverNode>();
			for (int i = 0; i < node.children.size(); i++) {
				SolverNode n = (SolverNode) node.children.get(i);
				if (!n.proven)
					candidates.add(n);
			}
			if (candidates.isEmpty())
				return null;
			else
				return candidates.get(KnuthRandom.nextInt(candidates.size()));
		}
	};*/
}
