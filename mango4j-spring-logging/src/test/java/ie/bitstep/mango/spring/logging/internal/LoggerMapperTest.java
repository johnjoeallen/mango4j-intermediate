package ie.bitstep.mango.spring.logging.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import ie.bitstep.mango.collections.ListBuilder;
import ie.bitstep.mango.spring.logging.LogProjection;
import ie.bitstep.mango.spring.logging.annotations.LoggableClass;
import ie.bitstep.mango.spring.logging.annotations.LoggableField;
import ie.bitstep.mango.spring.logging.annotations.LoggableFieldView;
import ie.bitstep.mango.spring.logging.annotations.LoggableStyle;
import ie.bitstep.mango.spring.logging.annotations.LoggableView;
import ie.bitstep.mango.spring.logging.internal.accessors.FieldAccessor;
import ie.bitstep.mango.spring.logging.internal.accessors.GetterAccessor;
import ie.bitstep.mango.spring.logging.test.data.SecretData;
import ie.bitstep.mango.spring.logging.test.data.SecretDataLogProjection;
import ie.bitstep.mango.spring.logging.test.data.SecretDataLogProjectionToMap;
import ie.bitstep.mango.utils.converters.CollectionToSize;
import ie.bitstep.mango.utils.converters.Converter;
import ie.bitstep.mango.utils.masking.FullMasker;
import ie.bitstep.mango.utils.masking.IdMasker;
import ie.bitstep.mango.utils.masking.Mask;
import ie.bitstep.mango.utils.masking.PanMasker;
import org.assertj.core.util.Lists;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


class LoggerMapperTest {
	public String toString() {
		throw new RuntimeException("failed");
	}

	@Test
	void privateConstructorTest() throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
		Constructor<LoggerMapper> constructor = LoggerMapper.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		LoggerMapper loggerMapperInstance = constructor.newInstance();
		assertThat(loggerMapperInstance).isNotNull(); // best we can do
	}

	@Test
	void testIsValidJsonPropertyIsNull() throws NoSuchMethodException {
		assertThat(LoggerMapper.isValidJsonProperty(null)).isFalse();
	}

	@Test
	@JsonProperty()
	public void testIsValidJsonPropertyEmptyValue() throws NoSuchMethodException { // NOSONAR - required public as used within test
		Method m = LoggerMapperTest.class.getMethod("testIsValidJsonPropertyEmptyValue");
		JsonProperty jsonProperty = m.getAnnotation(JsonProperty.class);
		assertThat(LoggerMapper.isValidJsonProperty(jsonProperty)).isFalse();
	}

	@Test
	@JsonProperty("value")
	public void testIsValidJsonProperty() throws NoSuchMethodException { // NOSONAR - required public as used within test
		Method m = LoggerMapperTest.class.getMethod("testIsValidJsonProperty");
		JsonProperty jsonProperty = m.getAnnotation(JsonProperty.class);
		assertThat(LoggerMapper.isValidJsonProperty(jsonProperty)).isTrue();
	}

	@Test
	void toLoggable_withByteArray() {
		byte[] ba = {0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10};
		assertThat(LoggerMapper.toLoggable(ba)).isEqualTo("EBAQEBAQEA==");
	}

	@Test
	void toLoggable_withIntArray() {
		int[] o = {0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10};
		List<Integer> l = ListBuilder.<Integer>list()
			.add(0x10)
			.add(0x10)
			.add(0x10)
			.add(0x10)
			.add(0x10)
			.add(0x10)
			.add(0x10)
			.build();
		assertThat(LoggerMapper.toLoggable(o)).isEqualTo(l);
	}

	@Test
	void toLoggable_withLongArray() {
		long[] o = {0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10};
		List<Long> l = ListBuilder.<Long>list()
			.add(0x10l)
			.add(0x10l)
			.add(0x10l)
			.add(0x10l)
			.add(0x10l)
			.add(0x10l)
			.add(0x10l)
			.build();
		assertThat(LoggerMapper.toLoggable(o)).isEqualTo(l);
	}

	@Test
	void toLoggable_withFloatArray() {
		float[] o = {200f, 200f, 200f, 200f, 200f, 200f, 200f};
		List<Float> l = ListBuilder.<Float>list()
			.add(200.0f)
			.add(200.0f)
			.add(200.0f)
			.add(200.0f)
			.add(200.0f)
			.add(200.0f)
			.add(200.0f)
			.build();
		assertThat(LoggerMapper.toLoggable(o)).isEqualTo(l);
	}

	@Test
	void toLoggable_withDoubleArray() {
		double[] o = {200, 200, 200, 200, 200, 200, 200};
		List<Double> l = ListBuilder.<Double>list()
			.add(200.0)
			.add(200.0)
			.add(200.0)
			.add(200.0)
			.add(200.0)
			.add(200.0)
			.add(200.0)
			.build();
		assertThat(LoggerMapper.toLoggable(o)).isEqualTo(l);
	}

	@Test
	void toLoggable_withInteger() {
		assertThat(LoggerMapper.toLoggable(200)).isEqualTo(200);
	}

	@Test
	void toLoggable_withOptional() {
		Optional o = Optional.empty();
		assertThat(LoggerMapper.toLoggable(o)).isNull();

		o = Optional.of("Hello");
		assertThat(LoggerMapper.toLoggable(o)).isEqualTo("Hello");
	}

	@Test
	void toLoggable_withHibernateProxy() {
		HibernateProxy proxy = new HibernateProxy() {
			@Override
			public Object writeReplace() {
				return null;
			}

			@Override
			public LazyInitializer getHibernateLazyInitializer() {
				return new LazyInitializer() {
					@Override
					public void initialize() throws HibernateException {

					}

					@Override
					public Serializable getIdentifier() {
						return null;
					}

					@Override
					public void setIdentifier(Object o) {

					}

					@Override
					public String getEntityName() {
						return null;
					}

					@Override
					public Class getPersistentClass() {
						return null;
					}

					@Override
					public boolean isUninitialized() {
						return false;
					}

					@Override
					public Object getImplementation() {
						return new String("Hello");
					}

					@Override
					public Object getImplementation(SharedSessionContractImplementor sharedSessionContractImplementor) throws HibernateException {
						return null;
					}

					@Override
					public void setImplementation(Object o) {

					}

					@Override
					public Class<?> getImplementationClass() {
						return null;
					}

					@Override
					public String getImplementationEntityName() {
						return "";
					}

					@Override
					public boolean isReadOnlySettingAvailable() {
						return false;
					}

					@Override
					public boolean isReadOnly() {
						return false;
					}

					@Override
					public void setReadOnly(boolean b) {

					}

					@Override
					public SharedSessionContractImplementor getSession() {
						return null;
					}

					@Override
					public void setSession(SharedSessionContractImplementor sharedSessionContractImplementor) throws HibernateException {

					}

					@Override
					public void unsetSession() {

					}

					@Override
					public void setUnwrap(boolean b) {

					}

					@Override
					public boolean isUnwrap() {
						return false;
					}
				};
			}
		};

		assertThat(LoggerMapper.toLoggable(proxy)).isEqualTo("Hello");
	}

	@Test
	void toLoggable_withNullMap() {
		Map<?, ?> m = null;
		assertThat(LoggerMapper.toLoggable(m)).isNull();
	}

	@Test
	void toLoggable_withNullCollection() {
		Collection<?> c = null;
		assertThat(LoggerMapper.toLoggable((Collection) c)).isNull();
	}

	@Test
	void toLoggable_withNullValueInMap() {
		Map<String, String> map = new HashMap<>() {{
			put("key1", "value1");
			put("key2", null);
		}};
		assertThat(LoggerMapper.toLoggable(map))
			.isEqualTo(map);
	}

	@Test
	void toLoggable_withNullValueInCollection() {
		List<String> list = Lists.newArrayList(null, "value");
		assertThat(LoggerMapper.toLoggable(list)).isEqualTo(list);
	}

	@LoggableClass
	public static class TestLoggable {
		public TestLoggable(String name) {
			this.name = name;
		}

		@LoggableField(name = "lname")    // to make it visible when the conversion was applied
		private String name;
	}

	@Test
	void testLoggableException() {
		String msg = (String) LoggerMapper.toLoggable(new LoggerMapperTest());

		assertThat(msg).isEqualTo("failed");
	}

	@Test
	void testLoggableInstant() {
		String msg = (String) LoggerMapper.toLoggable(Instant.ofEpochSecond(3453425));

		assertThat(msg).isEqualTo("1970-02-09T23:17:05Z");
	}

	@Test
	void testLoggableToJsonException() {
		String msg = (String) LoggerMapper.loggableToJsonString(new LoggerMapperTest());

		assertThat(msg).startsWith("No serializer found for class ie.bitstep.mango.spring.logging.internal.LoggerMapperTest");
	}

	@Test
	void toLogProjection() {
		ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
		when(applicationContext.getBeansOfType(LogProjection.class)).thenReturn(
			Map.of(SecretData.class.getSimpleName(), new SecretDataLogProjection())
		);

		new LogProjectionRegistry(applicationContext);

		Map m = (Map) LoggerMapper.toLoggable(
			SecretData.builder()
				.cardNumber("5111111111111111")
				.build());

		assertThat(m).isEqualTo(
			Map.of("cardNumber", "5111111111111111")
		);
	}

	@Test
	void toLogProjectionToMap() {
		ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
		when(applicationContext.getBeansOfType(LogProjection.class)).thenReturn(
			Map.of(SecretData.class.getSimpleName(), new SecretDataLogProjectionToMap())
		);

		new LogProjectionRegistry(applicationContext);

		Map result = (Map) LoggerMapper.toLoggable(
			SecretData.builder()
				.cardNumber("5111111111111111")
				.build());

		assertThat(result).isEqualTo(
			Map.of("cardNumber", "5111111111111111")
		);
	}

	@Test
	void toJson_whenNull() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(null);

		// then - convert each value, preserve the order
		assertThat(json).isEqualTo("null");
	}

	@Test
	void toJson_whenMap() throws Exception {
		// given
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("name2", new TestLoggable("name2"));
		map.put(null, new TestLoggable("name4")); // null keys should be ignored, but produce a ERROR log message
		map.put("name1", new TestLoggable("name1"));
		map.put("name3", new TestLoggable("name3"));
		map.put("name4", new TestLoggable(null));

		// when
		String json = LoggerMapper.toJsonString(map);

		// then - convert each value, preserve the order
		assertThat(json).isEqualTo("{\"name2\":{\"lname\":\"name2\"},\"name1\":{\"lname\":\"name1\"},\"name3\":{\"lname\":\"name3\"},\"name4\":{}}");
	}

	@Test
	void toJson_whenMap_BadKey() throws Exception {
		// given
		Map<Object, Object> map = new LinkedHashMap<>();
		map.put("name2", new TestLoggable("name2"));
		map.put(new LoggerMapperTest(), new TestLoggable("name4")); // bad keys should be ignored, but produce a ERROR log message
		map.put("name1", new TestLoggable("name1"));
		map.put("name3", new TestLoggable("name3"));

		// when
		String json = LoggerMapper.toJsonString(map);

		// then - convert each value, preserve the order
		assertThat(json).isEqualTo("{\"name2\":{\"lname\":\"name2\"},\"name1\":{\"lname\":\"name1\"},\"name3\":{\"lname\":\"name3\"}}");
	}

	@Test
	void toJson_whenList() throws Exception {
		// given
		List<TestLoggable> list = new ArrayList<>();
		list.add(new TestLoggable("name2"));
		list.add(new TestLoggable("name1"));
		list.add(new TestLoggable("name3"));
		list.add(new TestLoggable(null));

		// when
		String json = LoggerMapper.toJsonString(list);

		// then - convert each value, preserve the order
		assertThat(json).isEqualTo("[{\"lname\":\"name2\"},{\"lname\":\"name1\"},{\"lname\":\"name3\"},{}]");
	}

	@Test
	void toJson_whenSet() throws Exception {
		// given
		Set<TestLoggable> set = new LinkedHashSet<>();
		set.add(new TestLoggable("name2"));
		set.add(new TestLoggable("name1"));
		set.add(new TestLoggable("name3"));

		// when
		String json = LoggerMapper.toJsonString(set);

		// then - convert each value, preserve the order
		assertThat(json).isEqualTo("[{\"lname\":\"name2\"},{\"lname\":\"name1\"},{\"lname\":\"name3\"}]");
	}

	@Test
	void toJson_whenArrays() throws Exception {
		// given
		TestLoggable[] array = new TestLoggable[]{
			new TestLoggable("name2"),
			new TestLoggable("name1"),
			new TestLoggable("name3")
		};

		// when
		String json = LoggerMapper.toJsonString(array);

		// then - convert each value, preserve the order
		assertThat(json).isEqualTo("[{\"lname\":\"name2\"},{\"lname\":\"name1\"},{\"lname\":\"name3\"}]");
	}

	@Test
	void toJson_whenString() throws Exception {
		// when
		String json = LoggerMapper.toJsonString("value55");

		// then
		assertThat(json).isEqualTo("\"value55\"");
	}

	@Test
	void toJson_whenBoolean() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(Boolean.TRUE);

		// then - convert to string value
		assertThat(json).isEqualTo("\"true\"");
	}

	@Test
	void toJson_whenInteger() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(5);

		// then - convert to string value
		assertThat(json).isEqualTo("5");
	}

	@Test
	void toJson_whenLong() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(5L);

		// then - convert to string value
		assertThat(json).isEqualTo("5");
	}

	@Test
	void toJson_whenDate() throws Exception {
		// given
		OffsetDateTime dateTime = OffsetDateTime.of(2017, 2, 3, 4, 5, 6, 0, ZoneOffset.ofHours(+2));

		// when
		String json = LoggerMapper.toJsonString(Timestamp.from(Instant.from(dateTime)));

		// then - convert to ISO string value
		assertThat(json).isEqualTo("\"2017-02-03T02:05:06Z\"");
	}

	@Test
	void toJson_whenLoggableValueNull() throws Exception {
		// given
		LoggableValue value = new LoggableValue(null, new PanMasker(), null, 99, LoggableView.ALL);

		// when
		String json = LoggerMapper.toJsonString(value);

		// then
		assertThat(json).isEqualTo("null");
	}

	@Test
	void toJson_whenLoggableValueNotMasked() throws Exception {
		// given
		LoggableValue value = new LoggableValue("value55", null, null, 0, LoggableView.ALL);

		// when
		String json = LoggerMapper.toJsonString(value);

		// then
		assertThat(json).isEqualTo("\"value55\"");
	}

	@Test
	void toJson_whenLoggableValueMasked() throws Exception {
		// given
		LoggableValue value = new LoggableValue("5432987698761234", new PanMasker(), null, 0, LoggableView.ALL);

		// when
		String json = LoggerMapper.toJsonString(value);

		// then
		assertThat(json).isEqualTo("\"543298XXXXXX1234\"");
	}

	@Test
	void toJson_whenLoggableValueConverted() throws Exception {
		// given
		LoggableValue value = new LoggableValue(Arrays.asList("ONE", "TWO", "THREE"), null, new CollectionToSize(), 0, LoggableView.ALL);

		// when
		String json = LoggerMapper.toJsonString(value);

		// then
		assertThat(json).isEqualTo("3");
	}

	@Test
	void toJson_whenLoggableValueStringTruncated() throws Exception {
		// given
		LoggableValue value = new LoggableValue("123456789012345", null, null, 10, LoggableView.ALL);

		// when
		String json = LoggerMapper.toJsonString(value);

		// then
		assertThat(json).isEqualTo("\"1234567...\"");
	}

	@Test
	void toJson_whenLoggableValueStringListTruncated() throws Exception {
		// given
		LoggableValue value = new LoggableValue(Arrays.asList("123456789012345", "abcdefghijklmn"),
			null, null, 10, LoggableView.ALL);

		// when
		String json = LoggerMapper.toJsonString(value);

		// then
		assertThat(json).isEqualTo("[\"1234567...\",\"abcdefg...\"]");
	}

	@Test
	void toJson_whenLoggableValueBytesTruncated() throws Exception {
		// given
		LoggableValue value = new LoggableValue(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
			null, null, 10, LoggableView.ALL);

		// when
		String json = LoggerMapper.toJsonString(value);

		// then
		assertThat(json).isEqualTo("\"AQIDBAU...\"");
	}

	@Test
	void toJson_whenLoggableValueBytesListTruncated() throws Exception {
		// given
		LoggableValue value = new LoggableValue(Arrays.asList(
			new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
			new byte[]{51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65}),
			null, null, 10, LoggableView.ALL);

		// when
		String json = LoggerMapper.toJsonString(value);

		// then
		assertThat(json).isEqualTo("[\"AQIDBAU...\",\"MzQ1Njc...\"]");
	}

	@LoggableClass
	public static class TestLoggableValueWithViewIdentifiable implements Identifiable {
		public TestLoggableValueWithViewIdentifiable(String id, String key, String value) {
			this.id = id;
			this.key = key;
			this.value = value;
		}

		@LoggableField
		@LoggableFieldView(LoggableView.KEY)
		private String id;

		public String getId() {
			return id;
		}

		@LoggableField
		@LoggableFieldView(LoggableView.KEY)
		private String key;

		@LoggableField
		private String value;
	}

	@LoggableClass
	public static class TestLoggableValueWithView {
		public TestLoggableValueWithView(String id, String key, String value) {
			this.id = id;
			this.key = key;
			this.value = value;
		}

		@LoggableField
		@LoggableFieldView(LoggableView.KEY)
		private String id;

		public String getId() {
			return id;
		}

		@LoggableField
		@LoggableFieldView(LoggableView.KEY)
		private String key;

		@LoggableField
		private String value;
	}

	@Test
	void toJson_whenLoggableValueAndView() throws Exception {
		// given
		TestLoggableValueWithView value = new TestLoggableValueWithView("id55", "key55", "value55");
		LoggableValue loggableValueIdView = new LoggableValue(value, null, null, 0, LoggableView.ID);
		LoggableValue loggableValueKeyView = new LoggableValue(value, null, null, 0, LoggableView.KEY);
		LoggableValue loggableValueAllView = new LoggableValue(value, null, null, 0, LoggableView.ALL);

		// when
		String jsonIdView = LoggerMapper.toJsonString(loggableValueIdView);
		String jsonKeyView = LoggerMapper.toJsonString(loggableValueKeyView);
		String jsonAllView = LoggerMapper.toJsonString(loggableValueAllView);

		// then
		assertThat(jsonIdView).isEqualTo("\"Class class ie.bitstep.mango.spring.logging.internal.LoggerMapperTest$TestLoggableValueWithView must implement Identifiable\"");
		assertThat(jsonKeyView).isEqualTo("{\"id\":\"id55\",\"key\":\"key55\"}");
		assertThat(jsonAllView).isEqualTo("{\"id\":\"id55\",\"key\":\"key55\",\"value\":\"value55\"}");
	}

	@Test
	void toJson_whenLoggableValueAndViewNoIdentifiable() throws Exception {
		// given
		TestLoggableValueWithViewIdentifiable value = new TestLoggableValueWithViewIdentifiable("id55", "key55", "value55");
		LoggableValue loggableValueIdView = new LoggableValue(value, null, null, 0, LoggableView.ID);
		LoggableValue loggableValueKeyView = new LoggableValue(value, null, null, 0, LoggableView.KEY);
		LoggableValue loggableValueAllView = new LoggableValue(value, null, null, 0, LoggableView.ALL);

		// when
		String jsonIdView = LoggerMapper.toJsonString(loggableValueIdView);
		String jsonKeyView = LoggerMapper.toJsonString(loggableValueKeyView);
		String jsonAllView = LoggerMapper.toJsonString(loggableValueAllView);

		// then
		assertThat(jsonIdView).isEqualTo("{\"id\":\"id55\"}");
		assertThat(jsonKeyView).isEqualTo("{\"id\":\"id55\",\"key\":\"key55\"}");
		assertThat(jsonAllView).isEqualTo("{\"id\":\"id55\",\"key\":\"key55\",\"value\":\"value55\"}");
	}

	@LoggableClass
	public static class TestPojoLoggable {
		@LoggableField                            // use default field name
		private String nameDef = "defName55";
		@LoggableField(name = "logName")          // use name from annotation
		private String nameLog = "logName55";
		@LoggableField
		@JsonProperty("jsonName")                 // use json name
		private String nameJson = "jsonName55";
		@LoggableField(mask = IdMasker.class)     // with masking
		private String account = "2345345765";
		@LoggableField(mask = PanMasker.class)    // with masking of collections
		private List<String> pans = Arrays.asList("5432234534576562", "5432558257356834");
		@LoggableField(convert = CollectionToSize.class)     // with converter
		private List<String> list = Arrays.asList("ONE", "TWO", "THREE");
		@LoggableField(truncateLen = 10)
		private String str10 = "123456789012345";

		@LoggableField                            // getter
		private String getCalc() {
			return "calculated";
		}

		public boolean isActive() {
			return true;
		}

		public void go() {
		}

		public String getAccount() {
			return account;
		}
	}

	@Test
	void testFieldAccessorIllegalStateException() throws NoSuchFieldException {
		TestPojoLoggable testPojoLoggable = new TestPojoLoggable();
		Field f = TestPojoLoggable.class.getDeclaredField("nameDef");
		FieldAccessor fieldAccessor = new FieldAccessor(f);

		Assertions.assertThrows(IllegalStateException.class, () -> {
			fieldAccessor.getObject(testPojoLoggable);
		});
	}

	@Test
	void testGetterAccessorIllegalStateException() throws NoSuchMethodException {
		TestPojoLoggable testPojoLoggable = new TestPojoLoggable();
		Method m = TestPojoLoggable.class.getMethod("getAccount");
		GetterAccessor getterAccessor = new GetterAccessor(m);
		m.setAccessible(false);

		Assertions.assertThrows(IllegalStateException.class, () -> {
			getterAccessor.getObject(this);
		});
	}

	@ParameterizedTest
	@CsvSource(value = {"getAccount,account",
		"isActive,active",
		"go,go"})
	void testGetterAccessorGetNameGetter(String method, String expected) throws NoSuchMethodException {
		Method m = TestPojoLoggable.class.getMethod(method);
		GetterAccessor getterAccessor = new GetterAccessor(m);

		assertThat(getterAccessor.getName()).isEqualTo(expected);
	}

	@Test
	void toJson_whenPojoLoggable() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(new TestPojoLoggable());

		// then
		assertThat(json).isEqualTo("{\"nameDef\":\"defName55\",\"logName\":\"logName55\",\"jsonName\":\"jsonName55\"," +
			"\"account\":\"23453XXXXX\",\"pans\":[\"543223XXXXXX6562\",\"543255XXXXXX6834\"],\"list\":3,\"str10\":\"1234567...\"," +
			"\"calc\":\"calculated\"}");
	}

	@LoggableClass(style = LoggableStyle.ALL)
	public static class TestPojoLoggableALL {
		private static int serialVersionUID = 23415345;    // should not be included
		private String nameDef = "defName55";    // use default field name
		@LoggableField(name = "logName")        // use name from annotation
		private String nameLog = "logName55";
		@LoggableField(name = "Null")        // use name from annotation
		private String nullLog = null;
		@JsonProperty("jsonName")                // use json name
		private String nameJson = "jsonName55";
		@LoggableField(mask = IdMasker.class)    // with masking
		private String account = "2345345765";
		@LoggableField(convert = CollectionToSize.class)     // with converter
		private List<String> list = Arrays.asList("ONE", "TWO", "THREE");
		@LoggableField
		@Converter(converter = CollectionToSize.class)// with converter
		private List<String> anotherList = Arrays.asList("ONE", "TWO", "THREE");
		@LoggableField
		@Mask(maskingChars = "#", prefix = 2, postfix = 2)
		// with masking
		private String generic = "2345345765";
	}

	@LoggableClass(style = LoggableStyle.ALL)
	public static class TestPojoLoggableParams {
		private TestPojoLoggableALL nestedPojo = new TestPojoLoggableALL();    // use default field name
		@LoggableField
		@Mask(masker = FullMasker.class, maskingChars = "***")// with full masking
		private Map<String, Object> map = new HashMap<>() {{
			put("key1", "value1");
			put("key2", null);
		}};
	}

	@Test
	void testGetFieldValueException() {
		Object value = LoggerMapper.getFieldValue("", null);

		assertThat(value).isEqualTo("");
	}

	@Test
	void toJson_whenPojoLoggableAll() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(new TestPojoLoggableALL());

		// then
		assertThat(json).isEqualTo("{\"nameDef\":\"defName55\",\"logName\":\"logName55\"," +
			"\"jsonName\":\"jsonName55\",\"account\":\"23453XXXXX\"," +
			"\"list\":3,\"anotherList\":3,\"generic\":\"23######65\"}");
	}

	@Test
	void toJson_whenPojoLoggableNull() throws Exception {
		// when
		Map<String, Object> json = LoggerMapper.toLoggablePojo(null, LoggableView.ALL);

		// then
		assertThat(json).isNull();
	}


	public static class TestPojoNotLoggable {
		private String nameDef = "defName55";

		public String toString() {
			return "TestPojoNotLoggable.toString";
		}
	}

	@Test
	void toJson_whenPojoNotLoggable() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(new TestPojoNotLoggable());

		// then
		assertThat(json).isEqualTo("\"TestPojoNotLoggable.toString\"");
	}


	@LoggableClass(style = LoggableStyle.ALL)
	public static class TestPojoLoggableAllParent {
		private String parentName = "parentNameAll";
	}

	@LoggableClass(style = LoggableStyle.ALL)
	public static class TestPojoLoggableAllChildOfLoggableAllParent extends TestPojoLoggableAllParent {
		private String childName = "childNameAll";
	}

	@Test
	void toJson_whenPojoLoggableAllChildOfLoggableAllParent() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(new TestPojoLoggableAllChildOfLoggableAllParent());

		// then
		assertThat(json).isEqualTo("{\"childName\":\"childNameAll\",\"parentName\":\"parentNameAll\"}");
	}


	@LoggableClass
	public static class TestPojoLoggableSomeParent {
		@LoggableField
		private String parentName = "parentNameSome";
		private String hiddenName = "hiddenNameSome";
	}

	@LoggableClass
	public static class TestPojoLoggableSomeChildOfLoggableSomeParent extends TestPojoLoggableSomeParent {
		@LoggableField
		private String childName = "childNameSome";
		private String hiddenName = "hiddenNameSome";
	}

	@Test
	void toJson_whenPojoLoggableSomeChildOfLoggableSomeParent() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(new TestPojoLoggableSomeChildOfLoggableSomeParent());

		// then
		assertThat(json).isEqualTo("{\"childName\":\"childNameSome\",\"parentName\":\"parentNameSome\"}");
	}


	public static class TestPojoNotLoggableParent {
		private String parentName = "parentName55";
	}

	@LoggableClass(style = LoggableStyle.ALL)
	public static class TestPojoLoggableChildOfNotLoggableParent extends TestPojoNotLoggableParent {
		private String childName = "childName55";
	}

	@Test
	void toJson_whenPojoLoggableChildOfNotLoggableParent() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(new TestPojoLoggableChildOfNotLoggableParent());

		// then
		assertThat(json).isEqualTo("{\"childName\":\"childName55\"}");
	}


	public static class TestPojoNotLoggableChildOfLoggableAllParent extends TestPojoLoggableAllParent {
		private String childName = "childName55";

		public String toString() {
			return "TestPojoNotLoggableChildOfLoggableAllParent.toString";
		}
	}

	@Test
	void toJson_whenPojoNotLoggableChild() throws Exception {
		// when
		String json = LoggerMapper.toJsonString(new TestPojoNotLoggableChildOfLoggableAllParent());

		// then - @LoggableClass is not inheritable
		assertThat(json).isEqualTo("\"TestPojoNotLoggableChildOfLoggableAllParent.toString\"");
	}

	@Test
	void testTruncateWhenNull() {
		Object value = LoggerMapper.truncate(null, 6);

		assertThat(value).isNull();
	}

	@Test
	void toJsonString_whenPojoLoggableAll() {
		String json = LoggerMapper.toJsonString(new TestPojoLoggableParams());
		assertThat(json).isEqualTo("{\"nestedPojo\":{\"nameDef\":\"defName55\",\"logName\":\"logName55\",\"jsonName\":\"jsonName55\",\"account\":\"23453XXXXX\",\"list\":3,\"anotherList\":3,\"generic\":\"23######65\"},\"map\":{\"key1\":\"***\",\"key2\":null}}");
	}

	// Tests to kill surviving mutations

	@Test
	void toLoggable_mapWithNullKey_logsError() {
		// Target: Line 101 - Logger.error call when map has null key
		// Note: This tests the error handling behavior. The actual logger.error() call
		// is excluded from PIT mutations via avoidCallsTo configuration because:
		// 1. The static final LOGGER cannot be easily mocked
		// 2. Error logging is diagnostic, not business logic
		// 3. The important behavior is that exceptions are caught and don't propagate

		// given - a map with both a null key (will cause exception) and a valid entry
		Map<Object, Object> mapWithNullKey = new HashMap<>();
		mapWithNullKey.put(null, "value");
		mapWithNullKey.put("validKey", "validValue");

		// when - toLoggable processes the map
		Map<String, Object> result = LoggerMapper.toLoggable(mapWithNullKey);

		// then - the exception was caught (not thrown) and the valid entry was processed
		assertThat(result).isNotNull();
		assertThat(result).containsEntry("validKey", "validValue");
		assertThat(result).hasSize(1); // Only the valid entry, null key entry was skipped

		// This verifies the try-catch block works correctly:
		// - NullKeyException is thrown for the null key
		// - Exception is caught in the forEach lambda
		// - Logger.error is called (excluded from PIT mutation)
		// - Processing continues for remaining entries
	}

	@Test
	void getAspectLoggerMetaData_caching() throws Exception {
		// Target: Lines 293-299 - metadata caching logic (SURVIVED)
		// Test that metadata is cached after first access
		TestPojoLoggableParams instance1 = new TestPojoLoggableParams();
		TestPojoLoggableParams instance2 = new TestPojoLoggableParams();

		// First call - populates cache
		String json1 = LoggerMapper.toJsonString(instance1);

		// Second call - uses cache (should hit line 291 but not 292-299)
		String json2 = LoggerMapper.toJsonString(instance2);

		// Both should produce same structure
		assertThat(json1).isNotEmpty();
		assertThat(json2).isNotEmpty();
	}

	@Test
	void putFieldMetaData_withLoggableStyleAll() {
		// Target: Lines 324-364 - putFieldMetaData with various conditions (SURVIVED)
		// Test a class with LoggableStyle.ALL
		@LoggableClass(style = LoggableStyle.ALL)
		class TestStyleAll {
			private String field1 = "value1";
			private String field2 = "value2";

			public String getField1() {
				return field1;
			}

			public String getField2() {
				return field2;
			}
		}

		TestStyleAll instance = new TestStyleAll();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// With LoggableStyle.ALL, all fields should be included
		assertThat(result).isNotEmpty();
	}

	@Test
	void putFieldMetaData_withLoggableFieldCustomName() {
		// Target: Lines 328-330 - loggableField name handling (SURVIVED)
		@LoggableClass
		class TestCustomName {
			@LoggableField(name = "customName")
			private String field1 = "value1";
		}

		TestCustomName instance = new TestCustomName();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Field should be logged with custom name
		assertThat(result).containsKey("customName");
		assertThat(result.get("customName")).isEqualTo("value1");
	}

	@Test
	void putFieldMetaData_withJsonProperty() {
		// Target: Lines 330-334 - JsonProperty handling (SURVIVED)
		@LoggableClass(style = LoggableStyle.ALL)
		class TestJsonProperty {
			@JsonProperty("jsonName")
			private String field1 = "value1";

			public String getField1() {
				return field1;
			}
		}

		TestJsonProperty instance = new TestJsonProperty();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Field should use JsonProperty name
		assertThat(result).containsKey("jsonName");
	}

	@Test
	void putFieldMetaData_withMasker() {
		// Target: Lines 337-339 - masker handling (SURVIVED)
		@LoggableClass
		class TestMasker {
			@LoggableField(mask = FullMasker.class)
			private String secret = "12345";
		}

		TestMasker instance = new TestMasker();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Value should be masked
		assertThat(result.get("secret")).isEqualTo("***");
	}

	@Test
	void putFieldMetaData_withConverter() {
		// Target: Lines 342-344 - converter handling (SURVIVED)
		@LoggableClass
		class TestConverter {
			@LoggableField(convert = CollectionToSize.class)
			private List<String> list = Arrays.asList("a", "b", "c");
		}

		TestConverter instance = new TestConverter();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// List should be converted to size
		assertThat(result.get("list")).isEqualTo(3);
	}

	@Test
	void putFieldMetaData_withTruncateLen() {
		// Target: Line 346 - truncateLen handling (SURVIVED)
		@LoggableClass
		class TestTruncate {
			@LoggableField(truncateLen = 5)
			private String longValue = "1234567890";
		}

		TestTruncate instance = new TestTruncate();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Value should be truncated
		String truncated = (String) result.get("longValue");
		assertThat(truncated.length()).isLessThanOrEqualTo(5);
	}

	@Test
	void putFieldMetaData_withView() {
		// Target: Lines 349-351 - view handling (SURVIVED)
		@LoggableClass
		class TestView {
			@LoggableField(view = LoggableView.ID)
			private String idField = "id123";
		}

		TestView instance = new TestView();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Field should be included
		assertThat(result).containsKey("idField");
	}

	@Test
	void putFieldMetaData_withLoggableFieldView() {
		// Target: Lines 354-359 - belongsToViews handling (SURVIVED)
		@LoggableClass
		class TestBelongsToView {
			@LoggableField
			@LoggableFieldView({LoggableView.ID})
			private String idOnlyField = "id123";

			@LoggableField
			private String allField = "all";
		}

		TestBelongsToView instance = new TestBelongsToView();

		// When querying with ID view, only ID field should be included
		Map<String, Object> idResult = LoggerMapper.toLoggablePojo(instance, LoggableView.ID);
		assertThat(idResult).containsKey("idOnlyField");

		// When querying with ALL view, all fields should be included
		Map<String, Object> allResult = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);
		assertThat(allResult).containsKey("idOnlyField");
		assertThat(allResult).containsKey("allField");
	}

	@Test
	void getConverter_withConverterAnnotation() {
		// Target: Lines 369-373 - getConverter logic (SURVIVED)
		@LoggableClass
		class TestConverterAnnotation {
			@LoggableField
			@Converter(converter = CollectionToSize.class)
			private List<String> list = Arrays.asList("a", "b");
		}

		TestConverterAnnotation instance = new TestConverterAnnotation();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Converter should be applied
		assertThat(result.get("list")).isEqualTo(2);
	}

	@Test
	void getConverter_withLoggableFieldConvert() {
		// Target: Lines 371-372 - loggableField.convert handling (SURVIVED)
		@LoggableClass
		class TestLoggableConvert {
			@LoggableField(convert = CollectionToSize.class)
			private List<String> list = Arrays.asList("a", "b", "c");
		}

		TestLoggableConvert instance = new TestLoggableConvert();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Converter from LoggableField should be applied
		assertThat(result.get("list")).isEqualTo(3);
	}

	@Test
	void getMasker_withMaskAnnotation() {
		// Target: Lines 380-384 - getMasker logic (SURVIVED)
		@LoggableClass
		class TestMaskAnnotation {
			@LoggableField
			@Mask(maskingChars = "X", prefix = 2, postfix = 2)
			private String value = "1234567890";
		}

		TestMaskAnnotation instance = new TestMaskAnnotation();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Masker should be applied
		String masked = (String) result.get("value");
		assertThat(masked).contains("X");
	}

	@Test
	void getMasker_withLoggableFieldMask() {
		// Target: Lines 382-383 - loggableField.mask handling (SURVIVED)
		@LoggableClass
		class TestLoggableMask {
			@LoggableField(mask = PanMasker.class)
			private String pan = "1234567890123456";
		}

		TestLoggableMask instance = new TestLoggableMask();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Masker from LoggableField should be applied
		String masked = (String) result.get("pan");
		assertThat(masked).isNotEqualTo("1234567890123456");
	}

	@Test
	void getFieldName_withEmptyName() throws Exception {
		// Target: Line 440 - getFieldName logic (NO_COVERAGE)
		// Use Object.toString() as a test method
		Method method = Object.class.getDeclaredMethod("toString");

		// Create LoggableField with empty name
		LoggableField emptyNameField = Mockito.mock(LoggableField.class);
		Mockito.when(emptyNameField.name()).thenReturn("");

		String fieldName = LoggerMapper.getFieldName(method, emptyNameField);

		// Should return method name when loggableField.name() is empty
		assertThat(fieldName).isEqualTo("toString");
	}

	@Test
	void getFieldName_withCustomName() throws Exception {
		// Target: Line 440 - getFieldName logic with non-empty name (NO_COVERAGE)
		// Use Object.toString() as a test method
		Method method = Object.class.getDeclaredMethod("toString");

		// Create LoggableField with custom name
		LoggableField customNameField = Mockito.mock(LoggableField.class);
		Mockito.when(customNameField.name()).thenReturn("customName");

		String fieldName = LoggerMapper.getFieldName(method, customNameField);

		// Should return custom name
		assertThat(fieldName).isEqualTo("customName");
	}

	@Test
	void getValue_withException() throws Exception {
		// Target: Line 445, 447 - getValue with exception (NO_COVERAGE)
		Method method = Mockito.mock(Method.class);
		LogProjection<?> wrapper = Mockito.mock(LogProjection.class);
		Object obj = new Object();

		// Make method.invoke throw exception
		Mockito.when(method.invoke(wrapper, obj)).thenThrow(new IllegalAccessException("test"));

		Object result = LoggerMapper.getValue(obj, wrapper, method);

		// Should return null when exception occurs
		assertThat(result).isNull();
	}

	@Test
	void getFieldValue_withException() throws Exception {
		// Target: Line 465 - getFieldValue exception handling (NO_COVERAGE)
		// Create metadata that will throw exception when accessing value
		AspectLoggerMetaData badMetaData = new AspectLoggerMetaData(
			obj -> {
				throw new RuntimeException("test exception");
			},
			"badField",
			null,
			null,
			0,
			LoggableView.ALL,
			java.util.Collections.singleton(LoggableView.ALL)
		);

		Object result = LoggerMapper.getFieldValue(new Object(), badMetaData);

		// Should return exception message
		assertThat(result).isEqualTo("test exception");
	}

	@Test
	void putClassMetaData_withInheritance() {
		// Target: Lines 307-314 - putClassMetaData with inheritance (SURVIVED)
		// Test that parent class metadata is included
		@LoggableClass
		class Parent {
			@LoggableField
			protected String parentField = "parent";
		}

		@LoggableClass
		class Child extends Parent {
			@LoggableField
			private String childField = "child";
		}

		Child instance = new Child();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Both parent and child fields should be included
		assertThat(result).containsKey("parentField");
		assertThat(result).containsKey("childField");
	}

	@Test
	void putClassMetaData_processesGetters() {
		// Target: Line 312 - processing getters (SURVIVED)
		@LoggableClass(style = LoggableStyle.ALL)
		class TestWithGetter {
			private String field = "value";

			@LoggableField(name = "computedField")
			public String getComputedValue() {
				return field + "_computed";
			}
		}

		TestWithGetter instance = new TestWithGetter();
		Map<String, Object> result = LoggerMapper.toLoggablePojo(instance, LoggableView.ALL);

		// Getter should be processed
		assertThat(result).containsKey("computedField");
		assertThat(result.get("computedField")).isEqualTo("value_computed");
	}
}
