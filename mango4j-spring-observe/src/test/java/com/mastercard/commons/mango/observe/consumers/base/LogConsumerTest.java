package com.mastercard.commons.mango.observe.consumers.base;

import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.collections.MapBuilder;
import com.mastercard.commons.mango.observe.context.Observation;
import net.logstash.logback.argument.StructuredArgument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogConsumerTest {
	private final LogConsumer logConsumer = new LogConsumer();

	@Mock
	private Logger logger;

	@Mock
	private Observation observation;

	@Mock
	private MethodInfo methodInfo;

	@Test
	void getLogger() {
		// GIVEN
		when(observation.methodInfo()).thenReturn(methodInfo);
		when(methodInfo.logger()).thenReturn(logger);

		// WHEN

		// THEN
		assertThat(logConsumer.getLogger(observation)).isEqualTo(logger);
	}

	@Test
	void getLoggerObservationNull() {
		// GIVEN
		// WHEN

		// THEN
		assertThat(logConsumer.getLogger(null)).isNotEqualTo(logger);
	}

	@Test
	void getLoggerMethodInfoIsNull() {
		// GIVEN
		when(observation.methodInfo()).thenReturn(null);

		// WHEN

		// THEN
		assertThat(logConsumer.getLogger(observation)).isNotEqualTo(logger);
	}

	@Test
	void accept() {
		// GIVEN
		Map<String, Object> data = MapBuilder.<String, Object>map().build();
		Log log = new Log(observation, data);

		when(observation.methodInfo()).thenReturn(methodInfo);
		when(methodInfo.logger()).thenReturn(logger);

		// WHEN
		logConsumer.accept(log);

		// THEN
		verify(logger, times(1)).info(isNull(), any(StructuredArgument.class));
	}

	@Test
	void acceptThrowable() {
		// GIVEN
		Map<String, Object> data = MapBuilder.<String, Object>map().build();
		Log log = new Log(observation, data);

		when(observation.methodInfo()).thenReturn(methodInfo);
		when(observation.throwable()).thenReturn(new RuntimeException());
		when(methodInfo.logger()).thenReturn(logger);

		// WHEN
		logConsumer.accept(log);

		// THEN
		verify(logger, times(1)).error(isNull(), any(StructuredArgument.class));
	}
}