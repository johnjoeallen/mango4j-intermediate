package ie.bitstep.mango.spring.aop;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class ThrowableInfoTest {
	private final MethodInfo methodInfo = new MethodInfo(ThrowableInfoTest.class, null);

	@Test
	void throwableInfoTest() {
		// given
		RuntimeException ex = new RuntimeException("failed");
		ThrowableInfo throwableInfo = new ThrowableInfo(ex, methodInfo);

		assertThat(throwableInfo.getMethodInfo()).isEqualTo(methodInfo);
		assertThat(throwableInfo.getThrowable()).isEqualTo(ex);
	}
}