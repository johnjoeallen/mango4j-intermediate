package ie.bitstep.mango.spring.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static ie.bitstep.mango.spring.aop.TestMethods.getMethod;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParameterMapperTest {
	@Mock
	private ProceedingJoinPoint joinPoint;

	@Mock
	private MethodSignature methodSignature;

	@Mock
	private ConstructorSignature constructorSignature;

	void dummy(String s, int x) {
		// dummy method for use in tests
	}

	@Test
	void testMap() {
		// GIVEN
		Method method = getMethod(this.getClass(), "dummy", String.class, int.class);
		Object[] args = new Object[]{"Hello", 99};
		MappedMethodParameters expected = new MappedMethodParameters(method, args);

		// WHEN
		when(joinPoint.getSignature()).thenReturn(methodSignature);
		when(joinPoint.getArgs()).thenReturn(args);
		when(methodSignature.getMethod()).thenReturn(method);

		MappedMethodParameters mappedMethodParameters = ParameterMapper.map(joinPoint);

		// THEN
		assertThat(mappedMethodParameters).usingRecursiveComparison().isEqualTo(expected);
	}

	@Test
	void testMapIllegalArgumentException() {
		// GIVEN

		// WHEN
		when(joinPoint.getSignature()).thenReturn(constructorSignature);

		// THEN
		assertThrows(IllegalArgumentException.class, () -> ParameterMapper.map(joinPoint));
	}

	@Test
	void testCache() {
		// GIVEN
		Method method = getMethod(this.getClass(), "dummy", String.class, int.class);
		ParameterMapper.clear(); // clear cache
		MethodParameterMetadata methodParameterMetadata;

		// First hit
		// WHEN
		methodParameterMetadata = ParameterMapper.getOrLoadMetadata(method);

		// THEN
		assertThat(methodParameterMetadata).isNotNull();

		// Second hit
		// WHEN
		methodParameterMetadata = ParameterMapper.getOrLoadMetadata(method);

		// THEN
		assertThat(methodParameterMetadata).isNotNull();

		// WHEN
		ParameterMapper.clear();
		methodParameterMetadata = ParameterMapper.getFromCache(method);

		// THEN
		assertThat(methodParameterMetadata).isNull();
	}
}
