package ie.bitstep.mango.spring.observe.consumers.base;

import ie.bitstep.mango.spring.observe.context.Observation;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(fluent = true)
public class Log {
	private final Observation observation;
	private final Map<String, Object> data;
}
