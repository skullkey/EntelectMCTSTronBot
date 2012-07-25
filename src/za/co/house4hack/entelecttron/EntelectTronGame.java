package za.co.house4hack.entelecttron;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.unimaas.games.tron.engine.Board;
import nl.unimaas.games.tron.engine.Game;
import nl.unimaas.games.tron.engine.GameReplay;
import nl.unimaas.games.tron.player.Player;

public class EntelectTronGame {

	private final static Logger logger=Logger.getLogger("Tron");;

	public static void runGame(String boardPath, boolean crossOver) throws IOException {
		Board board;
		try {
			//board = Board.fromFile(boardPath);
			board = Board.fromEntelectFile(boardPath);
		} catch (Exception ex) {
			System.out.println("Unable to load the map: file not found");
			return;
		}
		ArrayList<Player> players = new ArrayList<Player>();
		Player p = Player.fromType("MCTSPlayer", "You", players.size(),
				Color.RED);
		players.add(p);
		p = Player.fromType("MCTSPlayer", "Other", players.size(), Color.BLUE);
		players.add(p);
		Game game = new Game(players.toArray(new Player[players.size()]), board);
		
		logger.setUseParentHandlers(false);
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(java.util.logging.Level.FINEST);
		logger.addHandler(consoleHandler);
		logger.setLevel(Level.ALL);
		
		game.setLogger(logger);
		game.setStartPositionsScrambled(false);
		game.setup();
		game.runOnce();
		
		board.toEntelectFile(boardPath, crossOver);
		
		for(int j=0; j<board.getHeight(); j++){
			for(int i=0; i<board.getWidth(); i++){
				System.out.print(board.getField(i, j));
			}
			System.out.println("");
		}
		
		System.exit(0);
		

	}

	public static void main(String[] args) throws IOException {
		boolean crossover = false;
		String path=null;
		if (args.length == 2) {
			crossover = args[1].equalsIgnoreCase("crossover");
		}
		if (args.length >= 1) {
			path = args[0];
			runGame(path, crossover);
		}


	}

}
