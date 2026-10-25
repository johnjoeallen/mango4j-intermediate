package ie.bitstep.mango.spring.logging.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import ie.bitstep.mango.spring.logging.LogProjection;
import ie.bitstep.mango.spring.logging.LogProjectionToMap;
import ie.bitstep.mango.spring.logging.adapter.AbstractLoggerAdapter;
import ie.bitstep.mango.spring.logging.annotations.LoggableClass;
import ie.bitstep.mango.spring.logging.annotations.LoggableField;
import ie.bitstep.mango.spring.logging.annotations.LoggableFieldView;
import ie.bitstep.mango.spring.logging.annotations.LoggableStyle;
import ie.bitstep.mango.spring.logging.annotations.LoggableView;
import ie.bitstep.mango.spring.logging.internal.accessors.FieldAccessor;
import ie.bitstep.mango.spring.logging.internal.accessors.GetterAccessor;
import ie.bitstep.mango.spring.logging.internal.accessors.PropertyAccessor;
import ie.bitstep.mango.spring.logging.converters.Converter;
import ie.bitstep.mango.spring.logging.converters.ConverterFactory;
import ie.bitstep.mango.utils.masking.Mask;
import ie.bitstep.mango.utils.masking.Masker;
import ie.bitstep.mango.utils.masking.MaskerFactory;
import ie.bitstep.mango.utils.masking.NoMasker;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Prepares objects for logging.
 */
public class LoggerMapper {

	private static final Map<Class<?> /* type */, Map<String /* field-name */, AspectLoggerMetaData>> META_DATA = new ConcurrentHashMap<>();
	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
			.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
			.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.build();
	public static final Logger LOGGER = LoggerFactory.getLogger(LoggerMapper.class);

	private LoggerMapper() {
		// NOSONAR
	}

	/**
	 * Prepares an object for logging and serialises it to json.
	 *
	 * @param object object to convert
	 * @return json string (or string with exception message if failed)
	 */
	public static String toJsonString(Object object) {
		return loggableToJsonString(toLoggable(object));
	}

	/**
	 * Serialises an object prepared for logging to json.
	 *
	 * @param loggable loggable object to serialise
	 * @return json string (or string with exception message if failed)
	 */
	public static String loggableToJsonString(Object loggable) {
		try {
			return OBJECT_MAPPER.writeValueAsString(loggable);
		} catch (Exception ex) {  // NOSONAR return exception message instead of re-throwing it
			return ex.getMessage();
		}
	}

	public static Map<String, Object> toLoggable(Map<?, ?> m) {
		if (m == null) {
			return null;    // NOSONAR null for null
		}

		Map<String, Object> result = new LinkedHashMap<>();

		m.forEach((k, v) -> {
			try {
				if (k == null) {
					throw new NullKeyException(v);
				} else {
					result.put(k.toString(), toLoggable(v));
				}
			} catch (Exception e) {
				// ignore, cannot log, make sure the app code does not fail because of this
				LOGGER.error("Failed to convert map to Loggable", e);
			}
		});

		return AbstractLoggerAdapter.loggableToMap(result);
	}

	static Collection<Object> toLoggable(Collection<?> l) {
		if (l == null) {
			return null;    // NOSONAR null for null
		}
		return l.stream()
			.map(LoggerMapper::toLoggable)
			.toList();
	}

	static Object toLoggable(LoggableValue loggableValue) {
		if (loggableValue.getValue() == null) {
			return null;    // NOSONAR null for null
		} else if (loggableValue.getMasker() != null) {
			return logMasked(loggableValue);
		} else if (loggableValue.getConverter() != null) {
			return logConverted(loggableValue);
		} else if (loggableValue.getTruncateLen() > 0) {
			return logTruncated(loggableValue);
		} else if (loggableValue.getView() != LoggableView.ALL) {
			return logIdentifiers(loggableValue);
		} else {
			return toLoggable(loggableValue.getValue());
		}
	}

	private static List<Integer> toCollection(int[] o) {
		return Arrays.stream(o).boxed().toList();
	}

	private static List<Long> toCollection(long[] o) {
		return Arrays.stream(o).boxed().toList();
	}

	private static List<Float> toCollection(float[] o) {
		List<Float> l = new ArrayList<>();

		for (float i : o) {
			l.add(i);
		}
		return l;
	}

	private static List<Double> toCollection(double[] o) {
		return Arrays.stream(o).boxed().toList();
	}

	/**
	 * Converts an object to a loggable form.
	 *
	 * @param o object to convert
	 * @return loggable object (or string with exception message if failed)
	 */
	public static Object toLoggable(Object o) {        // NOSONAR ignore complexity
		LogProjection<?> projection = null;

		try {
			if (o == null) {
				return null;
			} else if (o instanceof HibernateProxy hibernateProxy) {
				return toLoggable(hibernateProxy);
			} else if (o instanceof Optional<?> optional) {
				return toLoggable(optional);
			} else if (o instanceof Map<?, ?> map) {
				return toLoggable(map);
			} else if (o instanceof Collection<?> collection) {
				return toLoggable(collection);
			} else if (o instanceof int[] intArray) {
				return toCollection(intArray);
			} else if (o instanceof long[] longArray) {
				return toCollection(longArray);
			} else if (o instanceof float[] floatArray) {
				return toCollection(floatArray);
			} else if (o instanceof double[] doubleArray) {
				return toCollection(doubleArray);
			} else if (o instanceof byte[] byteArray) {
				return Base64.getEncoder().encodeToString(byteArray);
			} else if (o.getClass().isArray()) {
				return toLoggable(Arrays.asList((Object[]) o));
			} else if (o instanceof Number number) {
				return number;
			} else if (o instanceof Date date) {
				return date.toInstant().toString();    // uses ISO format
			} else if (o instanceof Instant instant) {
				return instant.toString();    // uses ISO format
			} else if (o instanceof LoggableValue loggableValue) {
				return toLoggable(loggableValue); // NOSONAR, cast is necessary for over-loaded method matching
			} else if (o instanceof ToLoggable toLoggable) {
				return toLoggable.toLoggable();
			} else if (o.getClass().isAnnotationPresent(LoggableClass.class)) {
				return toLoggablePojo(o, LoggableView.ALL);
			} else if ((projection = LogProjectionRegistry.getLogProjection(o.getClass())) != null) {
				return toLoggablePojo(o, projection, LoggableView.ALL);
			} else {
				return o.toString();
			}
		} catch (Exception ex) {  // NOSONAR return exception message instead of re-throwing it
			return ex.getMessage();
		}
	}

	private static Object toLoggable(Optional<?> o) {
		Optional<?> optional = o;
		return optional.isPresent() ? toLoggable(optional.get()) : null;
	}

	private static Object toLoggable(HibernateProxy o) {
		HibernateProxy hibernateProxy = o;
		LazyInitializer initializer =
			hibernateProxy.getHibernateLazyInitializer();
		return toLoggable(initializer.getImplementation());
	}

	private static Object logIdentifiers(LoggableValue loggableValue) {
		if (loggableValue.getView() == LoggableView.ID) {
			if (loggableValue.getValue() instanceof Identifiable identifiable) {
				return Map.of("id", identifiable.getId());
			} else {
				return "Class " + loggableValue.getValue().getClass() + " must implement Identifiable";
			}
		} else {
			return toLoggablePojo(loggableValue.getValue(), loggableValue.getView());
		}
	}

	private static Object logTruncated(LoggableValue loggableValue) {
		if (loggableValue.getValue() instanceof Collection) {
			return ((Collection<?>) loggableValue.getValue()).stream()
				.map(item -> truncate(item, loggableValue.getTruncateLen()))
				.toList();
		} else {
			return truncate(loggableValue.getValue(), loggableValue.getTruncateLen());
		}
	}

	private static Object logMasked(LoggableValue loggableValue) {
		if (loggableValue.getValue() instanceof Map<?, ?> m) {
			return toMasked(loggableValue.getMasker(), m);
		} else if (loggableValue.getValue() instanceof Collection) {
			return ((Collection<?>) loggableValue.getValue()).stream()
				.map(item -> toMasked(loggableValue.getMasker(), item))
				.toList();
		} else {
			return toMasked(loggableValue.getMasker(), loggableValue.getValue());
		}
	}

	private static Object toMasked(Masker masker, Object o) {
		if (o == null) {
			return null;
		} else if (o instanceof String s) {
			return masker.mask(s);
		} else if (o instanceof Map<?, ?> m) {
			return toMasked(masker, m);
		} else if (o instanceof Collection<?> c) {
			return toMasked(masker, c);
		} else if (o.getClass().isAnnotationPresent(LoggableClass.class)) {
			return toLoggablePojo(o, LoggableView.ALL);
		} else {
			return masker.mask(o.toString());
		}
	}

	private static Object toMasked(Masker masker, Map<?, ?> m) {
		Map<Object, Object> map = new HashMap<>();
		m.forEach((key, value) -> map.put(key, value == null ? null : toMasked(masker, value)));
		return map;
	}

	private static Object toMasked(Masker masker, Collection<?> c) {
		return c.stream().map(
			e -> toMasked(masker, e)
		).toList();
	}

	private static Object logConverted(LoggableValue loggableValue) {
		return loggableValue.getConverter().apply(loggableValue.getValue());
	}

	/**
	 * @param object instance of a class that has the {@link LoggableClass} annotation
	 * @return Map containing the names of {@code object}'s fields that should be logged
	 */
	private static Map<String, AspectLoggerMetaData> getAspectLoggerMetaData(Object object) {
		return META_DATA.computeIfAbsent(object.getClass(), clazz -> {
				Map<String, AspectLoggerMetaData> metaData = new LinkedHashMap<>();
				while (clazz != null) {
					putClassMetaData(metaData, clazz);

					clazz = clazz.getSuperclass();
				}

				return metaData;
			}
		);
	}

	private static void putClassMetaData(Map<String, AspectLoggerMetaData> metaData, Class<?> clazz) {
		LoggableClass loggableClass = clazz.getAnnotation(LoggableClass.class);

		if (loggableClass != null) {
			for (Field f : clazz.getDeclaredFields()) {
				putFieldMetaData(metaData, new FieldAccessor(f), loggableClass);
			}
			for (Method m : clazz.getDeclaredMethods()) {
				putFieldMetaData(metaData, new GetterAccessor(m), loggableClass);
			}
		}
	}

	private static void putFieldMetaData(Map<String, AspectLoggerMetaData> fieldMap, PropertyAccessor f, LoggableClass loggableClass) {  // NOSONAR - accept complexity
		LoggableField loggableField = f.getAnnotation(LoggableField.class);
		Mask mask = f.getAnnotation(Mask.class);
		Converter converter = f.getAnnotation(Converter.class);
		LoggableFieldView loggableFieldView = f.getAnnotation(LoggableFieldView.class);
		JsonProperty jsonProperty = f.getAnnotation(JsonProperty.class);

		if (loggableField != null
			|| loggableClass.style() == LoggableStyle.ALL && f.isIncludeInAll()) {

			String fieldName;
			if (loggableField != null && !loggableField.name().isEmpty()) {
				fieldName = loggableField.name();
			} else if (isValidJsonProperty(jsonProperty)) {
				fieldName = jsonProperty.value();
			} else {
				fieldName = f.getName();
			}

			Masker valueMasker = null;
			if (loggableField != null) {
				valueMasker = getMasker(loggableField, mask);
			}

			UnaryOperator<Object> valueConverter = null;
			if (loggableField != null) {
				valueConverter = getConverter(converter);
			}

			int valueTruncateLen = (loggableField != null) ? loggableField.truncateLen() : 0;

			LoggableView valueView = LoggableView.ALL;
			if (loggableField != null) {
				valueView = loggableField.view();
			}

			Set<LoggableView> belongsToViews;
			if (loggableFieldView != null) {
				belongsToViews = new HashSet<>(Arrays.asList(loggableFieldView.value()));
				belongsToViews.add(LoggableView.ALL);
			} else {
				belongsToViews = Collections.singleton(LoggableView.ALL);
			}

			AspectLoggerMetaData aspectLoggerMetaData = new AspectLoggerMetaData(
				f::getValue, fieldName, valueMasker, valueConverter, valueTruncateLen, valueView, belongsToViews);
			fieldMap.put(aspectLoggerMetaData.getName(), aspectLoggerMetaData);
		}
	}

	private static UnaryOperator<Object> getConverter(Converter converter) {
		return converter != null ? ConverterFactory.getConverter(converter.converter()) : null;
	}

	@SuppressWarnings({"deprecation", "squid:CallToDeprecatedMethod"})
	private static Masker getMasker(LoggableField loggableField, Mask mask) {
		if (mask != null) {
			return MaskerFactory.getMasker(mask);
		} else if (loggableField.mask() != NoMasker.class) {
			return MaskerFactory.getMasker(loggableField.mask());
		}

		return null;
	}

	static boolean isValidJsonProperty(JsonProperty jsonProperty) {
		return jsonProperty != null && !jsonProperty.value().isEmpty();
	}

	/**
	 * Converts {@code object} with {@link LoggableClass} annotation into a Map of the field values that are contained inside the object.
	 * If any fields are of type {@link LoggableClass} then they are also converted into a Map in the same format.
	 * The defaults for each field can be modified with {@link LoggableField} annotation on that field.
	 *
	 * @param object object containing some fields
	 * @return Map of field name to objects
	 */
	static Map<String, Object> toLoggablePojo(Object object, LoggableView view) {
		if (object == null) {
			return null;    // NOSONAR null for null
		}

		Map<String, Object> result = new LinkedHashMap<>();

		for (AspectLoggerMetaData fieldMetaData : getAspectLoggerMetaData(object).values()) {
			if (fieldMetaData.getBelongsToViews().contains(view)) {
				Object value = getFieldValue(object, fieldMetaData);
				if (value != null) {
					result.put(fieldMetaData.getName(), value);
				}
			}
		}

		return result;
	}

	/**
	 * Non optimal implementation for testing, no caching etc... no field meta data etc....
	 */
	/**
	 * @param o
	 * @param projection
	 * @param view
	 * @return
	 */
	@SuppressWarnings({"unchecked", "java:S3740"})
	static Map<String, Object> toLoggablePojo(Object o, LogProjection projection, LoggableView view) {
		if (projection instanceof LogProjectionToMap logProjectionToMap) {
			return logProjectionToMap.toMap(o);
		}

		return toLoggablePojo(LogProjectionMapper.map(o, projection.getClass()), view);
	}

	static String getFieldName(Method method, LoggableField loggableField) {
		return loggableField.name().isEmpty() ? method.getName() : loggableField.name();
	}

	static Object getValue(Object o, LogProjection<?> wrapper, Method method) {
		try {
			return method.invoke(wrapper, o);
		} catch (Exception e) {
			return null;
		}
	}

	static Object getFieldValue(Object object, AspectLoggerMetaData fieldMetaData) {
		if (object == null || fieldMetaData == null) {
			return object;
		}

		try {
			LoggableValue loggableValue = new LoggableValue(
				fieldMetaData.getAccessor().apply(object),
				fieldMetaData.getValueMasker(),
				fieldMetaData.getValueConverter(),
				fieldMetaData.getTruncateLen(),
				fieldMetaData.getValueView());
			return toLoggable(loggableValue);
		} catch (Exception e) { // NOSONAR
			return e.getLocalizedMessage();
		}
	}

	static String truncate(Object value, int truncateLen) {
		if (value == null) {
			return null;    // NOSONAR null for null
		}
		if (value instanceof byte[] ba) {
			return StringUtils.abbreviate(Base64.getEncoder().encodeToString(ba), truncateLen);
		} else {
			return StringUtils.abbreviate(value.toString(), truncateLen);
		}
	}


}
