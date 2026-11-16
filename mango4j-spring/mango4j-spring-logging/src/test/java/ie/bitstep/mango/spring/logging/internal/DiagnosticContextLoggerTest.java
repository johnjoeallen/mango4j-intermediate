package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.aop.ThrowableInfo;
import ie.bitstep.mango.spring.logging.LogUtils;
import ie.bitstep.mango.spring.logging.adapter.LoggerAdapter;
import ie.bitstep.mango.spring.logging.annotations.LoggableMethod;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.level.ExceptionLogLevelStrategy;
import ie.bitstep.mango.spring.logging.test.MDCTestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
class DiagnosticContextLoggerTest {
	@InjectMocks
	private DiagnosticContextLogger diagnosticContextLogger;

	@Mock
	private LoggerAdapter mockLoggerAdapter;

	@Mock
	private ExceptionLogLevelStrategy exceptionLogLevelStrategy;

	private Logger testLogger = LoggerFactory.getLogger(DiagnosticContextLoggerTest.class);

	@Captor
	private ArgumentCaptor<Object> loggableCaptor;
	@Captor
	private ArgumentCaptor<String> messageCaptor;

	private MethodInfo methodInfo;
	private DiagnosticContext dc;
	private DiagnosticContextItem dci;

	private LoggableMethod loggableMethod;

	@BeforeEach
	@LoggableMethod
	public void setup() throws Exception {
		MDCTestUtils.init();
		DiagnosticContextLogger.setEnabled(true); // Reset to enabled for each test
		methodInfo = new MethodInfo(DiagnosticContextLoggerTest.class, DiagnosticContextLoggerTest.class.getMethod("setup"));
		loggableMethod = LogUtils.getLoggableMethod(methodInfo);

		dc = new DiagnosticContext();
		dc.push(methodInfo);

		dci = dc.getRootItem();
	}

	@AfterEach
	void destroy() {
		DiagnosticContextLogger.setEnabled(true); // Reset to enabled after each test
		MDCTestUtils.cleanup();
	}

	@LoggableMethod(logException = false)
	public void logExceptionDisabled() {

	}

	@Test
	void logEntry_whenEnabled() {
		// given
		given(mockLoggerAdapter.isLogEntryEnabled(testLogger)).willReturn(true);

		// when
		diagnosticContextLogger.logEntry(dci);

		// then
		verify(mockLoggerAdapter).logEntry(eq(testLogger), loggableCaptor.capture());

		assertThat(LoggerMapper.loggableToJsonString(loggableCaptor.getValue())).isEqualTo("{\"method\":\"DiagnosticContextLoggerTest.setup\"}");
	}

	@Test
	void logEntry_exception() throws NoSuchFieldException, IllegalAccessException {
		// given
		RuntimeException runtimeException = new RuntimeException();
		given(mockLoggerAdapter.isLogEntryEnabled(testLogger)).willThrow(runtimeException);
		Logger mockedLogger = mock(Logger.class);

		Field loggerField = DiagnosticContextLogger.class.getDeclaredField("logger");
		Object initialvalue = loggerField.get(diagnosticContextLogger);
		loggerField.setAccessible(true);

		loggerField.set(diagnosticContextLogger, mockedLogger);

		// when
		diagnosticContextLogger.logEntry(dci);

		// then
		verify(mockLoggerAdapter, never()).logEntry(any(), any());
		verifyNoMoreInteractions(mockLoggerAdapter);
		verify(mockedLogger, times(1)).error("Failed to log method entry", runtimeException);
		verifyNoMoreInteractions(mockedLogger);
		loggerField.set(diagnosticContextLogger, initialvalue);

	}

	@Test
	void logEntry_whenEnabledException() {
		// given

		// when
		diagnosticContextLogger.logEntry(dci);

		assertThatNoException();
	}

	@Test
	void logEntry_whenDisabled() {
		// given
		given(mockLoggerAdapter.isLogEntryEnabled(testLogger)).willReturn(false);

		// when
		diagnosticContextLogger.logEntry(dci);

		// then
		verify(mockLoggerAdapter).isLogEntryEnabled(testLogger);
		verifyNoMoreInteractions(mockLoggerAdapter);
	}

	@Test
	void logExitOrException_whenExitIsEnabled() {

		// given
		given(mockLoggerAdapter.isLogExitEnabled(testLogger)).willReturn(true);

		// when
		diagnosticContextLogger.logExitOrException(loggableMethod, dc);

		// then
		verify(mockLoggerAdapter).logExit(eq(testLogger), loggableCaptor.capture());
		Map<String, Object> captured = (Map<String, Object>) loggableCaptor.getValue();
		captured.remove(DiagnosticContext.LOG_KEY_TIMESTAMP);

		assertThat(LoggerMapper.loggableToJsonString(captured)).isEqualTo("{\"method\":\"DiagnosticContextLoggerTest.setup\"}");
	}

	@Test
	void logExitOrException_whenExitIsDisabled() {
		// given
		given(mockLoggerAdapter.isLogExitEnabled(testLogger)).willReturn(false);

		// when
		diagnosticContextLogger.logExitOrException(loggableMethod, dc);

		// then
		verify(mockLoggerAdapter).isLogExitEnabled(testLogger);
		verifyNoMoreInteractions(mockLoggerAdapter);
	}

	@Test
	void logExitOrException_whenException() {
		// given
		RuntimeException ex = new RuntimeException("failed");
		dc.getThrown().add(new ThrowableInfo(ex, methodInfo));
		when(exceptionLogLevelStrategy.getLogLevel(any(), any())).thenReturn(Level.ERROR);

		// when
		diagnosticContextLogger.logExitOrException(loggableMethod, dc);

		// then
		verify(mockLoggerAdapter).logException(eq(testLogger), eq(Level.ERROR), loggableCaptor.capture(), messageCaptor.capture());
		Map<String, Object> captured = (Map<String, Object>) loggableCaptor.getValue();
		captured.remove(DiagnosticContext.LOG_KEY_TIMESTAMP);

		assertThat(LoggerMapper.loggableToJsonString(captured)).isEqualTo("{\"method\":\"DiagnosticContextLoggerTest.setup\"}");
		assertThat(messageCaptor.getValue()).isEqualTo("failed");
	}

	@Test
	void logExitOrException_whenExceptionLogExceptionDisabled() throws NoSuchMethodException {
		// given
		methodInfo = new MethodInfo(DiagnosticContextLoggerTest.class, DiagnosticContextLoggerTest.class.getMethod("logExceptionDisabled"));
		loggableMethod = LogUtils.getLoggableMethod(methodInfo);
		dc = new DiagnosticContext();
		dc.push(methodInfo);

		dci = dc.getRootItem();
		RuntimeException ex = new RuntimeException("failed");
		dc.getThrown().add(new ThrowableInfo(ex, methodInfo));
		when(exceptionLogLevelStrategy.getLogLevel(any(), any())).thenReturn(Level.ERROR);


		// when
		diagnosticContextLogger.logExitOrException(loggableMethod, dc);

		// then
		verify(mockLoggerAdapter).logException(eq(testLogger), eq(Level.ERROR), loggableCaptor.capture(), messageCaptor.capture());
		Map<String, Object> captured = (Map<String, Object>) loggableCaptor.getValue();
		captured.remove(DiagnosticContext.LOG_KEY_TIMESTAMP);

		assertThat(LoggerMapper.loggableToJsonString(captured)).isEqualTo("{\"method\":\"DiagnosticContextLoggerTest.logExceptionDisabled\"}");
		assertThat(messageCaptor.getValue()).isEmpty();
	}

	@Test
	void logExitOrException_whenAdapterIsNull() {
		// given
		RuntimeException ex = new RuntimeException("failed");
		dc.getThrown().add(new ThrowableInfo(ex, methodInfo));

		// when
		diagnosticContextLogger.logExitOrException(loggableMethod, dc);
		assertThatNoException();
	}

	@Test
	void logExit_whenException() {
		// given

		// when
		diagnosticContextLogger.logExit(null);
		assertThatNoException();
	}

	@Test
	void testGetLoggerNullContextItem() {
		assertThat(diagnosticContextLogger.getLogger(null)).isEqualTo(DiagnosticContextLogger.logger);
	}

	@Test
	void testGetLoggerContextItemNullMethodInfo() {
		DiagnosticContextItem dci = new DiagnosticContextItem(null, null);
		assertThat(diagnosticContextLogger.getLogger(dci)).isEqualTo(DiagnosticContextLogger.logger);
	}

	@Test
	void testGetLoggerContextItem() {
		MethodInfo mi = new MethodInfo(DiagnosticContextLoggerTest.class, null);
		Logger logger = LoggerFactory.getLogger(DiagnosticContextLoggerTest.class);
		ReflectionTestUtils.setField(mi, "logger", logger);
		DiagnosticContextItem item = new DiagnosticContextItem(mi, null);
		assertThat(diagnosticContextLogger.getLogger(item)).isEqualTo(logger);
	}

	@Test
	void testSetEnabled_setsEnabledFlag() {
		// given
		DiagnosticContextLogger.setEnabled(false);

		// then
		assertThat(DiagnosticContextLogger.isEnabled()).isFalse();

		// cleanup
		DiagnosticContextLogger.setEnabled(true);
	}

	@Test
	void testIsEnabled_defaultsToTrue() {
		// then
		assertThat(DiagnosticContextLogger.isEnabled()).isTrue();
	}

	@Test
	void testLogEntry_whenGloballyDisabled() {
		// given
		DiagnosticContextLogger.setEnabled(false);

		// when
		diagnosticContextLogger.logEntry(dci);

		// then - adapter should not be called at all
		verify(mockLoggerAdapter, never()).isLogEntryEnabled(any());
		verify(mockLoggerAdapter, never()).logEntry(any(), any());
	}

	@Test
	void testLogExit_whenGloballyDisabled() {
		// given
		DiagnosticContextLogger.setEnabled(false);

		// when
		diagnosticContextLogger.logExit(dc);

		// then - adapter should not be called at all
		verify(mockLoggerAdapter, never()).isLogExitEnabled(any());
		verify(mockLoggerAdapter, never()).logExit(any(), any());
	}

	@Test
	void testLogException_whenGloballyDisabled() {
		// given
		DiagnosticContextLogger.setEnabled(false);
		RuntimeException ex = new RuntimeException("failed");
		dc.getThrown().add(new ThrowableInfo(ex, methodInfo));

		// when
		diagnosticContextLogger.logExitOrException(loggableMethod, dc);

		// then - adapter should not be called at all
		verify(mockLoggerAdapter, never()).logException(any(), any(), any(), any());
	}
}
