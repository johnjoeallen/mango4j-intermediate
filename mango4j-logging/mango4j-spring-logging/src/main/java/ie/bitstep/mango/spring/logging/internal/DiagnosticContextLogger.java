package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.LogUtils;

import ie.bitstep.mango.logging.internal.LoggerMapper;

import ie.bitstep.mango.spring.aop.ThrowableInfo;
import ie.bitstep.mango.logging.LogUtils;
import ie.bitstep.mango.logging.adapter.LoggerAdapter;
import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.level.ExceptionLogLevelStrategy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DiagnosticContextLogger {

	// DO NOT MAKE THIS FINAL, tests replace with mock via reflection
	static Logger logger = LoggerFactory.getLogger(DiagnosticContextLogger.class);
	private final LoggerAdapter loggerAdapter;
	private final ExceptionLogLevelStrategy exceptionLogLevelStrategy;

	private static boolean enabled = true; // Default: enabled for backward compatibility

	/**
	 * Set whether diagnostic context logging (entry/exit) is enabled.
	 * When disabled, no entry/exit logging will occur regardless of LoggerAdapter settings.
	 *
	 * @param enabled true to enable diagnostic context logging, false to disable
	 */
	public static void setEnabled(boolean enabled) {
		DiagnosticContextLogger.enabled = enabled;
	}

	/**
	 * Check if diagnostic context logging is enabled.
	 *
	 * @return true if diagnostic context logging is enabled
	 */
	public static boolean isEnabled() {
		return enabled;
	}

	void logEntry(DiagnosticContextItem dci) {
		if (!enabled) {
			return;
		}

		try {
			Logger dciLogger = getLogger(dci);
			if (loggerAdapter.isLogEntryEnabled(dciLogger)) {
				loggerAdapter.logEntry(dciLogger, LoggerMapper.toLoggable(dci));
			}
		} catch (Exception e) {
			logger.error("Failed to log method entry", e);
		}
	}

	public void logExitOrException(LoggableMethod loggableMethod, DiagnosticContext dc) {
		List<ThrowableInfo> thrown = dc.getThrown();

		if (!thrown.isEmpty()) {
			logException(loggableMethod, dc, thrown);
		} else {
			logExit(dc);
		}
	}

	void logExit(DiagnosticContext dc) {
		if (!enabled) {
			return;
		}

		try {
			Logger dcLogger = getLogger(dc.getRootItem());
			if (loggerAdapter.isLogExitEnabled(dcLogger)) {
				loggerAdapter.logExit(dcLogger, LoggerMapper.toLoggable(dc));
			}
		} catch (Exception e) {
			logger.error("Failed to log method exit", e);
		}
	}

	void logException(LoggableMethod loggableMethod, DiagnosticContext dc, List<ThrowableInfo> thrown) {
		if (!enabled) {
			return;
		}

		try {
			// exceptions whose resolved detail is empty are not logged at all
			List<ThrowableInfo> toLog = thrown.stream()
				.filter(ti -> !ExceptionDetailResolver.resolve(ti.getMethodInfo(), ti.getThrowable()).isEmpty())
				.toList();

			if (toLog.isEmpty()) {
				return;
			}

			Logger dcLogger = getLogger(dc.getRootItem());

			Level logLevel = toLog.stream()
				.map(ti -> exceptionLogLevelStrategy.getLogLevel(ti.getThrowable(), ti.getMethodInfo()))
				.max(Comparator.reverseOrder())
				.orElse(Level.INFO);

			// the same exception can be recorded on several items (a parent opting in with @OnException):
			// it is emitted as an exception node on each, but its text is only joined into the message once
			Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
			String exceptionMessage = toLog.stream()
				.filter(ti -> seen.add(ti.getThrowable()))
				.map(ti -> LogUtils.toLogSafeValue(ExceptionDetailResolver.describe(
					ti.getThrowable(), ExceptionDetailResolver.resolve(ti.getMethodInfo(), ti.getThrowable()))))
				.filter(m -> m != null && !m.isEmpty())
				.collect(Collectors.joining("; "));

			loggerAdapter.logException(dcLogger,
				logLevel,
				LoggerMapper.toLoggable(dc),
				exceptionMessage);
		} catch (Exception e) {
			logger.error("Failed to log exception", e);
		}
	}

	Logger getLogger(DiagnosticContextItem dci) {
		return (dci != null && dci.getMethodInfo() != null) ? dci.getMethodInfo().logger() : logger;
	}
}
