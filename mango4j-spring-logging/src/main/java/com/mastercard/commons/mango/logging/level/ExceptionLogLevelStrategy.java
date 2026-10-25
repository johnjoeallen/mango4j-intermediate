package com.mastercard.commons.mango.logging.level;

import com.mastercard.commons.mango.aop.MethodInfo;
import org.slf4j.event.Level;

@FunctionalInterface
public interface ExceptionLogLevelStrategy {
	/**
	 * Identifies log level appropriate for the given exception.
	 */
	Level getLogLevel(Throwable t, MethodInfo methodInfo);
}
