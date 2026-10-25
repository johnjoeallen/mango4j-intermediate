package com.mastercard.commons.mango.observe.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@RequiredArgsConstructor
@Component
public class ObservationAspect {

	private final ObservationProcessor observationProcessor;

	@Around("execution(@com.mastercard.commons.mango.observe.annotations.Observe * *(..))")
	public Object intercept(ProceedingJoinPoint joinPoint) throws Throwable { // NOSONAR
		return observationProcessor.observe(joinPoint);
	}

}
