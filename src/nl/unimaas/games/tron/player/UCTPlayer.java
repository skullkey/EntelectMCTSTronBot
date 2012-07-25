package nl.unimaas.games.tron.player;

import java.awt.Color;
import nl.unimaas.games.tron.player.mcts.SelectionStrategy;

import org.w3c.dom.NamedNodeMap;

public class UCTPlayer extends MCTSPlayer {
	private static final long serialVersionUID = 1L;
	public final static double DEFAULT_C = 10;
	public final static int DEFAULT_T = 30;
	protected double C = DEFAULT_C;
	protected int T = DEFAULT_T;

	public UCTPlayer(String name, int num, Color c) {
		super(name, num, c);
		super.select = new SelectionStrategy.UCT(C, T);
	}
	
	public static Player fromXml(org.w3c.dom.Node playerRoot, int num, Color color) {
		return fromXml(playerRoot, new UCTPlayer(UCTPlayer.class.getSimpleName(), num, color));
	}
	
	public static Player fromXml(org.w3c.dom.Node playerRoot, UCTPlayer player) {
		MCTSPlayer.fromXml(playerRoot, player);
		NamedNodeMap attrs = playerRoot.getAttributes();
		double C = DEFAULT_C;
		int T = DEFAULT_T;
		if (attrs.getNamedItem("C") != null) {
			try {
				C = Double.parseDouble(attrs.getNamedItem("C").getNodeValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (attrs.getNamedItem("T") != null) {
			try {
				T = Integer.parseInt(attrs.getNamedItem("T").getNodeValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		player.C = C;
		player.T = T;
		player.select = new SelectionStrategy.UCT(C, T);
		player.description = String.format("select=%s playout=%s cut=%s final=%s C=%.1f T=%d", player.select, player.playout, player.cutOff, player.finalMove, C, T);
		return player;		
	}
}
