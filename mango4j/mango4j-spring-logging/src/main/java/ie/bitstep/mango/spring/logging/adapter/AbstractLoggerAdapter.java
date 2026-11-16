package ie.bitstep.mango.spring.logging.adapter;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractLoggerAdapter implements LoggerAdapter {
	protected final List<String> excludedKeys = new ArrayList<>();

	public void excludeKeys(String... keys) {
		excludedKeys.addAll(Arrays.asList(keys));
	}

	public void includeKeys(String... keys) {
		excludedKeys.removeAll(Arrays.asList(keys));
	}

	public static Map<String, Object> loggableToMap(Object loggable) {
		if (loggable instanceof Map) {
			return (Map<String, Object>) loggable;    //NOSONAR
		} else {
			// would like to fix this so that if it's an object it will get ObjectMapper mapped
			Map<String, Object> failure = new LinkedHashMap<>();
			failure.put("failure", String.valueOf(loggable));
			return failure;
		}
	}

	public Map<String, Object> removeExcludedKeys(Map<String, Object> map) {
		excludedKeys.forEach(map::remove);
		return map;
	}


	@Override
	public boolean isLogEntryEnabled(Logger logger) {
		return logger.isDebugEnabled();
	}

	@Override
	public boolean isLogExitEnabled(Logger logger) {
		return logger.isInfoEnabled();
	}

	@Override
	public void logException(Logger logger, Level logLevel, Object loggable, String message) {
		switch (logLevel) {
			case TRACE:
				logTrace(logger, loggableToMap(loggable), message);
				break;
			case DEBUG:
				logDebug(logger, loggableToMap(loggable), message);
				break;
			case INFO:
				logInfo(logger, loggableToMap(loggable), message);
				break;
			case WARN:
				logWarn(logger, loggableToMap(loggable), message);
				break;
			case ERROR:
			default:
				logError(logger, loggableToMap(loggable), message);
				break;
		}
	}

	protected abstract void logError(Logger logger, Object loggable, String message);

	protected abstract void logWarn(Logger logger, Object loggable, String message);

	protected abstract void logInfo(Logger logger, Object loggable, String message);

	protected abstract void logDebug(Logger logger, Object loggable, String message);

	protected abstract void logTrace(Logger logger, Object loggable, String message);
}
