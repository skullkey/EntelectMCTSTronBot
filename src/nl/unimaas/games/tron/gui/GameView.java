package nl.unimaas.games.tron.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JComponent;

import nl.unimaas.games.tron.engine.Board;
import nl.unimaas.games.tron.engine.Game;
import nl.unimaas.games.tron.player.HumanPlayer;

public class GameView extends JComponent implements MouseListener {
	private static final long serialVersionUID = 7977936660875789427L;
	private static final int PLAYER_MARKER_WIDTH = 8;
	
	private Game game;
	private Board board;
	private HumanPlayer player = null;
	private Point playerPos = null;
	private boolean turnMode = false;
	private Point selectedSquare = null;
	private Point[] validPositions = null;
	
	private int fieldW, fieldH;
	
	public GameView() {
		super();
		addMouseListener(this);
	}
	
	public void setGame(Game game) {
		this.game = game;
		this.board = game.board;
		invalidate();
	}
	
	public void assignHumanPlayer(HumanPlayer player) {
		this.player = player; 
	}
	
	public HumanPlayer getAssignedHumanPlayer() {
		return player;
	}
	
	public void beginGetTurn() {
		if (player == null)
			throw new IllegalStateException("no player!");
		turnMode = true;
		int pos = board.getPlayerPosition(player);
		this.playerPos = new Point(Board.posToX(pos), Board.posToY(pos));
		ArrayList<Integer> validPos = board.getValidMovePositions(playerPos.x, playerPos.y, true);
		this.validPositions = new Point[validPos.size()];
		for (int i = 0; i < validPositions.length; i++)
			validPositions[i] = new Point(Board.posToX(validPos.get(i)), Board.posToY(validPos.get(i)));
		/*Integer[] validPos = board.getValidMovePositions(playerPos.x, playerPos.y, true);
		this.validPositions = new Point[validPos.length];
		for (int i = 0; i < validPositions.length; i++)
			validPositions[i] = new Point(Board.posToX(validPos[i]), Board.posToY(validPos[i]));*/
		repaint();
	}
	
	public void endGetTurn() {
		turnMode = false;
		this.validPositions = null;
		selectedSquare = null;
		repaint();
	}
	
	protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (game == null)
        	return;
        
    	fieldW = getWidth() / board.getWidth();
    	fieldH = getHeight() / board.getHeight();
    	for (int x = 0; x < board.getWidth(); x++) {
    		for (int y = 0; y < board.getHeight(); y++) {
    			if (board.isWall(x, y)) {
    				Color fieldColor;
    				int field = board.getField(x, y);
    				if (field == Board.FILLED_FIELD)
    					fieldColor = Color.GRAY;
    				else
    					fieldColor = game.getPlayer(field).getColor();
    				g2.setColor(fieldColor);
    				g2.fillRect(fieldW * x, fieldH * y, fieldW, fieldH);
    			}
    		}
    	}
    	if (turnMode && validPositions != null) {
    		g2.setColor(Color.WHITE);
    		g2.drawRect(fieldW * playerPos.x, fieldH * playerPos.y, fieldW, fieldH);
    		
    		g2.setColor(Color.LIGHT_GRAY);
    		for (int i = 0; i < validPositions.length; i++)
    			g2.drawRect(fieldW * validPositions[i].x , fieldH * validPositions[i].y, fieldW, fieldH);
    		
    		if (selectedSquare != null) {
    			g2.setColor(Color.GREEN);
    			g2.drawRect(fieldW * selectedSquare.x, fieldH * selectedSquare.y, fieldW, fieldH);
    		}
    	}
    	
    	g2.setColor(Color.BLACK);
    	for (int i = 0; i < game.getPlayerCount(); i++) {
    		int pos = board.getPlayerPosition(game.getPlayer(i));
    		g2.fillRect((int) (fieldW * (Board.posToX(pos) + 0.5)) - PLAYER_MARKER_WIDTH / 2, 
    					(int) (fieldH * (Board.posToY(pos) + 0.5)) - PLAYER_MARKER_WIDTH / 2, 
    					PLAYER_MARKER_WIDTH, PLAYER_MARKER_WIDTH);
    	}
    }

	@Override
	public void mouseClicked(MouseEvent m) {}

	@Override
	public void mouseEntered(MouseEvent m) {}

	@Override
	public void mouseExited(MouseEvent m) {}

	@Override
	public void mousePressed(MouseEvent m) {
		if (m.getButton() != MouseEvent.BUTTON1)
			return;
		if (!turnMode)
			return;
		selectedSquare = getFieldFromPosition(m.getX(), m.getY());
		if (!board.isValidPlayerMove(selectedSquare.x, selectedSquare.y, player))
			selectedSquare = null;
		repaint();
	}

	@Override
	public void mouseReleased(MouseEvent m) {
		if (m.getButton() != MouseEvent.BUTTON1)
			return;
		if (!turnMode)
			return;
		Point p = getFieldFromPosition(m.getX(), m.getY());
		if (p.equals(selectedSquare)) {
			int pos = board.getPlayerPosition(player);
			int move = board.getMoveFromPositions(Board.posToX(pos), Board.posToY(pos), selectedSquare.x, selectedSquare.y);
			player.setMove(move);
			endGetTurn();
		}
	}
	
	private Point getFieldFromPosition(int x, int y) {
		return new Point(x / fieldW, y / fieldH);
	}
}
