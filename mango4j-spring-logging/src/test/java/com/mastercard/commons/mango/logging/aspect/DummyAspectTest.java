package com.mastercard.commons.mango.logging.aspect;

import com.mastercard.commons.mango.logging.internal.AspectLoggerDelegate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DummyAspectTest {
	private static final String TEST_RESULT = "TEST_RESULT";

	@InjectMocks
	private AspectLogger aspectLogger;

	@Mock
	private AspectLoggerDelegate mockDelegate;

	@Mock
	private ProceedingJoinPoint mockJoinPoint;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(aspectLogger, "delegate", mockDelegate);
	}

	@Test
	void interceptLoggableMethod() throws Throwable {
		// given
		given(mockDelegate.interceptLoggableMethod(mockJoinPoint)).willReturn(TEST_RESULT);

		// when
		Object actualResult = aspectLogger.interceptLoggableMethod(mockJoinPoint);

		// then
		assertThat(actualResult).isEqualTo(TEST_RESULT);
	}

	@Test
	void interceptExceptionHandler() throws Throwable {
		// given
		given(mockDelegate.interceptExceptionHandler(mockJoinPoint)).willReturn(TEST_RESULT);

		// when
		Object actualResult = aspectLogger.interceptExceptionHandler(mockJoinPoint);

		// then
		assertThat(actualResult).isEqualTo(TEST_RESULT);
	}
}
