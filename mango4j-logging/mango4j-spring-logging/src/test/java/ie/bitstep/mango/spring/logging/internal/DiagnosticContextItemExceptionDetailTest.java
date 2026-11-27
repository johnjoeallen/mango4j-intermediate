package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.annotations.LoggableClass;
import ie.bitstep.mango.logging.annotations.LoggableField;
import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.internal.LoggerMapper;
import ie.bitstep.mango.spring.aop.MethodInfo;
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

		@LoggableMethod(logException = {CAUSE})
		public void causeOnly() {
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
	private static List<String> stackTraceOf(Map<String, Object> loggable) {
		return (List<String>) loggable.get("stacktrace");
	}

	@Test
	void stackTrace_withMessageSelected_includesMessagesOnExceptionAndCause() throws Exception {
		RuntimeException ex = new RuntimeException("outer-msg", new IllegalStateException("inner-msg"));

		List<String> trace = stackTraceOf(loggable("messageCauseStackTrace", ex));

		assertThat(trace.get(0)).isEqualTo("java.lang.RuntimeException: outer-msg");
		assertThat(trace).contains("Caused by: java.lang.IllegalStateException: inner-msg");
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
	void stackTrace_causesOnlyWhenCauseSelected() throws Exception {
		RuntimeException ex = new RuntimeException("outer-msg", new IllegalStateException("inner-msg"));
		ex.addSuppressed(new IllegalArgumentException("suppressed-msg"));

		for (String method : new String[]{"messageWithoutReason", "classNameWithStackTrace", "stackTraceOnly"}) {
			assertThat(stackTraceOf(loggable(method, ex)))
				.noneMatch(l -> l.startsWith("Caused by") || l.startsWith("Suppressed"));
		}
		assertThat(stackTraceOf(loggable("messageWithoutReason", ex)).get(0)).isEqualTo("java.lang.RuntimeException: outer-msg");
		assertThat(stackTraceOf(loggable("messageCauseStackTrace", ex)))
			.anyMatch(l -> l.startsWith("Caused by: java.lang.IllegalStateException"))
			.anyMatch(l -> l.startsWith("Suppressed: java.lang.IllegalArgumentException"));
	}

	@Test
	void cause_selectedWithReason_causeChainAppendedToReason() throws Exception {
		RuntimeException ex = new RuntimeException("outer", new IllegalStateException("middle", new IllegalArgumentException("inner")));

		Map<String, Object> loggable = loggable("reasonAndCause", ex);

		assertThat(loggable).containsEntry("reason", "outer; Cause: middle; Cause: inner").doesNotContainKey("stacktrace");
	}

	@Test
	void cause_selectedButNoCause_reasonHasNoCauseSuffix() throws Exception {
		Map<String, Object> loggable = loggable("reasonAndCause", new RuntimeException("outer"));

		assertThat(loggable).containsEntry("reason", "outer");
	}

	@Test
	void cause_selectedWithoutReasonOrStackTrace_nothingToShow() throws Exception {
		Map<String, Object> loggable = loggable("causeOnly", new RuntimeException("outer", new IllegalStateException("inner")));

		assertThat(loggable).doesNotContainKeys("reason", "stacktrace", "exception");
	}

	@Test
	void cause_selectedWithStackTraceAndReason_bothShowTheCause() throws Exception {
		RuntimeException ex = new RuntimeException("outer", new IllegalStateException("inner"));

		Map<String, Object> loggable = loggable("defaults", ex);

		assertThat(loggable).containsEntry("reason", "outer; Cause: inner");
		assertThat(stackTraceOf(loggable)).contains("Caused by: java.lang.IllegalStateException: inner");
	}

	@Test
	void stackTrace_causeWithoutMessage_showsCauseClassNamesOnly() throws Exception {
		RuntimeException ex = new RuntimeException("outer-msg", new IllegalStateException("inner-msg"));

		List<String> trace = stackTraceOf(loggable("causeStackTrace", ex));

		assertThat(trace.get(0)).isEqualTo("java.lang.RuntimeException");
		assertThat(trace).contains("Caused by: java.lang.IllegalStateException");
		assertThat(String.join("\n", trace)).doesNotContain("outer-msg").doesNotContain("inner-msg");
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
