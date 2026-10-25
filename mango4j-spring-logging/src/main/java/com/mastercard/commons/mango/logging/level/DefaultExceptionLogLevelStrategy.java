package com.mastercard.commons.mango.logging.level;

import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.logging.LogUtils;
import com.mastercard.commons.mango.logging.annotations.LoggableLevel;
import com.mastercard.commons.mango.logging.annotations.LoggableMethod;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

@Component
@RequiredArgsConstructor
public class DefaultExceptionLogLevelStrategy implements ExceptionLogLevelStrategy {

	/**
	 * Identifies log level appropriate for the given exception.
	 */
	@Override
	public Level getLogLevel(Throwable t, MethodInfo methodInfo) {
		if (isErrorLevelForced(t, methodInfo)) {
			return Level.ERROR;
		} else {
			return getLogLevel(t);
		}
	}

	protected boolean isErrorLevelForced(Throwable t, MethodInfo methodInfo) {      //NOSONAR the Throwable parameter is used in other projects that extend this class
		LoggableMethod loggableMethod = LogUtils.getLoggableMethod(methodInfo);
		return loggableMethod != null && loggableMethod.forceErrorOnException();
	}

	protected Level getLogLevel(Throwable t) {
		Level logLevel;
		LoggableLevel logLevelAnnotation = findAnnotation(t.getClass(), LoggableLevel.class);
		if (logLevelAnnotation != null) {
			logLevel = logLevelAnnotation.level();

			// common spring request validation failures
		} else if (t instanceof HttpMediaTypeNotSupportedException
			|| t instanceof HttpMessageNotReadableException
			|| t instanceof MethodArgumentNotValidException) {
			logLevel = Level.INFO;

		} else if (t instanceof RuntimeException) {
			logLevel = Level.ERROR;
		} else {
			logLevel = Level.WARN;
		}
		return logLevel;
	}
}
