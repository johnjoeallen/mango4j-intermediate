package ie.bitstep.mango.spring.logging.sanitiser;

import ie.bitstep.mango.logging.sanitiser.ExceptionSanitiser;
import ie.bitstep.mango.logging.sanitiser.ExceptionSanitiserManager;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Spring module contributes {@link MethodArgumentNotValidExceptionSanitiser} to the core manager through
 * META-INF/services.
 */
class ExceptionSanitiserManagerSpringTest {

	@Test
	@SuppressWarnings("unchecked")
	void sanitiserIsRegisteredThroughServiceLoader() {
		List<ExceptionSanitiser> sanitisers = (List<ExceptionSanitiser>) ReflectionTestUtils.getField(ExceptionSanitiserManager.class, "SANITISERS");

		assertThat(sanitisers.get(0)).isNotNull();
		assertThat(sanitisers).hasAtLeastOneElementOfType(MethodArgumentNotValidExceptionSanitiser.class);
	}

	@Test
	void getSanitisedMessage_whenMethodArgumentNotValidException() throws Exception {
		MapBindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), "TEST_OBJECT_NAME");
		bindingResult.addError(new FieldError("TEST_OBJECT_NAME", "TEST_FIELD", "TEST_MESSAGE"));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(buildMethodParameter(), bindingResult);

		String result = ExceptionSanitiserManager.getSanitisedMessage(ex);

		assertThat(result).isEqualTo("Field error in object 'TEST_OBJECT_NAME' on field 'TEST_FIELD': TEST_MESSAGE");
	}

	MethodParameter buildMethodParameter() throws Exception {
		return new MethodParameter(getClass().getDeclaredMethod("buildMethodParameter"), -1);
	}
}
