package ie.bitstep.mango.spring.logging.masking;

/**
 * Masks sensitive data in values (eg for logging).
 */
@FunctionalInterface
public interface Masker {
	/**
	 * Masks the supplied value.
	 *
	 * @param value the input value
	 * @return the masked value
	 */
	String mask(String value);
}
