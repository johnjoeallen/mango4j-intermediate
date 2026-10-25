package ie.bitstep.mango.spring.observe.factory;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.collections.ListBuilder;
import ie.bitstep.mango.spring.observe.annotations.Observe;
import ie.bitstep.mango.spring.observe.context.Observation;
import ie.bitstep.mango.spring.observe.context.ObservationContext;
import ie.bitstep.mango.spring.observe.context.ObservationContextThreadLocal;
import ie.bitstep.mango.spring.observe.context.WebContext;
import ie.bitstep.mango.spring.observe.initialisers.DefaultObservationInitializer;
import ie.bitstep.mango.spring.observe.initialisers.ObservationInitializer;
import ie.bitstep.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static ie.bitstep.mango.spring.observe.initialisers.DefaultObservationInitializer.CORRELATION_ID;
import static ie.bitstep.mango.spring.observe.initialisers.DefaultObservationInitializer.DEFAULT_SERVICE_ID;
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