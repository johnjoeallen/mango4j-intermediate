package com.mastercard.commons.mango.insights.test;

import com.mastercard.commons.mango.insights.initialiser.DefaultInsightsContextInitialiser;
import com.mastercard.commons.mango.insights.internal.InsightsProvider;
import com.mastercard.commons.mango.insights.internal.InsightsThreadContext;

import java.lang.reflect.Method;
import java.util.Collections;

public class TestUtils {
	private static final InsightsProvider MOCK_INSIGHTS_PROVIDER = new InsightsProvider(
		Collections.singletonList(new DefaultInsightsContextInitialiser()),
		Collections.emptyList(),
		null);

	private TestUtils() { // NOSONAR
		// SONAR
	}

	public static void fakeInsight(Object object, String method) throws NoSuchMethodException {
		InsightsThreadContext.get().push(object.getClass(), getMethod(object, method), MOCK_INSIGHTS_PROVIDER);
	}

	public static void fakeInsight(Object object, String method, Class... params) throws NoSuchMethodException {
		InsightsThreadContext.get().push(object.getClass(), getMethod(object, method, params), MOCK_INSIGHTS_PROVIDER);
	}

	private static Method getMethod(Object object, String method, Class[] params) throws NoSuchMethodException {
		return object.getClass().getMethod(method, params);
	}

	private static Method getMethod(Object object, String method) throws NoSuchMethodException {
		return object.getClass().getMethod(method);
	}
}
