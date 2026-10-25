package com.mastercard.commons.mango.observe.consumers.base;

import com.mastercard.commons.mango.collections.MapBuilder;
import com.mastercard.commons.mango.observe.context.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.mastercard.commons.mango.observe.context.Observation.RTT;
import static com.mastercard.commons.mango.observe.context.ObservationContext.LOG_KEY_TIMESTAMP;
import static com.mastercard.commons.mango.observe.observers.base.TelemetryObserver.RTT_RANGE;
import static net.logstash.logback.argument.StructuredArguments.entries;

@Component
public class TelemetryLogConsumer implements Consumer<List<Telemetry>> {
	public static final Logger LOGGER = LoggerFactory.getLogger(TelemetryLogConsumer.class);
	public static final String TELEMETRY = "telemetry";
	public static final String ID = "id";
	public static final String LABEL = "label";
	public static final String TAGS = "tags";

	Logger getLogger(Observation observation) {
		return (observation != null && observation.methodInfo() != null) ? observation.methodInfo().logger() : LOGGER;
	}

	/**
	 * Log the first Telemetry object (all tags set, ie; no permutations)
	 *
	 * @param telemetries a list of Telemetry objects created from the Observation
	 */
	@Override
	public void accept(List<Telemetry> telemetries) {
		Telemetry telemetry = telemetries.get(0);
		Map<String, Object> tags = new HashMap<>(telemetry.tags());
		MapBuilder<String, Object> mapBuilder = new MapBuilder<>(new HashMap<>());

		tags.remove(RTT_RANGE);
		tags.put(RTT, telemetry.observation().roundTripTime());

		Map<String, Object> msg = mapBuilder
			.with(TELEMETRY, MapBuilder.map(new HashMap<>())
				.with(ID, telemetry.observation().observationId())
				.with(LABEL, telemetry.observation().label())
				.with(LOG_KEY_TIMESTAMP, Instant.ofEpochMilli(telemetry.observation().startTimeMillis()).toString())
				.with(TAGS, tags)
				.build())
			.build();

		if (telemetry.observation().throwable() != null) {
			getLogger(telemetry.observation()).error(null, entries(msg));
		} else {
			getLogger(telemetry.observation()).info(null, entries(msg));
		}
	}
}
