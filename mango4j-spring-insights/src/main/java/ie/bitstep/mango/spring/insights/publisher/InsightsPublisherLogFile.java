package ie.bitstep.mango.spring.insights.publisher;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import ie.bitstep.mango.spring.insights.internal.AppInsight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.entries;

public class InsightsPublisherLogFile implements InsightsPublisher {

	private final Logger logger = LoggerFactory.getLogger(InsightsPublisherLogFile.class);

	private final ObjectMapper objectMapper = JsonMapper.builder()
			.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
			.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
			.build();

	@Override
	public void publish(AppInsight insight) {
		Map<String, Object> msg = new LinkedHashMap<>();
		msg.put("Insight", objectMapper.convertValue(insight, Map.class));
		logger.info(null, entries(msg));
	}
}
