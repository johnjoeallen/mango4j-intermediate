package ie.bitstep.mango.spring.insights.internal;

import ie.bitstep.mango.spring.insights.annotations.Insight;
import ie.bitstep.mango.spring.insights.initialiser.InsightsContextInitialiser;
import ie.bitstep.mango.spring.insights.publisher.InsightsPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.List;

@Slf4j
@Component
public class InsightsProvider {
	private final List<InsightsContextInitialiser> insightsContextInitialisers;
	private final List<InsightsPublisher> insightsPublishers;
	private final TaskExecutor asyncExecutor;

	@PostConstruct
	public void setup() {
		insightsContextInitialisers.sort(AnnotationAwareOrderComparator.INSTANCE);
	}

	public InsightsProvider(List<InsightsContextInitialiser> insightsContextInitialisers, List<InsightsPublisher> insightsPublishers, @Qualifier("applicationTaskExecutor") TaskExecutor asyncExecutor) {
		this.insightsContextInitialisers = insightsContextInitialisers;
		this.insightsPublishers = insightsPublishers;
		this.asyncExecutor = asyncExecutor;
	}

	public AppInsight newInsight(Method method) {
		AppInsight appInsight = new AppInsight(method.getAnnotation(Insight.class));

		insightsContextInitialisers.stream().forEach(insightsContextInitialiser ->
			insightsContextInitialiser.initialise(appInsight)
		);

		return appInsight;
	}

	public void publish(AppInsight appInsight) {
		for (InsightsPublisher publisher : insightsPublishers) {
			try {
				asyncExecutor.execute(() -> publisher.publish(appInsight));
			} catch (Exception ex) {
				log.error("Publishing insight has failed within publisher " + publisher.getClass().getCanonicalName(), ex);
			}
		}
	}

}
