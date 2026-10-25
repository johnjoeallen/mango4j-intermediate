package com.mastercard.commons.mango.insights.publisher;

import com.mastercard.commons.mango.insights.internal.AppInsight;

public interface InsightsPublisher {
	void publish(AppInsight insight);
}
