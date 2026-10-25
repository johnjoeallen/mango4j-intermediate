package ie.bitstep.mango.spring.insights.internal.storage;

import ie.bitstep.mango.spring.insights.internal.AppInsight;

import java.time.Duration;

public interface InsightsStorage {

	void store(AppInsight insight);

	int purge(Duration retentionDuration, String serviceId, int limit, Duration maxTime);
}
