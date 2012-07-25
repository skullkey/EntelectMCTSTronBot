package nl.unimaas.games.tron.util.logging;

import java.awt.EventQueue;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import nl.unimaas.games.tron.TronGame;

public class TronGameLogHandler extends Handler {
	private final TronGame tron;
	
	public TronGameLogHandler(TronGame g) {
		this.tron = g;
	}
	
	@Override
	public void close() throws SecurityException {}

	@Override
	public void flush() {}

	@Override
	public void publish(final LogRecord rec) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				tron.addLogEntry(rec.getMessage());
			}
		});
	}
}
