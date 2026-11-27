package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.adapter.LoggerAdapter;
import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.annotations.OnException;
import ie.bitstep.mango.logging.test.MDCTestUtils;
import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.aop.ThrowableInfo;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.level.ExceptionLogLevelStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;

import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CAUSE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CLASS_NAME;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * How {@link DiagnosticContextLogger} turns the resolved exception detail into the logged message and level.
 */
@ExtendWith(MockitoExtension.class)
class DiagnosticContextLoggerExceptionDetailTest {

	@SuppressWarnings("unused")
	static class Fixture {
		@LoggableMethod
		public void defaults() {
		}

		@LoggableMethod(logException = {})
		public void none() {
		}

		@LoggableMethod(logException = {CLASS_NAME})
		public void classOnly() {
		}

		@LoggableMethod(logException = {MESSAGE, CAUSE})
		public void messageAndCause() {
		}

		@LoggableMethod
		@OnException(type = IllegalStateException.class, detail = {})
		public void onExceptionNone() {
		}
	}

	@InjectMocks
	private DiagnosticContextLogger diagnosticContextLogger;

	@Mock
	private LoggerAdapter loggerAdapter;

	@Mock
	private ExceptionLogLevelStrategy levelStrategy;

	private DiagnosticContext dc;
	private LoggableMethod rootMethod;

	@BeforeEach
	void setup() throws Exception {
		MDCTestUtils.init();
		DiagnosticContextLogger.setEnabled(true);
		MethodInfo root = mi("defaults");
		rootMethod = Fixture.class.getMethod("defaults").getAnnotation(LoggableMethod.class);
		dc = new DiagnosticContext();
		dc.push(root);
	}

	@AfterEach
	void destroy() {
		DiagnosticContextLogger.setEnabled(true);
		MDCTestUtils.cleanup();
	}

	private static MethodInfo mi(String name) throws NoSuchMethodException {
		return new MethodInfo(Fixture.class, Fixture.class.getMethod(name));
	}

	private void thrown(Throwable t, String method) throws NoSuchMethodException {
		dc.getThrown().add(new ThrowableInfo(t, mi(method)));
	}

	@Test
	void classNameOnly_messageIsClassName() throws Exception {
		IllegalStateException ex = new IllegalStateException("secret");
		thrown(ex, "classOnly");
		given(levelStrategy.getLogLevel(eq(ex), any())).willReturn(Level.WARN);

		diagnosticContextLogger.logExitOrException(rootMethod, dc);

		verify(loggerAdapter).logException(any(), eq(Level.WARN), any(), eq("java.lang.IllegalStateException"));
	}

	@Test
	void messageAndCause_messageIncludesCause() throws Exception {
		RuntimeException ex = new RuntimeException("outer", new IllegalArgumentException("inner"));
		thrown(ex, "messageAndCause");
		given(levelStrategy.getLogLevel(eq(ex), any())).willReturn(Level.ERROR);

		diagnosticContextLogger.logExitOrException(rootMethod, dc);

		verify(loggerAdapter).logException(any(), eq(Level.ERROR), any(), eq("outer; Cause: inner"));
	}

	@Test
	void mixedDetail_droppedExceptionIsIgnoredForMessageAndLevel() throws Exception {
		IllegalStateException dropped = new IllegalStateException("dropped");
		RuntimeException logged = new RuntimeException("kept");
		thrown(dropped, "none");
		thrown(logged, "defaults");
		given(levelStrategy.getLogLevel(eq(logged), any())).willReturn(Level.WARN);

		diagnosticContextLogger.logExitOrException(rootMethod, dc);

		verify(loggerAdapter).logException(any(), eq(Level.WARN), any(), eq("kept"));
		verify(levelStrategy, never()).getLogLevel(eq(dropped), any());
	}

	@Test
	void mixedDetail_messagesJoinedForLoggedExceptionsOnly() throws Exception {
		RuntimeException first = new RuntimeException("first");
		IllegalStateException dropped = new IllegalStateException("dropped");
		IllegalArgumentException second = new IllegalArgumentException("second");
		thrown(first, "defaults");
		thrown(dropped, "none");
		thrown(second, "classOnly");
		given(levelStrategy.getLogLevel(any(), any())).willReturn(Level.WARN);

		diagnosticContextLogger.logExitOrException(rootMethod, dc);

		verify(loggerAdapter).logException(any(), eq(Level.WARN), any(), eq("first; java.lang.IllegalArgumentException"));
	}

	@Test
	void onExceptionWithEmptyDetail_nothingLogged() throws Exception {
		thrown(new IllegalStateException("x"), "onExceptionNone");

		diagnosticContextLogger.logExitOrException(rootMethod, dc);

		verify(loggerAdapter, never()).logException(any(), any(), any(), any());
	}

	@Test
	void onExceptionNotMatching_usesLogException() throws Exception {
		RuntimeException ex = new RuntimeException("kept");
		thrown(ex, "onExceptionNone");
		given(levelStrategy.getLogLevel(eq(ex), any())).willReturn(Level.WARN);

		diagnosticContextLogger.logExitOrException(rootMethod, dc);

		verify(loggerAdapter).logException(any(), eq(Level.WARN), any(), eq("kept"));
	}
}
