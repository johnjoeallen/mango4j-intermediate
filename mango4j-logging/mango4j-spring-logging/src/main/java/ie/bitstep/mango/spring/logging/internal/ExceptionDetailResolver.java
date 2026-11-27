package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.LogUtils;
import ie.bitstep.mango.logging.annotations.ExceptionDetail;
import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.annotations.OnException;
import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.detail.ExceptionDetailStrategy;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CAUSE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CLASS_NAME;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.EXCEPTION;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.MESSAGE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.REASON;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.STACK_TRACE;

/**
 * Works out what to log about an exception thrown by a method, in this order:
 * <ol>
 *   <li>the first matching {@link OnException} on the method</li>
 *   <li>the global {@link ExceptionDetailStrategy}, if one is registered and has a rule for it</li>
 *   <li>{@link LoggableMethod#logException()}</li>
 *   <li>the default (reason with message and causes, exception fields, stack trace)</li>
 * </ol>
 */
public final class ExceptionDetailResolver {

	private static final Set<ExceptionDetail> DEFAULT = EnumSet.of(REASON, MESSAGE, CAUSE, EXCEPTION, STACK_TRACE);

	private static volatile ExceptionDetailStrategy strategy;

	private ExceptionDetailResolver() {
	}

	public static void setStrategy(ExceptionDetailStrategy strategy) {
		ExceptionDetailResolver.strategy = strategy;
	}

	public static Set<ExceptionDetail> resolve(MethodInfo mi, Throwable t) {
		Method method = mi != null ? mi.getMethod() : null;

		if (method != null) {
			OnException rule = findOnException(method, t);
			if (rule != null) {
				return toSet(rule.detail());
			}
		}

		ExceptionDetailStrategy global = strategy;
		if (global != null) {
			Optional<Set<ExceptionDetail>> detail = global.getDetail(t, mi);
			if (detail.isPresent()) {
				return detail.get();
			}
		}

		LoggableMethod loggableMethod = method != null ? method.getAnnotation(LoggableMethod.class) : null;
		return loggableMethod != null ? toSet(loggableMethod.logException()) : DEFAULT;
	}

	public static boolean hasMatchingOnException(MethodInfo mi, Throwable t) {
		Method method = mi != null ? mi.getMethod() : null;
		return method != null && findOnException(method, t) != null;
	}

	/**
	 * The text for the {@code reason}: empty unless REASON is selected. CLASS_NAME, MESSAGE and CAUSE shape the
	 * content (CAUSE appends the cause chain; with none of the three, the message is used).
	 */
	public static String describe(Throwable t, Set<ExceptionDetail> details) {
		if (!details.contains(REASON)) {
			return "";
		}

		boolean withClass = details.contains(CLASS_NAME);
		boolean withMessage = details.contains(MESSAGE);
		boolean withCause = details.contains(CAUSE);

		if (withClass && !withMessage && !withCause) {
			return t.getClass().getName();
		}

		String text = LogUtils.exception2Message(t, withCause);
		String className = t.getClass().getName();
		return withClass && !text.startsWith(className) ? className + ": " + text : text;
	}

	private static OnException findOnException(Method method, Throwable t) {
		for (OnException rule : AnnotationUtils.getRepeatableAnnotations(method, OnException.class)) {
			for (Class<? extends Throwable> type : rule.type()) {
				if (type.isInstance(t)) {
					return rule;
				}
			}
		}
		return null;
	}

	private static Set<ExceptionDetail> toSet(ExceptionDetail[] details) {
		return details.length == 0 ? EnumSet.noneOf(ExceptionDetail.class) : EnumSet.copyOf(Arrays.asList(details));
	}
}
