package com.mastercard.commons.mango.insights.publisher;

import com.mastercard.commons.mango.insights.internal.AppInsight;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;

@RequiredArgsConstructor
public class InsightsAsyncPublisher implements InsightsPublisher {

	private final InsightsPublisher delegate;
	private final TaskExecutor asyncExecutor;

	@Override
	public void publish(AppInsight insight) {
		asyncExecutor.execute(() -> delegate.publish(insight));
	}
}
