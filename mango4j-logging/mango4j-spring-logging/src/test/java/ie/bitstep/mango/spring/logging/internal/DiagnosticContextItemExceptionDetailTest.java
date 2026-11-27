package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.annotations.LoggableClass;
import ie.bitstep.mango.logging.annotations.LoggableField;
import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.internal.LoggerMapper;
import ie.bitstep.mango.spring.aop.MethodInfo;
import org.junit.jupiter.api.Test;

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
