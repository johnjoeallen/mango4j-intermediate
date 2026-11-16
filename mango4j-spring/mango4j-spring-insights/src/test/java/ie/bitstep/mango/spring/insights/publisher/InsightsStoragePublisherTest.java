package ie.bitstep.mango.spring.insights.publisher;

import ie.bitstep.mango.spring.insights.internal.AppInsight;
import ie.bitstep.mango.spring.insights.internal.storage.InsightsStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class InsightsStoragePublisherTest {

	@InjectMocks
	private InsightsStoragePublisher insightsStoragePublisher;

	@Mock
	private InsightsStorage mockStorageService;

	private final AppInsight testInsight = new AppInsight();

	@Test
	void publish() {
		// when
		insightsStoragePublisher.publish(testInsight);

		// then
		then(mockStorageService).should().store(testInsight);
	}
}