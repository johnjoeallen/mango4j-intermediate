package com.mastercard.commons.mango.observe.initialisers;

import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.collections.ListBuilder;
import com.mastercard.commons.mango.collections.MapBuilder;
import com.mastercard.commons.mango.observe.annotations.ObservationTags;
import com.mastercard.commons.mango.observe.annotations.Observe;
import com.mastercard.commons.mango.observe.context.Observation;
import com.mastercard.commons.mango.observe.context.ObservationContext;
import com.mastercard.commons.mango.observe.context.ObservationContextThreadLocal;
import com.mastercard.commons.mango.observe.context.WebContext;
import com.mastercard.commons.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.mastercard.commons.mango.observe.initialisers.DefaultObservationInitializer.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultObservationInitializerTest {

	public static final String CORRELATION_ID_VALUE = "correlationId";
	@Spy
	private Observation observation = new Observation();

	@Mock
	private MethodInfo methodInfo;

	@Mock
	private ObservationTags observationTags;

	@InjectMocks
	DefaultObservationInitializer defaultObservationInitializer;

	@Mock
	private WebContext webContext;

	@Observe(name = "DefaultObservationInitializerTestDummy")
	@ObservationTags(base = {"first", "last"})
	public void dummyMethod() {
		// NOSONAR: dummy method for testing annotations
	}

	@BeforeEach
	void setup() {
		MDCTestUtils.init();
		ObservationContextThreadLocal.reset();
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void initialise() {
		// GIVEN
		String[] baseTags = {"a", "b", "c", "service.id"};
		String[] permutationTags = {"a", "b"};

		Map<String, String> expectedTags = MapBuilder.<String, String>map()
			.with("a", "")
			.with("b", "")
			.with("c", "")
			.with("service.id", DefaultObservationInitializer.DEFAULT_SERVICE_ID)
			.build();

		when(observation.methodInfo()).thenReturn(methodInfo);
		when(methodInfo.getDeclaredAnnotation(ObservationTags.class)).thenReturn(observationTags);
		when(observationTags.base()).thenReturn(baseTags);
		when(observationTags.permutations()).thenReturn(permutationTags);

		// WHEN
		defaultObservationInitializer.initialise(observation);

		// THEN
		assertThat(observation.serviceId()).isEqualTo(DefaultObservationInitializer.DEFAULT_SERVICE_ID);
		assertThat(observation.tags()).isEqualTo(expectedTags);
		assertThat(observation.permutations()).isEqualTo(ListBuilder.<String>list().add(permutationTags).build());
		assertThat(observation.correlationid()).isNotEqualTo(CORRELATION_ID_VALUE);
	}

	@Test
	void initCorrelationId() {
		// GIVEN
		String[] baseTags = {"a", "b", "c", "service.id"};
		String[] permutationTags = {"a", "b"};

		Map<String, String> expectedTags = MapBuilder.<String, String>map()
			.with("a", "")
			.with("b", "")
			.with("c", "")
			.with("service.id", DefaultObservationInitializer.DEFAULT_SERVICE_ID)
			.build();

		when(observation.methodInfo()).thenReturn(methodInfo);
		when(methodInfo.getDeclaredAnnotation(ObservationTags.class)).thenReturn(observationTags);
		when(observationTags.base()).thenReturn(baseTags);
		when(observationTags.permutations()).thenReturn(permutationTags);
		when(webContext.getHeader(CORRELATION_ID)).thenReturn(CORRELATION_ID_VALUE);
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		observationContext.setWebContext(webContext);

		// WHEN
		defaultObservationInitializer.initialise(observation);

		// THEN
		assertThat(observation.serviceId()).isEqualTo(DefaultObservationInitializer.DEFAULT_SERVICE_ID);
		assertThat(observation.tags()).isEqualTo(expectedTags);
		assertThat(observation.permutations()).isEqualTo(ListBuilder.<String>list().add(permutationTags).build());
		assertThat(observation.correlationid()).isEqualTo(CORRELATION_ID_VALUE);
	}
}