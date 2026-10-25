package com.mastercard.commons.mango.insights.internal;

import lombok.Data;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

@Data
public class InsightsContext {

	public static final String HTTP_KEY = "http";
	public static final String HTTP_METHOD_KEY = "method";
	public static final String HTTP_URL_KEY = "url";
	public static final String HTTP_STATUS_CODE_KEY = "status";
	public static final String HTTP_MAPPING_KEY = "mapping";

	private final Deque<AppInsight> insights = new ArrayDeque<>();
	private final Deque<AppInsight> stack = new ArrayDeque<>();

	public AppInsight push(Class<?> targetClass, Method method, InsightsProvider insightsProvider) {
		return push(initialiseInsight(targetClass, method, insightsProvider));
	}

	public void pop() {
		if (!stack.isEmpty()) {
			stack.pop();
		}
	}

	public void reset() {
		insights.clear();
		stack.clear();
	}

	public boolean isStackEmpty() {
		return stack.isEmpty();
	}

	public Optional<AppInsight> current() {
		return !isStackEmpty() ? Optional.of(stack.getFirst()) : Optional.empty();
	}

	public void publish(InsightsProvider insightsProvider) {
		for (AppInsight appInsight : insights) {
			insightsProvider.publish(appInsight);
		}
	}

	private AppInsight initialiseInsight(Class<?> targetClass, Method method, InsightsProvider insightsProvider) {
		AppInsight appInsight = insightsProvider.newInsight(method);
		setMethodName(appInsight, targetClass, method);
		return appInsight;
	}

	private void setMethodName(AppInsight appInsight, Class<?> targetClass, Method method) {
		appInsight.setMethod(targetClass.getSimpleName() + "." + method.getName());
	}

	AppInsight push(AppInsight appInsight) {
		insights.add(appInsight);
		stack.push(appInsight);
		return appInsight;
	}
}
