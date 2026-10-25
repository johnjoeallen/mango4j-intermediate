package ie.bitstep.mango.spring.logging.internal.accessors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldAccessorTest {

	@Mock
	private Field field;

	@Test
	void constructorTest() {
		FieldAccessor accessor = new FieldAccessor(field);
		assertThat(accessor).isNotNull(); // best we can do
	}

	@Test
	void getNameTest() {
		String fieldName = "fieldName";
		when(field.getName()).thenReturn(fieldName);
		FieldAccessor accessor = new FieldAccessor(field);

		String actual = accessor.getName();
		assertThat(actual).isEqualTo(fieldName);
	}

	@Test
	void getAnnotationTest() {
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
		when(field.getAnnotation(TestAnnotation.class)).thenReturn(testAnnotation);
		FieldAccessor accessor = new FieldAccessor(field);

		TestAnnotation actual = accessor.getAnnotation(TestAnnotation.class);
		assertThat(actual).isEqualTo(testAnnotation);
	}

	@Test
	void isIncludeInAllTrueTest() {
		when(field.getModifiers()).thenReturn(2);
		FieldAccessor accessor = new FieldAccessor(field);

		boolean actual = accessor.isIncludeInAll();
		assertThat(actual).isTrue();
	}

	@Test
	void isIncludeInAllFalseTest() {
		when(field.getModifiers()).thenReturn(8);
		FieldAccessor accessor = new FieldAccessor(field);

		boolean actual = accessor.isIncludeInAll();
		assertThat(actual).isFalse();
	}

	@Test
	void getValueTest() throws IllegalAccessException {
		Object target = new Object();
		when(field.get(target)).thenReturn(10);
		FieldAccessor accessor = new FieldAccessor(field);

		Object actual = accessor.getValue(target);

		verify(field, times(1)).setAccessible(true);
		assertThat(actual).isEqualTo(10);
	}

}