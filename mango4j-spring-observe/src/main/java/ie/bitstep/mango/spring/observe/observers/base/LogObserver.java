package ie.bitstep.mango.spring.observe.observers.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ie.bitstep.mango.collections.MapBuilder;
import ie.bitstep.mango.spring.observe.consumers.base.Log;
import ie.bitstep.mango.spring.observe.context.Observation;
import ie.bitstep.mango.spring.observe.observers.Observer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class LogObserver extends Observer<Log> {
	private final ObjectMapper objectMapper = getObjectMapper();

	static ObjectMapper getObjectMapper() {
		return new ObjectMapper()
			.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.registerModule(new JavaTimeModule())
			.registerModule(new Jdk8Module());
	}

	public LogObserver(List<Consumer<Log>> consumers) {
		super(consumers);
	}

	public void onEntry(Observation observation) {
		// NOSONAR: DO nothing
	}

	@Override
	public void onExit(Observation observation) {
		Log log = new Log(observation,
			MapBuilder.<String, Object>map()
				.with(
					"observation",
					objectMapper.convertValue(observation, Map.class))
				.build());
		consume(log);
	}
}
