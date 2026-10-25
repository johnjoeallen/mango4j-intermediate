package ie.bitstep.mango.utils.mapping.exceptions;

import tools.jackson.core.JacksonException;

public class MappingException extends RuntimeException {
	/**
	 * Wraps a JSON processing exception.
	 *
	 * @param e the underlying exception
	 */
	public MappingException(JacksonException e) {
		super(e);
	}
}
