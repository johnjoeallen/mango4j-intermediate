package ie.bitstep.mango.spring.logging.adapter;

import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import net.logstash.logback.argument.StructuredArgument;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;

import static ie.bitstep.mango.spring.logging.dc.DiagnosticContext.LOG_KEY_ID;
import static net.logstash.logback.argument.StructuredArguments.entries;

public class MapLoggerAdapter extends AbstractLoggerAdapter {

	public MapLoggerAdapter() {
		excludeKeys(DiagnosticContext.LOG_KEY_CORRELATION_ID,
			DiagnosticContext.LOG_KEY_TIMESTAMP,
			DiagnosticContext.LOG_KEY_URL);
	}

	public StructuredArgument buildLogEntries(Object loggable) {
		return entries(buildLogMap(loggable));
	}

	public Map<String, Object> buildLogMap(Object loggable) {
		Map<String, Object> result = new LinkedHashMap<>();

		putIfNotEmptyToLogKeyId(result);
		result.putAll(loggableToMap(loggable));

		return removeExcludedKeys(result);
	}

	@Override
	public void logEntry(Logger logger, Object loggable) {
		logDebug(logger, loggable, null);
	}

	@Override
	public void logExit(Logger logger, Object loggable) {
		logInfo(logger, loggable, null);
	}

	@Override
	protected void logError(Logger logger, Object loggable, String message) {
		if (logger.isErrorEnabled()) {
			logger.error(message, buildLogEntries(loggable));
		}
	}

	@Override
	protected void logWarn(Logger logger, Object loggable, String message) {
		if (logger.isWarnEnabled()) {
			logger.warn(message, buildLogEntries(loggable));
		}
	}

	@Override
	protected void logInfo(Logger logger, Object loggable, String message) {
		if (logger.isInfoEnabled()) {
			logger.info(message, buildLogEntries(loggable));
		}
	}

	@Override
	protected void logDebug(Logger logger, Object loggable, String message) {
		if (logger.isDebugEnabled()) {
			logger.debug(message, buildLogEntries(loggable));
		}
	}

	@Override
	protected void logTrace(Logger logger, Object loggable, String message) {
		if (logger.isTraceEnabled()) {
			logger.trace(message, buildLogEntries(loggable));
		}
	}

	private void putIfNotEmptyToLogKeyId(Map<String, Object> result) {
		String value = MDC.get(LOG_KEY_ID);

		if (StringUtils.isNotEmpty(value)) {
			result.put(LOG_KEY_ID, value);
		}
	}
}
