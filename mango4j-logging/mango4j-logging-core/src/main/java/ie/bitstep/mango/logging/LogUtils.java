package ie.bitstep.mango.logging;

import ie.bitstep.mango.logging.sanitiser.ExceptionSanitiserManager;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Helper methods for logging.
 */
public class LogUtils {

	private LogUtils() {
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
	 * Like {@link #getStackTrace(Throwable)} but never includes exception messages: each exception
	 * (including causes and suppressed exceptions) is shown as its class name only, followed by its frames.
	 *
	 * @see #getStackTrace(Throwable, boolean, boolean)
	 */
	public static List<String> getStackTraceWithoutMessages(Throwable throwable) {
		return getStackTrace(throwable, false, true);
	}

	/**
	 * Stack trace as a list of lines, with the parts that can hold sensitive data under the caller's control.
	 * Shape: {@code "java.lang.RuntimeException: msg", "at ...", "Caused by: java.io.IOException: msg", "at ...", ...}.
	 * Frames shared with the enclosing exception are omitted, as in {@link Throwable#printStackTrace()}.
	 * With both flags true the result is the same as {@link #getStackTrace(Throwable)}.
	 *
	 * @param withMessages include exception messages (otherwise each exception is just its class name; an exception
	 *                     whose message is only its cause's text shows no message when causes are not included)
	 * @param withCauses   include the cause chain and suppressed exceptions
	 */
	public static List<String> getStackTrace(Throwable throwable, boolean withMessages, boolean withCauses) {
		List<String> lines = new ArrayList<>();
		addTrace(throwable, "", new StackTraceElement[0], lines, Collections.newSetFromMap(new IdentityHashMap<>()),
			withMessages, withCauses);
		return ExceptionSanitiserManager.sanitiseStackTrace(throwable, lines);
	}

	private static void addTrace(Throwable t, String caption, StackTraceElement[] enclosingTrace, List<String> lines,
								 Set<Throwable> seen, boolean withMessages, boolean withCauses) {
		if (!seen.add(t)) {
			lines.add(caption + "[CIRCULAR REFERENCE: " + t.getClass().getName() + "]");
			return;
		}

		lines.add((caption + traceHeader(t, withMessages, withCauses)).stripTrailing());    // same trimming as getStackTrace

		StackTraceElement[] trace = t.getStackTrace();
		int m = trace.length - 1;
		int n = enclosingTrace.length - 1;
		while (m >= 0 && n >= 0 && trace[m].equals(enclosingTrace[n])) {
			m--;
			n--;
		}

		for (int i = 0; i <= m; i++) {
			lines.add("at " + trace[i]);
		}

		if (withCauses) {
			for (Throwable suppressed : t.getSuppressed()) {
				addTrace(suppressed, "Suppressed: ", trace, lines, seen, withMessages, true);
			}

			Throwable cause = t.getCause();
			if (cause != null) {
				addTrace(cause, "Caused by: ", trace, lines, seen, withMessages, true);
			}
		}
	}

	private static String traceHeader(Throwable t, boolean withMessages, boolean withCauses) {
		String className = t.getClass().getName();
		String message = withMessages ? t.getLocalizedMessage() : null;

		if (message == null) {
			return className;
		}

		// the JDK derives a wrapper's message from its cause (cause.toString()); don't leak that when causes are excluded
		Throwable cause = t.getCause();
		if (!withCauses && cause != null && message.equals(cause.toString())) {
			return className;
		}

		return className + ": " + message;
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
}
