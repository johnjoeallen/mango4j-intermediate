package com.mastercard.commons.mango4j.crypto.caas.delegate.config;

import com.mastercard.commons.mango4j.crypto.caas.delegate.net.CaasRestTemplate;
import com.mastercard.commons.mango4j.crypto.caas.delegate.net.CappedKeepAliveStrategy;
import com.mastercard.commons.mango4j.crypto.caas.delegate.net.ConnectionPoolMonitor;
import com.mastercard.commons.mango4j.crypto.caas.delegate.ssl.SslFactory;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.web.client.RestTemplate;

import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasConstants.SERVICE_NAME;

@DoNotMutate("Not really testable - Theres' no conditional logic in this class so further abstraction is pointless")
public class RestTemplateBuilder {

	private RestTemplateBuilder() {
		// Suppress default constructor for noninstantiability
	}

	public static RestTemplate build(CaasDelegateConfig config) {
		PoolingHttpClientConnectionManager mgr = poolingConnectionManager(
				new SslFactory(config), config);

		mgr.setDefaultConnectionConfig(ConnectionConfig.custom()
				.setConnectTimeout(Timeout.of(config.getConnectionTimeout()))
				.build());

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.of(config.getConnectionTimeout()))
				.build();

		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(mgr)
				.setDefaultRequestConfig(requestConfig)
				.setKeepAliveStrategy(new CappedKeepAliveStrategy(config.getMaxKeepAlive()))
				.evictExpiredConnections()
				.disableConnectionState()   // to allow MTLS connections to be reusable. See https://issues.apache.org/jira/browse/HTTPASYNC-103
				.disableCookieManagement()
				.build();

		CaasRestTemplate restTemplate = new CaasRestTemplate(httpClient);

		ConnectionPoolMonitor monitor = new ConnectionPoolMonitor(SERVICE_NAME, mgr, config.getPoolStats());
		restTemplate.setConnectionPoolMonitor(monitor);
		monitor.start();

		return restTemplate;
	}

	@SuppressWarnings("deprecation") // will update in future version
	private static PoolingHttpClientConnectionManager poolingConnectionManager(SslFactory sslFactory, CaasDelegateConfig config) {
		SSLConnectionSocketFactory sslConnectionSocketFactory = sslFactory.getSSLConnectionSocketFactory(config.getKeyStorePassword(), config.getTrustStorePassword());

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
				.<ConnectionSocketFactory>create().register("https", sslConnectionSocketFactory)
				.register("http", new PlainConnectionSocketFactory())
				.build();

		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setValidateAfterInactivity(TimeValue.of(config.getValidateAfterInactivity()))
				.build();

		PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		poolingConnectionManager.setMaxTotal(config.getMaxConnections());
		poolingConnectionManager.setDefaultMaxPerRoute(config.getMaxPerRouteConnections());
		poolingConnectionManager.setDefaultConnectionConfig(connectionConfig);
		return poolingConnectionManager;
	}
}
