package com.mastercard.commons.mango.logging.internal;

import com.mastercard.commons.mango.logging.LogProjection;
import com.mastercard.commons.mango.logging.annotations.LoggableClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LogProjectionRegistry {
	private static final Map<Class<?>, LogProjection<?>> LOG_PROJECTION_MAP = new HashMap<>();

	@Autowired
	@SuppressWarnings("java:S1118")
	public LogProjectionRegistry(ApplicationContext context) {
		LOG_PROJECTION_MAP.clear();
		LOG_PROJECTION_MAP.putAll(context.getBeansOfType(LogProjection.class).values().stream()
			.filter(w -> w.getClass().isAnnotationPresent(LoggableClass.class))
			.collect(Collectors.<LogProjection, Class<?>, LogProjection<?>>toMap(LogProjection::getProjectedType, w -> w))
		);
	}

	@SuppressWarnings("unchecked")
	public static <T> LogProjection<T> getLogProjection(Class<T> clazz) {
		return (LogProjection<T>) LOG_PROJECTION_MAP.get(clazz);
	}
}