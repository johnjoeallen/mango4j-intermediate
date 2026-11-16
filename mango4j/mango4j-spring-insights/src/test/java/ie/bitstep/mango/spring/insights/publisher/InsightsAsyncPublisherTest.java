package ie.bitstep.mango.spring.insights.publisher;

import ie.bitstep.mango.spring.insights.internal.AppInsight;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class InsightsAsyncPublisherTest {

	@InjectMocks
	private InsightsAsyncPublisher insightsAsyncPublisher;

	@Mock
	private InsightsPublisher mockDelegate;
	@Mock
	private TaskExecutor mockAsyncExecutor;
	@Captor

	private ArgumentCaptor<Runnable> runnableCaptor;

	private final AppInsight testInsight = new AppInsight();

	@Test
	void publish() {
		// when
		insightsAsyncPublisher.publish(testInsight);

		// then
		then(mockAsyncExecutor).should().execute(runnableCaptor.capture());
		runnableCaptor.getValue().run();
		then(mockDelegate).should().publish(testInsight);
	}
}