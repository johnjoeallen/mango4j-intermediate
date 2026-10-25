package ie.bitstep.mango.spring.observe.context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.internal.LoggerMapper;
import ie.bitstep.mango.spring.observe.annotations.ObservationTags;
import ie.bitstep.mango.spring.observe.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Remove direct use of Jackson once the UltraMapper is available
 */

@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@FieldNameConstants
@Builder
public class Observation {

	public static final String RTT = "rtt";

	@JsonProperty("values")
	@Builder.Default
	private Map<String, Object> values = new LinkedHashMap<>();

	@JsonProperty("tags")
	@Builder.Default
	private Map<String, String> tags = new LinkedHashMap<>();

	@Builder.Default
	private List<String> permutations = new ArrayList<>();

	@JsonProperty("observationId")
	@Builder.Default
	private String observationId = UUID.randomUUID().toString(); // ex-logId 76fb5ee5-2557-41f7-a314-676dbda34195

	@JsonProperty("name")
	private String name;

	@JsonProperty("serviceId")
	private String serviceId;

	@JsonProperty("node")
	private String node; // small-green-cat

	@JsonIgnore
	private String correlationid;

	@JsonProperty("method")
	private MethodInfo methodInfo;

	@JsonProperty("startTimeMillis")
	private long startTimeMillis;

	@JsonProperty("endTimeMillis")
	private long endTimeMillis;

	@JsonProperty(RTT)
	private long roundTripTime;

	@JsonProperty("status")
	private Status status;

	@JsonProperty("user")
	private String user;

	@JsonProperty("context")
	private Map<String, Object> context; // Open - but we could restrict a bit more (from insights)

	@JsonProperty("params")
	@Builder.Default
	private Map<String, Object> params = new LinkedHashMap<>();

	@JsonProperty("observations")
	@Builder.Default
	private List<Observation> observations = new ArrayList<>();

	@JsonIgnore
	@Builder.Default
	WeakReference<Observation> parent = new WeakReference<>(null);

	@JsonProperty("returned")
	private Object returned;

	private Throwable throwable;

	public Observation tag(String name, Object value) {
		tags.computeIfPresent(name, (k, v) -> String.valueOf(value));

		return this;
	}

	@JsonProperty("label")
	public String label() {
		return methodInfo.getSimpleClassName() + "/" + name();
	}


	public void addChild(Observation observation) {
		observation.parent = new WeakReference<>(this);
		observations.add(observation);
	}

	public void removeChild(Observation child) {
		observations.remove(child);
	}

	@JsonIgnore
	public List<Observation> getChildren() {
		return observations;
	}

	public Observation putValue(String key, Object value) {
		// ideally, we need to clone the value to ensure its immutability,
		// but for now we'll just convert the value into the map which is mostly immutable
		values.put(key, LoggerMapper.toLoggable(value));
		return this;
	}

	public Object getValue(String key) {
		return values.get(key);
	}

	public boolean containsKey(String key) {
		return values.containsKey(key);
	}

	public Observation addValue(String key, Object value) {
		@SuppressWarnings("unchecked")
		List<Object> list = (List<Object>) values.computeIfAbsent(key, k -> new ArrayList<>());
		list.add(LoggerMapper.toLoggable(value));
		return this;
	}

	public Observation mapValue(String key, String name, Object value) {
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) values.computeIfAbsent(key, k -> new LinkedHashMap<>());
		map.put(name, LoggerMapper.toLoggable(value));
		return this;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public Observation putThrowable(Throwable throwable) {
		this.throwable = throwable;
		return this;
	}

	public void setAllowedTags(ObservationTags observationTags) {
		for (String tag : observationTags.base()) {
			tags.put(tag, "");
		}

		for (String tag : observationTags.permutations()) {
			tags.put(tag, "");
		}
	}

	public void setTagPermutations(ObservationTags observationTags) {
		permutations.addAll(Arrays.asList(observationTags.permutations()));
	}
}
