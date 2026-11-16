package ie.bitstep.mango.spring.logging.sanitiser;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Manager for sanitising exceptions, e.g. to remove unnecessary or sensitive information.
 */
public class ExceptionSanitiserManager {

	private static final List<ExceptionSanitiser> SANITISERS = new ArrayList<>(Arrays.asList(
		new IgnoredPackageSanitiser(),
		new MethodArgumentNotValidExceptionSanitiser()));

	private ExceptionSanitiserManager() { // SONAR
	}

	public static void addSanitiser(ExceptionSanitiser sanitiser) {
		SANITISERS.add(sanitiser);
	}

	public static String getSanitisedMessage(Throwable throwable) {
		String originalMessage = throwable.getMessage();

		// evaluate in reverse order so that custom sanitisers take precedence
		ReverseListIterator<ExceptionSanitiser> reverseListIterator = new ReverseListIterator<>(SANITISERS);

		while (reverseListIterator.hasNext()) {
			ExceptionSanitiser sanitiser = reverseListIterator.next();
			String sanitisedMessage = sanitiser.getSanitisedMessage(throwable);

			if (!StringUtils.equals(sanitisedMessage, originalMessage)) {
				return sanitisedMessage;
			}
		}

		return originalMessage;
	}

	public static List<String> sanitiseStackTrace(Throwable throwable, List<String> stackTrace) {
		for (ExceptionSanitiser sanitiser : SANITISERS) {
			stackTrace = sanitiser.sanitiseStackTrace(throwable, stackTrace);
		}

		return stackTrace;
	}

}
