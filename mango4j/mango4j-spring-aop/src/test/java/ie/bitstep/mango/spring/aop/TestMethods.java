package ie.bitstep.mango.spring.aop;

import java.lang.reflect.Method;

final class TestMethods {
	private TestMethods() {
		// utility class
	}

	/**
	 * Returns a declared method of the class by name and parameter types.
	 *
	 * @param clazz the class to inspect
	 * @param name the method name
	 * @param parameterTypes the parameter types
	 * @return the method
	 */
	static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
		try {
			return clazz.getDeclaredMethod(name, parameterTypes);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
