package com.mastercard.commons.mango.observe.factory;

import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.collections.ListBuilder;
import com.mastercard.commons.mango.observe.annotations.Observe;
import com.mastercard.commons.mango.observe.context.Observation;
import com.mastercard.commons.mango.observe.context.ObservationContext;
import com.mastercard.commons.mango.observe.context.ObservationContextThreadLocal;
import com.mastercard.commons.mango.observe.context.WebContext;
import com.mastercard.commons.mango.observe.initialisers.DefaultObservationInitializer;
import com.mastercard.commons.mango.observe.initialisers.ObservationInitializer;
import com.mastercard.commons.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.mastercard.commons.mango.observe.initialisers.DefaultObservationInitializer.CORRELATION_ID;
import static com.mastercard.commons.mango.observe.initialisers.DefaultObservationInitializer.DEFAULT_SERVICE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservationFactoryTest {
	@Mock
	private WebContext webContext;
	private ObservationFactory observationFactory;

	class ExceptionObservationInitializer implements ObservationInitializer {
		@Override
		public void initialise(Observation observation) {
			throw new RuntimeException();
		}
	}

	@Observe(name = "ObservationFactoryTestDummy")
	public void dummy() {

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
	public void create() throws NoSuchMethodException { // NOSONAR
		// GIVEN
		Observation observation;
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		MethodInfo methodInfo = new MethodInfo(this.getClass(), this.getClass().getMethod("dummy"));
		observationFactory = new ObservationFactory(
			ListBuilder.<ObservationInitializer>list().add(new DefaultObservationInitializer()).build()
		);

		// WHEN
		observation = observationFactory.create(methodInfo);

		// THEN
		assertThat(observation.serviceId()).isEqualTo(DEFAULT_SERVICE_ID);

		// WHEN
		observationContext.setWebContext(webContext);
		when(webContext.getHeader(CORRELATION_ID)).thenReturn(CORRELATION_ID);
		observation = observationFactory.create(methodInfo);

		// THEN
		assertThat(observation.serviceId()).isEqualTo(DEFAULT_SERVICE_ID);
		assertThat(observation.correlationid()).isEqualTo(CORRELATION_ID);
	}

	@Test
	public void createException() throws NoSuchMethodException { // NOSONAR
		// GIVEN
		Observation observation;
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		observationFactory = new ObservationFactory(
			ListBuilder.<ObservationInitializer>list().add(new ExceptionObservationInitializer()).build()
		);
		MethodInfo methodInfo = new MethodInfo(this.getClass(), this.getClass().getMethod("dummy"));

		// WHEN
		observation = observationFactory.create(methodInfo);
		assertThat(observation.correlationid()).isNull();
	}

	@Test
	public void createNoInitialisers() throws NoSuchMethodException { // NOSONAR
		// GIVEN
		Observation observation;
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		observationFactory = new ObservationFactory(List.of());
		MethodInfo methodInfo = new MethodInfo(this.getClass(), this.getClass().getMethod("dummy"));

		// WHEN
		observation = observationFactory.create(methodInfo);
		assertThat(observation.correlationid()).isNull();
	}

	@Test
	public void createNillInitialisers() throws NoSuchMethodException { // NOSONAR
		// GIVEN
		Observation observation;
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		observationFactory = new ObservationFactory(null);
		MethodInfo methodInfo = new MethodInfo(this.getClass(), this.getClass().getMethod("dummy"));

		// WHEN
		observation = observationFactory.create(methodInfo);
		assertThat(observation.correlationid()).isNull();
	}
}