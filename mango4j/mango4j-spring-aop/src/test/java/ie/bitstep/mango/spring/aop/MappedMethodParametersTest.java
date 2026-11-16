package ie.bitstep.mango.spring.aop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static ie.bitstep.mango.spring.aop.TestMethods.getMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class MappedMethodParametersTest {
	void dummy(String s, int x) {
		// dummy method for use in tests
	}

	@Test
	void testSanitiseArguments() {
		// GIVEN
		final Object[] args = {"Hello"};

		// WHEN
		Object[] result = MappedMethodParameters.sanitiseArguments(args);

		// THEN
		assertThat(result).isEqualTo(args);
	}

	@Test
	void testSanitiseArgumentsWhenNull() {
		// GIVEN
		final Object[] args = null;

		// WHEN
		Object[] result = MappedMethodParameters.sanitiseArguments(args);

		// THEN
		assertThat(result).isEqualTo(new Object[0]);
	}

	@Test
	void testInvalidArgsLength() {
		// GIVEN
		Method method = getMethod(this.getClass(), "dummy", String.class, int.class);

		// WHEN
		final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new MappedMethodParameters(method, new Object[]{"Hello"});
		});

		// THEN
		assertThat(exception.getMessage()).isSameAs("Parameter count does not match argument count");
	}

	@Test
	void testGetters() {
		// GIVEN
		Method method = getMethod(this.getClass(), "dummy", String.class, int.class);
		Object[] args = new Object[]{"Hello", 99};

		// WHEN
		MappedMethodParameters mappedMethodParameters = new MappedMethodParameters(method, args);

		// THEN
		assertThat(mappedMethodParameters.size()).isEqualTo(args.length);
		assertThat(mappedMethodParameters.getArguments()).isEqualTo(args);
		assertThat(mappedMethodParameters.getParameters()).isEqualTo(method.getParameters());
		assertThat(mappedMethodParameters.getParameter(0)).isEqualTo(method.getParameters()[0]);
		assertThat(mappedMethodParameters.getParameter(1)).isEqualTo(method.getParameters()[1]);
		assertThat(mappedMethodParameters.getArgument(0)).isEqualTo("Hello");
		assertThat(mappedMethodParameters.getArgument(1)).isEqualTo(99);
	}

	@Test
	void testModifiedArgs() {
		// GIVEN
		Method method = getMethod(this.getClass(), "dummy", String.class, int.class);
		Object[] args = new Object[]{"Hello", 99};
		Object[] modifiedArgs = new Object[]{"Goodbye", 99};

		// WHEN
		MappedMethodParameters mappedMethodParameters = new MappedMethodParameters(method, args);

		mappedMethodParameters.getParameterValue(0).setValue("Goodbye");

		// THEN
		assertThat(mappedMethodParameters.getArguments()).isEqualTo(args);
		assertThat(mappedMethodParameters.getModifiedArguments()).isEqualTo(modifiedArgs);
		assertThat(mappedMethodParameters.getParameters()).isEqualTo(method.getParameters());
	}

	@Test
	void testGetMethod() {
		// GIVEN
		Method method = getMethod(this.getClass(), "dummy", String.class, int.class);

		// WHEN
		MappedMethodParameters mappedMethodParameters = new MappedMethodParameters(method, new Object[]{"Hello", 99});

		// THEN
		assertThat(mappedMethodParameters.getMethod()).isSameAs(method);
	}
}
