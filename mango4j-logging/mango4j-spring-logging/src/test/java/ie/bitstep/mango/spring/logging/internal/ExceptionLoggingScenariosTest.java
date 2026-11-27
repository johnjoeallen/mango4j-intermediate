package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.adapter.LoggerAdapter;
import ie.bitstep.mango.logging.annotations.LoggableClass;
import ie.bitstep.mango.logging.annotations.LoggableField;
import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.annotations.OnException;
import ie.bitstep.mango.logging.internal.LoggerMapper;
import ie.bitstep.mango.logging.test.MDCTestUtils;
import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContextThreadLocal;
import ie.bitstep.mango.spring.logging.detail.ExceptionDetailRules;
import ie.bitstep.mango.spring.logging.level.DefaultExceptionLogLevelStrategy;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CAUSE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CLASS_NAME;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.MESSAGE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.STACK_TRACE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End to end exception logging scenarios: the real aspect delegate, the real {@link DiagnosticContextLogger}
 * and the real level strategy, with an adapter that captures what would be logged.
 * Each scenario's captured output is written to {@code target/scenario-output/<test>.txt} for the test report.
 */
class ExceptionLoggingScenariosTest {

	// ---- fixtures ------------------------------------------------------------------------------

	static class PaymentException extends RuntimeException {
		PaymentException(String m) {
			super(m);
		}
	}

	@LoggableClass
	static class SecretException extends RuntimeException {
		@LoggableField(name = "code")
		private final String code;

		SecretException(String code) {
			super("secret failure");
			this.code = code;
		}
	}

	public static class DefaultDetail {
		@LoggableMethod
		public void call() {
		}
	}

	public static class NoDetail {
		@LoggableMethod(logException = {})
		public void call() {
		}
	}

	public static class ClassNameOnly {
		@LoggableMethod(logException = {CLASS_NAME})
		public void call() {
		}
	}

	public static class MessageAndCause {
		@LoggableMethod(logException = {MESSAGE, CAUSE})
		public void call() {
		}
	}

	public static class StackTraceOnly {
		@LoggableMethod(logException = {STACK_TRACE})
		public void call() {
		}
	}

	public static class MessageOnly {
		@LoggableMethod(logException = {MESSAGE})
		public void call() {
		}
	}

	public static class OnExceptionClassNameForIo {
		@LoggableMethod(logException = {MESSAGE})
		@OnException(type = IOException.class, detail = {CLASS_NAME})
		public void call() {
		}
	}

	public static class OnExceptionMessageForPayment {
		@LoggableMethod
		@OnException(type = PaymentException.class, detail = {MESSAGE})
		public void call() {
		}
	}

	// ---- harness -------------------------------------------------------------------------------

	private record Hop(Object target, UnaryOperator<Throwable> rethrow) {
		static Hop of(Object target) {
			return new Hop(target, t -> t);
		}

		static Hop wrapping(Object target, UnaryOperator<Throwable> wrap) {
			return new Hop(target, wrap);
		}
	}

	private record Logged(Level level, String message, String json) {
	}

	private final List<Logged> logged = new ArrayList<>();
	private String testName;
	private AspectLoggerDelegate delegate;

	@BeforeEach
	void setup(TestInfo info) {
		testName = info.getTestMethod().orElseThrow().getName();
		MDCTestUtils.init();
		DiagnosticContextThreadLocal.unset();
		ReflectionTestUtils.setField(DiagnosticContext.class, "clock", Clock.fixed(Instant.ofEpochMilli(1529000000000L), ZoneId.of("UTC")));

		LoggerAdapter capturing = new LoggerAdapter() {
			public boolean isLogEntryEnabled(Logger logger) {
				return false;
			}

			public void logEntry(Logger logger, Object loggable) {
			}

			public boolean isLogExitEnabled(Logger logger) {
				return true;
			}

			public void logExit(Logger logger, Object loggable) {
				logged.add(new Logged(Level.INFO, "", LoggerMapper.loggableToJsonString(loggable)));
			}

			public void logException(Logger logger, Level level, Object loggable, String message) {
				logged.add(new Logged(level, message, LoggerMapper.loggableToJsonString(loggable)));
			}
		};

		delegate = new AspectLoggerDelegate(new DiagnosticContextLogger(capturing, new DefaultExceptionLogLevelStrategy()));
		ReflectionTestUtils.setField(delegate, "clock", Clock.fixed(Instant.ofEpochMilli(1529000000000L), ZoneId.of("UTC")));
	}

	@AfterEach
	void destroy() throws IOException {
		ExceptionDetailResolver.setStrategy(null);
		MDCTestUtils.cleanup();

		StringBuilder out = new StringBuilder();
		if (logged.isEmpty()) {
			out.append("(nothing logged)\n");
		}
		for (Logged l : logged) {
			out.append("LEVEL:   ").append(l.level()).append('\n')
				.append("MESSAGE: ").append(l.message()).append('\n')
				.append("LOG:     ").append(l.json()).append('\n');
		}
		Path dir = Path.of("target", "scenario-output");
		Files.createDirectories(dir);
		Files.writeString(dir.resolve(testName + ".txt"), out.toString());
	}

	private ProceedingJoinPoint joinPoint(Object target) throws Exception {
		ProceedingJoinPoint jp = Mockito.mock(ProceedingJoinPoint.class);
		MethodSignature sig = Mockito.mock(MethodSignature.class);
		Mockito.when(sig.getMethod()).thenReturn(target.getClass().getMethod("call"));
		Mockito.when(jp.getSignature()).thenReturn(sig);
		Mockito.when(jp.getTarget()).thenReturn(target);
		Mockito.when(jp.getArgs()).thenReturn(new Object[0]);
		return jp;
	}

	/** hops[0] is the outermost method; the innermost throws {@code thrown}; each outer hop may rethrow a wrapper. */
	private void run(Throwable thrown, Hop... hops) throws Throwable {
		ProceedingJoinPoint[] jps = new ProceedingJoinPoint[hops.length];
		for (int i = 0; i < hops.length; i++) {
			jps[i] = joinPoint(hops[i].target());
		}
		for (int i = 0; i < hops.length; i++) {
			final int idx = i;
			if (i == hops.length - 1) {
				Mockito.when(jps[i].proceed()).thenThrow(thrown);
			} else {
				Mockito.when(jps[i].proceed()).thenAnswer(inv -> {
					try {
						return delegate.interceptLoggableMethod(jps[idx + 1]);
					} catch (Throwable t) {
						throw hops[idx].rethrow().apply(t);
					}
				});
			}
		}
		try {
			delegate.interceptLoggableMethod(jps[0]);
		} catch (Throwable expected) {
			// the exception always propagates to the caller
		}
	}

	private String only() {
		assertThat(logged).hasSize(1);
		return logged.get(0).json();
	}

	// ---- single method: detail from logException -----------------------------------------------

	@Test
	void singleMethod_default_logsMessageCauseAndStackTrace() throws Throwable {
		run(new IllegalStateException("boom", new IllegalArgumentException("inner")), Hop.of(new DefaultDetail()));

		assertThat(logged.get(0).level()).isEqualTo(Level.ERROR);
		assertThat(logged.get(0).message()).isEqualTo("boom; Cause: inner");
		assertThat(only()).contains("\"reason\":\"boom; Cause: inner\"").contains("\"stacktrace\"");
	}

	@Test
	void singleMethod_emptyDetail_logsNothing() throws Throwable {
		run(new IllegalStateException("boom"), Hop.of(new NoDetail()));

		assertThat(logged).isEmpty();
	}

	@Test
	void singleMethod_classNameOnly_noMessageNoStackTrace() throws Throwable {
		run(new IllegalStateException("boom-secret"), Hop.of(new ClassNameOnly()));

		assertThat(logged.get(0).message()).isEqualTo("java.lang.IllegalStateException");
		assertThat(only()).contains("\"reason\":\"java.lang.IllegalStateException\"")
			.doesNotContain("boom-secret").doesNotContain("\"stacktrace\"");
	}

	@Test
	void singleMethod_messageOnly_noCauseNoStackTrace() throws Throwable {
		run(new IllegalStateException("boom", new IllegalArgumentException("inner")), Hop.of(new MessageOnly()));

		assertThat(logged.get(0).message()).isEqualTo("boom");
		assertThat(only()).contains("\"reason\":\"boom\"").doesNotContain("inner").doesNotContain("\"stacktrace\"");
	}

	@Test
	void singleMethod_stackTraceOnly_stackTraceWithoutReason() throws Throwable {
		run(new IllegalStateException("boom"), Hop.of(new StackTraceOnly()));

		assertThat(logged.get(0).message()).isEmpty();
		assertThat(only()).contains("\"stacktrace\"").doesNotContain("\"reason\"");
	}

	@Test
	void singleMethod_messageAndCause_messageIncludesCause() throws Throwable {
		run(new IllegalStateException("boom", new IllegalArgumentException("inner")), Hop.of(new MessageAndCause()));

		assertThat(logged.get(0).message()).isEqualTo("boom; Cause: inner");
		assertThat(only()).doesNotContain("\"stacktrace\"");
	}

	// ---- precedence: @OnException > global rules > logException --------------------------------

	@Test
	void precedence_onExceptionOverridesLogException() throws Throwable {
		run(new IOException("disk-secret"), Hop.of(new OnExceptionClassNameForIo()));

		assertThat(logged.get(0).message()).isEqualTo("java.io.IOException");
		assertThat(only()).doesNotContain("disk-secret");
	}

	@Test
	void precedence_onExceptionNotMatching_fallsBackToLogException() throws Throwable {
		run(new IllegalStateException("boom"), Hop.of(new OnExceptionClassNameForIo()));

		assertThat(logged.get(0).message()).isEqualTo("boom");
	}

	@Test
	void precedence_globalRuleOverridesLogException() throws Throwable {
		ExceptionDetailResolver.setStrategy(ExceptionDetailRules.builder().on(IllegalStateException.class, CLASS_NAME).build());

		run(new IllegalStateException("boom-secret"), Hop.of(new DefaultDetail()));

		assertThat(logged.get(0).message()).isEqualTo("java.lang.IllegalStateException");
		assertThat(only()).doesNotContain("boom-secret").doesNotContain("\"stacktrace\"");
	}

	@Test
	void precedence_globalRuleWithNoDetail_logsNothing() throws Throwable {
		ExceptionDetailResolver.setStrategy(ExceptionDetailRules.builder().on(IllegalStateException.class).build());

		run(new IllegalStateException("boom"), Hop.of(new DefaultDetail()));

		assertThat(logged).isEmpty();
	}

	@Test
	void precedence_globalRuleNotMatching_fallsBackToLogException() throws Throwable {
		ExceptionDetailResolver.setStrategy(ExceptionDetailRules.builder().on(IOException.class, CLASS_NAME).build());

		run(new IllegalStateException("boom"), Hop.of(new MessageOnly()));

		assertThat(logged.get(0).message()).isEqualTo("boom");
	}

	@Test
	void precedence_onExceptionOverridesGlobalRule() throws Throwable {
		ExceptionDetailResolver.setStrategy(ExceptionDetailRules.builder().on(IOException.class).build());

		run(new IOException("disk"), Hop.of(new OnExceptionClassNameForIo()));

		assertThat(logged.get(0).message()).isEqualTo("java.io.IOException");
	}

	// ---- parent / child dedupe -----------------------------------------------------------------

	@Test
	void parentPropagatesChildException_loggedOnceAtChild() throws Throwable {
		run(new IllegalStateException("boom"), Hop.of(new DefaultDetail()), Hop.of(new MessageOnly()));

		assertThat(logged).hasSize(1);
		assertThat(only()).contains("\"called\"").contains("\"reason\":\"boom\"");
		assertThat(countOccurrences(only(), "\"reason\"")).isEqualTo(1);
	}

	@Test
	void parentWithMatchingOnException_loggedAtChildAndParent() throws Throwable {
		run(new PaymentException("card declined"), Hop.of(new OnExceptionMessageForPayment()), Hop.of(new MessageOnly()));

		assertThat(logged).hasSize(1);
		assertThat(countOccurrences(only(), "\"reason\"")).isEqualTo(2);
	}

	@Test
	void parentWrapsChildException_wrapperLoggedByParentAndChildKeepsOriginal() throws Throwable {
		run(new PaymentException("card declined"),
			Hop.wrapping(new MessageOnly(), t -> new RuntimeException("order failed", t)),
			Hop.of(new MessageOnly()));

		assertThat(countOccurrences(only(), "\"reason\"")).isEqualTo(2);
		assertThat(only()).contains("order failed").contains("card declined");
	}

	@Test
	void parentThrowsUnrelatedExceptionWithoutCause_childDetailsStillLogged() throws Throwable {
		run(new PaymentException("card declined"),
			Hop.wrapping(new MessageOnly(), t -> new IllegalStateException("order failed")),    // no cause: child exception is dropped
			Hop.of(new MessageAndCause()));

		assertThat(logged).hasSize(1);
		assertThat(logged.get(0).message()).isEqualTo("card declined; order failed");
		assertThat(only())
			.contains("\"called\":[{\"method\":\"MessageAndCause.call\",\"reason\":\"card declined\"")    // child's detail kept
			.contains("\"reason\":\"order failed\"");                                                          // parent's own
		assertThat(countOccurrences(only(), "\"reason\"")).isEqualTo(2);
	}

	@Test
	void childWithEmptyDetailParentThrowsUnrelatedException_onlyParentExceptionLogged() throws Throwable {
		run(new PaymentException("card-secret"),
			Hop.wrapping(new MessageOnly(), t -> new IllegalStateException("order failed")),
			Hop.of(new NoDetail()));

		assertThat(logged).hasSize(1);
		assertThat(logged.get(0).message()).isEqualTo("order failed");
		assertThat(only()).doesNotContain("card-secret");
	}

	@Test
	void exceptionPropagatesThroughTwoParents_loggedOnce() throws Throwable {
		run(new IllegalStateException("boom"), Hop.of(new DefaultDetail()), Hop.of(new DefaultDetail()), Hop.of(new MessageOnly()));

		assertThat(countOccurrences(only(), "\"reason\"")).isEqualTo(1);
	}

	@Test
	void childWithEmptyDetailPropagatesThroughPlainParent_nothingLoggedAtAll() throws Throwable {
		run(new IllegalStateException("boom"), Hop.of(new DefaultDetail()), Hop.of(new NoDetail()));

		assertThat(logged).isEmpty();
	}

	@Test
	void childWithEmptyDetailWrappedByParent_wrapperIsLogged() throws Throwable {
		run(new IllegalStateException("inner-boom"),
			Hop.wrapping(new MessageOnly(), t -> new RuntimeException("outer failed", t)),
			Hop.of(new NoDetail()));

		assertThat(logged.get(0).message()).isEqualTo("outer failed");
	}

	@Test
	void globalRuleDoesNotMakeParentRerecordChildException() throws Throwable {
		ExceptionDetailResolver.setStrategy(ExceptionDetailRules.builder().on(IllegalStateException.class, MESSAGE).build());

		run(new IllegalStateException("boom"), Hop.of(new DefaultDetail()), Hop.of(new DefaultDetail()));

		assertThat(countOccurrences(only(), "\"reason\"")).isEqualTo(1);
	}

	// ---- @LoggableClass exceptions -------------------------------------------------------------

	@Test
	void loggableClassException_withMessage_logsStructuredFields() throws Throwable {
		run(new SecretException("X-42"), Hop.of(new MessageOnly()));

		assertThat(only()).contains(SecretException.class.getName()).contains("X-42");
	}

	@Test
	void loggableClassException_classNameOnly_fieldsAreNotLogged() throws Throwable {
		run(new SecretException("X-42"), Hop.of(new ClassNameOnly()));

		assertThat(only()).contains("\"reason\":\"" + SecretException.class.getName() + "\"").doesNotContain("X-42");
	}

	private static int countOccurrences(String s, String sub) {
		int n = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + sub.length())) {
			n++;
		}
		return n;
	}
}
