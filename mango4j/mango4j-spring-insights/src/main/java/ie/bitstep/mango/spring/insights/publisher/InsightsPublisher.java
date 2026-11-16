package ie.bitstep.mango.spring.insights.publisher;

import ie.bitstep.mango.spring.insights.internal.AppInsight;

public interface InsightsPublisher {
	void publish(AppInsight insight);
}
