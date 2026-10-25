package com.mastercard.commons.mango.observe.observers.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.collections.ListBuilder;
import com.mastercard.commons.mango.collections.MapBuilder;
import com.mastercard.commons.mango.observe.annotations.ObservationTags;
import com.mastercard.commons.mango.observe.consumers.base.Telemetry;
import com.mastercard.commons.mango.observe.consumers.base.TelemetryLogConsumer;
import com.mastercard.commons.mango.observe.context.Observation;
import com.mastercard.commons.mango.observe.context.ObservationContextThreadLocal;
import com.mastercard.commons.mango.observe.context.WebContext;
import com.mastercard.commons.mango.observe.initialisers.DefaultObservationInitializer;
import com.mastercard.commons.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.mastercard.commons.mango.observe.initialisers.DefaultObservationInitializer.CORRELATION_ID;
import static com.mastercard.commons.mango.observe.observers.base.TelemetryObserver.MAX_ELAPSED_TIME;
import static com.mastercard.commons.mango.observe.observers.base.TelemetryObserver.RTT_RANGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryObserverTest {
	public static final String CORRELATION_ID_VALUE = "correlationId";

	private TelemetryObserver telemetryObserver;

	@Mock
	private TelemetryLogConsumer telemetryLogConsumer;

	@Captor
	private ArgumentCaptor<List<Telemetry>> listTelemetryCaptor;

	@Spy
	private Observation observation = new Observation();

	@Mock
	private MethodInfo methodInfo;

	@Mock
	private ObservationTags observationTags;

	@Mock
	private WebContext webContext;

	@InjectMocks
	DefaultObservationInitializer defaultObservationInitializer;

	@BeforeEach
	void setUp() {
		MDCTestUtils.init();
		telemetryObserver = new TelemetryObserver(
			ListBuilder.<Consumer<List<Telemetry>>>list().add(telemetryLogConsumer).build()
		);
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void getObjectMapper() {
		ObjectMapper om = TelemetryObserver.getObjectMapper();

		assertThat(om).isNotNull();
		assertThat(om.isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS)).isTrue();
		assertThat(om.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
	}

	@Test
	void withinRange() {
		// GIVEN

		// WHEN

		// THEN
		assertThat(TelemetryObserver.withinRange(1000, 1000)).isFalse();
		assertThat(TelemetryObserver.withinRange(1000, 2048)).isTrue();
		assertThat(TelemetryObserver.withinRange(2049, 2048)).isFalse();
	}

	@Test
	void lessThanMax() {
		// GIVEN

		// WHEN

		// THEN
		assertThat(TelemetryObserver.lessThanMax(1000)).isTrue();
		assertThat(TelemetryObserver.lessThanMax(MAX_ELAPSED_TIME - 1)).isTrue();
		assertThat(TelemetryObserver.lessThanMax(MAX_ELAPSED_TIME)).isFalse();
	}

	@Test
	void onEntry() {
		telemetryObserver.onEntry(observation);
		assertThat(observation).isNotNull(); // Avoid sonar - nothing to test, no changes on entry
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 100, 1000, 5000, 6000})
	void onExit(long rtt) {
		// GIVEN
		String[] baseTags = {"a", "b", "c", RTT_RANGE, "service.id"};
		String[] permutationTags = {"a", "b"};
		Map<String, String> tags;

		tags = MapBuilder.<String, String>map()
			.with("a", "Hello")
			.with("b", "Goodbye")
			.with("c", "Dolly")
			.build();

		// WHEN
		when(observation.methodInfo()).thenReturn(methodInfo);
		when(methodInfo.getDeclaredAnnotation(ObservationTags.class)).thenReturn(observationTags);
		when(observationTags.base()).thenReturn(baseTags);
		when(observationTags.permutations()).thenReturn(permutationTags);
		when(observation.tags()).thenReturn(tags);
		when(observation.roundTripTime()).thenReturn(rtt);
		when(webContext.getHeader(CORRELATION_ID)).thenReturn(CORRELATION_ID_VALUE);

		ObservationContextThreadLocal.get().setWebContext(webContext);

		defaultObservationInitializer.initialise(observation);
		telemetryObserver.onExit(observation);

		tags = MapBuilder.<String, String>map()
			.with("a", "Hello")
			.with("b", "Goodbye")
			.with("c", "Dolly")
			.with(RTT_RANGE, "")
			.build();

		// WHEN
		when(observation.methodInfo()).thenReturn(methodInfo);
		when(methodInfo.getDeclaredAnnotation(ObservationTags.class)).thenReturn(observationTags);
		when(observationTags.base()).thenReturn(baseTags);
		when(observationTags.permutations()).thenReturn(permutationTags);
		when(observation.tags()).thenReturn(tags);
		when(observation.roundTripTime()).thenReturn(rtt);

		defaultObservationInitializer.initialise(observation);
		telemetryObserver.onExit(observation);

		// THEN
		verify(telemetryLogConsumer, times(2)).accept(any(List.class));
		verify(telemetryLogConsumer, times(2)).accept(listTelemetryCaptor.capture());

		assertThat(listTelemetryCaptor.getValue()).hasSize(11);

		listTelemetryCaptor.getValue().forEach(telemetry -> {
			assertThat(telemetry.observation()).isEqualTo(observation);
		});
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 100, 1000, 5000, 6000})
	void onExitNoPermutations(long rtt) {
		// GIVEN
		String[] baseTags = {"a", "b", "c", RTT_RANGE, "service.id"};
		String[] permutationTags = {};
		Map<String, String> tags;

		tags = MapBuilder.<String, String>map()
			.with("a", "Hello")
			.with("b", "Goodbye")
			.with("c", "Dolly")
			.build();

		// WHEN
		when(observation.methodInfo()).thenReturn(methodInfo);
		when(methodInfo.getDeclaredAnnotation(ObservationTags.class)).thenReturn(observationTags);
		when(observationTags.base()).thenReturn(baseTags);
		when(observationTags.permutations()).thenReturn(permutationTags);
		when(observation.tags()).thenReturn(tags);
		when(observation.roundTripTime()).thenReturn(rtt);
		when(webContext.getHeader(CORRELATION_ID)).thenReturn(CORRELATION_ID_VALUE);

		ObservationContextThreadLocal.get().setWebContext(webContext);

		defaultObservationInitializer.initialise(observation);
		telemetryObserver.onExit(observation);

		tags = MapBuilder.<String, String>map()
			.with("a", "Hello")
			.with("b", "Goodbye")
			.with("c", "Dolly")
			.with(RTT_RANGE, "")
			.build();

		// WHEN
		when(observation.methodInfo()).thenReturn(methodInfo);
		when(methodInfo.getDeclaredAnnotation(ObservationTags.class)).thenReturn(observationTags);
		when(observationTags.base()).thenReturn(baseTags);
		when(observationTags.permutations()).thenReturn(permutationTags);
		when(observation.tags()).thenReturn(tags);
		when(observation.roundTripTime()).thenReturn(rtt);

		defaultObservationInitializer.initialise(observation);
		telemetryObserver.onExit(observation);

		// THEN
		verify(telemetryLogConsumer, times(2)).accept(any(List.class));
		verify(telemetryLogConsumer, times(2)).accept(listTelemetryCaptor.capture());

		assertThat(listTelemetryCaptor.getValue()).hasSize(1);

		listTelemetryCaptor.getValue().forEach(telemetry -> {
			assertThat(telemetry.observation()).isEqualTo(observation);
		});
	}

	@Test
	void buildPermutedTags() {
		// GIVEN
		Set<String> toBlank = new HashSet<>();
		Map<String, String> tags = MapBuilder.<String, String>map()
			.with("partner_id", "2345234")
			.with("fi_id", "2346456")
			.with("customer_id", "0076120934875")
			.build();
		Map<String, String> expected = MapBuilder.<String, String>map()
			.with("partner_id", "")
			.with("fi_id", "2346456")
			.with("customer_id", "0076120934875")
			.build();

		toBlank.add("partner_id");

		// WHEN
		Map<String, String> permutedTags = TelemetryObserver.buildPermutedTags(toBlank, tags);

		// THEN
		assertThat(permutedTags).isEqualTo(expected);
	}

	@Test
	void buildTagsWithSegmentedExecutionTime() {
		// GIVEN
		Map<String, String> tags = MapBuilder.<String, String>map()
			.with(RTT_RANGE, "")
			.build();

		// WHEN
		when(observation.roundTripTime()).thenReturn(3000L);
		when(observation.tags()).thenReturn(tags);
		Map<String, String> result = TelemetryObserver.buildTagsWithSegmentedExecutionTime(observation);

		// THEN
		assertThat(result).containsEntry(RTT_RANGE, "2048-4096");
	}

	@Test
	void buildTagsWithSegmentedExecutionTimeNoRTTRange() {
		// GIVEN
		Map<String, String> tags = MapBuilder.<String, String>map()
			.with("name", "discovery")
			.build();

		// WHEN
		when(observation.tags()).thenReturn(tags);
		Map<String, String> result = TelemetryObserver.buildTagsWithSegmentedExecutionTime(observation);

		// THEN
		assertThat(result).isEqualTo(tags);
	}
}
