package ie.bitstep.mango.logging.internal;

import ie.bitstep.mango.logging.LogProjection;
import ie.bitstep.mango.logging.annotations.LoggableClass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the {@link LogProjection}s known to the logging mapper, keyed by the type they project. Framework
 * integrations (e.g. the Spring module) call {@link #register(Collection)} with the projections they discover.
 */
public class LogProjectionRegistry {
	private static final Map<Class<?>, LogProjection<?>> LOG_PROJECTION_MAP = new HashMap<>();

	private LogProjectionRegistry() {
	}

	/**
	 * Replaces the registered projections. Only projections whose class is annotated with
	 * {@link LoggableClass} are kept.
	 *
	 * @param projections the discovered projections
	 */
	public static synchronized void register(Collection<? extends LogProjection<?>> projections) {
		LOG_PROJECTION_MAP.clear();
		for (LogProjection<?> projection : projections) {
			if (projection.getClass().isAnnotationPresent(LoggableClass.class)) {
				LOG_PROJECTION_MAP.put(projection.getProjectedType(), projection);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static synchronized <T> LogProjection<T> getLogProjection(Class<T> clazz) {
		return (LogProjection<T>) LOG_PROJECTION_MAP.get(clazz);
	}
}
