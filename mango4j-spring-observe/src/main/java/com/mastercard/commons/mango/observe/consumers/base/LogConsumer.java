package com.mastercard.commons.mango.observe.consumers.base;

import com.mastercard.commons.mango.observe.context.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static net.logstash.logback.argument.StructuredArguments.entries;

@Component
public class LogConsumer implements Consumer<Log> {
	public static final Logger LOGGER = LoggerFactory.getLogger(LogConsumer.class);

	Logger getLogger(Observation observation) {
		return (observation != null && observation.methodInfo() != null) ? observation.methodInfo().logger() : LOGGER;
	}

	@Override
	public void accept(Log log) {
		if (log.observation().throwable() != null) {
			getLogger(log.observation()).error(null, entries(log.data()));
		} else {
			getLogger(log.observation()).info(null, entries(log.data()));
		}
	}
}
