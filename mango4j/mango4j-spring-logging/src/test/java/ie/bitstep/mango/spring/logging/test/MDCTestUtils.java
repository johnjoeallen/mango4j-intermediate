package ie.bitstep.mango.spring.logging.test;

import org.slf4j.MDC;
import org.slf4j.helpers.BasicMDCAdapter;

import java.lang.reflect.Field;

/**
 * Resets the SLF4J MDC adapter so MDC works the same way in tests whichever logging binding is present.
 */
public final class MDCTestUtils {
	private static final String[] MDC_ADAPTER_FIELDS = {
		"MDC_ADAPTER", // SLF4J 2.x
		"mdcAdapter" // SLF4J 1.x
	};

	private MDCTestUtils() {
		// utility class
	}

	/**
	 * Installs a fresh MDC adapter.
	 */
	public static void init() {
		setAdapter(new BasicMDCAdapter());
	}

	/**
	 * Removes the MDC adapter installed by {@link #init()}.
	 */
	public static void cleanup() {
		setAdapter(null);
	}

	private static void setAdapter(Object adapter) {
		for (String fieldName : MDC_ADAPTER_FIELDS) {
			try {
				Field field = MDC.class.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(null, adapter);
			} catch (ReflectiveOperationException | RuntimeException e) {
				// field does not exist in this SLF4J version
			}
		}
	}
}
