package nl.unimaas.games.tron;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import nl.unimaas.games.tron.engine.Board;
import nl.unimaas.games.tron.engine.Game;
import nl.unimaas.games.tron.engine.GameListener;
import nl.unimaas.games.tron.engine.GameReplay;
//import nl.unimaas.games.tron.experiment.Experiment;
//import nl.unimaas.games.tron.experiment.GameAnalyzer;
import nl.unimaas.games.tron.gui.GameView;
import nl.unimaas.games.tron.gui.NewGameDialog;
import nl.unimaas.games.tron.player.*;
import nl.unimaas.games.tron.util.logging.TronGameLogHandler;

public final class TronGame implements GameListener {
	private final JFrame frame;
	private final GameView gameView;
	private JLabel statusLabel;
	private JList logList;
	private DefaultListModel logModel;
	private Game game;
	private Logger logger;
	private final static String DEFAULT_MAP = "maps/sphere.txt";
	
	public TronGame() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } catch(Exception e) {}
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(640, 480);
		frame.setLocationRelativeTo(null);
		frame.setTitle("Tron");
		gameView = new GameView();
		createMenuBar();
		createGamePane();
		createLogger();
	}
	
	public static void main(String[] args) {
		final TronGame me = new TronGame();
		me.show();
		
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase("-r")) {
				System.out.println("Loading replay...");
				try {
					Game game = GameReplay.loadReplay(args[1]);
					me.startGame(game);
				} catch (IOException e) {
					System.err.println("Unable to load the replay file: " + e.getMessage());
				}
			}
		}
	}
	
	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Game");
		menu.setMnemonic(KeyEvent.VK_G);
		menu.getAccessibleContext().setAccessibleDescription("Game menu");
		menuBar.add(menu);
		frame.setJMenuBar(menuBar);
		//new game
		JMenuItem newGameItem = new JMenuItem("New game...", KeyEvent.VK_N);
		newGameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		newGameItem.getAccessibleContext().setAccessibleDescription("Start a new game");
		newGameItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				NewGameDialog d = new NewGameDialog(frame);
				d.setMaxPlayerCount(2);
				boolean result = d.showDialog();
				if (!result)
					return;
				String[] types = new String[d.getMaxPlayerCount()];
				String[] names = new String[d.getMaxPlayerCount()];
				for (int i = 0; i < d.getMaxPlayerCount(); i++) {
					types[i] = d.getPlayerType(i);
					names[i] = d.getPlayerName(i);
				}
				startNewGame(types, names, DEFAULT_MAP);
			}
		});
		menu.add(newGameItem);
		menu.addSeparator();
		/* replay */
		//save replay
		JMenuItem saveReplayItem = new JMenuItem("Save replay");
		saveReplayItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (game == null || game.getReplay() == null) {
					JOptionPane.showMessageDialog(frame, "No replay to save", "Tron", JOptionPane.ERROR_MESSAGE);
					return;
				}
				final JFileChooser fc = new JFileChooser(new File("."));
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fc.setFileFilter(GameReplay.fileFilter);
				int returnVal = fc.showSaveDialog(frame);
				
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						GameReplay replay = game.getReplay();
						String path = fc.getSelectedFile().getPath();
						if (!path.endsWith(".rp"))
							path += ".rp";
						replay.save(path);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(frame, "Unable to save the replay '" + fc.getSelectedFile().getPath() + "'\n: " + e.getMessage(), "Tron", JOptionPane.ERROR_MESSAGE);						
					}
				}
			}
		});
		menu.add(saveReplayItem);
		
		//load replay
		JMenuItem loadReplayItem = new JMenuItem("Load replay");
		loadReplayItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser(new File("."));
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fc.setFileFilter(GameReplay.fileFilter);
				int returnVal = fc.showOpenDialog(frame);
				
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					try {
						Game game = GameReplay.loadReplay(fc.getSelectedFile().getPath());
						startGame(game);
						System.out.println("Started the replay");
					} catch (IOException e) {
						JOptionPane.showMessageDialog(frame, "Unable to load the replay '" + fc.getSelectedFile().getPath() + "': " + e.getMessage());						
					}
				}			
			}
		});
		menu.add(loadReplayItem);
		menu.addSeparator();
		/* experiments */
		//start experiment
		JMenuItem startExpItem = new JMenuItem("Start experiment...");
		startExpItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new JFileChooser(new File("."));
				//fc.setFileFilter(Experiment.fileFilter);
				int result = fc.showOpenDialog(frame);
				if (result == JFileChooser.APPROVE_OPTION) {
					
				}
			}
		});
		menu.add(startExpItem);
		//
		menu.addSeparator();
		//exit
		JMenuItem exitItem = new JMenuItem("Exit", KeyEvent.VK_E);
		exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK));
		exitItem.getAccessibleContext().setAccessibleDescription("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				frame.setVisible(false);
				System.exit(0);
			}
		});
		menu.add(exitItem);
		
		
		
		JMenuItem markItem = new JMenuItem("Mark", KeyEvent.VK_E);
		markItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.err.println("MARK " + mark++);
			}
		});
		menuBar.add(markItem);
	}
	private int mark = 0;
	
	private void createGamePane() {
		Container contentPane = frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		Border border = new EmptyBorder(10, 10, 0, 10);
		gameView.setBorder(border);
		contentPane.add(gameView, BorderLayout.CENTER);
		//log pane
		logModel = new DefaultListModel();
		logList = new JList(logModel);
		logList.setBorder(new EmptyBorder(5, 5, 0, 5));
		logList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane listScroller = new JScrollPane(logList);
		listScroller.setPreferredSize(new Dimension(200, 0));
		contentPane.add(listScroller, BorderLayout.EAST);
		
		//status bar
		JPanel statusBar = new JPanel();
		FlowLayout statusLayout = new FlowLayout(FlowLayout.LEFT);
		statusLayout.setHgap(10);
		statusLabel = new JLabel("Tron");
		statusBar.add(statusLabel);
		statusBar.setLayout(statusLayout);
		contentPane.add(statusBar, BorderLayout.SOUTH);
	}
	
	private void createLogger() {
		 logger = Logger.getLogger("Tron");
		 logger.setUseParentHandlers(false);
		 logger.setLevel(Level.ALL);
		 logger.addHandler(new TronGameLogHandler(this));
	}
	
	public void show() {
		frame.setVisible(true);
	}
	
	private void setStatus(String text) {
		statusLabel.setText(text);
	}
	
	public void addLogEntry(String text) {
		logModel.insertElementAt(text, 0);
	}
	
	public void clearLog() {
		logModel.clear();
	}
	
	public void startGame(Game game) {
		//GameAnalyzer ana = new GameAnalyzer(game);
		clearLog();
		if (game != null && game.isRunning()) {
			System.out.println("Ending the previous game..");
			game.stop();
		}
		
		this.game = game;
		game.addGameListener(this);
		game.setLogger(logger);
		game.setStartPositionsScrambled(false);
		gameView.setGame(game);
		
		//assign human
		for (int i = 0; i < game.players.length; i++) {
			if (game.players[i] instanceof HumanPlayer)
				gameView.assignHumanPlayer((HumanPlayer) game.players[i]);
		}

		game.setup();
		game.start();
		gameView.repaint();
	}
	
	public void startNewGame(Player[] players, Board board) {
		int humanCount = 0;
		//check if there's only one human
		for (int i = 0; i < players.length; i++) {
			if (players[i] instanceof HumanPlayer)
				humanCount++;
		}
		if (humanCount > 1)
			throw new IllegalStateException("Only 1 human allowed");
		Game game = new Game(players, board);
		startGame(game);
	}
	
	public void startNewGame(String[] playerTypes, String[] names, String boardPath) {
		ArrayList<Player> players = new ArrayList<Player>();
		for (int i = 0; i < playerTypes.length; i++) {
			String type = playerTypes[i];
			String name = names[i];
			Color color;
			switch (players.size()) {
			case 0:
				color = Color.RED;
				break;
			case 1:
				color = Color.BLUE;
				break;
			default:
				color = Color.MAGENTA;
			}
			Player p = Player.fromType(type, name, players.size(), color);
			if (p != null)
				players.add(p);
		}
		
		Board board;
		try {
			board = Board.fromFile(boardPath);
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, "Unable to load the map: file not found", "Oops!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		startNewGame(players.toArray(new Player[players.size()]), board);
	}

	@Override
	public void onStart(Game g) {
		setStatus(String.format("Starting a %d-player game...", game.getPlayerCount()));
	}

	@Override
	public void onPause(Game g) {
		setStatus(String.format("Game paused"));
	}

	@Override
	public void onResume(Game g) {
		setStatus(String.format("Game resumed"));
	}

	@Override
	public void onGameOver(Game g) {
		setStatus(String.format("Game over!"));
	}

	@Override
	public void onNextTurn(Game g) {
		setStatus(String.format("Playing. Turn %d.", game.getTurn()));
	}

	@Override
	public void onMoveBegin(Game g) {
		if (gameView.getAssignedHumanPlayer() != null)
			gameView.beginGetTurn();
	}

	@Override
	public void onMoveEnd(Game g) {
		gameView.endGetTurn();
		gameView.repaint();
	}

	@Override
	public void onDraw(Game g, Player[] winning) {}

	@Override
	public void onWin(Game g, Player p) {
		setStatus(String.format("Player %s won the game!" , p.getName()));
	}

	@Override
	public void onPlayerDied(Game g, Player p) {}

	@Override
	public void onPlayerMove(Game g, Board b, Player p, int m) {
		// TODO Auto-generated method stub
		
	}
}
