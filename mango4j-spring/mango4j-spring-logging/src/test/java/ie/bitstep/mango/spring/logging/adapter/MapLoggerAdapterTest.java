package ie.bitstep.mango.spring.logging.adapter;

import ie.bitstep.mango.spring.logging.test.MapBuilder;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.slf4j.helpers.NOPLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static ie.bitstep.mango.spring.logging.dc.DiagnosticContext.LOG_KEY_ID;
import static net.logstash.logback.argument.StructuredArguments.entries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class MapLoggerAdapterTest {
	private static final String TEST_MESSAGE = "TEST_MESSAGE";

	@Mock
	private NOPLogger mockLogger;

	@InjectMocks
	private MapLoggerAdapter adapter;

	private Map<String, String> testLoggable;

	private Map<String, String> expectedLoggable;

	@BeforeEach
	void setup() throws Exception {
		MDCTestUtils.init();
		// Needed because NOOPLogger initialises MDC with NOOPAdapter

		testLoggable = new HashMap<>();
		testLoggable.put("ts", "ts1");
		testLoggable.put("key1", "value1");
		testLoggable.put("key2", "value2");

		expectedLoggable = new HashMap<>();
		expectedLoggable.put("key1", "value1");
		expectedLoggable.put("key2", "value2");
		// without the "ts"
		adapter.excludeKeys(DiagnosticContext.LOG_KEY_TIMESTAMP, LOG_KEY_ID);
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void postConstructorSetupTest() {
		MapLoggerAdapter adapterTest = new MapLoggerAdapter();
		assertThat(adapterTest.excludedKeys).containsExactly(DiagnosticContext.LOG_KEY_CORRELATION_ID,
			DiagnosticContext.LOG_KEY_TIMESTAMP,
			DiagnosticContext.LOG_KEY_URL);
	}

	@Test
	void testTimestampAndLogId() {
		MDC.put(LOG_KEY_ID, UUID.randomUUID().toString());
		adapter.includeKeys(DiagnosticContext.LOG_KEY_TIMESTAMP, LOG_KEY_ID);

		Map<String, Object> map = adapter.buildLogMap(testLoggable);

		assertThat(map)
			.containsKey(LOG_KEY_ID)
			.containsKey(DiagnosticContext.LOG_KEY_TIMESTAMP);

		map.remove(LOG_KEY_ID);
		map.remove(DiagnosticContext.LOG_KEY_TIMESTAMP);

		assertThat(map).isEqualTo(expectedLoggable);
	}

	@Test
	void isLogEntryEnabled_whenEnabled() throws Exception {
		// given
		given(mockLogger.isDebugEnabled()).willReturn(true);

		// when
		boolean actual = adapter.isLogEntryEnabled(mockLogger);

		// then
		assertThat(actual).isTrue();
	}

	@Test
	void isLogEntryEnabled_whenDisabled() throws Exception {
		// given
		given(mockLogger.isDebugEnabled()).willReturn(false);

		// when
		boolean actual = adapter.isLogEntryEnabled(mockLogger);

		// then
		assertThat(actual).isFalse();
	}

	@Test
	void logEntry_whenEnabled() throws Exception {
		// given
		given(mockLogger.isDebugEnabled()).willReturn(true);

		// when
		adapter.logEntry(mockLogger, testLoggable);

		// then
		verify(mockLogger).isDebugEnabled();
		verify(mockLogger).debug(null, entries(expectedLoggable));
	}

	@Test
	void logEntry_whenDisabled() throws Exception {
		// given
		given(mockLogger.isDebugEnabled()).willReturn(false);

		// when
		adapter.logEntry(mockLogger, testLoggable);

		// then
		verify(mockLogger).isDebugEnabled();
		verifyNoMoreInteractions(mockLogger);
	}

	@Test
	void logEntry_whenFailed() throws Exception {
		// given
		given(mockLogger.isDebugEnabled()).willReturn(true);

		// when
		adapter.logEntry(mockLogger, "failed to convert");

		// then
		verify(mockLogger).debug(null, entries(MapBuilder.<String, String>map().with("failure", "failed to convert").build()));
	}

	@Test
	void isLogExitEnabled_whenEnabled() throws Exception {
		// given
		given(mockLogger.isInfoEnabled()).willReturn(true);

		// when
		boolean actual = adapter.isLogExitEnabled(mockLogger);

		// then
		assertThat(actual).isTrue();
	}

	@Test
	void isLogExitEnabled_whenDisabled() throws Exception {
		// given
		given(mockLogger.isInfoEnabled()).willReturn(false);

		// when
		boolean actual = adapter.isLogExitEnabled(mockLogger);

		// then
		assertThat(actual).isFalse();
	}

	@Test
	void logExit_whenEnabled() throws Exception {
		// given
		given(mockLogger.isInfoEnabled()).willReturn(true);

		// when
		adapter.logExit(mockLogger, testLoggable);

		// then
		verify(mockLogger).isInfoEnabled();
		verify(mockLogger).info(null, entries(expectedLoggable));
	}

	@Test
	void logExit_whenDisabled() throws Exception {
		// given
		given(mockLogger.isInfoEnabled()).willReturn(false);

		// when
		adapter.logExit(mockLogger, testLoggable);

		// then
		verify(mockLogger).isInfoEnabled();
		verifyNoMoreInteractions(mockLogger);
	}

	@Test
	void logExit_whenFailed() throws Exception {
		// given
		given(mockLogger.isInfoEnabled()).willReturn(true);

		// when
		adapter.logExit(mockLogger, "failed to convert");

		// then
		verify(mockLogger).info(null, entries(MapBuilder.<String, String>map().with("failure", "failed to convert").build()));
	}

	@Test
	void logException_whenTraceAndEnabled() throws Exception {
		// given
		given(mockLogger.isTraceEnabled()).willReturn(true);

		// when
		adapter.logException(mockLogger, Level.TRACE, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).isTraceEnabled();
		verify(mockLogger).trace(TEST_MESSAGE, entries(expectedLoggable));
	}

	@Test
	void logException_whenTraceAndDisabled() throws Exception {
		// given
		given(mockLogger.isTraceEnabled()).willReturn(false);

		// when
		adapter.logException(mockLogger, Level.TRACE, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).isTraceEnabled();
		verifyNoMoreInteractions(mockLogger);
	}

	@Test
	void logException_whenDebugAndEnabled() throws Exception {
		// given
		given(mockLogger.isDebugEnabled()).willReturn(true);

		// when
		adapter.logException(mockLogger, Level.DEBUG, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).isDebugEnabled();
		verify(mockLogger).debug(TEST_MESSAGE, entries(expectedLoggable));
	}

	@Test
	void logException_whenDebugAndDisabled() throws Exception {
		// given
		given(mockLogger.isDebugEnabled()).willReturn(false);

		// when
		adapter.logException(mockLogger, Level.DEBUG, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).isDebugEnabled();
		verifyNoMoreInteractions(mockLogger);
	}

	@Test
	void logException_whenInfoAndEnabled() throws Exception {
		// given
		given(mockLogger.isInfoEnabled()).willReturn(true);

		// when
		adapter.logException(mockLogger, Level.INFO, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).isInfoEnabled();
		verify(mockLogger).info(TEST_MESSAGE, entries(expectedLoggable));
	}

	@Test
	void logException_whenInfoAndDisabled() throws Exception {
		// given
		given(mockLogger.isInfoEnabled()).willReturn(false);

		// when
		adapter.logException(mockLogger, Level.INFO, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).isInfoEnabled();
		verifyNoMoreInteractions(mockLogger);
	}

	@Test
	void logException_whenWarnAndEnabled() throws Exception {
		// given
		given(mockLogger.isWarnEnabled()).willReturn(true);

		// when
		adapter.logException(mockLogger, Level.WARN, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).isWarnEnabled();
		verify(mockLogger).warn(TEST_MESSAGE, entries(expectedLoggable));
	}

	@Test
	void logException_whenWarnAndDisabled() throws Exception {
		// given
		given(mockLogger.isWarnEnabled()).willReturn(false);

		// when
		adapter.logException(mockLogger, Level.WARN, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).isWarnEnabled();
		verifyNoMoreInteractions(mockLogger);
	}

	@Test
	void logException_whenErrorAndEnabled() throws Exception {
		// given
		given(mockLogger.isErrorEnabled()).willReturn(true);

		// when
		adapter.logException(mockLogger, Level.ERROR, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).isErrorEnabled();
		verify(mockLogger).error(TEST_MESSAGE, entries(expectedLoggable));
	}

	@Test
	void logException_whenErrorAndDisabled() throws Exception {
		// given
		given(mockLogger.isErrorEnabled()).willReturn(false);

		// when
		adapter.logException(mockLogger, Level.ERROR, testLoggable, TEST_MESSAGE);

		// then
		verify(mockLogger).isErrorEnabled();
		verifyNoMoreInteractions(mockLogger);
	}
}
