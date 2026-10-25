package ie.bitstep.mango.spring.logging.sanitiser;

import java.util.List;

/**
 * Decorator for sanitising exceptions, e.g. to remove unnecessary or sensitive information.
 */
public interface ExceptionSanitiser {

	default String getSanitisedMessage(Throwable throwable) {
		return throwable.getMessage();
	}

	default List<String> sanitiseStackTrace(Throwable throwable, List<String> stackTrace) {
		return stackTrace;
	}
}
