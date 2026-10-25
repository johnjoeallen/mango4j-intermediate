package ie.bitstep.mango.spring.logging.adapter;

import ie.bitstep.mango.spring.logging.test.MapBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultLoggerAdapterTest {
	private static final String TEST_MESSAGE = "TEST_MESSAGE";

	@Mock
	private Logger mockLogger;

	@InjectMocks
	private DefaultLoggerAdapter loggerAdapter;

	private Map<String, String> testLoggable = MapBuilder.<String, String>map()
		.with(
			"key1", "val1")
		.with(
			"key2", "val2")
		.build();

	private String testLoggableStr = "{\"key1\":\"val1\",\"key2\":\"val2\"}";

	@Test
	void isLogEntryEnabled_whenTrue() {
		// given
		when(mockLogger.isDebugEnabled()).thenReturn(true);

		// when
		boolean result = loggerAdapter.isLogEntryEnabled(mockLogger);

		// then
		assertThat(result).isTrue();
	}

	@Test
	void isLogEntryEnabled_whenFalse() {
		// given
		when(mockLogger.isDebugEnabled()).thenReturn(false);

		// when
		boolean result = loggerAdapter.isLogEntryEnabled(mockLogger);

		// then
		assertThat(result).isFalse();
	}

	@Test
	void logEntry() {
		// given
		when(mockLogger.isDebugEnabled()).thenReturn(true);

		// when
		loggerAdapter.logEntry(mockLogger, testLoggable);

		// then
		verify(mockLogger).debug(testLoggableStr);
	}

	@Test
	void logEntryDisabled() {
		// given
		when(mockLogger.isDebugEnabled()).thenReturn(false);

		// when
		loggerAdapter.logEntry(mockLogger, testLoggable);

		// then
		verify(mockLogger, never()).debug(testLoggableStr);
	}

	@Test
	void isLogExitEnabled_whenTrue() {
		// given
		when(mockLogger.isInfoEnabled()).thenReturn(true);

		// when
		boolean result = loggerAdapter.isLogExitEnabled(mockLogger);

		// then
		assertThat(result).isTrue();
	}

	@Test
	void isLogExitEnabled_whenFalse() {
		// given
		when(mockLogger.isInfoEnabled()).thenReturn(false);

		// when
		boolean result = loggerAdapter.isLogExitEnabled(mockLogger);

		// then
		assertThat(result).isFalse();
	}

	@Test
	void logExit() {
		// given
		when(mockLogger.isInfoEnabled()).thenReturn(true);

		// when
		loggerAdapter.logExit(mockLogger, testLoggable);

		// then
		verify(mockLogger).info(testLoggableStr);
	}

	@Test
	void logExitDisabled() {
		// given
		when(mockLogger.isInfoEnabled()).thenReturn(false);

		// when
		loggerAdapter.logExit(mockLogger, testLoggable);

		// then
		verify(mockLogger, never()).info(testLoggableStr);
	}

	@Test
	void logException_whenTrace() {
		// given
		when(mockLogger.isTraceEnabled()).thenReturn(true);

		// when
		loggerAdapter.logException(mockLogger, Level.TRACE, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).trace("{}: {}", testLoggableStr, TEST_MESSAGE);
	}

	@Test
	void logException_whenTrace_Disabled() {
		// given
		when(mockLogger.isTraceEnabled()).thenReturn(false);

		// when
		loggerAdapter.logException(mockLogger, Level.TRACE, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger, never()).trace("{}: {}", testLoggableStr, TEST_MESSAGE);
	}

	@Test
	void logException_whenDebug() {
		// given
		when(mockLogger.isDebugEnabled()).thenReturn(true);

		// when
		loggerAdapter.logException(mockLogger, Level.DEBUG, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).debug("{}: {}", testLoggableStr, TEST_MESSAGE);
	}

	@Test
	void logException_whenDebug_Disabled() {
		// given
		when(mockLogger.isDebugEnabled()).thenReturn(false);

		// when
		loggerAdapter.logException(mockLogger, Level.DEBUG, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger, never()).debug("{}: {}", testLoggableStr, TEST_MESSAGE);
	}

	@Test
	void logException_whenInfo() {
		// given
		when(mockLogger.isInfoEnabled()).thenReturn(true);

		// when
		loggerAdapter.logException(mockLogger, Level.INFO, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).info("{}: {}", testLoggableStr, TEST_MESSAGE);
	}

	@Test
	void logException_whenInfo_Disabled() {
		// given
		when(mockLogger.isInfoEnabled()).thenReturn(false);

		// when
		loggerAdapter.logException(mockLogger, Level.INFO, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger, never()).info("{}: {}", testLoggableStr, TEST_MESSAGE);
	}

	@Test
	void logException_whenWarn() {
		// given
		when(mockLogger.isWarnEnabled()).thenReturn(true);

		// when
		loggerAdapter.logException(mockLogger, Level.WARN, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).warn("{}: {}", testLoggableStr, TEST_MESSAGE);
	}

	@Test
	void logException_whenWarn_Disabled() {
		// given
		when(mockLogger.isWarnEnabled()).thenReturn(false);

		// when
		loggerAdapter.logException(mockLogger, Level.WARN, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger, never()).warn("{}: {}", testLoggableStr, TEST_MESSAGE);
	}

	@Test
	void logException_whenError() {
		// given
		when(mockLogger.isErrorEnabled()).thenReturn(true);

		// when
		loggerAdapter.logException(mockLogger, Level.ERROR, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).error("{}: {}", testLoggableStr, TEST_MESSAGE);
	}

	@Test
	void logException_whenError_Disabled() {
		// given
		when(mockLogger.isErrorEnabled()).thenReturn(false);

		// when
		loggerAdapter.logException(mockLogger, Level.ERROR, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger, never()).error("{}: {}", testLoggableStr, TEST_MESSAGE);
	}

}