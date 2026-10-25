package ie.bitstep.mango.spring.crypto.caas.delegate.net;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.pool.PoolStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ConnectionPoolMonitorTest {

	private static final Duration TEST_PERIOD = Duration.ofSeconds(30);
	private static final String EXECUTOR_SERVICE_FIELD_NAME = "executorService";
	private static final HttpRoute TEST_ROUTE = new HttpRoute(new HttpHost("host", 80));
	private static final PoolStats TEST_TOTAL_STATS = new PoolStats(1, 2, 3, 4);
	private static final PoolStats TEST_ROUTE_STATS = new PoolStats(5, 6, 7, 8);

	private ConnectionPoolMonitor monitor;

	@Mock
	private PoolingHttpClientConnectionManager mockConnectionManager;

	@Mock
	private ScheduledExecutorService mockExecutorService;

	@Mock
	private System.Logger mockLogger;

	@Mock
	private ScheduledExecutorService mockExecutor;

	@Captor
	private ArgumentCaptor<Runnable> runnableCaptor;


	@BeforeEach
	void setUp() {
		monitor = Mockito.spy(new ConnectionPoolMonitor("CaaS", mockConnectionManager, TEST_PERIOD));
		ReflectionTestUtils.setField(monitor, "logger", mockLogger);
	}

	@Test
	void start() {
		given(monitor.createExecutor()).willReturn(mockExecutorService);
		given(mockConnectionManager.getTotalStats()).willReturn(TEST_TOTAL_STATS);
		given(mockConnectionManager.getRoutes()).willReturn(Collections.singleton(TEST_ROUTE));
		given(mockConnectionManager.getStats(TEST_ROUTE)).willReturn(TEST_ROUTE_STATS);

		monitor.start();

		then(mockExecutorService).should().scheduleAtFixedRate(runnableCaptor.capture(), eq(TEST_PERIOD.getSeconds()), eq(TEST_PERIOD.getSeconds()), eq(TimeUnit.SECONDS));
		assertThat(getExecutorService()).isEqualTo(mockExecutorService);
		Runnable task = runnableCaptor.getValue();
		task.run();
		then(mockLogger).should().log(System.Logger.Level.INFO, "CaaS: Total Pool Stats - Available: 3, Leased: 1, Pending: 2, Max: 4. Route Pool Stats ({}->[http://host:80]) - Available: 7, Leased: 5, Pending: 6, Max: 8.");
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void startWithoutRoutes() {
		given(monitor.createExecutor()).willReturn(mockExecutorService);
		given(mockConnectionManager.getTotalStats()).willReturn(TEST_TOTAL_STATS);
		given(mockConnectionManager.getRoutes()).willReturn(Collections.emptySet());

		monitor.start();

		then(mockExecutorService).should().scheduleAtFixedRate(runnableCaptor.capture(), eq(TEST_PERIOD.getSeconds()), eq(TEST_PERIOD.getSeconds()), eq(TimeUnit.SECONDS));
		assertThat(getExecutorService()).isEqualTo(mockExecutorService);
		Runnable task = runnableCaptor.getValue();
		task.run();
		then(mockLogger).should().log(System.Logger.Level.INFO, "CaaS: Total Pool Stats - Available: 3, Leased: 1, Pending: 2, Max: 4.");
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void close() {
		setExecutorService(mockExecutorService);

		monitor.close();

		then(mockExecutorService).should().shutdownNow();
		assertThat(getExecutorService()).isNull();
	}

	@Test
	void closeWhenNotStarted() {
		monitor.close();

		then(mockExecutorService).shouldHaveNoInteractions();
		then(mockLogger).shouldHaveNoInteractions();
	}

	@Test
	void createExecutor() {
		try (MockedStatic<Executors> scheduledExecutorServiceMockedStatic = Mockito.mockStatic(Executors.class)) {
			scheduledExecutorServiceMockedStatic.when(Executors::newSingleThreadScheduledExecutor).thenReturn(mockExecutor);

			ScheduledExecutorService executor = monitor.createExecutor();

			assertThat(executor).isSameAs(mockExecutor);
			scheduledExecutorServiceMockedStatic.verify(Executors::newSingleThreadScheduledExecutor);
		}
	}

	private ScheduledExecutorService getExecutorService() {
		return (ScheduledExecutorService) ReflectionTestUtils.getField(monitor, EXECUTOR_SERVICE_FIELD_NAME);
	}

	private void setExecutorService(ScheduledExecutorService executorService) {
		ReflectionTestUtils.setField(monitor, EXECUTOR_SERVICE_FIELD_NAME, executorService);
	}
}