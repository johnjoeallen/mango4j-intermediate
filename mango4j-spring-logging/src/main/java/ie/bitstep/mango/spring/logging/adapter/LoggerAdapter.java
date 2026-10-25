package ie.bitstep.mango.spring.logging.adapter;

import org.slf4j.Logger;
import org.slf4j.event.Level;

public interface LoggerAdapter {

	boolean isLogEntryEnabled(Logger logger);

	void logEntry(Logger logger, Object loggable);

	boolean isLogExitEnabled(Logger logger);

	void logExit(Logger logger, Object loggable);

	void logException(Logger logger, Level logLevel, Object loggable, String message);
}
