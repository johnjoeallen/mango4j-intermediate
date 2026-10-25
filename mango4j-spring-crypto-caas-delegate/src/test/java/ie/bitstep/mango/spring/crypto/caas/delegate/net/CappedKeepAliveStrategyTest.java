package ie.bitstep.mango.spring.crypto.caas.delegate.net;

import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static java.lang.System.Logger.Level.DEBUG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CappedKeepAliveStrategyTest {

	private static final Duration MAX_KEEP_ALIVE_DURATION = Duration.ofMillis(30000);
	private static final TimeValue MAX_KEEP_ALIVE_TIME_VALUE = TimeValue.of(MAX_KEEP_ALIVE_DURATION);

	private final CappedKeepAliveStrategy keepAliveStrategy = new CappedKeepAliveStrategy(MAX_KEEP_ALIVE_TIME_VALUE.toDuration());

	@Mock
	private System.Logger logger;

	private HttpContext httpContext;
	private HttpResponse httpResponse;

	@BeforeEach
	void setup() {
		httpContext = new BasicHttpContext();
		httpResponse = new BasicHttpResponse(200);
		ReflectionTestUtils.setField(keepAliveStrategy, "logger", logger);
	}

	@Test
	void getKeepAliveDurationWhenHeaderNotPresent() {
		TimeValue keepAliveDuration = keepAliveStrategy.getKeepAliveDuration(httpResponse, httpContext);

		assertThat(keepAliveDuration).isEqualTo(MAX_KEEP_ALIVE_TIME_VALUE);
		then(logger).should().log(DEBUG, "Keep-Alive header not present - setting to max {0}ms", MAX_KEEP_ALIVE_DURATION.toMillis());
		then(logger).shouldHaveNoMoreInteractions();
	}

	@Test
	void getKeepAliveDurationWhenHeaderPresent() {
		int timeoutInSeconds = (int) MAX_KEEP_ALIVE_DURATION.toSeconds();
		addKeepAliveHeader(timeoutInSeconds);

		TimeValue keepAliveDuration = keepAliveStrategy.getKeepAliveDuration(httpResponse, httpContext);

		assertThat(keepAliveDuration).isEqualTo(TimeValue.ofSeconds(timeoutInSeconds));
		then(logger).should().log(DEBUG, "keep alive duration {0}ms", TimeValue.ofSeconds(timeoutInSeconds));
		then(logger).shouldHaveNoMoreInteractions();
	}

	@Test
	void getKeepAliveDurationWhenTooHigh() {
		int timeoutInSeconds = 100;
		addKeepAliveHeader(timeoutInSeconds);

		TimeValue keepAliveDuration = keepAliveStrategy.getKeepAliveDuration(httpResponse, httpContext);

		assertThat(keepAliveDuration).isEqualTo(MAX_KEEP_ALIVE_TIME_VALUE);
		then(logger).should().log(DEBUG, "Keep-Alive header value of {0}ms was above the cap of {1}ms - Setting it to the cap", TimeValue.ofSeconds(timeoutInSeconds), MAX_KEEP_ALIVE_DURATION.toMillis());
		then(logger).shouldHaveNoMoreInteractions();
	}

	private void addKeepAliveHeader(int timeoutInSeconds) {
		httpResponse.addHeader("Keep-Alive", "timeout=" + timeoutInSeconds);
	}
}