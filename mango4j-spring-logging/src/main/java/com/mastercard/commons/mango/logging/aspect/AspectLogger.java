package com.mastercard.commons.mango.logging.aspect;

import com.mastercard.commons.mango.logging.internal.AspectLoggerDelegate;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.mastercard.commons.mango.logging.aspect.AspectLogger.ORDER;

/**
 * Aspect for Diagnostic Context logging.
 */
@Aspect
@Order(ORDER)
@Component
@RequiredArgsConstructor
public class AspectLogger {
	private final AspectLoggerDelegate delegate;
	public static final int ORDER = Integer.MIN_VALUE;

	@Around("execution(@com.mastercard.commons.mango.logging.annotations.LoggableMethod * *(..))")
	public Object interceptLoggableMethod(ProceedingJoinPoint joinPoint) throws Throwable {        // NOSONAR
		return delegate.interceptLoggableMethod(joinPoint);
	}

	@Around("execution(@org.springframework.web.bind.annotation.ExceptionHandler * *(..))")
	public Object interceptExceptionHandler(ProceedingJoinPoint joinPoint) throws Throwable {    // NOSONAR
		return delegate.interceptExceptionHandler(joinPoint);
	}

}
