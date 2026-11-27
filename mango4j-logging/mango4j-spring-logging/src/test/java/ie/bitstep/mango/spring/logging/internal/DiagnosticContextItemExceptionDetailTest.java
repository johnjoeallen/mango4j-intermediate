package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.annotations.LoggableClass;
import ie.bitstep.mango.logging.annotations.LoggableField;
import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.internal.LoggerMapper;
import ie.bitstep.mango.spring.aop.MethodInfo;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CLASS_NAME;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.MESSAGE;
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
		@LoggableMethod(logException = {MESSAGE})
		public void withMessage() {
		}

		@LoggableMethod(logException = {CLASS_NAME})
		public void classNameOnly() {
		}
	}

	private static DiagnosticContextItem item(String method) throws NoSuchMethodException {
		return new DiagnosticContextItem(new MethodInfo(Fixture.class, Fixture.class.getMethod(method)), null);
	}

	@Test
	void loggableClassException_withMessage_reasonIsStructured() throws Exception {
		DiagnosticContextItem item = item("withMessage");
		item.putThrowable(new SecretException("X-42"));

		Map<String, Object> loggable = item.toLoggable();

		assertThat(loggable.get("reason")).isInstanceOf(Map.class);
		assertThat(LoggerMapper.loggableToJsonString(loggable))
			.contains(SecretException.class.getName())
			.contains("X-42");
	}

	@Test
	void loggableClassException_withoutMessage_reasonIsClassNameAndFieldsAreNotLogged() throws Exception {
		DiagnosticContextItem item = item("classNameOnly");
		item.putThrowable(new SecretException("X-42"));

		Map<String, Object> loggable = item.toLoggable();

		assertThat(loggable).containsEntry("reason", SecretException.class.getName());
		assertThat(LoggerMapper.loggableToJsonString(loggable)).doesNotContain("X-42");
	}

	@Test
	void clearReferences_clearsPropagatedThrowable() throws Exception {
		DiagnosticContextItem item = item("withMessage");
		item.putPropagatedThrowable(new RuntimeException("x"));
		assertThat(item.getPropagatedThrowable()).isNotNull();

		item.clearReferences();

		assertThat(item.getPropagatedThrowable()).isNull();
	}

	@Test
	void propagatedThrowable_isNotSerialised() throws Exception {
		DiagnosticContextItem item = item("withMessage");
		item.putPropagatedThrowable(new RuntimeException("propagated-secret"));

		assertThat(LoggerMapper.loggableToJsonString(item.toLoggable())).doesNotContain("propagated-secret");
	}
}
