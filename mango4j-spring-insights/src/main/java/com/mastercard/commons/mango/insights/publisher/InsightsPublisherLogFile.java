package com.mastercard.commons.mango.insights.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mastercard.commons.mango.collections.MapBuilder;
import com.mastercard.commons.mango.insights.internal.AppInsight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static net.logstash.logback.argument.StructuredArguments.entries;

public class InsightsPublisherLogFile implements InsightsPublisher {

	private final Logger logger = LoggerFactory.getLogger(InsightsPublisherLogFile.class);

	private final ObjectMapper objectMapper = new ObjectMapper()
			.enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
			.registerModule(new JavaTimeModule())
			.registerModule(new Jdk8Module());

	@Override
	public void publish(AppInsight insight) {
		logger.info(null, entries(
				MapBuilder.<String, Object>map().with("Insight", objectMapper.convertValue(insight, Map.class)).build()
		));
	}
}
