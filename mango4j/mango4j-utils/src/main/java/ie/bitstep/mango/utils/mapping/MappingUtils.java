package ie.bitstep.mango.utils.mapping;

import ie.bitstep.mango.utils.mapping.exceptions.MappingException;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class MappingUtils {
	public static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
	};

	private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
			.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
			.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.build();

	/**
	 * Prevents instantiation.
	 */
	private MappingUtils() {
		// NOSONAR
	}

	/**
	 * Convert an object to a generic Map
	 * This is based on ability to serialize and deserialize into json the object to be converted
	 * json limitations apply (note the serialization and deserialization if the object to be converted has jackson annotations e.g: JsonIgnore, etc.)
	 *
	 * @param object the object to be mapped
	 * @return A generic map of representing the object
	 * @throws MappingException as runtime exception in case there is an issue processing JSON
	 */
	public static Map<String, Object> fromObjectToMap(Object object) {
		try {
			String json = JSON_MAPPER.writeValueAsString(object);
			return JSON_MAPPER.readValue(json, MAP_TYPE_REFERENCE);
		} catch (JacksonException e) { // NOSONAR this should not happen at this stage, however gracefully handle it
			throw new MappingException(e);
		}
	}

	/**
	 * Convert an object to a JSON string
	 * @param object the object to be mapped
	 * @return A JSON string representation of the object
	 * @throws MappingException as runtime exception in case there is an issue processing JSON
	 */
	public static String fromObjectToJson(Object object) {
		try {
			return JSON_MAPPER.writeValueAsString(object);
		} catch (JacksonException e) { // NOSONAR this should not happen at this stage, however gracefully handle it
			throw new MappingException(e);
		}
	}

	/**
	 * Convert a JSON string to a map
	 * @param json The JSON string
	 * @return The map equivalent of the JSON string
	 * @throws MappingException as runtime exception in case there is an issue processing JSON
	 */
	public static Map<String, Object> fromJsonToMap(String json) {
		try {
			if (StringUtils.isEmpty(json)) {
				return new LinkedHashMap<>();
			} else {
					return JSON_MAPPER.readValue(json, MAP_TYPE_REFERENCE);
			}
		} catch (JacksonException e) { // NOSONAR this should not happen at this stage, however gracefully handle it
			throw new MappingException(e);
		}
	}
}
