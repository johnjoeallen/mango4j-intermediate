package ie.bitstep.mango.spring.insights.publisher;

import ie.bitstep.mango.spring.insights.internal.AppInsight;
import ie.bitstep.mango.spring.insights.internal.storage.InsightsStorage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InsightsStoragePublisher implements InsightsPublisher {

	private final InsightsStorage storageService;

	@Override
	public void publish(AppInsight insight) {
		storageService.store(insight);
	}

}
