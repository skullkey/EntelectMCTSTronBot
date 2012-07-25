package nl.unimaas.games.tron.engine;

import nl.unimaas.games.tron.player.Player;

public interface GameListener {
	public void onStart(Game g);
	public void onPause(Game g);
	public void onResume(Game g);
	public void onDraw(Game g, Player[] winning);
	public void onWin(Game g, Player p);
	public void onGameOver(Game g);
	
	public void onPlayerDied(Game g, Player p);
	public void onPlayerMove(Game g, Board b, Player p, int m);
	
	public void onNextTurn(Game g);
	public void onMoveBegin(Game g);
	public void onMoveEnd(Game g);
}
