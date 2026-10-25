package com.mastercard.commons.mango.insights.internal;

import java.util.Map;
import java.util.Optional;

public class Insights {

	private Insights() {
	}

	public static Optional<AppInsight> current() {
		return InsightsThreadContext.get().current();
	}

	public static void setContextValue(String key, Object value) {
		current().ifPresent(insight -> insight.setContextValue(key, value));
	}

	public static Object getContextValue(String key) {
		return current().map(insight ->
			insight.getContext().get(key)
		).orElse(null);
	}

	public static void setContextValues(Map<String, Object> keyValues) {
		keyValues.forEach(Insights::setContextValue);
	}

	public static void setTag(String key, Object value) {
		current().ifPresent(insight -> insight.setTag(key, value));
	}
}
