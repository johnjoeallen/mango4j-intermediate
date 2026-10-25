package ie.bitstep.mango.spring.crypto.caas.delegate.net;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Field;

import static java.lang.System.Logger.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class CaasRestTemplateTest {

	@Mock
	private CloseableHttpClient mockHttpClient;

	@Mock
	private ConnectionPoolMonitor mockMonitor;

	@Mock
	private System.Logger mockLogger;

	@InjectMocks
	private CaasRestTemplate caasRestTemplate;

	@BeforeEach
	void setup() {
		ReflectionTestUtils.setField(caasRestTemplate, "log", mockLogger);
	}

	@Test
	void constructor() throws NoSuchFieldException, IllegalAccessException {
		caasRestTemplate = new CaasRestTemplate(mockHttpClient);

		Field requestFactoryField = CaasRestTemplate.class.getSuperclass().getSuperclass().getSuperclass().getDeclaredField("requestFactory");
		requestFactoryField.setAccessible(true);
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = (HttpComponentsClientHttpRequestFactory) requestFactoryField.get(caasRestTemplate);
		assertThat(clientHttpRequestFactory).isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
		assertThat(clientHttpRequestFactory.getHttpClient()).isSameAs(mockHttpClient);
	}

	@Test
	void destroyWithoutMonitor() throws Exception {
		caasRestTemplate.destroy();

		then(mockHttpClient).should().close();
		then(mockLogger).should().log(INFO, "Shutting down");
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void destroyWithMonitor() throws Exception {
		caasRestTemplate.setConnectionPoolMonitor(mockMonitor);

		caasRestTemplate.destroy();

		then(mockMonitor).should().close();
		then(mockHttpClient).should().close();
		then(mockLogger).should().log(INFO, "Shutting down");
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void destroyExceptionOnClose() throws Exception {
		caasRestTemplate.setConnectionPoolMonitor(mockMonitor);
		IOException cause = new IOException();
		willThrow(cause).given(mockHttpClient).close();

		caasRestTemplate.destroy();

		then(mockMonitor).should().close();
		then(mockHttpClient).should().close();
		then(mockLogger).should().log(INFO, "Shutting down");
		then(mockLogger).should().log(INFO, "Failed to close the HttpClient", cause);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}
}