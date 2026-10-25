package ie.bitstep.mango.spring.insights.internal;

import ie.bitstep.mango.spring.insights.annotations.Insight;
import ie.bitstep.mango.spring.insights.consts.InsightType;
import ie.bitstep.mango.spring.insights.initialiser.InsightsContextInitialiser;
import ie.bitstep.mango.spring.insights.publisher.InsightsPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class InsightsProviderTest {
	public static final String MY_SERVICE_ID = "my service id";
	public static final String MY_CORRELATION_ID = "my correlation id";

	@Mock
	private InsightsPublisher publisher;

	@Mock
	private InsightsContextInitialiser insightsContextInitialiser1;

	@Mock
	private InsightsContextInitialiser insightsContextInitialiser2;

	@Spy
	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	@Spy
	private List<InsightsContextInitialiser> insightsContextInitialisers = new ArrayList<>();

	@Spy
	private List<InsightsPublisher> insightsPublishers = new ArrayList<>();

	@Captor
	private ArgumentCaptor<AppInsight> appInsightArgumentCaptor;

	@InjectMocks
	private InsightsProvider insightsProvider;

	@BeforeEach
	void setup() {
		insightsContextInitialisers.add(insightsContextInitialiser2);
		insightsContextInitialisers.add(insightsContextInitialiser1);
		insightsPublishers.add(publisher);

		ReflectionTestUtils.setField(insightsProvider, "insightsPublishers", insightsPublishers);
		ReflectionTestUtils.setField(insightsProvider, "insightsContextInitialisers", insightsContextInitialisers);
	}

	@Test
	void postConstructorTest() {
		ArrayList<InsightsContextInitialiser> contextInitialisersMock = mock(ArrayList.class);
		ReflectionTestUtils.setField(insightsProvider, "insightsContextInitialisers", contextInitialisersMock);
		insightsProvider.setup();

		verify(contextInitialisersMock, times(1)).sort(AnnotationAwareOrderComparator.INSTANCE);
		verifyNoMoreInteractions(contextInitialisersMock);
	}


	@Test
	@Insight(type = InsightType.API, action = "testNewInsight")
	@SuppressWarnings("java:S5786")
	public void testNewInsight() throws NoSuchMethodException {
		Method method = getClass().getMethod("testNewInsight");

		AppInsight appInsight = insightsProvider.newInsight(method);

		assertThat(appInsight).isNotNull();
		assertThat(appInsight.getType()).isEqualTo(InsightType.API);
		assertThat(appInsight.getAction()).isEqualTo("testNewInsight");
		verify(insightsContextInitialiser1, times(1)).initialise(appInsightArgumentCaptor.capture());
		verify(insightsContextInitialiser2, times(1)).initialise(appInsightArgumentCaptor.capture());
		assertThat(appInsightArgumentCaptor.getValue().getType()).isEqualTo(InsightType.API);
		assertThat(appInsightArgumentCaptor.getValue().getAction()).isEqualTo("testNewInsight");
		verifyNoInteractions(taskExecutor);
		verifyNoInteractions(publisher);
	}

	@Test
	@Insight(type = InsightType.API, action = "testPublishInsight")
	@SuppressWarnings("java:S5786")
	public void testPublishInsight() {
		AppInsight appInsight = new AppInsight(InsightType.API, "testPublishInsight");

		insightsProvider.publish(appInsight);

		verify(taskExecutor, times(1)).execute(any());
		verifyNoMoreInteractions(taskExecutor);
		verify(publisher, times(1)).publish(appInsight);
	}
}