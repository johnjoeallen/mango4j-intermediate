package com.mastercard.commons.mango.observe.consumers.base;

import com.mastercard.commons.mango.observe.context.Observation;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(fluent = true)
public class Log {
	private final Observation observation;
	private final Map<String, Object> data;
}
