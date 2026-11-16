package ie.bitstep.mango.spring.insights.internal;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static ie.bitstep.mango.spring.insights.internal.InsightsInterceptor.ORDER;

@Aspect
@Order(ORDER)
@RequiredArgsConstructor
@Component
public class InsightsInterceptor {
	private final InsightsProcessor processor;
	public static final int ORDER = Integer.MIN_VALUE + 1;

	@Around("execution(@ie.bitstep.mango.spring.insights.annotations.Insight * *(..))")
	public Object interceptor(ProceedingJoinPoint joinPoint) throws Throwable { // NOSONAR
		return processor.proceed(joinPoint);
	}

	@Around("execution(@org.springframework.web.bind.annotation.ExceptionHandler * *(..))")
	public Object interceptExceptionHandler(ProceedingJoinPoint joinPoint) throws Throwable {    // NOSONAR
		return processor.interceptExceptionHandler(joinPoint);
	}
}
