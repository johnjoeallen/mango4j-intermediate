package ie.bitstep.mango.spring.observe.observers.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ie.bitstep.mango.spring.observe.consumers.base.Telemetry;
import ie.bitstep.mango.spring.observe.context.Observation;
import ie.bitstep.mango.spring.observe.observers.Observer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * <p>Creates a list of Telemetry objects and calls any registered consumers for each one</p>
 *
 * <p>There is guaranteed to be at least one telemetry object in addition to a series of
 * permuted telemetry objects based on the @ObservationTags annotation</p>
 */
@Component
public class TelemetryObserver extends Observer<List<Telemetry>> {
	public static final int MAX_ELAPSED_TIME = 5000;
	public static final String RTT_RANGE = "rtt-range";

	private final ObjectMapper objectMapper = getObjectMapper();

	static ObjectMapper getObjectMapper() {
		return new ObjectMapper()
			.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.registerModule(new JavaTimeModule())
			.registerModule(new Jdk8Module());
	}

	public TelemetryObserver(List<Consumer<List<Telemetry>>> consumers) {
		super(consumers);
	}

	@Override
	public void onEntry(Observation observation) {
		// NOSONAR: DO nothing
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onExit(Observation observation) {
		List<Telemetry> telemetries = new ArrayList<>();
		Map<String, String> tags = buildTagsWithSegmentedExecutionTime(observation);

		telemetries.add(new Telemetry(
				observation,
				objectMapper.convertValue(tags, Map.class)
			)
		);

		int start = 0;
		for (String permutation: observation.permutations()) { // NOSONAR
			createPermutations(telemetries, observation, tags, start++, observation.permutations());
		}

		consume(telemetries);
	}

	private void createPermutations(List<Telemetry> telemetries, Observation observation, Map<String, String> tags, int start, List<String> permutations) {
		Set<String> toBlank = new HashSet<>();

		for (int tagIndex = start; tagIndex < permutations.size(); tagIndex++) {
			toBlank.add(permutations.get(tagIndex));

			telemetries.add(getTelemetry(observation, tags, toBlank));
		}
	}

	@SuppressWarnings("unchecked")
	private Telemetry getTelemetry(Observation observation, Map<String, String> tags, Set<String> toBlank) {
		return new Telemetry(
			observation,
			objectMapper.convertValue(buildPermutedTags(toBlank, tags), Map.class)
		);
	}

	static Map<String, String> buildTagsWithSegmentedExecutionTime(Observation observation) {
		if (observation.tags().containsKey(RTT_RANGE)) {
			Map<String, String> tags = new HashMap<>(observation.tags());

			long executionTimeMills = observation.roundTripTime();
			long from = 0L;
			long to = 0L;
			long elapsed = 1 ;

			while (to == 0L && lessThanMax(elapsed)) {
				if (withinRange(executionTimeMills, elapsed)) {
					to = elapsed;
				} else {
					from = elapsed;
				}

				elapsed *= 2;
			}

			if (to == 0L) {
				tags.put(RTT_RANGE, MAX_ELAPSED_TIME + "ms+");
			} else {
				tags.put(RTT_RANGE, from + "-" + to);
			}

			return tags;
		}

		return observation.tags();
	}

	static boolean withinRange(long executionTimeMills, long elapsed) {
		return executionTimeMills < elapsed;
	}

	static boolean lessThanMax(long elapsed) {
		return elapsed < MAX_ELAPSED_TIME;
	}

	static Map<String, String> buildPermutedTags(Set<String> toBlank, Map<String, String> tags) {
		Map<String, String> permutedTags = new HashMap<>();

		for (Map.Entry<String, String> tag : tags.entrySet()) {
			if (!toBlank.contains(tag.getKey())) {
				permutedTags.put(tag.getKey(), tag.getValue());
			} else {
				permutedTags.put(tag.getKey(), "");
			}
		}

		return permutedTags;
	}
}
