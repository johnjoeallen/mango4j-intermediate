package ie.bitstep.mango.spring.observe.aspect;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.internal.LoggerMapper;
import ie.bitstep.mango.spring.observe.annotations.Observe;
import ie.bitstep.mango.spring.observe.annotations.Tag;
import ie.bitstep.mango.spring.observe.context.Observation;
import ie.bitstep.mango.spring.observe.context.ObservationContext;
import ie.bitstep.mango.spring.observe.context.ObservationContextThreadLocal;
import ie.bitstep.mango.spring.observe.enums.Status;
import ie.bitstep.mango.spring.observe.factory.ObservationFactory;
import ie.bitstep.mango.spring.observe.observers.ObserverService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static ie.bitstep.mango.spring.observe.context.Observation.RTT;

@RequiredArgsConstructor
@Component
public class ObservationProcessor {

	private static Logger logger = LoggerFactory.getLogger(ObservationProcessor.class);    // NOSONAR: using logger of AspectLogger class

	private final ObserverService observerService;
	private final ObservationFactory observationFactory;

	public Object observe(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodInfo methodInfo = new MethodInfo(joinPoint);
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		Observation observation = observationContext.push(observationFactory.create(methodInfo));
		Object[] args = joinPoint.getArgs();

		initialiseParams(args, observation);
		initialiseTags(args, observation);

		onEntry(observation);

		try {
			return observe(joinPoint, observation);
		} catch (Throwable t) {
			observation.throwable(t);
			throw t;
		} finally {
			onExit(observationContext, observation);
		}
	}

	private void onEntry(Observation observation) {
		try {
			observerService.observeOnEntry(observation);
		} finally {
			// NOSONAR
		}
	}

	private void onExit(ObservationContext observationContext, Observation observation) {
		observationContext.pop();

		try {
			observation.tag("status", observation.status().name());
			observerService.observeOnExit(observation);
		} finally {
			// NOSONAR
		}
	}

	private void initialiseParams(Object[] args, Observation observation) {
		if (ArrayUtils.isNotEmpty(args)) {
			observation.params().putAll(getParameters(observation, args));
		}
	}

	private void initialiseTags(Object[] args, Observation observation) {
		if (ArrayUtils.isNotEmpty(args)) {
			initTags(observation, getTags(observation, args));
		}
	}

	private static void initTags(Observation observation, Map<String, Object> tags) {
		for (Map.Entry<String, Object> tag : tags.entrySet()) {
			observation.tag(tag.getKey(), tag.getValue());
		}
	}

	private Object observe(ProceedingJoinPoint joinPoint, Observation observation) throws Throwable {
		try {
			observation.startTimeMillis(Instant.now().toEpochMilli());
			Object result = joinPoint.proceed();
			observation.status(Status.SUCCEEDED);
			return result;
		} catch (Throwable t) {
			observation.status(Status.FAILED);
			throw t;
		} finally {
			observation.endTimeMillis(Instant.now().toEpochMilli());
			observation.roundTripTime(observation.endTimeMillis() - observation.startTimeMillis());
			observation.tag(RTT, String.valueOf(observation.roundTripTime()));
		}
	}

	private Map<String, Object> getTags(Observation observation, Object[] args) {
		Annotation[][] parameterAnnotations = observation.methodInfo().getParameterAnnotations();
		Iterator<Annotation[]> iterator = Arrays.stream(parameterAnnotations).iterator();
		Map<String, Object> result = new HashMap<>();

		for (Object arg : args) {
			// Really need bitstep annotation utils here, get rid of this loop
			for (Annotation pa : iterator.next()) {
				if (pa instanceof Tag t) {
					result.putAll(getTagValue(arg, t));
				}
			}

			if (arg != null) {
				for (Field f : arg.getClass().getDeclaredFields()) {
					Tag tag = AnnotationUtils.findAnnotation(f, Tag.class);

					if (tag != null) {
						result.putAll(getTagValue(getObjectField(arg, f), tag));
					}
				}
			}
		}

		return result;
	}

	static Object getObjectField(Object arg, Field f) {
		try {
			return f.get(arg);
		} catch (Exception e1) {
			try {
				f.setAccessible(true);  // NOSONAR: required till new reflection library is available
				return f.get(arg);
			} catch (Exception e2) {
				logger.error("Failed to argument field value", e2);
				return "Failed to get field value: " + e2.getClass().getSimpleName();
			}
		}
	}

	private Map<String, Object> getParameters(Observation observation, Object[] args) {
		Annotation[][] parameterAnnotations = observation.methodInfo().getParameterAnnotations();
		Iterator<Annotation[]> iterator = Arrays.stream(parameterAnnotations).iterator();
		Map<String, Object> result = new HashMap<>();

		for (Object arg : args) {
			// Really need bitstep annotation utils here, get rid of this loop
			for (Annotation pa : iterator.next()) {
				if (pa instanceof Observe o) {
					result.putAll(getParamValue(arg, o));
				}
				if (pa instanceof Tag t) {
					result.putAll(getTagValue(arg, t));
				}
			}
		}

		return result;
	}

	private Map<String, Object> getTagValue(Object arg, Tag tag) {
		String name = tag.name();

		if (StringUtils.isNotEmpty(name)) {
			Map<String, Object> tagValue = new LinkedHashMap<>();
			tagValue.put(name, String.valueOf(arg));
			return tagValue;
		}

		return Collections.emptyMap();
	}


	private Map<String, Object> getParamValue(Object arg, Observe pa) {
		String name = pa.name();

		if (StringUtils.isNotEmpty(name)) {
			Map<String, Object> param = new LinkedHashMap<>();
			param.put(name, LoggerMapper.toLoggable(arg));
			return param;
		}

		return Collections.emptyMap();
	}
}
