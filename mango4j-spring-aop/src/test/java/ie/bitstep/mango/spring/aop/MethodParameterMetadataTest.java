package ie.bitstep.mango.spring.aop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static ie.bitstep.mango.spring.aop.TestMethods.getMethod;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MethodParameterMetadataTest {
	void dummy(String s, int x) {
		// dummy method for use in tests
	}

	@Test
	void testGetters() {
		// GIVEN
		Method method = getMethod(this.getClass(), "dummy", String.class, int.class);

		// WHEN
		MethodParameterMetadata metadata = new MethodParameterMetadata(method);

		// THEN
		assertThat(metadata.getMethod()).isEqualTo(method);
		assertThat(metadata.getParameters()).hasSameSizeAs(method.getParameters());
		assertThat(metadata.getParameterCount()).isEqualTo(method.getParameters().length);
		assertThat(metadata.getParameters()).isEqualTo(method.getParameters());
		assertThat(metadata.getParameter(0)).isEqualTo(method.getParameters()[0]);
		assertThat(metadata.getParameter(1)).isEqualTo(method.getParameters()[1]);
	}
}
