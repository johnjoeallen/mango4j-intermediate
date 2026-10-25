package ie.bitstep.mango.spring.logging.internal;

import tools.jackson.databind.ObjectMapper;
import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.test.ListBuilder;
import ie.bitstep.mango.spring.logging.test.MapBuilder;
import ie.bitstep.mango.spring.logging.LogUtils;
import ie.bitstep.mango.spring.logging.annotations.LoggableClass;
import ie.bitstep.mango.spring.logging.annotations.LoggableField;
import ie.bitstep.mango.spring.logging.annotations.LoggableView;
import ie.bitstep.mango.utils.converters.CollectionToSize;
import ie.bitstep.mango.utils.converters.ConverterFactory;
import ie.bitstep.mango.utils.masking.FullMasker;
import ie.bitstep.mango.utils.masking.PanMasker;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


class DiagnosticContextItemTest {

	private ObjectMapper objectMapper = new ObjectMapper();

	@LoggableClass
	public static class TestLoggable {
		public TestLoggable(String name) {
			this.name = name;
		}

		@LoggableField(name = "lname")    // to make it visible when the conversion was applied
		private String name;
	}

	@LoggableClass
	public static class TestLoggableException extends Exception {
		public TestLoggableException() {
			// Dummy class for testing
		}

		@LoggableField(name = "name")    // to make it visible when the conversion was applied
		private String name = "BadThingHappened";
	}

	@Test
	public void testContextItemHasParent() throws Exception { // NOSONAR - required public as used within test
		// given
		Method thisMethod = getClass().getMethod("testContextItemHasParent");

		DiagnosticContextItem parent = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);
		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), parent);

		assertThat(item.hasParent()).isTrue();
	}

	@Test
	public void testContextItemExceptionHasNoMessage() throws Exception { // NOSONAR - required public as used within test
		// given
		Method thisMethod = getClass().getMethod("testContextItemExceptionHasNoMessage");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);
		Throwable ex = new RuntimeException("");
		item.putThrowable(ex);

		// when
		String actual = objectMapper.writeValueAsString(item.toLoggable());

		// then
		String jsonStackTrace = objectMapper.writeValueAsString(LogUtils.getStackTrace(ex));
		assertThat(actual).isEqualTo(
			"{\"method\":\"DiagnosticContextItemTest.testContextItemExceptionHasNoMessage\"," +
				"\"stacktrace\":" + jsonStackTrace + "}"
		);
	}

	@Test
	public void testContextItemExceptionIsLoggableClass() throws Exception { // NOSONAR - required public as used within test
		// given
		Method thisMethod = getClass().getMethod("testContextItemExceptionIsLoggableClass");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);
		Throwable ex = new TestLoggableException();
		item.putThrowable(ex);

		// when
		String actual = objectMapper.writeValueAsString(item.toLoggable());

		// then
		String jsonStackTrace = objectMapper.writeValueAsString(LogUtils.getStackTrace(ex));
		assertThat(actual).isEqualTo(
			"{\"method\":\"DiagnosticContextItemTest.testContextItemExceptionIsLoggableClass\",\"reason\":{\"ie.bitstep.mango.spring.logging.internal.DiagnosticContextItemTest$TestLoggableException\":{\"name\":\"BadThingHappened\"}}," +
				"\"stacktrace\":" + jsonStackTrace + "}"
		);
	}

	@Test
	public void toLoggable_whenEmpty() throws Exception { // NOSONAR - required public as used within test
		// given
		Method thisMethod = getClass().getMethod("toLoggable_whenEmpty");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);

		// when
		Map<String, Object> result = item.toLoggable();

		// then
		assertThat(objectMapper.writeValueAsString(result)).isEqualTo(
			"{\"method\":\"DiagnosticContextItemTest.toLoggable_whenEmpty\"}");
	}

	@Test
	public void toLoggable_whenFull() throws Exception { // NOSONAR - required public as used within test
		// given
		Method thisMethod = getClass().getMethod("toLoggable_whenFull");
		Method childMethod = getClass().getMethod("toLoggable_whenEmpty");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);
		item.putValue(DiagnosticContextItem.KEY_PARAMS, MapBuilder.<String, LoggableValue>map()
			.with(
				"p1", new LoggableValue("v1", null, null, 0, LoggableView.ALL))
			.with(
				"p2", new LoggableValue("v2", null, null, 0, LoggableView.ALL))
			.build());
		item.putResultValue(DiagnosticContextItem.KEY_RESPONSE_TIME, 230);
		Throwable ex = new RuntimeException("failed");
		item.putThrowable(ex);
		item.putResultValue(DiagnosticContextItem.KEY_RESPONSE, new TestLoggable("result9"));

		DiagnosticContextItem childItem = new DiagnosticContextItem(new MethodInfo(getClass(), childMethod), null);
		item.addChild(childItem);

		// when
		Map<String, Object> actual = item.toLoggable();

		// then
		String jsonStackTrace = objectMapper.writeValueAsString(LogUtils.getStackTrace(ex));
		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"method\":\"DiagnosticContextItemTest.toLoggable_whenFull\"," +
				"\"params\":{\"p1\":\"v1\",\"p2\":\"v2\"}," +
				"\"called\":[{\"method\":\"DiagnosticContextItemTest.toLoggable_whenEmpty\"}]," +
				"\"reason\":\"failed\"," +
				"\"stacktrace\":" + jsonStackTrace + "," +
				"\"responsetime\":230," +
				"\"response\":{\"lname\":\"result9\"}}"
		);
	}

	@Test
	void putValueTest() throws Exception {
		// given
		Method thisMethod = getClass().getMethod("toLoggable_whenMasking");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);

		// when
		DiagnosticContextItem response = item.putValue(DiagnosticContextItem.KEY_PARAMS, MapBuilder.<String, LoggableValue>map()
			.with(
				"p1", new LoggableValue("5432111122223333", new PanMasker(), null, 0, LoggableView.ALL))
			.build());

		// then
		assertThat(response).isEqualTo(item);
	}

	@Test
	void addValueTest() throws Exception {
		// given
		Method thisMethod = getClass().getMethod("toLoggable_whenMasking");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);

		// when
		DiagnosticContextItem response = item.addValue("valueKey", "value");

		// then
		assertThat(response).isEqualTo(item);
	}

	@Test
	void mapValueTest() throws Exception {
		// given
		Method thisMethod = getClass().getMethod("toLoggable_whenMasking");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);

		// when
		DiagnosticContextItem response = item.mapValue("key", "name", "value");

		// then
		assertThat(response).isEqualTo(item);
	}

	@Test
	void putResultValueTest() throws Exception {
		// given
		Method thisMethod = getClass().getMethod("toLoggable_whenMasking");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);

		// when
		DiagnosticContextItem response = item.putResultValue("key", "value");

		// then
		assertThat(response).isEqualTo(item);
	}

	@Test
	void putThrowableTest() throws Exception {
		// given
		Method thisMethod = getClass().getMethod("toLoggable_whenMasking");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);

		// when
		DiagnosticContextItem response = item.putThrowable(new Exception());

		// then
		assertThat(response).isEqualTo(item);
	}

	@Test
	void setWithExternalExceptionHandlerTest() throws Exception {
		// given
		Method thisMethod = getClass().getMethod("toLoggable_whenMasking");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);

		// when
		DiagnosticContextItem response = item.setWithExternalExceptionHandler(true);

		// then
		assertThat(response).isEqualTo(item);
	}

	@Test
	public void toLoggable_whenMasking() throws Exception { // NOSONAR - required public as used within test
		// given
		Method thisMethod = getClass().getMethod("toLoggable_whenMasking");

		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);
		DiagnosticContextItem response = item.putValue(DiagnosticContextItem.KEY_PARAMS, MapBuilder.<String, LoggableValue>map()
			.with(
				"p1",
				new LoggableValue("5432111122223333",
					new PanMasker(),
					null,
					0,
					LoggableView.ALL)
			)
			.with(
				"p2",
				new LoggableValue(Long.valueOf("5432111122223333"),
					new PanMasker(),
					null,
					0,
					LoggableView.ALL)
			)
			.with(
				"p3",
				new LoggableValue(MapBuilder.<String, String>map()
					.with("pass", "secret")
					.build(),
					new FullMasker(),
					null,
					0,
					LoggableView.ALL)
			)
			.with(
				"p4",
				new LoggableValue(MapBuilder.<String, Object>map()
					.with("pass", MapBuilder.<String, String>map()
						.with("pass", "secret")
						.build())
					.build(),
					new FullMasker(),
					null,
					0,
					LoggableView.ALL)
			)
			.with(
				"p5",
				new LoggableValue(MapBuilder.<String, Object>map()
					.with("test",
						MapBuilder.<String, Object>map()
							.with("loggable", new TestLoggable("discovery"))
							.build())
					.build(),
					new FullMasker(),
					null,
					0,
					LoggableView.ALL)
			)
			.with(
				"p6",
				new LoggableValue(MapBuilder.<String, Object>map()
					.with("test",
						ListBuilder.<String>list()
							.add("hello")
							.add("dolly")
							.build())
					.build(),
					new FullMasker(),
					null,
					0,
					LoggableView.ALL)
			)
			.with(
				"p7",
				new LoggableValue(ListBuilder.<String>list()
					.add("hello")
					.add("dolly")
					.build(),
					null,
					ConverterFactory.getConverter(CollectionToSize.class),
					0,
					LoggableView.ALL)
			)
			.build());

		// when
		Map<String, Object> actual = item.toLoggable();

		// then
		assertThat(response).isNotNull();
		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"method\":\"DiagnosticContextItemTest.toLoggable_whenMasking\",\"params\":{" +
				"\"p1\":\"543211XXXXXX3333\"," +
				"\"p2\":\"543211XXXXXX3333\"," +
				"\"p3\":{\"pass\":\"***\"}," +
				"\"p4\":{\"pass\":{\"pass\":\"***\"}}," +
				"\"p5\":{\"test\":{\"loggable\":{\"lname\":\"discovery\"}}}," +
				"\"p6\":{\"test\":[\"***\",\"***\"]},\"p7\":2}}"
		);
	}

	@Test
	void setAndGetFinalMethodInfo() {
		DiagnosticContextItem item = new DiagnosticContextItem(null, null);
		assertThat(item.isFinalMethodInfo()).isTrue();

		item.setFinalMethodInfo(false);
		assertThat(item.isFinalMethodInfo()).isFalse();
	}

	@Test
	void setAndGetWithExternalExceptionHandler() {
		DiagnosticContextItem item = new DiagnosticContextItem(null, null);
		assertThat(item.isWithExternalExceptionHandler()).isFalse();

		item.setWithExternalExceptionHandler(true);
		assertThat(item.isWithExternalExceptionHandler()).isTrue();
	}

	@Test
	public void toLoggable_thenImmutable() throws Exception { // NOSONAR - required public as used within test
		// given
		Method thisMethod = getClass().getMethod("toLoggable_thenImmutable");
		DiagnosticContextItem item = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);

		TestLoggable testLoggable = new TestLoggable("nameBefore");

		item.putValue(DiagnosticContextItem.KEY_PARAMS, MapBuilder.<String, LoggableValue>map()
			.with(
				"p1", new LoggableValue(testLoggable, null, null, 0, LoggableView.ALL))
			.build());
		item.putResultValue(DiagnosticContextItem.KEY_RESPONSE, testLoggable);

		item.putValue("value", testLoggable);
		item.addValue("list", testLoggable);
		item.mapValue("map", "key", testLoggable);

		testLoggable.name = "nameAfter";

		// when
		Map<String, Object> actual = item.toLoggable();

		// then
		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"method\":\"DiagnosticContextItemTest.toLoggable_thenImmutable\"," +
				"\"params\":{\"p1\":{\"lname\":\"nameBefore\"}}," +
				"\"value\":{\"lname\":\"nameBefore\"}," +
				"\"list\":[{\"lname\":\"nameBefore\"}]," +
				"\"map\":{\"key\":{\"lname\":\"nameBefore\"}}," +
				"\"response\":{\"lname\":\"nameBefore\"}}");
	}

	@Test
	void addChild_withinLimit_doesNotLogError() throws Exception {
		// Save original value
		int originalMax = ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.getMaxChildren();

		try {
			// Set a limit
			ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.setMaxChildren(5);

			Method thisMethod = getClass().getDeclaredMethod("addChild_withinLimit_doesNotLogError");
			DiagnosticContextItem parent = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);

			// Add children within limit
			for (int i = 0; i < 4; i++) {
				DiagnosticContextItem child = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), parent);
				parent.addChild(child);
			}

			// Verify children were added
			assertThat(parent.getChildren()).hasSize(4);
		} finally {
			// Restore original value
			ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.setMaxChildren(originalMax);
		}
	}

	@Test
	void addChild_exceedsLimit_logsErrorButStillAdds() throws Exception {
		// Save original value
		int originalMax = ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.getMaxChildren();

		try {
			// Set a low limit for testing
			ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.setMaxChildren(3);

			Method thisMethod = getClass().getDeclaredMethod("addChild_exceedsLimit_logsErrorButStillAdds");
			DiagnosticContextItem parent = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), null);

			// Add children up to and beyond limit
			for (int i = 0; i < 5; i++) {
				DiagnosticContextItem child = new DiagnosticContextItem(new MethodInfo(getClass(), thisMethod), parent);
				parent.addChild(child);
			}

			// Verify all children were still added (we log error but don't prevent adding)
			assertThat(parent.getChildren()).hasSize(5);
		} finally {
			// Restore original value
			ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.setMaxChildren(originalMax);
		}
	}

	@Test
	void maxChildren_getterAndSetter() {
		// Save original value
		int originalMax = ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.getMaxChildren();

		try {
			// Test setter and getter
			ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.setMaxChildren(50);
			assertThat(ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.getMaxChildren()).isEqualTo(50);

			ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.setMaxChildren(200);
			assertThat(ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.getMaxChildren()).isEqualTo(200);
		} finally {
			// Restore original value
			ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.setMaxChildren(originalMax);
		}
	}

	@Test
	void maxChildren_setterValidation() {
		// Save original value
		int originalMax = ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.getMaxChildren();

		try {
			// Test that setting 0 or negative throws exception
			org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
				ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.setMaxChildren(0));

			org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
				ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.setMaxChildren(-1));

			// Verify original value is still intact
			assertThat(ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.getMaxChildren()).isEqualTo(originalMax);
		} finally {
			// Restore original value (in case it changed)
			ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger.setMaxChildren(originalMax);
		}
	}

}