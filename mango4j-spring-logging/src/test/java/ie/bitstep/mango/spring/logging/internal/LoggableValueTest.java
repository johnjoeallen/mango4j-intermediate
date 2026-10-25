package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.spring.logging.annotations.LoggableView;
import ie.bitstep.mango.utils.converters.CollectionToSize;
import ie.bitstep.mango.utils.masking.IdMasker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoggableValueTest {

	private static final Object TEST_VALUE = "TEST_VALUE";

	@Test
	void build_whenAll() {
		// when
		LoggableValue result = LoggableValue.builder()
			.value(TEST_VALUE)
			.masker(new IdMasker())
			.converter(new CollectionToSize())
			.truncateLen(15)
			.view(LoggableView.KEY)
			.build();

		// then
		assertThat(result.getValue()).isEqualTo(TEST_VALUE);
		assertThat(result.getMasker()).isInstanceOf(IdMasker.class);
		assertThat(result.getConverter()).isInstanceOf(CollectionToSize.class);
		assertThat(result.getTruncateLen()).isEqualTo(15);
		assertThat(result.getView()).isEqualTo(LoggableView.KEY);
	}

	@Test
	void build_whenNone() {
		// when
		LoggableValue result = LoggableValue.builder()
			.build();

		// then
		assertThat(result.getValue()).isNull();
		assertThat(result.getMasker()).isNull();
		assertThat(result.getConverter()).isNull();
		assertThat(result.getTruncateLen()).isZero();
		assertThat(result.getView()).isNull();
	}
}