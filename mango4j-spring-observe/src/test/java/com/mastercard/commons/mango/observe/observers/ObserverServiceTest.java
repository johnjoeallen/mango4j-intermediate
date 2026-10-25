package com.mastercard.commons.mango.observe.observers;

import com.mastercard.commons.mango.collections.ListBuilder;
import com.mastercard.commons.mango.observe.context.Observation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObserverServiceTest {
	private ObserverService observerService;

	@Mock
	private Logger logger;

	@Mock
	private Observation observation;

	@Mock
	private Observer observer;

	@BeforeEach
	void setUp() {
		observerService = new ObserverService(
			ListBuilder.<Observer>list().add(observer).build()
		);

		ReflectionTestUtils.setField(observerService, "logger", logger);
	}

	@Test
	void observeOnExit() {
		// GIVEN

		// WHEN
		observerService.observeOnExit(observation);

		// THEN
		verify(observer, times(1)).onExit(observation);
	}

	@Test
	void observeOnExitException() {
		// GIVEN

		// WHEN
		when(observation.label()).thenReturn("label");
		doThrow(new RuntimeException()).when(observer).onExit(observation);
		observerService.observeOnExit(observation);

		// THEN
		verify(observer, times(1)).onExit(observation);
		verify(logger, times(1)).error(anyString(), any(Exception.class));
	}

	@Test
	void observeOnEntry() {
		// GIVEN

		// WHEN
		observerService.observeOnEntry(observation);

		// THEN
		verify(observer, times(1)).onEntry(observation);
	}

	@Test
	void observeOnEntryException() {
		// GIVEN

		// WHEN
		when(observation.label()).thenReturn("label");
		doThrow(new RuntimeException()).when(observer).onEntry(observation);
		observerService.observeOnEntry(observation);

		// THEN
		verify(observer, times(1)).onEntry(observation);
	}
}
