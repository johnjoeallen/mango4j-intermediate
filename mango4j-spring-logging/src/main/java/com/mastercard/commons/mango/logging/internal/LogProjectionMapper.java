package com.mastercard.commons.mango.logging.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mastercard.commons.mango.logging.LogProjection;
import com.mastercard.commons.mango.logging.annotations.LoggableField;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@UtilityClass
@Slf4j
public class LogProjectionMapper {

	public static <T extends LogProjection> T map(Object source, Class<T> targetClass) {
		T instance = createInstance(targetClass);
		Map<String, Method> sourceGetters = getJsonPropertyGetterMap(source.getClass());
		Map<String, Method> targetSetters = getSetterMethods(targetClass);
		Set<String> declaredTargetFields = getDeclaredLoggableTargetFields(targetClass);

		return setTargetFields(source, targetSetters, declaredTargetFields, sourceGetters, instance);
	}

	private static <T extends LogProjection> T setTargetFields(Object source, Map<String, Method> targetSetters, Set<String> declaredTargetFields, Map<String, Method> sourceGetters, T instance) {
		for (Map.Entry<String, Method> entry : targetSetters.entrySet()) {
			if (declaredTargetFields.contains(entry.getKey())) {
				setTargetField(source, sourceGetters, instance, entry);
			}
		}

		return instance;
	}

	private static <T extends LogProjection> void setTargetField(Object source, Map<String, Method> sourceGetters, T instance, Map.Entry<String, Method> entry) {
		String name = entry.getKey();
		Method setter = entry.getValue();
		Method getter = sourceGetters.get(name);
		Object value = null;
		Type paramType = null;
		Object converted = null;

		if (getter != null) {
			try {
				value = getter.invoke(source);
				paramType = setter.getGenericParameterTypes()[0];
				converted = convert(value, paramType);
				setter.invoke(instance, converted);
			} catch (Exception e) {
				log.warn(
					String.format("Failed to set field(%s): %s: <%s> cannot be assigned to <%s>", name, e.getMessage(), getTypeName(converted), getTypeName(paramType)));
			}
		}
	}

	static String getTypeName(Object object) {
		return getStringValue(object, () -> object.getClass().getCanonicalName());
	}

	static String getTypeName(Type type) {
		return getStringValue(type, () -> type.getTypeName()); // Don't change to method reference or it can throw null pointer (method references and lamba's are not completely equivalent)
	}

	private static String getStringValue(Object object, Supplier<String> typeStringValueSupplier) {
		return object == null ? "null" : typeStringValueSupplier.get();
	}

	private static <T extends LogProjection> Set<String> getDeclaredLoggableTargetFields(Class<T> targetClass) {
		return Arrays.stream(targetClass.getDeclaredFields())
			.filter(f -> f.isAnnotationPresent(LoggableField.class))
			.map(Field::getName)
			.collect(Collectors.toSet());
	}

	private static <T extends LogProjection> T createInstance(Class<T> targetClass) {
		T instance;
		try {
			Constructor<T> ctor = targetClass.getDeclaredConstructor();
			instance = ctor.newInstance();
		} catch (Exception e) {
			throw new IllegalArgumentException(
				String.format("Target LogProjection class(%s) must have a public no-arg constructor", targetClass.getCanonicalName()),
				e);
		}
		return instance;
	}

	private static Object convert(Object source, Type targetType) {
		if (source == null) return null;

		if (targetType instanceof Class<?> clazz) {
			return mapEmbeddedProjection(source, clazz);
		}

		if (targetType instanceof ParameterizedType pt) {
			return handleParameterizedType(source, pt);
		}

		return source;
	}

	private static Object handleParameterizedType(Object source, ParameterizedType pt) {
		Class<?> raw = (Class<?>) pt.getRawType();

		if (List.class.isAssignableFrom(raw)) {
			return ((List<?>) source).stream()
				.map(e -> convert(e, pt.getActualTypeArguments()[0]))
				.toList();
		}

		if (Map.class.isAssignableFrom(raw)) {
			return ((Map<?, ?>) source).entrySet().stream()
				.collect(Collectors.toMap(
					e -> convert(e.getKey(), pt.getActualTypeArguments()[0]),
					e -> convert(e.getValue(), pt.getActualTypeArguments()[1])
				));
		}

		log.warn(
			String.format("Only List<>, Map<> supported for ParameterizedType, %s supplied", raw.getCanonicalName()));

		return null;
	}

	private static Object mapEmbeddedProjection(Object source, Class<?> clazz) {
		return (LogProjection.class.isAssignableFrom(clazz) && !clazz.isInterface())
			? map(source, clazz.asSubclass(LogProjection.class)) : source;
	}

	private static Map<String, Method> getJsonPropertyGetterMap(Class<?> clazz) {
		Map<String, Method> result = new HashMap<>();

		for (Field f : clazz.getDeclaredFields()) {
			JsonProperty jp = f.getAnnotation(JsonProperty.class);
			if (jp == null || jp.value().isEmpty()) continue;
			String propName = jp.value();
			String getterName = "get" + Character.toUpperCase(f.getName().charAt(0)) + f.getName().substring(1);
			try {
				Method getter = clazz.getMethod(getterName);
				result.put(propName, getter);
			} catch (NoSuchMethodException e) {
				log.warn(
					String.format("Cannot set field, getter not defined: %s", getterName),
					e
				);
			}
		}

		for (Method m : clazz.getDeclaredMethods()) {
			JsonProperty jp = m.getAnnotation(JsonProperty.class);
			if (jp == null || jp.value().isEmpty()) continue;
			String propName = jp.value();

			if (m.getName().startsWith("get")) {
				result.put(propName, m);
			}
		}

		return result;
	}

	private static Map<String, Method> getSetterMethods(Class<?> clazz) {
		Map<String, Method> result = new HashMap<>();
		for (Method m : clazz.getMethods()) {
			if (m.getName().startsWith("set") && m.getParameterCount() == 1) {
				String field = Character.toLowerCase(m.getName().charAt(3)) + m.getName().substring(4);
				result.put(field, m);
			}
		}
		return result;
	}
}
