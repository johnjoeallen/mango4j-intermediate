package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.annotations.LoggableClass;
import ie.bitstep.mango.logging.annotations.LoggableField;
import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.internal.LoggerMapper;
import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.test.data.AccountLockedException;
import ie.bitstep.mango.spring.logging.test.data.PaymentDeclinedException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CAUSE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CLASS_NAME;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.EXCEPTION;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.MESSAGE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.REASON;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.STACK_TRACE;
import static org.assertj.core.api.Assertions.assertThat;

class DiagnosticContextItemExceptionDetailTest {

	@LoggableClass
	static class SecretException extends RuntimeException {
		@LoggableField(name = "code")
		private final String code;

		SecretException(String code) {
			super("failed");
			this.code = code;
		}
	}

	@SuppressWarnings("unused")
	static class Fixture {
		@LoggableMethod
		public void defaults() {
		}

		@LoggableMethod(logException = {REASON, MESSAGE})
		public void reasonOnly() {
		}

		@LoggableMethod(logException = {EXCEPTION})
		public void exceptionOnly() {
		}

		@LoggableMethod(logException = {REASON, CLASS_NAME})
		public void classNameOnly() {
		}

		@LoggableMethod(logException = {MESSAGE, STACK_TRACE})
		public void messageWithoutReason() {
		}

		@LoggableMethod(logException = {REASON, CLASS_NAME, STACK_TRACE})
		public void classNameWithStackTrace() {
		}

		@LoggableMethod(logException = {MESSAGE, CAUSE, STACK_TRACE})
		public void messageCauseStackTrace() {
		}

		@LoggableMethod(logException = {CAUSE, STACK_TRACE})
		public void causeStackTrace() {
		}

		@LoggableMethod(logException = {REASON, CAUSE})
		public void reasonAndCause() {
		}

		@LoggableMethod(logException = {REASON, CAUSE, STACK_TRACE})
		public void reasonCauseStackTrace() {
		}

		@LoggableMethod(logException = {CAUSE})
		public void causeOnly() {
		}

		@LoggableMethod(logException = {REASON, CLASS_NAME, CAUSE, STACK_TRACE})
		public void classNameAndCause() {
		}

		@LoggableMethod(logException = {STACK_TRACE})
		public void stackTraceOnly() {
		}

		@LoggableMethod(logException = {})
		public void none() {
		}
	}

	private static DiagnosticContextItem item(String method) throws NoSuchMethodException {
		return new DiagnosticContextItem(new MethodInfo(Fixture.class, Fixture.class.getMethod(method)), null);
	}

	private static Map<String, Object> loggable(String method, Throwable t) throws NoSuchMethodException {
		DiagnosticContextItem item = item(method);
		item.putThrowable(t);
		return item.toLoggable();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> exceptionOf(Map<String, Object> loggable) {
		return (Map<String, Object>) loggable.get("exception");
	}

	@Test
	void loggableClassException_default_logsReasonTextAndExceptionFieldsSeparately() throws Exception {
		Map<String, Object> loggable = loggable("defaults", new SecretException("X-42"));

		assertThat(loggable).containsEntry("reason", "failed").containsKey("stacktrace");
		assertThat(exceptionOf(loggable)).containsExactly(
			Map.entry("class", SecretException.class.getName()), Map.entry("code", "X-42"));
		assertThat(LoggerMapper.loggableToJsonString(loggable)).contains("\"exception\":{\"class\":\"" + SecretException.class.getName() + "\",\"code\":\"X-42\"}");
	}

	@LoggableClass
	static class ClashingFieldException extends RuntimeException {
		@LoggableField(name = "class")
		private final String clazz = "from-field";

		ClashingFieldException() {
			super("clash");
		}
	}

	@Test
	void exception_classIsAFieldAndCannotBeOverriddenByALoggableFieldNamedClass() throws Exception {
		Map<String, Object> loggable = loggable("defaults", new ClashingFieldException());

		assertThat(exceptionOf(loggable)).containsEntry("class", ClashingFieldException.class.getName());
		assertThat(LoggerMapper.loggableToJsonString(loggable)).doesNotContain("from-field");
	}

	@Test
	void loggableClassException_exceptionOnly_noReasonText() throws Exception {
		Map<String, Object> loggable = loggable("exceptionOnly", new SecretException("X-42"));

		assertThat(loggable).containsKey("exception").doesNotContainKeys("reason", "stacktrace");
	}

	@Test
	void loggableClassException_reasonOnly_noExceptionFields() throws Exception {
		Map<String, Object> loggable = loggable("reasonOnly", new SecretException("X-42"));

		assertThat(loggable).containsEntry("reason", "failed").doesNotContainKey("exception");
		assertThat(LoggerMapper.loggableToJsonString(loggable)).doesNotContain("X-42");
	}

	@Test
	void loggableClassException_classNameOnly_reasonIsClassNameAndFieldsAreNotLogged() throws Exception {
		Map<String, Object> loggable = loggable("classNameOnly", new SecretException("X-42"));

		assertThat(loggable).containsEntry("reason", SecretException.class.getName()).doesNotContainKey("exception");
		assertThat(LoggerMapper.loggableToJsonString(loggable)).doesNotContain("X-42");
	}

	@Test
	void messageWithoutReason_noReasonText_butStackTraceStillLogged() throws Exception {
		Map<String, Object> loggable = loggable("messageWithoutReason", new SecretException("X-42"));

		assertThat(loggable).doesNotContainKeys("reason", "exception").containsKey("stacktrace");
	}

	@SuppressWarnings("unchecked")
	private static List<Object> rawTraceOf(Map<String, Object> node) {
		return (List<Object>) node.get("stacktrace");
	}

	/** the string lines of the stack trace (the exception line and its frames), without the nested cause object */
	private static List<String> stackTraceOf(Map<String, Object> node) {
		return rawTraceOf(node).stream().filter(String.class::isInstance).map(String.class::cast).toList();
	}

	/** the cause nested at the end of the stack trace array, or null */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> causeOf(Map<String, Object> node) {
		List<Object> trace = node == null ? null : rawTraceOf(node);
		if (trace == null || trace.isEmpty() || !(trace.get(trace.size() - 1) instanceof Map<?, ?> last)) {
			return null;
		}
		return (Map<String, Object>) last.get("cause");
	}

	/** number of nested cause nodes below the item */
	private static int causeDepth(Map<String, Object> loggable) {
		int depth = 0;
		for (Map<String, Object> node = causeOf(loggable); node != null; node = causeOf(node)) {
			depth++;
		}
		return depth;
	}

	private static Throwable chain(int length) {
		Throwable t = null;
		for (int i = length - 1; i >= 0; i--) {
			t = new RuntimeException("level-" + i, t);
		}
		return t;
	}

	@Test
	void stackTrace_withMessageSelected_includesTheMessageOnEachExceptionsOwnLine() throws Exception {
		RuntimeException ex = new RuntimeException("outer-msg", new IllegalStateException("inner-msg"));

		Map<String, Object> loggable = loggable("messageCauseStackTrace", ex);

		assertThat(stackTraceOf(loggable).get(0)).isEqualTo("java.lang.RuntimeException: outer-msg");
		assertThat(stackTraceOf(loggable)).noneMatch(l -> l.startsWith("Caused by"));
		assertThat(stackTraceOf(causeOf(loggable)).get(0)).isEqualTo("java.lang.IllegalStateException: inner-msg");
	}

	@Test
	void stackTrace_withMessageSelectedButNoReason_stillShowsMessageInTraceOnly() throws Exception {
		Map<String, Object> loggable = loggable("messageWithoutReason", new RuntimeException("outer-msg"));

		assertThat(loggable).doesNotContainKey("reason");
		assertThat(stackTraceOf(loggable).get(0)).contains("outer-msg");
	}

	@Test
	void stackTrace_withoutMessageSelected_isClassNamesOnly() throws Exception {
		RuntimeException ex = new RuntimeException("outer-msg", new IllegalStateException("inner-msg"));

		for (String method : new String[]{"classNameWithStackTrace", "stackTraceOnly"}) {
			List<String> trace = stackTraceOf(loggable(method, ex));

			assertThat(trace.get(0)).isEqualTo("java.lang.RuntimeException");
			assertThat(String.join("\n", trace)).doesNotContain("outer-msg").doesNotContain("inner-msg");
		}
	}

	@Test
	void cause_nestedInTheStackTraceOnlyWhenCauseIsSelected() throws Exception {
		RuntimeException ex = new RuntimeException("outer-msg", new IllegalStateException("inner-msg"));
		ex.addSuppressed(new IllegalArgumentException("suppressed-msg"));

		for (String method : new String[]{"messageWithoutReason", "classNameWithStackTrace", "stackTraceOnly"}) {
			Map<String, Object> loggable = loggable(method, ex);

			assertThat(causeOf(loggable)).isNull();
			assertThat(loggable).doesNotContainKey("cause");
			assertThat(rawTraceOf(loggable)).allMatch(String.class::isInstance);    // strings only
		}

		Map<String, Object> withCause = loggable("messageCauseStackTrace", ex);
		assertThat(causeOf(withCause)).isNotNull();
		assertThat(withCause).doesNotContainKey("cause");                          // never beside the stack trace
		assertThat(stackTraceOf(withCause)).noneMatch(l -> l.startsWith("Caused by") || l.startsWith("Suppressed"));
		assertThat(stackTraceOf(withCause).get(0)).isEqualTo("java.lang.RuntimeException: outer-msg");
	}

	@Test
	void cause_eachCauseIsNestedInsideTheStackTraceOfItsParent_andReasonIsNotSuffixed() throws Exception {
		RuntimeException ex = new RuntimeException("outer", new IllegalStateException("middle", new IllegalArgumentException("inner")));

		Map<String, Object> loggable = loggable("reasonCauseStackTrace", ex);

		assertThat(loggable).containsEntry("reason", "outer");
		assertThat(causeOf(loggable)).containsEntry("reason", "middle");
		assertThat(causeOf(causeOf(loggable))).containsEntry("reason", "inner");
		assertThat(causeOf(causeOf(causeOf(loggable)))).isNull();
	}

	@Test
	void cause_withoutStackTrace_doesNothing() throws Exception {
		RuntimeException ex = new RuntimeException("outer", new IllegalStateException("inner"));

		Map<String, Object> loggable = loggable("reasonAndCause", ex);

		assertThat(loggable).containsEntry("reason", "outer").doesNotContainKeys("stacktrace", "cause");
	}

	@Test
	void cause_selectedButNoCause_noCauseObject() throws Exception {
		Map<String, Object> loggable = loggable("reasonCauseStackTrace", new RuntimeException("outer"));

		assertThat(loggable).containsEntry("reason", "outer");
		assertThat(causeOf(loggable)).isNull();
		assertThat(rawTraceOf(loggable)).allMatch(String.class::isInstance);
	}

	@Test
	void cause_selectedWithoutReasonOrStackTrace_nothingToShow() throws Exception {
		Map<String, Object> loggable = loggable("causeOnly", new RuntimeException("outer", new IllegalStateException("inner")));

		assertThat(loggable).doesNotContainKeys("reason", "stacktrace", "exception");
	}

	@Test
	void cause_defaultDetail_causeNodeHasItsOwnReasonAndStackTrace() throws Exception {
		RuntimeException ex = new RuntimeException("outer", new IllegalStateException("inner"));

		Map<String, Object> loggable = loggable("defaults", ex);

		assertThat(loggable).containsEntry("reason", "outer");
		assertThat(causeOf(loggable)).containsEntry("reason", "inner");
		assertThat(stackTraceOf(causeOf(loggable)).get(0)).isEqualTo("java.lang.IllegalStateException: inner");
	}

	@Test
	void cause_depthIsLimitedToFiveNestedNodes() throws Exception {
		Map<String, Object> loggable = loggable("reasonCauseStackTrace", chain(9));    // top + 8 causes

		assertThat(causeDepth(loggable)).isEqualTo(DiagnosticContextItem.MAX_CAUSE_DEPTH).isEqualTo(5);
		Map<String, Object> node = loggable;
		for (int i = 0; i < 5; i++) {
			node = causeOf(node);
		}
		assertThat(node).containsEntry("reason", "level-5").doesNotContainKey("cause");
		assertThat(LoggerMapper.loggableToJsonString(loggable)).doesNotContain("level-6");
	}

	@Test
	void cause_exactlyFiveCauses_allShown_andFewerAreNotPadded() throws Exception {
		assertThat(causeDepth(loggable("reasonCauseStackTrace", chain(6)))).isEqualTo(5);    // top + 5 causes
		assertThat(causeDepth(loggable("reasonCauseStackTrace", chain(3)))).isEqualTo(2);
		assertThat(causeDepth(loggable("reasonCauseStackTrace", chain(1)))).isZero();
	}

	@Test
	void cause_circularReference_stopsAtTheRepeat() throws Exception {
		RuntimeException a = new RuntimeException("a");
		RuntimeException b = new RuntimeException("b", a);
		a.initCause(b);

		Map<String, Object> loggable = loggable("reasonCauseStackTrace", a);

		assertThat(loggable).containsEntry("reason", "a");
		assertThat(causeOf(loggable)).containsEntry("reason", "b").doesNotContainKey("cause");
	}

	@Test
	void cause_wrapperWithDerivedMessage_reasonIsClassNameAndCauseTextIsInTheCauseNode() throws Exception {
		RuntimeException wrapper = new RuntimeException(new IllegalStateException("inner-MSG"));

		Map<String, Object> loggable = loggable("reasonCauseStackTrace", wrapper);

		assertThat(loggable).containsEntry("reason", "java.lang.RuntimeException");
		assertThat(causeOf(loggable)).containsEntry("reason", "inner-MSG");
	}

	@Test
	void cause_withClassName_causeReasonIsItsClassName() throws Exception {
		RuntimeException ex = new RuntimeException("outer-msg", new IllegalStateException("inner-msg"));

		Map<String, Object> loggable = loggable("classNameAndCause", ex);

		assertThat(loggable).containsEntry("reason", "java.lang.RuntimeException");
		assertThat(causeOf(loggable)).containsEntry("reason", "java.lang.IllegalStateException");
		assertThat(LoggerMapper.loggableToJsonString(loggable)).doesNotContain("outer-msg").doesNotContain("inner-msg");
	}

	@Test
	void loggableClassExceptionsInAChain_exceptionNodeIsOnlyForTheThrownExceptionAndIsMasked() throws Exception {
		AccountLockedException locked = new AccountLockedException("Account locked after 5 failed attempts", "ACC-77889900", 5,
			new java.io.IOException("Connection reset"));
		PaymentDeclinedException declined = new PaymentDeclinedException("Payment declined for order 1042", "1042",
			"4111111111111111", "05", locked);

		Map<String, Object> loggable = loggable("defaults", declined);

		@SuppressWarnings("unchecked")
		Map<String, Object> topException = (Map<String, Object>) loggable.get("exception");
		assertThat(topException).containsEntry("class", PaymentDeclinedException.class.getName())
			.containsEntry("orderId", "1042").containsEntry("cardNumber", "4111********1111").containsEntry("declineCode", "05");

		// causes are reason + stack trace only: "exception" is just the exception that was thrown
		Map<String, Object> cause = causeOf(loggable);
		assertThat(cause).containsEntry("reason", "Account locked after 5 failed attempts").doesNotContainKey("exception");
		Map<String, Object> io = causeOf(cause);
		assertThat(io).containsEntry("reason", "Connection reset").doesNotContainKey("exception");

		assertThat(LoggerMapper.loggableToJsonString(loggable)).doesNotContain("4111111111111111").doesNotContain("ACC-77889900")
			.doesNotContain("failedAttempts");
	}

	@Test
	void causeNode_withoutMessageSelected_classNamesOnly() throws Exception {
		RuntimeException ex = new RuntimeException("outer-msg", new IllegalStateException("inner-msg"));

		Map<String, Object> loggable = loggable("causeStackTrace", ex);

		assertThat(stackTraceOf(loggable).get(0)).isEqualTo("java.lang.RuntimeException");
		assertThat(causeOf(loggable)).doesNotContainKey("reason");
		assertThat(stackTraceOf(causeOf(loggable)).get(0)).isEqualTo("java.lang.IllegalStateException");
		assertThat(LoggerMapper.loggableToJsonString(loggable)).doesNotContain("outer-msg").doesNotContain("inner-msg");
	}

	@Test
	void exceptionSelectedButClassNotLoggable_noExceptionKey() throws Exception {
		Map<String, Object> loggable = loggable("defaults", new IllegalStateException("plain"));

		assertThat(loggable).containsEntry("reason", "plain").doesNotContainKey("exception");
	}

	@Test
	void emptyDetail_neitherReasonNorExceptionNorStackTrace() throws Exception {
		Map<String, Object> loggable = loggable("none", new SecretException("X-42"));

		assertThat(loggable).doesNotContainKeys("reason", "exception", "stacktrace");
	}

	@Test
	void clearReferences_clearsPropagatedThrowable() throws Exception {
		DiagnosticContextItem item = item("defaults");
		item.putPropagatedThrowable(new RuntimeException("x"));
		assertThat(item.getPropagatedThrowable()).isNotNull();

		item.clearReferences();

		assertThat(item.getPropagatedThrowable()).isNull();
	}

	@Test
	void propagatedThrowable_isNotSerialised() throws Exception {
		DiagnosticContextItem item = item("defaults");
		item.putPropagatedThrowable(new RuntimeException("propagated-secret"));

		assertThat(LoggerMapper.loggableToJsonString(item.toLoggable())).doesNotContain("propagated-secret");
	}
}
