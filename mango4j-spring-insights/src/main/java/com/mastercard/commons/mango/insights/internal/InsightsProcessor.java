package com.mastercard.commons.mango.insights.internal;

import com.mastercard.commons.mango.insights.annotations.InsightTag;
import com.mastercard.commons.mango.insights.annotations.InsightVariable;
import com.mastercard.commons.mango.insights.consts.InsightStatus;
import com.mastercard.commons.mango.logging.internal.LoggerMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;

@RequiredArgsConstructor
@Component
public class InsightsProcessor {
	private static Logger logger = LoggerFactory.getLogger(InsightsProcessor.class);    // NOSONAR using logger of AspectLogger class

	private final InsightsProvider insightsProvider;

	public Object proceed(ProceedingJoinPoint joinPoint) throws Throwable {
		Class<?> targetClass = joinPoint.getTarget().getClass();
		Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
		InsightsContext insightsContext = InsightsThreadContext.get();
		AppInsight appInsight;

		if (isController(targetClass)) {
			appInsight = insightsContext.current()
				.orElseThrow(() -> new IllegalStateException("Insights should have been initialised by InsightWebInterceptor"));
		} else {
			appInsight = insightsContext.push(targetClass, method, insightsProvider);
		}

		Object[] args = joinPoint.getArgs();

		if (ArrayUtils.isNotEmpty(args)) {
			Annotation[][] parameterAnnotations = method.getParameterAnnotations();

			Iterator<Annotation[]> iterator = Arrays.stream(parameterAnnotations).iterator();
			for (Object arg : args) {
				try {
					addParamaterToInsightContext(iterator.next(), arg);
				} catch (Exception e) {
					logger.error("Failed to log method arguments", e);
				}
			}
		}

		return proceed(joinPoint, appInsight);
	}

	private void processInsightTag(Object arg, InsightTag pa) {
		InsightTag insightTagAnnotation = pa;
		String name = insightTagAnnotation.name();
		if (StringUtils.isNotEmpty(name)) {
			Insights.setTag(name, LoggerMapper.toLoggable(arg));
		}
	}

	private void processInsightVariable(Object arg, InsightVariable pa) {
		InsightVariable insightVariableAnnotation = pa;
		String name = insightVariableAnnotation.name();
		if (StringUtils.isNotEmpty(name)) {
			Insights.setContextValue(name, LoggerMapper.toLoggable(arg));
		}
	}

	void addParamaterToInsightContext(Annotation[] parameterAnnotations, Object arg) {
		for (Annotation pa : parameterAnnotations) {
			if (pa instanceof InsightVariable iv) {
				processInsightVariable(arg, iv);
			}

			if (pa instanceof InsightTag it) {
				processInsightTag(arg, it);
			}
		}
	}


	private Object proceed(ProceedingJoinPoint joinPoint, AppInsight appInsight) throws Throwable {
		try {
			Object result = joinPoint.proceed();
			succeeded(appInsight);
			return result;
		} catch (Throwable t) {
			failed(appInsight, t);

			throw t;
		} finally {
			appInsight.setExecutionTimeMillis(Instant.now().toEpochMilli() - appInsight.getTimestamp().toEpochMilli());
			Class<?> targetClass = joinPoint.getTarget().getClass();

			if (!isController(targetClass)) {
				InsightsContext insightsContext = InsightsThreadContext.get();
				insightsContext.pop();
				if (insightsContext.isStackEmpty()) {
					insightsContext.publish(insightsProvider);
					InsightsThreadContext.unset();
				}
			}
		}
	}

	private boolean isController(Class<?> clazz) {
		return AnnotationUtils.findAnnotation(clazz, Controller.class) != null;
	}

	private void failed(AppInsight appInsight, Throwable t) {
		appInsight.setStatus(InsightStatus.FAILURE);
		appInsight.setException(ExceptionUtils.getCauses(t));
	}

	private void succeeded(AppInsight appInsight) {
		appInsight.setStatus(InsightStatus.SUCCESS);
	}

	public Object interceptExceptionHandler(ProceedingJoinPoint joinPoint) throws Throwable {
		if (!ArrayUtils.isEmpty(joinPoint.getArgs()) && joinPoint.getArgs()[0] instanceof Throwable thrown) {
			InsightsThreadContext.get().current().ifPresent(
				insight -> insight.setException(ExceptionUtils.getCauses(thrown)));
		}

		return joinPoint.proceed();
	}

}
