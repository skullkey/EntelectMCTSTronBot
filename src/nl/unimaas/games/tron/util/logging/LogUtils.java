package nl.unimaas.games.tron.util.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class LogUtils {
	/** Sets the given level for all handlers of this logger */
	public static void setLoggerLevel(Logger logger, Level level) {
		if (logger == null)
			return;
		
		logger.setLevel(level);
		if (logger.getHandlers() != null)
			for (Handler handler : logger.getHandlers())
				handler.setLevel(level);
	    
	    if (logger.getUseParentHandlers())
	    	setLoggerLevel(logger.getParent(), level);
	}
	
	public static ConsoleHandler getConsoleHandler(Logger logger) {
		if (logger == null)
			return null;
		
		if (logger.getHandlers() != null)
			for (Handler handler : logger.getHandlers())
				if (handler instanceof ConsoleHandler)
					return (ConsoleHandler) handler;
		
		if (logger.getUseParentHandlers())
	    	return getConsoleHandler(logger.getParent());
		return null;
	}
	
	private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
	
	public static final Formatter tinyFormatter = new Formatter() {
		@Override
		public String format(LogRecord r) {			
			return String.format("%s-%s: %s\n", timeFormat.format(new Date(r.getMillis())), r.getLevel(), r.getMessage());
		}
	};
}
