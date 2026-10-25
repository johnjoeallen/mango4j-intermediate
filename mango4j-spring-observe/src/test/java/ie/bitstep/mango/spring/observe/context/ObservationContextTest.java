package ie.bitstep.mango.spring.observe.context;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.aop.ThrowableInfo;
import ie.bitstep.mango.collections.ListBuilder;
import ie.bitstep.mango.collections.MapBuilder;
import ie.bitstep.mango.spring.observe.annotations.ObservationTags;
import ie.bitstep.mango.spring.observe.annotations.Segregate;
import ie.bitstep.mango.spring.observe.initialisers.DefaultObservationInitializer;
import ie.bitstep.mango.reflection.utils.test.MDCTestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.REQUEST_ID_ATTRIBUTE;
import static ie.bitstep.mango.spring.observe.context.ObservationContext.KEY_REQUESTOR_ID;
import static ie.bitstep.mango.spring.observe.context.ObservationContext.LOG_KEY_CORRELATION_ID;
import static ie.bitstep.mango.spring.observe.initialisers.DefaultObservationInitializer.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservationContextTest {
	public static final String CORRELATION_ID_VALUE = "correlationId";

	@Spy
	private Observation observation = new Observation();

	@Mock
	private HttpServletRequest httpServletRequest;

	@Mock
	private WebContext webContext;

	@Mock
	private MethodInfo methodInfo;

	@InjectMocks
	DefaultObservationInitializer defaultObservationInitializer;

	@Mock
	private ObservationTags observationTags;

	@Segregate
	public void segregated() {

	}

	public void nonSegregated() {

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
	void getInstance() {
		// GIVEN

		// WHEN
		ObservationContext instance = ObservationContext.getInstance();

		// THEN
		assertThat(instance).isNotNull();
	}

	@Test
	void getCurrentObservation() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		instance.push(observation);

		// WHEN
		Observation currentObservation = ObservationContext.getCurrentObservation();

		// THEN

		assertThat(currentObservation).isEqualTo(observation);
	}

	@Test
	void push() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Observation parent = new Observation();
		Observation child = new Observation();
		instance.push(parent);
		instance.push(child);

		// WHEN

		// THEN
		assertThat(ObservationContext.getCurrentObservation()).isEqualTo(child);
	}

	@Test
	void popSegregated() throws NoSuchMethodException {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Observation parent = Observation.builder()
			.methodInfo(
				new MethodInfo(this.getClass(), this.getClass().getMethod("segregated"))
			)
			.build();
		Observation child = Observation.builder()
			.methodInfo(
				new MethodInfo(this.getClass(), this.getClass().getMethod("segregated"))
			)
			.build();
		instance.push(parent);
		instance.push(child);

		// WHEN

		// THEN
		assertThat(ObservationContext.getCurrentObservation()).isEqualTo(child);

		// WHEN
		ObservationContext.putThrowable(new RuntimeException());
		instance.pop();

		// THEN
		assertThat(ObservationContext.getCurrentObservation()).isEqualTo(parent);
		assertThat(ObservationContext.getCurrentObservation().getChildren()).isEmpty();
		assertThat(child.parent()).isNull();
		assertThat(instance.getStatValues()).isEmpty();
		assertThat(instance.getThrown()).isNotEmpty();

		// WHEN
		ObservationContext.putThrowable(new RuntimeException());
		instance.pop();

		// THEN
		assertThat(instance.getObservation()).isNull();
		assertThat(instance.getStatValues()).isEmpty();
		assertThat(instance.getThrown()).isEmpty();
	}

	@Test
	void popNonSegregated() throws NoSuchMethodException {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Observation parent = Observation.builder()
			.methodInfo(
				new MethodInfo(this.getClass(), this.getClass().getMethod("nonSegregated"))
			)
			.build();
		Observation child = Observation.builder()
			.methodInfo(
				new MethodInfo(this.getClass(), this.getClass().getMethod("nonSegregated"))
			)
			.build();
		instance.push(parent);
		instance.push(child);

		// WHEN

		// THEN
		assertThat(ObservationContext.getCurrentObservation()).isEqualTo(child);

		// WHEN
		ObservationContext.putThrowable(new RuntimeException());
		instance.pop();

		// THEN
		assertThat(ObservationContext.getCurrentObservation()).isEqualTo(parent);
		assertThat(ObservationContext.getCurrentObservation().getChildren()).contains(child);
		assertThat(child.parent()).isNotNull();
		assertThat(instance.getStatValues()).isEmpty();
		assertThat(instance.getThrown()).isNotEmpty();

		// WHEN
		ObservationContext.putThrowable(new RuntimeException());
		instance.pop();

		// THEN
		assertThat(instance.getObservation()).isNull();
		assertThat(instance.getStatValues()).isEmpty();
		assertThat(instance.getThrown()).isEmpty();
	}

	@Test
	void popNullObservation() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();

		// WHEN
		instance.pop();

		// THEN
		Assertions.assertDoesNotThrow(instance::pop, "pop() with no current observation should not throw an exception");
	}

	@Test
	void setWebContext() {
		// GIVEN
		WebContext webContext = new WebContext(httpServletRequest);
		ObservationContext instance = ObservationContext.getInstance();

		// WHEN
		instance.setWebContext(webContext);

		// THEN
		assertThat(ObservationContext.getCurrentWebcontext()).isEqualTo(webContext);
	}

	@Test
	void clear() throws NoSuchMethodException {
		// GIVEN
		String[] baseTags = {"a", "b", "c", "service.id"};
		String[] permutationTags = {"a", "b"};
		ObservationContext instance = ObservationContext.getInstance();
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		observationContext.setWebContext(webContext);
		instance.push(observation);

		// WHEN
		when(observation.methodInfo()).thenReturn(methodInfo);
		when(methodInfo.getDeclaredAnnotation(ObservationTags.class)).thenReturn(observationTags);
		when(observationTags.base()).thenReturn(baseTags);
		when(observationTags.permutations()).thenReturn(permutationTags);
		when(webContext.getHeader(CORRELATION_ID)).thenReturn(CORRELATION_ID_VALUE);

		defaultObservationInitializer.initialise(observation);

		ObservationContext.putStat("tps", 200);
		instance.putValue(ObservationContext.getTopLevelKeys().get(0), 201);

		// THEN
		assertThat(instance.getStatValues()).isNotEmpty();

		// WHEN
		Map<String, Object> topValuesPreClear = (Map<String, Object>) ReflectionTestUtils.getField(instance, "topValues");

		// THEN
		assertThat(topValuesPreClear).isNotEmpty();

		// WHEN
		instance.clear();

		// THEN
		assertThat(instance.getStatValues()).isEmpty();
		assertThat(instance.getThrown()).isEmpty();
		Map<String, Object> topValuesPostClear = (Map<String, Object>) ReflectionTestUtils.getField(instance, "topValues");
		assertThat(topValuesPostClear).isEmpty();
	}

	@Test
	void topLevelKeys() {
		// GIVEN
		List<String> topLevelKeysOriginal = new ArrayList<>(Arrays.asList(LOG_KEY_CORRELATION_ID, KEY_REQUESTOR_ID, REQUEST_ID_ATTRIBUTE));
		List<String> topLevelKeysModified = new ArrayList<>(Arrays.asList("ABC", "XYZ"));

		// WHEN

		// THEN
		assertThat(ObservationContext.getTopLevelKeys()).isEqualTo(topLevelKeysOriginal);

		// WHEN
		ObservationContext.setTopLevelKeys(topLevelKeysModified);

		// THEN
		assertThat(ObservationContext.getTopLevelKeys()).isEqualTo(topLevelKeysModified);
	}

	@Test
	void fresh() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		ObservationContext.putStat("tps", 200);
		instance.putValue(ObservationContext.getTopLevelKeys().get(0), 201);

		// WHEN
		ObservationContext fresh = instance.fresh();

		// THEN
		assertThat(fresh).isEqualTo(instance);
	}

	@Test
	void getRoot() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();

		// THEN
		assertThat(instance.getRoot()).isNull();

		// WHEN
		instance.push(observation);

		// THEN
		assertThat(instance.getRoot()).isNotNull();
	}

	@Test
	void getObservation() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();

		// THEN
		assertThat(instance.getObservation()).isNull();

		// WHEN
		instance.push(observation);

		// THEN
		assertThat(instance.getObservation()).isNotNull();
	}

	@Test
	void getStatValues() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Map<String, Object> expectedValues = Map.of("TPS", 200);

		// WHEN
		instance.clear();
		ObservationContext.putStat("TPS", 200);

		// THEN
		assertThat(instance.getStatValues()).isEqualTo(expectedValues);
		assertThat(ObservationContext.getStat("TPS")).isEqualTo(200);
	}

	@Test
	void getStatValue() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();

		// WHEN
		ObservationContext.putStat("TPS", 200);

		// THEN
		assertThat(ObservationContext.getStat("TPS")).isEqualTo(200);
	}

	@Test
	void put() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Observation observation = new Observation();
		instance.push(observation);

		// WHEN
		ObservationContext.put("key", "value");

		// THEN
		assertThat(ObservationContext.get("key")).isEqualTo("value");
	}

	@Test
	void putToplevelKey() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Observation observation = new Observation();
		instance.push(observation);

		// WHEN
		ObservationContext.put(ObservationContext.getTopLevelKeys().get(0), "value");

		// THEN
		assertThat(ObservationContext.get(ObservationContext.getTopLevelKeys().get(0))).isEqualTo("value");
	}

	@Test
	void putNullObservation() {
		// GIVEN

		// WHEN
		ObservationContext.put("key", "value");

		// THEN
		assertThat(ObservationContext.get("key")).isNull();
	}

	@Test
	void contains() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Observation observation = new Observation();
		instance.push(observation);

		// THEN
		assertThat(ObservationContext.contains("key")).isFalse();
		assertThat(ObservationContext.contains(ObservationContext.getTopLevelKeys().get(0))).isFalse();

		// WHEN
		ObservationContext.put("key", "value");
		ObservationContext.put(ObservationContext.getTopLevelKeys().get(0), "value");

		// THEN
		assertThat(ObservationContext.contains("key")).isTrue();
		assertThat(ObservationContext.contains(ObservationContext.getTopLevelKeys().get(0))).isTrue();
	}

	@Test
	void add() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		List expectedList = ListBuilder.list()
			.add(MapBuilder.<String, Object>map()
				.with("value1", "value2")
				.build())
			.add(MapBuilder.<String, Object>map()
				.with("value1", "value2")
				.build())
			.add("value1")
			.build();

		// THEN
		ObservationContext.add("key", "value1", "value2");
		ObservationContext.add("key", "value1");
		assertThat(ObservationContext.contains("key")).isFalse();

		// WHEN
		Observation observation = new Observation();
		instance.push(observation);
		ObservationContext.add("key", "value1", "value2");
		ObservationContext.add("key", "value1", "value2");
		ObservationContext.add("key", "value1");

		// THEN
		assertThat(ObservationContext.contains("key")).isTrue();
		assertThat(ObservationContext.get("key")).isInstanceOf(List.class);
		assertThat((List) ObservationContext.get("key")).isEqualTo(expectedList);
	}

	@Test
	void map() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Observation observation = new Observation();
		instance.push(observation);
		Map<String, Object> expectedMap = MapBuilder.<String, Object>map()
			.with("value1", "value2")
			.with("valueA", "valueZ")
			.with("valueB", "valueY")
			.build();

		// THEN
		assertThat(ObservationContext.contains("key")).isFalse();

		// WHEN
		ObservationContext.map("key", "value1", "value2");
		ObservationContext.map("key", "value1", "value2");
		// Send an odd number of key/value pairs
		ObservationContext.map("key", "valueA", "valueZ", "valueB", "valueY", "BAD....IGNORED");

		// THEN
		assertThat(ObservationContext.contains("key")).isTrue();
		assertThat(ObservationContext.get("key")).isInstanceOf(Map.class);
		assertThat((Map) ObservationContext.get("key")).isEqualTo(expectedMap);
	}

	@Test
	void mapNullObservation() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Map<String, Object> expectedMap = MapBuilder.<String, Object>map()
			.with("value1", "value2")
			.build();

		// THEN
		assertThat(ObservationContext.contains("key")).isFalse();

		// WHEN

		// THEN
		Assertions.assertDoesNotThrow(() -> ObservationContext.map("key", "value1", "value2"));
		Assertions.assertDoesNotThrow(() -> ObservationContext.map("key", "value1", "value2"));
	}

	@Test
	void getThrown() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Exception exception = new RuntimeException("error");

		// WHEN
		when(observation.methodInfo()).thenReturn(methodInfo);

		// THEN
		assertThat(instance.getThrown()).isEmpty();

		// WHEN
		ThrowableInfo expected = new ThrowableInfo(exception, observation.methodInfo());
		ObservationContext.putThrowable(exception);

		// THEN
		assertThat(instance.getThrown()).isEmpty();

		// WHEN
		instance.push(observation);
		ObservationContext.putThrowable(exception);

		// THEN
		assertThat(instance.getThrown()).hasSize(1);
		assertThat(instance.getThrown().get(0)).isEqualTo(expected);

		// WHEN
		instance.clear();

		// THEN
		assertThat(instance.getThrown()).isEmpty();
	}

	@Test
	void putThrowable() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		Exception exception = new RuntimeException("error");

		// WHEN
		ObservationContext.putThrowable(exception);

		// THEN
		assertThat(ObservationContext.getThrowable()).isNull();

		// WHEN
		Observation observation = new Observation();
		instance.push(observation);
		ObservationContext.putThrowable(exception);

		// THEN
		assertThat(ObservationContext.getThrowable()).isEqualTo(exception);

		// WHEN
		ObservationContext.putThrowable(exception);

		// THEN
		assertThat(ObservationContext.getThrowable()).isEqualTo(exception);
	}

	@Test
	void toplevelKeys() {
		// GIVEN
		ObservationContext instance = ObservationContext.getInstance();
		String key = ObservationContext.getTopLevelKeys().get(0);

		// WHEN
		instance.putValue(key, "Hello");

		// THEN
		assertThat(instance.getTopValues()).containsEntry(key, "Hello");
	}
}