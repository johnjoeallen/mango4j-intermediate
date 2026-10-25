package com.mastercard.commons.mango.observe.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservationAspectTest {
	private static final Object RESULT = "Response";
	private ObservationAspect observationAspect;

	@Mock
	private ProceedingJoinPoint proceedingJoinPoint;

	@Mock
	private ObservationProcessor observationProcessor;


	@BeforeEach
	void setup() {
		observationAspect = new ObservationAspect(observationProcessor);
	}

	@Test
	void intercept() throws Throwable {
		// GIVEN
		String s = "Hello";

		// WHEN
		when(observationProcessor.observe(proceedingJoinPoint)).thenReturn(s);
		String result = (String) observationAspect.intercept(proceedingJoinPoint);

		// THEN
		verify(observationProcessor, times(1)).observe(proceedingJoinPoint);
		assertThat(result).isEqualTo(s);
	}
}