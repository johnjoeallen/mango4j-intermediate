package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.aop.MethodInfoFactory;
import ie.bitstep.mango.spring.logging.LogContext;
import ie.bitstep.mango.spring.logging.LogUtils;
import ie.bitstep.mango.spring.logging.annotations.LoggableMethod;
import ie.bitstep.mango.spring.logging.annotations.LoggableParam;
import ie.bitstep.mango.spring.logging.aspect.AspectLogger;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContextThreadLocal;
import ie.bitstep.mango.spring.logging.holders.ParameterHolder;
import ie.bitstep.mango.spring.logging.web.WebMDCField;
import ie.bitstep.mango.spring.logging.converters.Converter;
import ie.bitstep.mango.spring.logging.converters.ConverterFactory;
import ie.bitstep.mango.utils.masking.Mask;
import ie.bitstep.mango.utils.masking.Masker;
import ie.bitstep.mango.utils.masking.MaskerFactory;
import ie.bitstep.mango.utils.masking.NoMasker;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static ie.bitstep.mango.spring.logging.dc.DiagnosticContext.LOG_KEY_ID;

@Component
@RequiredArgsConstructor
public class AspectLoggerDelegate {
	private static Logger LOGGER = LoggerFactory.getLogger(AspectLogger.class);    // NOSONAR using logger of AspectLogger class
	private final DiagnosticContextLogger diagnosticContextLogger;
	private Clock clock = Clock.systemUTC();

	public Object interceptLoggableMethod(ProceedingJoinPoint joinPoint) throws Throwable {
		MethodInfo methodInfo = MethodInfoFactory.getMethodInfo(joinPoint);
		LoggableMethod loggableMethod = LogUtils.getLoggableMethod(methodInfo);

		if (loggableMethod == null) {
			// including all public methods adds too much noise
			return joinPoint.proceed();
		}

		return doLoggableMethod(joinPoint, methodInfo, loggableMethod);
	}

	public Object interceptExceptionHandler(ProceedingJoinPoint joinPoint) throws Throwable {
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();
		DiagnosticContextItem dci = dc.getCurrentItem();

		if (dci != null) {
			putThrowable(joinPoint);
		}

		Object returnedValue = joinPoint.proceed();

		if (dci != null) {
			putReturn(dci, returnedValue);
		}

		return returnedValue;
	}

	void putArgs(DiagnosticContextItem dci, MethodInfo mi, Object[] args) {
		try {
			if (args.length > 0) {
				Parameter[] parameters = mi.getMethod().getParameters();
				Annotation[][] parameterAnnotations = mi.getParameterAnnotations();
				Map<String, Object> params = new LinkedHashMap<>();

				int argIndex = 0;
				for (Object arg : args) {
					putParameter(new ParameterHolder(
							parameters[argIndex]),
						parameterAnnotations[argIndex],
						params,
						arg);
					argIndex++;
				}

				if (!params.isEmpty()) {
					dci.putValue(DiagnosticContextItem.KEY_PARAMS, params);
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to log method arguments", e);
		}
	}

	void putParameter(ParameterHolder parameterHolder, Annotation[] parameterAnnotation, Map<String, Object> params, Object arg) {
		LoggableParam loggableParam = null;
		Mask mask = null;
		Converter converter = null;

		for (Annotation pa : parameterAnnotation) {
			if (pa instanceof LoggableParam lp) {
				loggableParam = lp;
			} else if (pa instanceof Mask m) {
				mask = m;
			} else if (pa instanceof Converter c) {
				converter = c;
			}
		}

		if (loggableParam != null) {
			String paramName = getParamName(parameterHolder, loggableParam);
			params.put(paramName, new LoggableValue(arg, getMasker(loggableParam, mask), getConverter(converter), loggableParam.truncateLen(), loggableParam.view()));
		}
	}

	private static String getParamName(ParameterHolder parameterHolder, LoggableParam loggableParam) {
		if (StringUtils.isNotEmpty(loggableParam.name())) {
			return loggableParam.name();
		} else {
			return parameterHolder.getName();
		}
	}

	private UnaryOperator<Object> getConverter(Converter converter) {
		return converter != null ? ConverterFactory.getConverter(converter.converter()) : null;
	}

	@SuppressWarnings({"deprecation", "squid:CallToDeprecatedMethod"})
	private static Masker getMasker(LoggableParam loggableParam, Mask mask) {
		if (mask != null) {
			return MaskerFactory.getMasker(mask);
		} else if (loggableParam.mask() != NoMasker.class) {
			return MaskerFactory.getMasker(loggableParam.mask());
		}

		return null;
	}

	void putReturn(DiagnosticContextItem dci, Object returnedValue) {
		try {
			Object response = returnedValue;
			if (returnedValue instanceof HttpEntity he) { // NOSONAR no parameter for this generic type
				response = he.getBody();
			}

			HttpStatusCode responseStatus = null;
			if (returnedValue instanceof ResponseEntity re) { // NOSONAR no parameter for this generic type
				responseStatus = HttpStatusCode.valueOf(re.getStatusCode().value());
			}

			if (response != null) {
				dci.putResultValue(DiagnosticContextItem.KEY_RESPONSE, response);
			}

			if (responseStatus != null) {
				dci.putResultValue(DiagnosticContextItem.KEY_RESPONSE_STATUS, Integer.toString(responseStatus.value()));
			}

		} catch (Exception e) {
			LOGGER.error("Failed to log method return value", e);
		}
	}

	void putThrowable(ProceedingJoinPoint joinPoint) {
		Object arg0 = getArg0(joinPoint);
		if (arg0 instanceof Throwable thrown) {
			putThrowable(thrown);
		}
	}

	void putThrowable(Throwable t) {
		LogContext.putThrowable(t);
	}

	private Object doLoggableMethod(ProceedingJoinPoint joinPoint, MethodInfo methodInfo, LoggableMethod loggableMethod) throws Throwable {
		DiagnosticContext dc = getMethodDiagnosticContext(loggableMethod);
		MDC.put(LOG_KEY_ID, UUID.randomUUID().toString()); // create a new new log id

		long startMs = clock.millis();

		DiagnosticContextItem dci = dc.push(methodInfo);
		putArgs(dci, methodInfo, joinPoint.getArgs());

		diagnosticContextLogger.logEntry(dci);
		String currentCorrelationId = MDC.get(WebMDCField.CORRELATION_ID.getFieldName());

		try {
			switch (loggableMethod.tracking()) {
				case SPLIT:
					MDC.put(
						WebMDCField.CORRELATION_ID.getFieldName(),
						UUID.randomUUID().toString());
					break;

				case TRAIL:
					if (StringUtils.isEmpty(currentCorrelationId)) {
						MDC.put(
							WebMDCField.CORRELATION_ID.getFieldName(),
							UUID.randomUUID().toString()
						);
					} else {
						MDC.put(
							WebMDCField.CORRELATION_ID.getFieldName(),
							currentCorrelationId + "::" + UUID.randomUUID().toString()
						);
					}
					break;

				case DONT_TRACK:
					MDC.put(
						WebMDCField.CORRELATION_ID.getFieldName(),
						null);
					break;

				case TRACK:
				default:
					if (StringUtils.isEmpty(currentCorrelationId)) {
						MDC.put(
							WebMDCField.CORRELATION_ID.getFieldName(),
							UUID.randomUUID().toString());
					}
					break;
			}
			Object response = joinPoint.proceed();

			if (loggableMethod.logReturn()) {
				putReturn(dci, response);
			}

			return response;
		} catch (Throwable t) {
			if (!dci.hasParent()) {
				putThrowable(t);
			}

			throw t;
		} finally {
			dci.putResultValue(DiagnosticContextItem.KEY_RESPONSE_TIME, clock.millis() - startMs);

			if (!dci.isWithExternalExceptionHandler()) {
				if (!dci.hasParent()) {
					diagnosticContextLogger.logExitOrException(loggableMethod, dc);
				}

				dc.up();

				if (!dci.hasParent()) {
					DiagnosticContextThreadLocal.unset();
				}
			}

			if (LogUtils.getLoggableMethod(methodInfo).isolated()) {
				DiagnosticContextThreadLocal.pop();
			}

			MDC.put(WebMDCField.CORRELATION_ID.getFieldName(), currentCorrelationId);
		}
	}

	private DiagnosticContext getMethodDiagnosticContext(LoggableMethod loggableMethod) {
		if (loggableMethod.isolated()) {
			return DiagnosticContextThreadLocal.push();
		} else {
			return DiagnosticContextThreadLocal.get();
		}
	}

	private Object getArg0(ProceedingJoinPoint joinPoint) {
		if (!ArrayUtils.isEmpty(joinPoint.getArgs())) {
			return joinPoint.getArgs()[0];
		}

		return null;
	}

}
