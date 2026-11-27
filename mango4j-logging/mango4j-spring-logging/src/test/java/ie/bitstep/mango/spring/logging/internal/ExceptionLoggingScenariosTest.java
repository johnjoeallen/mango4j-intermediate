package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.adapter.LoggerAdapter;
import ie.bitstep.mango.logging.annotations.LoggableClass;
import ie.bitstep.mango.logging.annotations.LoggableField;
import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.annotations.OnException;
import ie.bitstep.mango.logging.internal.LoggerMapper;
import ie.bitstep.mango.logging.test.MDCTestUtils;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.test.data.AccountLockedException;
import ie.bitstep.mango.spring.logging.test.data.PaymentDeclinedException;
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
import tools.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.function.UnaryOperator;

import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CAUSE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CLASS_NAME;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.REASON;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.EXCEPTION;
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
		@LoggableMethod(logException = {REASON, CLASS_NAME})
		public void call() {
		}
	}

	public static class MessageAndCause {
		@LoggableMethod(logException = {REASON, MESSAGE, CAUSE})
		public void call() {
		}
	}

	public static class StackTraceOnly {
		@LoggableMethod(logException = {STACK_TRACE})
		public void call() {
		}
	}

	public static class MessageAndStackTrace {
		@LoggableMethod(logException = {MESSAGE, STACK_TRACE})
		public void call() {
		}
	}

	public static class ReasonAndCause {
		@LoggableMethod(logException = {REASON, CAUSE})
		public void call() {
		}
	}

	public static class ReasonExceptionCause {
		@LoggableMethod(logException = {REASON, EXCEPTION, CAUSE})
		public void call() {
		}
	}

	public static class MessageCauseAndStackTrace {
		@LoggableMethod(logException = {MESSAGE, CAUSE, STACK_TRACE})
		public void call() {
		}
	}

	public static class CauseAndStackTrace {
		@LoggableMethod(logException = {CAUSE, STACK_TRACE})
		public void call() {
		}
	}

	public static class ClassNameAndStackTrace {
		@LoggableMethod(logException = {REASON, CLASS_NAME, STACK_TRACE})
		public void call() {
		}
	}

	public static class MessageOnly {
		@LoggableMethod(logException = {REASON, MESSAGE})
		public void call() {
		}
	}

	public static class ExceptionFieldsOnly {
		@LoggableMethod(logException = {EXCEPTION})
		public void call() {
		}
	}

	public static class OnExceptionNone {
		@LoggableMethod
		@OnException(type = IllegalStateException.class, detail = {})
		public void call() {
		}
	}

	public static class OnExceptionTypeArray {
		@LoggableMethod
		@OnException(type = {IOException.class, IllegalStateException.class}, detail = {REASON, CLASS_NAME})
		public void call() {
		}
	}

	public static class OnExceptionCatchAll {
		@LoggableMethod(logException = {})
		@OnException(detail = {REASON, MESSAGE})
		public void call() {
		}
	}

	public static class OnExceptionBroadRuleFirst {
		@LoggableMethod
		@OnException(type = RuntimeException.class, detail = {REASON, CLASS_NAME})
		@OnException(type = IllegalStateException.class, detail = {REASON, MESSAGE})
		public void call() {
		}
	}

	public static class OnExceptionSpecificRuleFirst {
		@LoggableMethod
		@OnException(type = IllegalStateException.class, detail = {REASON, MESSAGE})
		@OnException(type = RuntimeException.class, detail = {REASON, CLASS_NAME})
		public void call() {
		}
	}

	public static class OnExceptionSuperclassRule {
		@LoggableMethod(logException = {})
		@OnException(type = RuntimeException.class, detail = {REASON, MESSAGE})
		public void call() {
		}
	}

	public static class OnExceptionFullDetail {
		@LoggableMethod(logException = {})
		@OnException(type = IOException.class, detail = {REASON, MESSAGE, CAUSE, STACK_TRACE})
		public void call() {
		}
	}

	public static class OnExceptionLoggableClass {
		@LoggableMethod(logException = {})
		@OnException(type = PaymentDeclinedException.class, detail = {REASON, EXCEPTION})
		public void call() {
		}
	}

	public static class OnExceptionPerType {
		@LoggableMethod(logException = {})
		@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})
		@OnException(type = PaymentException.class, detail = {REASON, MESSAGE, STACK_TRACE})
		public void call() {
		}
	}

	public static class OnExceptionOverEmptyLogException {
		@LoggableMethod(logException = {})
		@OnException(type = IllegalStateException.class, detail = {REASON, MESSAGE})
		public void call() {
		}
	}

	public static class OnExceptionClassNameAndMessage {
		@LoggableMethod
		@OnException(type = IllegalStateException.class, detail = {REASON, CLASS_NAME, MESSAGE})
		public void call() {
		}
	}

	public static class OnExceptionParentEmptyDetail {
		@LoggableMethod
		@OnException(type = PaymentException.class, detail = {})
		public void call() {
		}
	}

	public static class OnExceptionParentClassName {
		@LoggableMethod
		@OnException(type = PaymentException.class, detail = {REASON, CLASS_NAME})
		public void call() {
		}
	}

	public static class OnExceptionParentOtherType {
		@LoggableMethod
		@OnException(type = IOException.class, detail = {REASON, MESSAGE})
		public void call() {
		}
	}

	public static class OnExceptionClassNameForIo {
		@LoggableMethod(logException = {REASON, MESSAGE})
		@OnException(type = IOException.class, detail = {REASON, CLASS_NAME})
		public void call() {
		}
	}

	public static class OnExceptionMessageForPayment {
		@LoggableMethod
		@OnException(type = PaymentException.class, detail = {REASON, MESSAGE})
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

	@SuppressWarnings("unchecked")
	private Map<String, Object> logMap() {
		return new ObjectMapper().readValue(only(), Map.class);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> node(Map<String, Object> map, String key) {
		return (Map<String, Object>) map.get(key);
	}

	@SuppressWarnings("unchecked")
	private static List<String> trace(Map<String, Object> map) {
		return (List<String>) map.get("stacktrace");
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
		assertThat(logged.get(0).message()).isEqualTo("boom");
		Map<String, Object> log = logMap();
		assertThat(log).containsEntry("reason", "boom").containsKey("stacktrace");
		assertThat(node(log, "cause")).containsEntry("reason", "inner").containsKey("stacktrace");
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
	void singleMethod_stackTraceOnly_neverLeaksMessagesFromTheExceptionOrItsCauses() throws Throwable {
		run(new IllegalStateException("outer-LEAKA", new IllegalArgumentException("inner-LEAKB")), Hop.of(new StackTraceOnly()));

		assertThat(only()).contains("\"stacktrace\"")
			.contains("java.lang.IllegalStateException")
			.doesNotContain("Caused by")
			.doesNotContain("LEAK");
	}

	@Test
	void singleMethod_messageAndStackTrace_messageShownInTraceNotAsReason() throws Throwable {
		run(new IllegalStateException("boom-MSG", new IllegalArgumentException("inner-MSG")), Hop.of(new MessageAndStackTrace()));

		assertThat(only()).doesNotContain("\"reason\"")
			.contains("java.lang.IllegalStateException: boom-MSG")
			.doesNotContain("Caused by").doesNotContain("inner-MSG");
	}

	@Test
	void singleMethod_messageCauseAndStackTrace_causeNodeHasMessageAndOwnTrace() throws Throwable {
		run(new IllegalStateException("boom-MSG", new IllegalArgumentException("inner-MSG")), Hop.of(new MessageCauseAndStackTrace()));

		Map<String, Object> log = logMap();
		assertThat(log).doesNotContainKey("reason");
		assertThat(trace(log).get(0)).isEqualTo("java.lang.IllegalStateException: boom-MSG");
		assertThat(node(log, "cause")).doesNotContainKey("reason");
		assertThat(trace(node(log, "cause")).get(0)).isEqualTo("java.lang.IllegalArgumentException: inner-MSG");
		assertThat(only()).doesNotContain("Caused by");
	}

	@Test
	void singleMethod_causeAndStackTrace_causeNodeHasClassNamesOnly() throws Throwable {
		run(new IllegalStateException("boom-MSG", new IllegalArgumentException("inner-MSG")), Hop.of(new CauseAndStackTrace()));

		Map<String, Object> log = logMap();
		assertThat(trace(log).get(0)).isEqualTo("java.lang.IllegalStateException");
		assertThat(trace(node(log, "cause")).get(0)).isEqualTo("java.lang.IllegalArgumentException");
		assertThat(only()).doesNotContain("MSG");
	}

	@Test
	void singleMethod_classNameAndStackTrace_traceHasNoMessages() throws Throwable {
		run(new IllegalStateException("boom-MSG", new IllegalArgumentException("inner-MSG")), Hop.of(new ClassNameAndStackTrace()));

		assertThat(only()).contains("\"reason\":\"java.lang.IllegalStateException\"")
			.doesNotContain("Caused by")
			.doesNotContain("MSG");
	}

	@Test
	void cause_reasonAndCause_causeIsANestedNodeWithItsOwnReason() throws Throwable {
		run(new IllegalStateException("boom", new IllegalArgumentException("inner")), Hop.of(new ReasonAndCause()));

		assertThat(logged.get(0).message()).isEqualTo("boom");
		Map<String, Object> log = logMap();
		assertThat(log).containsEntry("reason", "boom").doesNotContainKey("stacktrace");
		assertThat(node(log, "cause")).containsEntry("reason", "inner").doesNotContainKey("stacktrace");
	}

	@Test
	void cause_chainOfThreeExceptions_nestedOneInsideTheOther() throws Throwable {
		run(new IllegalStateException("outer", new IllegalArgumentException("middle", new IOException("inner"))),
			Hop.of(new MessageAndCause()));

		Map<String, Object> log = logMap();
		assertThat(log).containsEntry("reason", "outer");
		assertThat(node(log, "cause")).containsEntry("reason", "middle");
		assertThat(node(node(log, "cause"), "cause")).containsEntry("reason", "inner").doesNotContainKey("cause");
	}

	@Test
	void cause_causeWithoutMessage_reasonIsClassName() throws Throwable {
		run(new IllegalStateException("boom", new IllegalArgumentException()), Hop.of(new MessageAndCause()));

		assertThat(node(logMap(), "cause")).containsEntry("reason", "java.lang.IllegalArgumentException");
	}

	@Test
	void cause_selectedButExceptionHasNoCause_nothingAppended() throws Throwable {
		run(new IllegalStateException("boom"), Hop.of(new DefaultDetail()));

		assertThat(logged.get(0).message()).isEqualTo("boom");
		assertThat(only()).contains("\"reason\":\"boom\"").doesNotContain("Cause:").doesNotContain("Caused by");
	}

	@Test
	void cause_defaultDetail_causeNodeHasItsOwnStackTrace() throws Throwable {
		run(new IllegalStateException("boom", new IllegalArgumentException("inner")), Hop.of(new DefaultDetail()));

		Map<String, Object> log = logMap();
		assertThat(trace(log)).noneMatch(l -> l.startsWith("Caused by"));
		assertThat(trace(node(log, "cause")).get(0)).isEqualTo("java.lang.IllegalArgumentException: inner");
	}

	@Test
	void cause_deepChainWithStackTraceOnly_eachCauseNodeHasItsOwnTrace() throws Throwable {
		run(new IllegalStateException("outer", new IllegalArgumentException("middle", new IOException("inner"))),
			Hop.of(new CauseAndStackTrace()));

		Map<String, Object> log = logMap();
		assertThat(trace(log).get(0)).isEqualTo("java.lang.IllegalStateException");
		assertThat(trace(node(log, "cause")).get(0)).isEqualTo("java.lang.IllegalArgumentException");
		assertThat(trace(node(node(log, "cause"), "cause")).get(0)).isEqualTo("java.io.IOException");
		assertThat(only()).doesNotContain("outer").doesNotContain("middle").doesNotContain("inner").doesNotContain("Caused by");
	}

	@Test
	void cause_depthLimit_onlyFiveNestedLevels() throws Throwable {
		Throwable chain = null;
		for (int i = 7; i >= 0; i--) {
			chain = new RuntimeException("level-" + i, chain);
		}

		run(chain, Hop.of(new ReasonAndCause()));

		Map<String, Object> node = logMap();
		int depth = 0;
		while (node.containsKey("cause")) {
			node = node(node, "cause");
			depth++;
		}
		assertThat(depth).isEqualTo(5);
		assertThat(node).containsEntry("reason", "level-5");
		assertThat(only()).doesNotContain("level-6").doesNotContain("level-7");
	}

	// ---- @LoggableClass exceptions in a cause chain, with masked fields ------------------------------

	private static Throwable declinedBecauseLocked() {
		AccountLockedException locked = new AccountLockedException("Account locked after 5 failed attempts", "ACC-77889900", 5,
			new IOException("Connection reset"));
		return new PaymentDeclinedException("Payment declined for order 1042", "1042", "4111111111111111", "05", locked);
	}

	@Test
	void loggableClassChain_default_eachNodeHasItsOwnMaskedExceptionFields() throws Throwable {
		run(declinedBecauseLocked(), Hop.of(new DefaultDetail()));

		Map<String, Object> log = logMap();
		assertThat(log).containsEntry("reason", "Payment declined for order 1042");
		assertThat(node(log, "exception")).containsEntry("class", PaymentDeclinedException.class.getName())
			.containsEntry("cardNumber", "4111********1111").containsEntry("orderId", "1042").containsEntry("declineCode", "05");
		Map<String, Object> cause = node(log, "cause");
		assertThat(cause).containsEntry("reason", "Account locked after 5 failed attempts");
		assertThat(node(cause, "exception")).containsEntry("class", AccountLockedException.class.getName())
			.containsEntry("failedAttempts", 5).containsKey("accountId");
		assertThat(node(cause, "cause")).containsEntry("reason", "Connection reset").doesNotContainKey("exception");
		assertThat(only()).doesNotContain("4111111111111111").doesNotContain("ACC-77889900");
	}

	@Test
	void loggableClassChain_reasonExceptionAndCause_noStackTraces() throws Throwable {
		run(declinedBecauseLocked(), Hop.of(new ReasonExceptionCause()));

		assertThat(only()).doesNotContain("stacktrace").contains("\"cardNumber\":\"4111********1111\"")
			.doesNotContain("4111111111111111").doesNotContain("ACC-77889900");
	}

	@Test
	void singleMethod_messageAndCause_causeIsANestedNode() throws Throwable {
		run(new IllegalStateException("boom", new IllegalArgumentException("inner")), Hop.of(new MessageAndCause()));

		assertThat(logged.get(0).message()).isEqualTo("boom");
		Map<String, Object> log = logMap();
		assertThat(log).containsEntry("reason", "boom").doesNotContainKey("stacktrace");
		assertThat(node(log, "cause")).containsEntry("reason", "inner").doesNotContainKey("stacktrace");
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
		ExceptionDetailResolver.setStrategy(ExceptionDetailRules.builder().on(IllegalStateException.class, REASON, CLASS_NAME).build());

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
		ExceptionDetailResolver.setStrategy(ExceptionDetailRules.builder().on(IOException.class, REASON, CLASS_NAME).build());

		run(new IllegalStateException("boom"), Hop.of(new MessageOnly()));

		assertThat(logged.get(0).message()).isEqualTo("boom");
	}

	@Test
	void precedence_onExceptionOverridesGlobalRule() throws Throwable {
		ExceptionDetailResolver.setStrategy(ExceptionDetailRules.builder().on(IOException.class).build());

		run(new IOException("disk"), Hop.of(new OnExceptionClassNameForIo()));

		assertThat(logged.get(0).message()).isEqualTo("java.io.IOException");
	}

	// ---- @OnException settings ---------------------------------------------------------------------

	@Test
	void onException_emptyDetail_logsNothingForTheMatchingType() throws Throwable {
		run(new IllegalStateException("boom"), Hop.of(new OnExceptionNone()));

		assertThat(logged).isEmpty();
	}

	@Test
	void onException_typeArray_ruleAppliesToEveryListedType() throws Throwable {
		run(new IllegalStateException("boom-MSG"), Hop.of(new OnExceptionTypeArray()));

		assertThat(logged.get(0).message()).isEqualTo("java.lang.IllegalStateException");
		assertThat(only()).doesNotContain("boom-MSG").doesNotContain("stacktrace");
	}

	@Test
	void onException_defaultType_matchesAnyException() throws Throwable {
		run(new IOException("disk full"), Hop.of(new OnExceptionCatchAll()));

		assertThat(logged.get(0).message()).isEqualTo("disk full");
		assertThat(logMap()).containsEntry("reason", "disk full").doesNotContainKey("stacktrace");
	}

	@Test
	void onException_repeated_firstMatchingRuleWins_broadRuleFirst() throws Throwable {
		run(new IllegalStateException("boom-MSG"), Hop.of(new OnExceptionBroadRuleFirst()));

		assertThat(logged.get(0).message()).isEqualTo("java.lang.IllegalStateException");
		assertThat(only()).doesNotContain("boom-MSG");
	}

	@Test
	void onException_repeated_firstMatchingRuleWins_specificRuleFirst() throws Throwable {
		run(new IllegalStateException("boom-MSG"), Hop.of(new OnExceptionSpecificRuleFirst()));

		assertThat(logged.get(0).message()).isEqualTo("boom-MSG");
	}

	@Test
	void onException_ruleForASuperclass_matchesSubclasses() throws Throwable {
		run(new PaymentException("card declined"), Hop.of(new OnExceptionSuperclassRule()));

		assertThat(logged.get(0).message()).isEqualTo("card declined");
	}

	@Test
	void onException_fullDetail_reasonCauseNodeAndStackTraces() throws Throwable {
		run(new IOException("disk full", new IllegalStateException("device lost")), Hop.of(new OnExceptionFullDetail()));

		Map<String, Object> log = logMap();
		assertThat(log).containsEntry("reason", "disk full");
		assertThat(trace(log).get(0)).isEqualTo("java.io.IOException: disk full");
		assertThat(node(log, "cause")).containsEntry("reason", "device lost");
		assertThat(trace(node(log, "cause")).get(0)).isEqualTo("java.lang.IllegalStateException: device lost");
	}

	@Test
	void onException_exceptionDetail_logsMaskedLoggableClassFields() throws Throwable {
		run(new PaymentDeclinedException("Payment declined for order 1042", "1042", "4111111111111111", "05", null),
			Hop.of(new OnExceptionLoggableClass()));

		Map<String, Object> log = logMap();
		assertThat(log).containsEntry("reason", "Payment declined for order 1042").doesNotContainKey("stacktrace");
		assertThat(node(log, "exception")).containsEntry("cardNumber", "4111********1111").containsEntry("orderId", "1042");
		assertThat(only()).doesNotContain("4111111111111111");
	}

	@Test
	void onException_differentRulesPerType_ioExceptionGetsClassNameOnly() throws Throwable {
		run(new IOException("disk-MSG"), Hop.of(new OnExceptionPerType()));

		assertThat(logged.get(0).message()).isEqualTo("java.io.IOException");
		assertThat(only()).doesNotContain("disk-MSG").doesNotContain("stacktrace");
	}

	@Test
	void onException_differentRulesPerType_paymentExceptionGetsMessageAndStackTrace() throws Throwable {
		run(new PaymentException("card declined"), Hop.of(new OnExceptionPerType()));

		Map<String, Object> log = logMap();
		assertThat(log).containsEntry("reason", "card declined");
		assertThat(trace(log).get(0)).endsWith("PaymentException: card declined");
	}

	@Test
	void onException_overridesAnEmptyLogException_forTheMatchingType() throws Throwable {
		run(new IllegalStateException("boom"), Hop.of(new OnExceptionOverEmptyLogException()));

		assertThat(logged.get(0).message()).isEqualTo("boom");
	}

	@Test
	void onException_nonMatchingTypeFallsBackToAnEmptyLogException_nothingLogged() throws Throwable {
		run(new IOException("disk"), Hop.of(new OnExceptionOverEmptyLogException()));

		assertThat(logged).isEmpty();
	}

	@Test
	void onException_classNameAndMessage_classNameThenMessage() throws Throwable {
		run(new IllegalStateException("boom"), Hop.of(new OnExceptionClassNameAndMessage()));

		assertThat(logged.get(0).message()).isEqualTo("java.lang.IllegalStateException: boom");
		assertThat(logMap()).containsEntry("reason", "java.lang.IllegalStateException: boom");
	}

	@Test
	void onException_parentOptsInWithEmptyDetail_onlyTheChildShowsTheException() throws Throwable {
		run(new PaymentException("card declined"), Hop.of(new OnExceptionParentEmptyDetail()), Hop.of(new MessageOnly()));

		assertThat(logged).hasSize(1);
		assertThat(logged.get(0).message()).isEqualTo("card declined");
		assertThat(countOccurrences(only(), "\"reason\"")).isEqualTo(1);    // the child's; the parent logs nothing for it
	}

	@Test
	void onException_parentOptsInWithDifferentDetail_eachItemUsesItsOwnDetail() throws Throwable {
		run(new PaymentException("card declined"), Hop.of(new OnExceptionParentClassName()), Hop.of(new MessageOnly()));

		Map<String, Object> log = logMap();
		assertThat(log).containsEntry("reason", PaymentException.class.getName());    // parent: class name
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> called = (List<Map<String, Object>>) log.get("called");
		assertThat(called.get(0)).containsEntry("reason", "card declined");           // child: message
		assertThat(logged.get(0).message()).isEqualTo("card declined");
	}

	@Test
	void onException_parentRuleForAnotherType_doesNotOptIn_childExceptionStaysOnTheChild() throws Throwable {
		run(new PaymentException("card declined"), Hop.of(new OnExceptionParentOtherType()), Hop.of(new MessageOnly()));

		assertThat(countOccurrences(only(), "\"reason\"")).isEqualTo(1);
		assertThat(logMap()).doesNotContainKey("reason");
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
	void parentWithMatchingOnException_exceptionNodeOnBothItemsButMessageOnlyOnce() throws Throwable {
		run(new PaymentException("card declined"), Hop.of(new OnExceptionMessageForPayment()), Hop.of(new MessageOnly()));

		assertThat(logged).hasSize(1);
		assertThat(logged.get(0).message()).isEqualTo("card declined");    // the same exception is not joined with itself
		assertThat(countOccurrences(only(), "\"reason\"")).isEqualTo(2);    // but it is recorded on the child and the parent
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
		ExceptionDetailResolver.setStrategy(ExceptionDetailRules.builder().on(IllegalStateException.class, REASON, MESSAGE).build());

		run(new IllegalStateException("boom"), Hop.of(new DefaultDetail()), Hop.of(new DefaultDetail()));

		assertThat(countOccurrences(only(), "\"reason\"")).isEqualTo(1);
	}

	// ---- @LoggableClass exceptions -------------------------------------------------------------

	@Test
	void loggableClassException_default_reasonTextAndExceptionFieldsLoggedSeparately() throws Throwable {
		run(new SecretException("X-42"), Hop.of(new DefaultDetail()));

		assertThat(only())
			.contains("\"reason\":\"secret failure\"")
			.contains("\"exception\":{\"class\":\"" + SecretException.class.getName() + "\",\"code\":\"X-42\"}");
	}

	@Test
	void loggableClassException_exceptionOnly_fieldsWithoutReasonText() throws Throwable {
		run(new SecretException("X-42"), Hop.of(new ExceptionFieldsOnly()));

		assertThat(only()).contains("\"exception\":{").contains("X-42")
			.doesNotContain("\"reason\"").doesNotContain("secret failure");
	}

	@Test
	void loggableClassException_reasonOnly_textWithoutFields() throws Throwable {
		run(new SecretException("X-42"), Hop.of(new MessageOnly()));

		assertThat(only()).contains("\"reason\":\"secret failure\"").doesNotContain("\"exception\"").doesNotContain("X-42");
	}

	@Test
	void loggableClassException_classNameOnly_fieldsAreNotLogged() throws Throwable {
		run(new SecretException("X-42"), Hop.of(new ClassNameOnly()));

		assertThat(only()).contains("\"reason\":\"" + SecretException.class.getName() + "\"")
			.doesNotContain("\"exception\"").doesNotContain("X-42");
	}

	private static int countOccurrences(String s, String sub) {
		int n = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + sub.length())) {
			n++;
		}
		return n;
	}
}
