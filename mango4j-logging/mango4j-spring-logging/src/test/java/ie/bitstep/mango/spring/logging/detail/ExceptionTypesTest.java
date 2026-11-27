package ie.bitstep.mango.spring.logging.detail;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTypesTest {

	interface Marker {
	}

	static class MarkedException extends IllegalStateException implements Marker {
	}

	@Test
	void distance_exactClassIsZero() {
		assertThat(ExceptionTypes.distance(IOException.class, IOException.class)).isZero();
	}

	@Test
	void distance_countsStepsUpTheSuperclassChain() {
		assertThat(ExceptionTypes.distance(IllegalStateException.class, RuntimeException.class)).isEqualTo(1);
		assertThat(ExceptionTypes.distance(IllegalStateException.class, Exception.class)).isEqualTo(2);
		assertThat(ExceptionTypes.distance(IllegalStateException.class, Throwable.class)).isEqualTo(3);
	}

	@Test
	void distance_isNegativeWhenTheTypeDoesNotMatch() {
		assertThat(ExceptionTypes.distance(IllegalStateException.class, IOException.class)).isEqualTo(-1);
		assertThat(ExceptionTypes.distance(Exception.class, RuntimeException.class)).isEqualTo(-1);    // a subclass does not match
	}

	@Test
	void distance_interfacesCountToo() {
		assertThat(ExceptionTypes.distance(MarkedException.class, Marker.class)).isEqualTo(1);
		assertThat(ExceptionTypes.distance(MarkedException.class, IllegalStateException.class)).isEqualTo(1);
		assertThat(ExceptionTypes.distance(MarkedException.class, Serializable.class)).isPositive();
	}

	@Test
	void distance_unrelatedSiblingsDoNotMatch() {
		assertThat(ExceptionTypes.distance(UncheckedIOException.class, IOException.class)).isEqualTo(-1);
	}
}
