package ie.bitstep.mango.spring.observe.context;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.test.ListBuilder;
import ie.bitstep.mango.spring.logging.test.MapBuilder;
import ie.bitstep.mango.spring.observe.annotations.ObservationTags;
import ie.bitstep.mango.spring.observe.annotations.Observe;
import ie.bitstep.mango.spring.observe.factory.ObservationFactory;
import ie.bitstep.mango.spring.observe.initialisers.DefaultObservationInitializer;
import ie.bitstep.mango.spring.observe.initialisers.ObservationInitializer;
import ie.bitstep.mango.spring.logging.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ObservationTest {
	private ObservationFactory observationFactory;

	@Observe(name = "ObservationTestDummy")
	@ObservationTags(base = {"first", "last"})
	public void dummyMethod() {

	}

	@BeforeEach
	void setup() {
		MDCTestUtils.init();
		ObservationContextThreadLocal.reset();

		observationFactory = new ObservationFactory(
			ListBuilder.<ObservationInitializer>list().add(new DefaultObservationInitializer()).build()
		);
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void tag() throws NoSuchMethodException {
		// GIVEN
		MethodInfo methodInfo = new MethodInfo(this.getClass(), this.getClass().getMethod("dummyMethod"));
		Map<String, Object> expected = MapBuilder.<String, Object>map()
			.with("first", "John")
			.with("last", "Allen")
			.build();

		// WHEN
		Observation observation = observationFactory.create(methodInfo);

		observation.tag("first", "John").tag("last", "Allen");

		// THEN
		assertThat(observation.tags()).isEqualTo(expected);
	}

	@Test
	void putValue() throws NoSuchMethodException {
		// GIVEN
		MethodInfo methodInfo = new MethodInfo(this.getClass(), this.getClass().getMethod("dummyMethod"));
		Map<String, Object> expected = MapBuilder.<String, Object>map()
			.with("first", "John")
			.with("last", "Allen")
			.build();

		// WHEN
		Observation observation = observationFactory.create(methodInfo);

		observation.putValue("first", "John").putValue("last", "Allen");

		// THEN
		assertThat(observation.values()).isEqualTo(expected);
	}

	@Test
	void addValue() throws NoSuchMethodException {
		// GIVEN
		MethodInfo methodInfo = new MethodInfo(this.getClass(), this.getClass().getMethod("dummyMethod"));
		Map<String, Object> expected = MapBuilder.<String, Object>map()
			.with("first", List.of("John", "Dave"))
			.with("last", List.of("Allen", "Allen"))
			.build();

		// WHEN
		Observation observation = observationFactory.create(methodInfo);

		observation.addValue("first", "John").addValue("last", "Allen");
		observation.addValue("first", "Dave").addValue("last", "Allen");

		// THEN
		assertThat(observation.values()).isEqualTo(expected);
	}

	@Test
	void mapValue() throws NoSuchMethodException {
		// GIVEN
		MethodInfo methodInfo = new MethodInfo(this.getClass(), this.getClass().getMethod("dummyMethod"));
		Map<String, Object> expected = MapBuilder.<String, Object>map()
			.with("Profile",
				MapBuilder.<String, Object>map()
					.with("First", "John")
					.with("Last", "Allen")
					.build())
			.build();

		// WHEN
		Observation observation = observationFactory.create(methodInfo);

		observation
			.mapValue("Profile", "First", "John")
			.mapValue("Profile", "Last", "Allen");

		// THEN
		assertThat(observation.values()).isEqualTo(expected);
	}

	@Test
	void putThrowable() throws NoSuchMethodException {
		// GIVEN
		MethodInfo methodInfo = new MethodInfo(this.getClass(), this.getClass().getMethod("dummyMethod"));
		Exception re1 = new RuntimeException();
		Exception re2 = new RuntimeException();

		// WHEN
		Observation observation = observationFactory.create(methodInfo);

		observation.putThrowable(re1).putThrowable(re2);

		// THEN
		assertThat(observation.getThrowable()).isEqualTo(re2);
	}
}