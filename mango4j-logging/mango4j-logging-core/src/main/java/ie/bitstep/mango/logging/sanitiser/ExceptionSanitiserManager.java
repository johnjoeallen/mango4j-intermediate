package ie.bitstep.mango.logging.sanitiser;

import org.apache.commons.collections4.iterators.ReverseListIterator;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Manager for sanitising exceptions, e.g. to remove unnecessary or sensitive information.
 */
public class ExceptionSanitiserManager {

	private static final List<ExceptionSanitiser> SANITISERS = new ArrayList<>(List.of(new IgnoredPackageSanitiser()));

	static {
		// other modules (e.g. the Spring one) contribute sanitisers through META-INF/services
		ServiceLoader.load(ExceptionSanitiser.class).forEach(SANITISERS::add);
	}

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
