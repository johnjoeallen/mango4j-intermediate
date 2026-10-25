package com.mastercard.commons.mango4j.crypto.caas.delegate.ssl;

import com.mastercard.commons.mango4j.crypto.caas.delegate.config.CaasDelegateConfig;
import com.mastercard.commons.mango4j.crypto.caas.delegate.exceptions.CaasConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SystemStubsExtension.class)
class SslFactoryTest {

	private static final String TEST_PROTOCOL = "TLSv1.2";
	private static final String TEST_CLIENT_ALIAS = "client-common";
	private static final String TEST_KEY_STORE_FILE = "test-dev-keystore.jks";
	private static final String TRUST_STORE_FILE_NAME = "src/test/resources/devpaas-truststore.jks";
	private static final String STORE_PASSWORD = "password";
	private static final String STORE_PASSWORD_SYSTEM_ENV_NAME = "test_store_password_system_env_name";
	private static final String STORE_PASSWORD_64 = "cGFzc3dvcmQ=";

	@SystemStub
	private final EnvironmentVariables environmentVariables = new EnvironmentVariables();

	private CaasDelegateConfig caasDelegateConfig;

	private SslFactory sslFactory;

	@BeforeEach
	void setUp() {
		caasDelegateConfig = new CaasDelegateConfig();
		caasDelegateConfig.setProtocol(TEST_PROTOCOL);
		caasDelegateConfig.setClientAlias(TEST_CLIENT_ALIAS);
		caasDelegateConfig.setKeyStoreLocation(TEST_KEY_STORE_FILE);
		caasDelegateConfig.setTrustStoreLocation(TRUST_STORE_FILE_NAME);
		sslFactory = new SslFactory(caasDelegateConfig);
	}

	@Test
	void getHostnameVerifier() {
		HostnameVerifier actual = sslFactory.getHostnameVerifier();

		assertThat(actual).isInstanceOf(DefaultHostnameVerifier.class);
	}

	@Test
	void getTrustManagersWhenFileKeyStore() {
		TrustManager[] trustManagers = sslFactory.getTrustManagers(STORE_PASSWORD.toCharArray());

		// then - verify that the store was loaded with the expected certificates
		X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
		assertThat(trustManager.getAcceptedIssuers()).hasSize(9);
	}

	@Test
	void getTrustManagersWhenNotSpecified() {
		ReflectionTestUtils.setField(sslFactory, "trustStoreLocation", null);

		TrustManager[] trustManagers = sslFactory.getTrustManagers(null);

		// then - verify that the store was loaded with the expected certificates
		X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
		assertThat(trustManager.getAcceptedIssuers()).hasSizeGreaterThan(1);
	}

	@Test
	void getTrustManagersWhenTrustStoreLocationIsEmpty() {
		ReflectionTestUtils.setField(sslFactory, "trustStoreLocation", null);

		TrustManager[] trustManagers = sslFactory.getTrustManagers("Password".toCharArray());

		// then - verify that the store was loaded with the expected certificates
		X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
		assertThat(trustManager.getAcceptedIssuers()).hasSizeGreaterThan(1);
	}

	@Test
	void getTrustManagersWhenTrustStorePasswordIsEmpty() {
		ReflectionTestUtils.setField(sslFactory, "trustStoreLocation", "Location");

		TrustManager[] trustManagers = sslFactory.getTrustManagers(StringUtils.EMPTY.toCharArray());

		// then - verify that the store was loaded with the expected certificates
		X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
		assertThat(trustManager.getAcceptedIssuers()).hasSizeGreaterThan(1);
	}

	@Test
	void getTrustManagersWhenTrustStorePasswordIsNull() {
		ReflectionTestUtils.setField(sslFactory, "trustStoreLocation", "Location");

		TrustManager[] trustManagers = sslFactory.getTrustManagers(null);

		// then - verify that the store was loaded with the expected certificates
		X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
		assertThat(trustManager.getAcceptedIssuers()).hasSizeGreaterThan(1);
	}

	@Test
	void getTrustManagersWhenFileKeystoreGeneralException() {
		try (MockedStatic<KeyStore> keyStoreMockedStatic = Mockito.mockStatic(KeyStore.class)) {
			RuntimeException cause = new RuntimeException("Test Exception Message");
			keyStoreMockedStatic.when(() -> KeyStore.getInstance(any())).thenThrow(cause);

			char[] storePasswordCharArray = STORE_PASSWORD.toCharArray();
			assertThatThrownBy(() -> sslFactory.getTrustManagers(storePasswordCharArray))
					.isInstanceOf(CaasConfigurationException.class)
					.hasCause(cause)
					.hasMessage("Failed to initialize Trust Managers");
		}
	}

	@Test
	void getTrustManagersDefaultTrustManagerException() {
		try (MockedStatic<TrustManagerFactory> trustManagerFactoryMockedStatic = Mockito.mockStatic(TrustManagerFactory.class)) {
			RuntimeException cause = new RuntimeException("Test Exception Message");
			trustManagerFactoryMockedStatic.when(() -> TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())).thenThrow(cause);

			assertThatThrownBy(() -> sslFactory.getTrustManagers(null))
					.isInstanceOf(CaasConfigurationException.class)
					.hasCause(cause)
					.hasMessage("Failed to initialize Default Trust Managers");
		}
	}

	@Test
	void getKeyManagerWhenFileKeyStore() {
		X509KeyManager keyManager = sslFactory.getKeyManager(STORE_PASSWORD.toCharArray());

		// then - verify that the store was loaded with the expected keys
		assertThat(keyManager.getPrivateKey(TEST_CLIENT_ALIAS)).isNotNull();
	}

	@Test
	void getKeyManagerWhenEnvKeyStore() throws Exception {
		caasDelegateConfig.setKeyStoreLocation("KEYSTORE");
		sslFactory = new SslFactory(caasDelegateConfig);
		environmentVariables.set("KEYSTORE", encodeKeyStore(TEST_KEY_STORE_FILE));

		X509KeyManager keyManager = sslFactory.getKeyManager(STORE_PASSWORD.toCharArray());

		// then - verify that the store was loaded with the expected keys
		assertThat(keyManager.getPrivateKey(TEST_CLIENT_ALIAS)).isNotNull();
	}

	@Test
	void getKeyManagerWhenNoKeyStoreAvailable_thenThrowCaasConfigurationException() {
		ReflectionTestUtils.setField(sslFactory, "keyStoreLocation", "Nothing Located here");
		char[] charArray = STORE_PASSWORD.toCharArray();

		assertThrows(CaasConfigurationException.class,
				() -> sslFactory.getKeyManager(charArray));
	}

	@Test
	void getTrustManagerWhenIncorrectPassword_thenThrowCaasConfigurationException() {
		char[] charArray = "Incorrect".toCharArray();

		assertThrows(CaasConfigurationException.class,
				() -> sslFactory.getTrustManagers(charArray));
	}

	@Test
	void getSSLConnectionSocketFactory() {
		SSLConnectionSocketFactory actual = sslFactory.getSSLConnectionSocketFactory(STORE_PASSWORD, STORE_PASSWORD);

		assertThat(actual).isNotNull();  // unfortunately, SSLConnectionSocketFactory does not expose the internals to check
	}

	@Test
	void getSSLConnectionSocketFactoryWhenReadFromSystemEnvironment() {
		environmentVariables.set(STORE_PASSWORD_SYSTEM_ENV_NAME, STORE_PASSWORD_64);

		SSLConnectionSocketFactory actual = sslFactory.getSSLConnectionSocketFactory(STORE_PASSWORD_SYSTEM_ENV_NAME, STORE_PASSWORD_SYSTEM_ENV_NAME);

		assertThat(actual).isNotNull();  // unfortunately, SSLConnectionSocketFactory does not expose the internals to check
	}

	@Test
	void getSSLConnectionSocketFactoryWhenFailedInitializeTrustStoreManagerThenThrowCaasConfigurationException() {
		assertThrows(CaasConfigurationException.class,
				() -> sslFactory.getSSLConnectionSocketFactory(STORE_PASSWORD, "Incorrect"));

	}

	private static String encodeKeyStore(String fileName) throws Exception {
		InputStream fileStream = SslFactoryTest.class.getClassLoader().getResourceAsStream(fileName);
		return Base64.getEncoder().encodeToString(fileStream.readAllBytes());
	}
}