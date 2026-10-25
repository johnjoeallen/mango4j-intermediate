package com.mastercard.commons.mango.observe.observers.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mastercard.commons.mango.collections.ListBuilder;
import com.mastercard.commons.mango.observe.consumers.base.Log;
import com.mastercard.commons.mango.observe.consumers.base.LogConsumer;
import com.mastercard.commons.mango.observe.context.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogObserverTest {
	@Mock
	private LogConsumer logConsumer;
	private LogObserver logObserver;

	@Mock
	private Observation observation;

	@BeforeEach
	void setup() {
		logObserver = new LogObserver(
			ListBuilder.<Consumer<Log>>list().add(logConsumer).build());
	}

	@Test
	void getObjectMapper() {
		ObjectMapper om = LogObserver.getObjectMapper();

		assertThat(om).isNotNull();
		assertThat(om.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isTrue();
		assertThat(om.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
	}

	@Test
	void onEntry() {
		logObserver.onEntry(observation);
		assertThat(observation).isNotNull(); // Avoid sonar - nothing to test, no changes on entry
	}

	@Test
	void onExit() {
		logObserver.onExit(observation);
		verify(logConsumer, times(1)).accept(any(Log.class));
	}
}