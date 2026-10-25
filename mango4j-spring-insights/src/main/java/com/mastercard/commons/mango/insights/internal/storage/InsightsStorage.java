package com.mastercard.commons.mango.insights.internal.storage;

import com.mastercard.commons.mango.insights.internal.AppInsight;

import java.time.Duration;

public interface InsightsStorage {

	void store(AppInsight insight);

	int purge(Duration retentionDuration, String serviceId, int limit, Duration maxTime);
}
