package com.mastercard.commons.mango.logging.adapter;

import com.mastercard.commons.mango.logging.LogUtils;
import com.mastercard.commons.mango.logging.internal.LoggerMapper;
import org.slf4j.Logger;

public class DefaultLoggerAdapter extends AbstractLoggerAdapter {

	private static final String LOG_FORMAT = "{}: {}";

	@Override
	public void logEntry(Logger logger, Object loggable) {
		if (isLogEntryEnabled(logger)) {
			logger.debug(loggableToString(loggable)); // NOSONAR
		}
	}

	@Override
	public void logExit(Logger logger, Object loggable) {
		if (isLogExitEnabled(logger)) {
			logger.info(loggableToString(loggable)); // NOSONAR
		}
	}

	@Override
	protected void logError(Logger logger, Object loggable, String message) {
		if (logger.isErrorEnabled()) {
			logger.error(LOG_FORMAT, loggableToString(loggable), message);
		}
	}

	@Override
	protected void logWarn(Logger logger, Object loggable, String message) {
		if (logger.isWarnEnabled()) {
			logger.warn(LOG_FORMAT, loggableToString(loggable), message);
		}
	}

	@Override
	protected void logInfo(Logger logger, Object loggable, String message) {
		if (logger.isInfoEnabled()) {
			logger.info(LOG_FORMAT, loggableToString(loggable), message);
		}
	}

	@Override
	protected void logDebug(Logger logger, Object loggable, String message) {
		if (logger.isDebugEnabled()) {
			logger.debug(LOG_FORMAT, loggableToString(loggable), message);
		}
	}

	@Override
	protected void logTrace(Logger logger, Object loggable, String message) {
		if (logger.isTraceEnabled()) {
			logger.trace(LOG_FORMAT, loggableToString(loggable), message);
		}
	}

	private String loggableToString(Object loggable) {
		return LogUtils.toLogSafeValue(LoggerMapper.loggableToJsonString(loggable));
	}

}
