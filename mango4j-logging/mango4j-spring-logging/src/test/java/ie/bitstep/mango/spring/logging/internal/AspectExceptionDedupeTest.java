package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.annotations.OnException;
import ie.bitstep.mango.logging.internal.LoggerMapper;
import ie.bitstep.mango.logging.test.MDCTestUtils;
import ie.bitstep.mango.spring.aop.ThrowableInfo;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContextThreadLocal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CLASS_NAME;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.REASON;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * End to end through {@link AspectLoggerDelegate}: a parent that lets its child's exception propagate
 * records it once, unless the parent opts in with {@link OnException}.
 */
class AspectExceptionDedupeTest {

	static class Child {
		@LoggableMethod
		public void fail() {
		}
	}

	static class ClassNameOnlyChild {
		@LoggableMethod(logException = {REASON, CLASS_NAME})
		public void fail() {
		}
	}

	static class OnExceptionChild {
		@LoggableMethod
		@OnException(type = IllegalStateException.class, detail = {})
		public void fail() {
		}
	}

	static class PlainParent {
		@LoggableMethod
		public void call() {
		}
	}

	static class OptInParent {
		@LoggableMethod
		@OnException(type = IllegalStateException.class, detail = {REASON, MESSAGE})
		public void call() {
		}
	}

	private final DiagnosticContextLogger dcLogger = Mockito.mock(DiagnosticContextLogger.class);
	private final AspectLoggerDelegate delegate = new AspectLoggerDelegate(dcLogger);
	private List<ThrowableInfo> thrownAtExit;
	private String jsonAtExit;

	@BeforeEach
	void setup() {
		MDCTestUtils.init();
		DiagnosticContextThreadLocal.unset();
		doAnswer(i -> {
			DiagnosticContext dc = i.getArgument(1);
			thrownAtExit = new ArrayList<>(dc.getThrown());
			jsonAtExit = LoggerMapper.loggableToJsonString(dc.toLoggable());
			return null;
		}).when(dcLogger).logExitOrException(any(LoggableMethod.class), any(DiagnosticContext.class));
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	private ProceedingJoinPoint joinPoint(Object target, String method) throws Exception {
		ProceedingJoinPoint jp = Mockito.mock(ProceedingJoinPoint.class);
		MethodSignature sig = Mockito.mock(MethodSignature.class);
		Mockito.when(sig.getMethod()).thenReturn(target.getClass().getMethod(method));
		Mockito.when(jp.getSignature()).thenReturn(sig);
		Mockito.when(jp.getTarget()).thenReturn(target);
		Mockito.when(jp.getArgs()).thenReturn(new Object[0]);
		return jp;
	}

	private void parentCallsFailingChild(Object parent, IllegalStateException ex) throws Throwable {
		parentCallsFailingChild(parent, new Child(), ex);
	}

	private void parentCallsFailingChild(Object parent, Object child, IllegalStateException ex) throws Throwable {
		ProceedingJoinPoint childJp = joinPoint(child, "fail");
		Mockito.when(childJp.proceed()).thenThrow(ex);

		ProceedingJoinPoint parentJp = joinPoint(parent, "call");
		Mockito.when(parentJp.proceed()).thenAnswer(i -> delegate.interceptLoggableMethod(childJp));

		assertThatThrownBy(() -> delegate.interceptLoggableMethod(parentJp)).isSameAs(ex);
	}

	@Test
	void parentWithoutOnException_exceptionRecordedOnceAgainstChild() throws Throwable {
		IllegalStateException ex = new IllegalStateException("boom");

		parentCallsFailingChild(new PlainParent(), ex);

		assertThat(thrownAtExit).hasSize(1);
		assertThat(thrownAtExit.get(0).getThrowable()).isSameAs(ex);
		assertThat(thrownAtExit.get(0).getMethodInfo().getMethodName()).isEqualTo("fail");
	}

	@Test
	void parentWithMatchingOnException_exceptionRecordedForChildAndParent() throws Throwable {
		IllegalStateException ex = new IllegalStateException("boom");

		parentCallsFailingChild(new OptInParent(), ex);

		assertThat(thrownAtExit).hasSize(2);
		assertThat(thrownAtExit).extracting(t -> t.getMethodInfo().getMethodName()).containsExactly("fail", "call");
	}

	@Test
	void defaultDetail_outputHasReasonAndStackTrace() throws Throwable {
		parentCallsFailingChild(new PlainParent(), new IllegalStateException("boom-secret"));

		assertThat(jsonAtExit).contains("\"reason\":\"boom-secret\"").contains("\"stacktrace\"");
	}

	@Test
	void classNameOnly_outputHasClassNameButNoMessageOrStackTrace() throws Throwable {
		parentCallsFailingChild(new PlainParent(), new ClassNameOnlyChild(), new IllegalStateException("boom-secret"));

		assertThat(jsonAtExit)
			.contains("\"reason\":\"java.lang.IllegalStateException\"")
			.doesNotContain("boom-secret")
			.doesNotContain("\"stacktrace\"");
	}

	@Test
	void onExceptionWithEmptyDetail_nothingLoggedForException() throws Throwable {
		parentCallsFailingChild(new PlainParent(), new OnExceptionChild(), new IllegalStateException("boom-secret"));

		assertThat(jsonAtExit).doesNotContain("boom-secret").doesNotContain("\"reason\"").doesNotContain("\"stacktrace\"");
	}
}
