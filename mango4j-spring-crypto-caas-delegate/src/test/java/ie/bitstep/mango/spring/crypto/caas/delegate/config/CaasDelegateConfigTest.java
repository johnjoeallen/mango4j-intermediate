package ie.bitstep.mango.spring.crypto.caas.delegate.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CaasDelegateConfigTest {

	private static final int MAX_CONNECTIONS = 100;
	private static final int MAX_PER_ROUTE = 10;
	private static final Duration DURATION_30_SECONDS = Duration.ofSeconds(30);
	private static final Duration DURATION_60_SECONDS = Duration.ofSeconds(60);
	private static final Duration DURATION_5_MINUTES = Duration.ofMinutes(5);
	private static final Duration DURATION_15_SECONDS = Duration.ofSeconds(15);
	private static final String KEYSTORE_PASSWORD = "TestKeyStorePassword";
	private static final String TRUSTSTORE_PASSWORD = "TestTrustStorePassword";
	private static final String PROTOCOL = "TestProtocol";
	private static final String CLIENT_ALIAS = "TestClientAlias";
	private static final String KEYSTORE_LOCATION = "/test/path/to/keystore";
	private static final String TRUSTSTORE_LOCATION = "/test/path/to/truststore";
	private static final String CAAS_ENDPOINT = "https://caas.example.com";

	private CaasDelegateConfig config;

	@BeforeEach
	void setUp() {
		config = new CaasDelegateConfig();
	}

	@Test
	void testMaxConnections() {
		config.setMaxConnections(MAX_CONNECTIONS);

		assertThat(config.getMaxConnections()).isEqualTo(MAX_CONNECTIONS);
	}

	@Test
	void testMaxPerRouteConnections() {
		config.setMaxPerRouteConnections(MAX_PER_ROUTE);

		assertThat(config.getMaxPerRouteConnections()).isEqualTo(MAX_PER_ROUTE);
	}

	@Test
	void testValidateAfterInactivity() {
		config.setValidateAfterInactivity(DURATION_30_SECONDS);

		assertThat(config.getValidateAfterInactivity()).isEqualTo(DURATION_30_SECONDS);
	}

	@Test
	void testConnectionTimeout() {
		config.setConnectionTimeout(DURATION_60_SECONDS);

		assertThat(config.getConnectionTimeout()).isEqualTo(DURATION_60_SECONDS);
	}

	@Test
	void testMaxKeepAlive() {
		config.setMaxKeepAlive(DURATION_5_MINUTES);

		assertThat(config.getMaxKeepAlive()).isEqualTo(DURATION_5_MINUTES);
	}

	@Test
	void testPoolStats() {
		config.setPoolStats(DURATION_15_SECONDS);

		assertThat(config.getPoolStats()).isEqualTo(DURATION_15_SECONDS);
	}

	@Test
	void testKeyStorePassword() {
		config.setKeyStorePassword(KEYSTORE_PASSWORD);

		assertThat(config.getKeyStorePassword()).isEqualTo(KEYSTORE_PASSWORD);
	}

	@Test
	void testTrustStorePassword() {
		config.setTrustStorePassword(TRUSTSTORE_PASSWORD);

		assertThat(config.getTrustStorePassword()).isEqualTo(TRUSTSTORE_PASSWORD);
	}

	@Test
	void testProtocol() {
		config.setProtocol(PROTOCOL);

		assertThat(config.getProtocol()).isEqualTo(PROTOCOL);
	}

	@Test
	void testClientAlias() {
		config.setClientAlias(CLIENT_ALIAS);

		assertThat(config.getClientAlias()).isEqualTo(CLIENT_ALIAS);
	}

	@Test
	void testKeyStoreLocation() {
		config.setKeyStoreLocation(KEYSTORE_LOCATION);

		assertThat(config.getKeyStoreLocation()).isEqualTo(KEYSTORE_LOCATION);
	}

	@Test
	void testTrustStoreLocation() {
		config.setTrustStoreLocation(TRUSTSTORE_LOCATION);

		assertThat(config.getTrustStoreLocation()).isEqualTo(TRUSTSTORE_LOCATION);
	}

	@Test
	void testCaasEndpointBase() {
		config.setCaasEndpointBase(CAAS_ENDPOINT);
		assertThat(config.getCaasEndpointBase()).isEqualTo(CAAS_ENDPOINT);
	}

}