package ie.bitstep.mango.spring.logging.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import ie.bitstep.mango.spring.logging.test.ListBuilder;
import ie.bitstep.mango.spring.logging.test.MapBuilder;
import ie.bitstep.mango.spring.logging.LogProjection;
import ie.bitstep.mango.spring.logging.annotations.LoggableClass;
import ie.bitstep.mango.spring.logging.annotations.LoggableField;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@Data
class LogProjectionMapperTest {
	@JsonProperty(value = "name")
	private String name = "Lucy";

	@JsonProperty(value = "address")
	private Address address = new Address();

	@Data
	static class Address {
		@JsonProperty(value = "address")
		private List<String> address = ListBuilder.<String>list().add("Lucerne").build();

		@JsonProperty(value = "phones")
		private Map<String, String> phones = MapBuilder.<String, String>map()
			.with("Home", "12345678")
			.with("Work", "93638734")
			.build();

		@JsonProperty(value = "ids")
		private Set<String> ids = new HashSet<>(List.of("work", "personal"));
	}

	@Data
	@LoggableClass
	static class TestProjection1 implements LogProjection<LogProjectionMapperTest> {
		private String name;

		@Override
		public Class<LogProjectionMapperTest> getProjectedType() {
			return LogProjectionMapperTest.class;
		}
	}

	@Data
	@LoggableClass
	static class TestProjection2 implements LogProjection<LogProjectionMapperTest> {
		@LoggableField
		private String name;

		@Override
		public Class<LogProjectionMapperTest> getProjectedType() {
			return LogProjectionMapperTest.class;
		}
	}

	@Data
	@LoggableClass
	static class TestProjection3 implements LogProjection<LogProjectionMapperTest> {
		@LoggableField
		private String name;

		private TestProjection3() {
		}

		@Override
		public Class<LogProjectionMapperTest> getProjectedType() {
			return LogProjectionMapperTest.class;
		}
	}

	@Data
	@LoggableClass
	static class AddressProjection implements LogProjection<Address> {
		@LoggableField
		private List<String> address;

		@LoggableField
		private Map<String, String> phones;

		@LoggableField
		private Set<String> ids;

		@Override
		public Class<Address> getProjectedType() {
			return Address.class;
		}
	}

	@Data
	@LoggableClass
	static class TestProjection4 implements LogProjection<LogProjectionMapperTest> {
		@LoggableField
		private String name;

		@LoggableField
		private AddressProjection address;

		@Override
		public Class<LogProjectionMapperTest> getProjectedType() {
			return LogProjectionMapperTest.class;
		}
	}

	@Test
	void testGetTypeName() throws NoSuchMethodException {
		assertThat(LogProjectionMapper.getTypeName(null)).isEqualTo("null");
		assertThat(LogProjectionMapper.getTypeName("Hello")).isEqualTo("java.lang.String");

		Method method = Address.class.getDeclaredMethod("setPhones", Map.class);
		assertThat(LogProjectionMapper.getTypeName(method.getGenericParameterTypes()[0])).isEqualTo("java.util.Map<java.lang.String, java.lang.String>");
	}

	@Test
	void mapTargetNoLoggableFields() {
		TestProjection1 testProjection = LogProjectionMapper.map(this, TestProjection1.class);

		assertThat(testProjection.getName()).isNull();
	}

	@Test
	void mapTargetLoggableFields() {
		TestProjection2 testProjection = LogProjectionMapper.map(this, TestProjection2.class);

		assertThat(testProjection.getName()).isEqualTo(name);
	}

	@Test
	void mapTargetNoPublicConstructor() {
		assertThatThrownBy(() -> LogProjectionMapper.map(this, TestProjection3.class))
			.isExactlyInstanceOf(IllegalArgumentException.class)
			.hasMessage("Target LogProjection class(ie.bitstep.mango.spring.logging.internal.LogProjectionMapperTest.TestProjection3) must have a public no-arg constructor");
	}

	@Test
	void mapTargetNestedLogProjection() {
		TestProjection4 testProjection = LogProjectionMapper.map(this, TestProjection4.class);

		assertThat(testProjection.getName()).isEqualTo(name);
		assertThat(testProjection.getAddress().getAddress()).isEqualTo(address.getAddress());
		assertThat(testProjection.getAddress().getPhones()).isEqualTo(address.getPhones());

		assertThat(testProjection.getAddress().getAddress()).isNotSameAs(address.getAddress());
		assertThat(testProjection.getAddress().getPhones()).isNotSameAs(address.getPhones());
	}
}