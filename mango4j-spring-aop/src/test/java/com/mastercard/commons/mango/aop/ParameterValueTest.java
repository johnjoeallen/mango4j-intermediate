package com.mastercard.commons.mango.aop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static com.mastercard.commons.mango.reflection.utils.ReflectionUtils.getMethod;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ParameterValueTest {
	void dummy(@Deprecated String s, int x) {
		// dummy method for use in tests
	}

	@Test
	void testGetters() {
		// GIVEN
		Method method = getMethod(this.getClass(), "dummy", String.class, int.class);

		// WHEN
		ParameterValue parameterValue = new ParameterValue(method.getParameters()[0], "Hello");

		// THEN
		assertThat(parameterValue.getName()).isEqualTo("s");
		assertThat(parameterValue.getType()).isEqualTo(String.class);
		assertThat(parameterValue.getAnnotation(Deprecated.class)).isInstanceOf(Deprecated.class);
	}
}
