package ie.bitstep.mango.spring.logging.test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent builder for insertion-ordered maps in tests.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class MapBuilder<K, V> {
	private final Map<K, V> map = new LinkedHashMap<>();

	private MapBuilder() {
		// use map()
	}

	public static <K, V> MapBuilder<K, V> map() {
		return new MapBuilder<>();
	}

	public MapBuilder<K, V> with(K key, V value) {
		map.put(key, value);
		return this;
	}

	public Map<K, V> build() {
		return map;
	}
}
