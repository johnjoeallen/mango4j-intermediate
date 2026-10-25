package ie.bitstep.mango.spring.crypto.caas.delegate.net;

import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static java.lang.System.Logger.Level.INFO;

public class CaasRestTemplate extends RestTemplate {

	private static System.Logger log = System.getLogger(CaasRestTemplate.class.getName());

	private final CloseableHttpClient httpClient;

	private ConnectionPoolMonitor connectionPoolMonitor;

	public CaasRestTemplate(CloseableHttpClient httpClient) {
		setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
		this.httpClient = httpClient;
	}

	public void setConnectionPoolMonitor(ConnectionPoolMonitor connectionPoolMonitor) {
		this.connectionPoolMonitor = connectionPoolMonitor;
	}

	@PreDestroy
	public void destroy() {
		log.log(INFO, "Shutting down");
		if (connectionPoolMonitor != null) {
			connectionPoolMonitor.close();
		}
		try {
			httpClient.close();
		} catch (IOException e) {
			log.log(INFO, "Failed to close the HttpClient", e);
		}
	}
}
