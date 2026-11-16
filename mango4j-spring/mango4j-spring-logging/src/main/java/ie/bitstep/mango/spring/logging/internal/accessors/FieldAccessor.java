package ie.bitstep.mango.spring.logging.internal.accessors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldAccessor implements PropertyAccessor {
	private final Field field;

	public FieldAccessor(Field field) {
		this.field = field;
	}

	@Override
	public String getName() {
		return field.getName();
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> clazz) {
		return field.getAnnotation(clazz);
	}

	@Override
	public boolean isIncludeInAll() {
		return !Modifier.isStatic(field.getModifiers());
	}

	@Override
	public Object getValue(Object target) {
		field.setAccessible(true); // NOSONAR
		return getObject(target);
	}

	public Object getObject(Object target) {
		try {
			return field.get(target);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
}