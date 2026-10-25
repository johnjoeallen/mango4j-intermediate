package com.mastercard.commons.mango.insights.publisher;

import com.mastercard.commons.mango.insights.internal.AppInsight;
import com.mastercard.commons.mango.insights.internal.storage.InsightsStorage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InsightsStoragePublisher implements InsightsPublisher {

	private final InsightsStorage storageService;

	@Override
	public void publish(AppInsight insight) {
		storageService.store(insight);
	}

}
