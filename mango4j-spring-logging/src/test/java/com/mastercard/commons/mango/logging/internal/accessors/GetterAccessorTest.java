package com.mastercard.commons.mango.logging.internal.accessors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GetterAccessorTest {

	private Method method;
	private final int valueToReturn = 12243;

	public int methodToTest() {
		return valueToReturn;
	}

	private int privateMethodToTest() {
		return valueToReturn;
	}

	@TestAnnotation(0L)
	public void methodToTestWithAnnotation() {
		// NOSONAR just for test
	}

	@BeforeEach
	public void setup() throws NoSuchMethodException {
		method = this.getClass().getMethod("methodToTest");
	}

	@Test
	void getAnnotationTest() throws NoSuchMethodException {
		method = this.getClass().getMethod("methodToTestWithAnnotation");

		TestAnnotation testAnnotation = new TestAnnotation() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return TestAnnotation.class;
			}

			@Override
			public long value() {
				return 0;
			}
		};

		GetterAccessor accessor = new GetterAccessor(method);

		TestAnnotation actual = accessor.getAnnotation(TestAnnotation.class);
		assertThat(actual).isEqualTo(testAnnotation);
	}

	@Test
	void isIncludeInAllTest() {
		GetterAccessor accessor = new GetterAccessor(method);

		boolean actual = accessor.isIncludeInAll();
		assertThat(actual).isFalse();
	}

	@Test
	void getValueTest() {
		GetterAccessor accessor = new GetterAccessor(method);

		Object actual = accessor.getValue(this);

		assertThat(actual).isEqualTo(valueToReturn);
	}

	@Test
	void getValuePrivateTest() throws NoSuchMethodException {
		method = this.getClass().getDeclaredMethod("privateMethodToTest");
		GetterAccessor accessor = new GetterAccessor(method);

		Object actual = accessor.getValue(this);

		assertThat(actual).isEqualTo(valueToReturn);
	}


}