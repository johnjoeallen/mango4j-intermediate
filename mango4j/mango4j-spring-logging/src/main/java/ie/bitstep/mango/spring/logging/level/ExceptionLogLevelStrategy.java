package ie.bitstep.mango.spring.logging.level;

import ie.bitstep.mango.spring.aop.MethodInfo;
import org.slf4j.event.Level;

@FunctionalInterface
public interface ExceptionLogLevelStrategy {
	/**
	 * Identifies log level appropriate for the given exception.
	 */
	Level getLogLevel(Throwable t, MethodInfo methodInfo);
}
