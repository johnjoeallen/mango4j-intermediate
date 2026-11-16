package ie.bitstep.mango.spring.logging.internal.accessors;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class GetterAccessor implements PropertyAccessor {
	private final Method method;

	public GetterAccessor(Method method) {
		this.method = method;
	}

	@Override
	public String getName() {
		if (method.getName().startsWith("get")) {
			return Introspector.decapitalize(method.getName().substring("get".length()));
		} else if (method.getName().startsWith("is")) {
			return Introspector.decapitalize(method.getName().substring("is".length()));
		} else {
			return method.getName();
		}
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> clazz) {
		return method.getAnnotation(clazz);
	}

	@Override
	public boolean isIncludeInAll() {
		return false;
	}

	@Override
	public Object getValue(Object target) {
		method.setAccessible(true);  // NOSONAR
		return getObject(target);
	}

	public Object getObject(Object target) {
		try {
			return method.invoke(target);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}