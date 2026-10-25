package ie.bitstep.mango.spring.observe.observers.base;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import ie.bitstep.mango.spring.observe.consumers.base.Log;
import ie.bitstep.mango.spring.observe.context.Observation;
import ie.bitstep.mango.spring.observe.observers.Observer;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class LogObserver extends Observer<Log> {
	private final ObjectMapper objectMapper = getObjectMapper();

	static ObjectMapper getObjectMapper() {
		return JsonMapper.builder()
			.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
			.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
			.build();
	}

	public LogObserver(List<Consumer<Log>> consumers) {
		super(consumers);
	}

	public void onEntry(Observation observation) {
		// NOSONAR: DO nothing
	}

	@Override
	public void onExit(Observation observation) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("observation", objectMapper.convertValue(observation, Map.class));
		consume(new Log(observation, data));
	}
}
