package ie.bitstep.mango.spring.insights.internal;

import java.util.ArrayList;
import java.util.List;

public class ExceptionUtils {
	private ExceptionUtils() {
		// SONAR
	}

	public static List<ExceptionDetail> getCauses(Throwable t) {
		List<ExceptionDetail> causes = new ArrayList<>();
		Throwable current = t;

		while (current != null) {
			causes.add(ExceptionDetail.builder().name(current.getClass().getSimpleName()).message(current.getMessage()).build());
			current = current.getCause();
		}

		return causes;
	}

}
