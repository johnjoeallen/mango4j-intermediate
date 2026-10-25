package com.mastercard.commons.mango.logging.sanitiser;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionSanitiserTest {

	private ExceptionSanitiser sanitiser = new ExceptionSanitiser() {
	};

	@Test
	void getSanitisedMessage() {
		RuntimeException ex = new RuntimeException("Oops");
		assertThat(sanitiser.getSanitisedMessage(ex)).isEqualTo("Oops");
	}

	@Test
	void sanitiseStackTrace() {
		RuntimeException ex = new RuntimeException("Oops");
		List<String> stackTrace = Arrays.asList("first", "second");
		assertThat(sanitiser.sanitiseStackTrace(ex, stackTrace)).isEqualTo(stackTrace);
	}
}
