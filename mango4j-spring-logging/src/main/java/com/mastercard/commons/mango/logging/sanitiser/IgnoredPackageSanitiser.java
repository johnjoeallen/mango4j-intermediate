package com.mastercard.commons.mango.logging.sanitiser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Exception sanitiser that removes unwanted packages from the stack trace.
 */
public class IgnoredPackageSanitiser implements ExceptionSanitiser {
	private static final List<String> IGNORED_PACKAGES = new ArrayList<>(Arrays.asList(
		"brave.",
		"java.lang.reflect.",
		"java.base/java.lang.reflect.",
		"java.base/jdk.internal.reflect.",
		"java.base/java.util.ArrayList.",
		"java.base/jdk.internal.reflect",
		"reactor.core.publisher",
		"java.base/java.lang",
		"java.lang.Thread.",
		"java.util.concurrent.",
		"javax.servlet.",
		"org.apache.catalina.",
		"org.apache.coyote.",
		"org.apache.logging.",
		"org.apache.tomcat.",
		"org.aspectj.",
		"org.jboss.as.",
		"org.springframework.aop.",
		"org.springframework.boot.actuate.",
		"org.springframework.cache.",
		"org.springframework.cglib.proxy.",
		"org.springframework.cloud.sleuth.",
		"org.springframework.scheduling.",
		"org.springframework.security.web.",
		"org.springframework.transaction.",
		"org.springframework.web.filter.",
		"org.springframework.web.method.",
		"org.springframework.web.servlet.",
		"org.junit.",
		"sun.reflect.",
		"org.pitest.",
		"com.mastercard.refarch.filter.",
		"com.mastercard.commons.mango.logging.internal.",
		"com.mastercard.commons.mango.logging.aspect."
	));

	public static void ignorePackage(String packageName) {
		IgnoredPackageSanitiser.IGNORED_PACKAGES.add(packageName);
	}

	public static List<String> sanitiseStackTrace(List<String> stack) {
		List<String> returnStack = new ArrayList<>();
		int traceLineIdx = -1;

		for (String s : stack) {
			if (!s.startsWith("at ")) {
				traceLineIdx = -1;
			}

			++traceLineIdx;

			String method = s.replaceAll("^\t*at ", "").trim();

			if (traceLineIdx > 1 && isIgnoredPackage(method)) {
				// make sure that the 0th (exception message) and the 1st (thrown from) records are never ignored
				continue;
			}

			returnStack.add(s.replace("\t", ""));
		}

		return returnStack;
	}

	@Override
	public List<String> sanitiseStackTrace(Throwable throwable, List<String> stackTrace) {
		return sanitiseStackTrace(stackTrace);
	}

	private static boolean isIgnoredPackage(String line) {
		return line.contains("$") ||
			IGNORED_PACKAGES.stream().anyMatch(line::startsWith);
	}
}
