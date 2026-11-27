package ie.bitstep.mango.spring.logging.detail;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * How specifically a rule's exception type matches a thrown exception, so that rules apply by the most specific
 * type and the order they are declared in does not matter.
 */
public final class ExceptionTypes {

	private ExceptionTypes() {
	}

	/**
	 * The number of steps up the class hierarchy (superclasses and interfaces) from the exception's class to the rule's
	 * type: 0 for the exact class, 1 for its direct superclass, and so on.
	 *
	 * @return the distance, or -1 if the rule's type does not match the exception's class
	 */
	public static int distance(Class<?> exceptionClass, Class<?> ruleType) {
		if (!ruleType.isAssignableFrom(exceptionClass)) {
			return -1;
		}

		Deque<Class<?>> current = new ArrayDeque<>();
		Set<Class<?>> seen = new HashSet<>();
		current.add(exceptionClass);
		int distance = 0;

		while (!current.isEmpty()) {
			Deque<Class<?>> next = new ArrayDeque<>();
			for (Class<?> c : current) {
				if (c == ruleType) {
					return distance;
				}
				if (seen.add(c)) {
					if (c.getSuperclass() != null) {
						next.add(c.getSuperclass());
					}
					next.addAll(Set.of(c.getInterfaces()));
				}
			}
			current = next;
			distance++;
		}
		return -1;
	}
}
