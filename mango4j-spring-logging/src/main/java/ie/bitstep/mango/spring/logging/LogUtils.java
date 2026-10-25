package ie.bitstep.mango.spring.logging;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.annotations.LoggableMethod;
import ie.bitstep.mango.spring.logging.sanitiser.ExceptionSanitiserManager;
import ie.bitstep.mango.spring.logging.web.WebMDCField;
import ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.slf4j.event.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Helper methods for logging.
 */
public class LogUtils {

	private LogUtils() {
	}

	public static LoggableMethod getLoggableMethod(MethodInfo methodInfo) {
		return methodInfo.getMethod().getAnnotation(LoggableMethod.class);
	}

	public static String exception2Message(Throwable ex) {
		return exception2Message(ex, true);
	}

	/**
	 * Returns a human-readable representation of an exception and all its causes.
	 * (The standard <code>Throwable.getMessage()</code> does not include information about
	 * exception's causes).
	 *
	 * @param ex         exception to convert to string
	 * @param withCauses whether to include exception's causes
	 * @return human-readable representation of exception
	 */
	public static String exception2Message(Throwable ex, boolean withCauses) {
		if (ex == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();

		String className = ex.getClass().getName();
		String message = ExceptionSanitiserManager.getSanitisedMessage(ex);

		// it turns out that if an exception does not have its own message but has a cause,
		// then getMessage() returns getCause().toString() instead of null,
		// so we need to fix this here
		if (withCauses && message != null && ex.getCause() != null && ex.getCause().toString().equals(message)) {
			message = null;
		}

		if (message != null) {
			sb.append(message);
		} else {
			sb.append(className);
		}

		if (withCauses && isValidCause(ex)) {
			sb.append("; Cause: ").append(exception2Message(ex.getCause()));
		}

		return sb.toString();
	}

	public static boolean isValidCause(Throwable throwable) {
		return throwable.getCause() != null;
	}

	/**
	 * Returns a stack trace of an exception prepared for logging.
	 */
	public static List<String> getStackTrace(Throwable throwable) {
		List<String> stackList = Arrays.stream(ExceptionUtils.getStackFrames(throwable))
			.map(String::trim)
			.filter(s -> !s.startsWith("... "))
			.toList();
		return ExceptionSanitiserManager.sanitiseStackTrace(throwable, stackList);
	}

	/**
	 * Finds the maximum between to log levels.
	 */
	public static Level maxLevel(Level level1, Level level2) {
		return level1.toInt() > level2.toInt() ? level1 : level2;
	}

	/**
	 * Makes sure that the value is safe to log.
	 * - converts new lines and tabs to \n and \t
	 * - converts any characters outside of ASCII printable range to Unicode escape format (eg '\u0108')
	 */
	public static String toLogSafeValue(String value) {
		if (StringUtils.isEmpty(value)) {
			return value;
		}

		if (StringUtils.isAsciiPrintable(value)) {
			return value;

		} else {
			StringBuilder sb = new StringBuilder();
			for (int idx = 0; idx < value.length(); idx++) {
				char ch = value.charAt(idx);
				if (CharUtils.isAsciiPrintable(ch)) {
					sb.append(ch);
				} else if (ch == '\n') {
					sb.append("\\n");
				} else if (ch == '\t') {
					sb.append("\\t");
				} else {
					sb.append("\\u").append(StringUtils.leftPad(Integer.toHexString(ch), 4, '0'));
				}
			}
			return sb.toString();
		}
	}

	public static void propagateToMDC(HttpServletRequest request, Optional<Set<String>> include) {
		toMDC(WebMDCField.REQUEST_ID.getFieldName(), include,
			() -> request.getAttribute(PopulatorConstants.REQUEST_ID_ATTRIBUTE));

		toMDC(WebMDCField.CORRELATION_ID.getFieldName(), include,
			() -> request.getAttribute(PopulatorConstants.CORRELATION_ID));

		toMDC(WebMDCField.TYPE.getFieldName(), include,
			() -> request.getAttribute(PopulatorConstants.TYPE));

		toMDC(WebMDCField.OPERATION.getFieldName(), include,
			() -> request.getAttribute(PopulatorConstants.OPERATION));

		toMDC(WebMDCField.USER_ID.getFieldName(), include, request::getRemoteUser);

		toMDC(WebMDCField.HTTP_METHOD.getFieldName(), include, request::getMethod);
		toMDC(WebMDCField.URL.getFieldName(), include, request::getRequestURI);
		toMDC(WebMDCField.REQUEST_PARAMS.getFieldName(), include, request::getQueryString);
		toMDC(WebMDCField.REMOTE_IP.getFieldName(), include, request::getRemoteAddr);
		toMDC(WebMDCField.PORT.getFieldName(), include, () -> Integer.toString(request.getRemotePort()));
	}

	private static void toMDC(String key, Optional<Set<String>> include, Supplier<Object> supplier) {
		if (!include.isPresent() || include.get().contains(key)) {
			Object value = supplier.get();
			if (!ObjectUtils.isEmpty(value)) {
				MDC.put(key, value.toString());
			}
		}
	}
}
