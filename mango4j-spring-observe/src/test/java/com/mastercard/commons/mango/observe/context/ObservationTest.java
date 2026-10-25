package com.mastercard.commons.mango.observe.context;

import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.collections.ListBuilder;
import com.mastercard.commons.mango.collections.MapBuilder;
import com.mastercard.commons.mango.observe.annotations.ObservationTags;
import com.mastercard.commons.mango.observe.annotations.Observe;
import com.mastercard.commons.mango.observe.factory.ObservationFactory;
import com.mastercard.commons.mango.observe.initialisers.DefaultObservationInitializer;
import com.mastercard.commons.mango.observe.initialisers.ObservationInitializer;
import com.mastercard.commons.mango.reflection.utils.test.MDCTestUtils;
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