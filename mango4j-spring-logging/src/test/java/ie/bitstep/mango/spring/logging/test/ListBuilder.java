package ie.bitstep.mango.spring.logging.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Fluent builder for lists in tests.
 *
 * @param <V> element type
 */
public final class ListBuilder<V> {
	private final List<V> list = new ArrayList<>();

	private ListBuilder() {
		// use list()
	}

	public static <V> ListBuilder<V> list() {
		return new ListBuilder<>();
	}

	public ListBuilder<V> add(V value) {
		list.add(value);
		return this;
	}

	public ListBuilder<V> add(V[] values) {
		list.addAll(Arrays.asList(values));
		return this;
	}

	public ListBuilder<V> add(Collection<V> values) {
		list.addAll(values);
		return this;
	}

	public List<V> build() {
		return list;
	}
}
