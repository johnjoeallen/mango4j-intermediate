package com.mastercard.commons.mango4j.crypto.caas.delegate.ssl;

import com.mastercard.commons.mango4j.crypto.caas.delegate.config.CaasDelegateConfig;
import com.mastercard.commons.mango4j.crypto.caas.delegate.exceptions.CaasConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.ssl.HttpsSupport;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;

import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasUtils.base64Decode;

@Slf4j
public class SslFactory {

	private final String protocol;
	private final String clientAlias;
	private final String keyStoreLocation;
	private final String trustStoreLocation;

	public SslFactory(CaasDelegateConfig config) {
		this.protocol = config.getProtocol();
		this.clientAlias = config.getClientAlias();
		this.keyStoreLocation = config.getKeyStoreLocation();
		this.trustStoreLocation = config.getTrustStoreLocation();
	}

	@SuppressWarnings("deprecation") // will update RestTemplate in future version
	public SSLConnectionSocketFactory getSSLConnectionSocketFactory(String keyStorePassword, String trustStorePassword) {
		try {
			SSLContext sslContext = SSLContext.getInstance(protocol);

			sslContext.init(new KeyManager[]{getKeyManager(getPassword(keyStorePassword).toCharArray())},
					getTrustManagers(getPassword(trustStorePassword).toCharArray()),
					new SecureRandom());


			return new SSLConnectionSocketFactory(
					sslContext,
					new String[]{protocol},
					null,
					getHostnameVerifier());

		} catch (Exception e) {
			throw new CaasConfigurationException("Failed to initialize SSLConnectionSocketFactory", e);
		}
	}

	protected HostnameVerifier getHostnameVerifier() {
		log.info("Using Default Hostname Verifier");
		return HttpsSupport.getDefaultHostnameVerifier();
	}

	protected TrustManager[] getTrustManagers(char[] trustStorePasswordCharArray) {
		if (StringUtils.isEmpty(trustStoreLocation) || trustStorePasswordCharArray == null || trustStorePasswordCharArray.length == 0) {
			log.info("Using Default Trust Managers");
			return getDefaultTrustManagers();
		} else {
			log.info("Initializing Trust Manager from [{}]", trustStoreLocation);
			try (InputStream trustStoreInput = getStoreInputStream(trustStoreLocation)) {
				KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				trustStore.load(trustStoreInput, trustStorePasswordCharArray);
				TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				trustManagerFactory.init(trustStore);
				return trustManagerFactory.getTrustManagers();
			} catch (Exception e) {
				throw new CaasConfigurationException("Failed to initialize Trust Managers", e);
			}
		}
	}

	private TrustManager[] getDefaultTrustManagers() {
		try {
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init((KeyStore) null);
			return trustManagerFactory.getTrustManagers();
		} catch (Exception e) {
			throw new CaasConfigurationException("Failed to initialize Default Trust Managers", e);
		}
	}

	protected X509KeyManager getKeyManager(char[] keyStorePasswordCharArray) {
		log.info("Initializing Key Manager from [{}] with client alias [{}]", keyStoreLocation, clientAlias);

		try (InputStream keyStoreInput = getStoreInputStream(keyStoreLocation)) {

			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(keyStoreInput, keyStorePasswordCharArray);

			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, keyStorePasswordCharArray);
			X509KeyManager keyManager = (X509KeyManager) keyManagerFactory.getKeyManagers()[0];

			return new ClientAliasKeyManager(keyManager, clientAlias);

		} catch (Exception e) {
			throw new CaasConfigurationException("Failed to initialize Key Manager", e);
		}
	}

	private InputStream getStoreInputStream(String storeLocation) throws IOException {
		// Look for the store file on the filesystem first, then in an environment variable, then finally on the classpath
		File filesystemFile = new File(storeLocation);
		if (filesystemFile.exists()) {
			log.debug("Loading crypto store from file: {}", storeLocation);
			return new FileInputStream(filesystemFile);
		}

		String encodedKeyStore = getSystemEnv(storeLocation);
		if (encodedKeyStore != null) {
			log.debug("Loading crypto store from environment variable: {}", storeLocation);
			return new ByteArrayInputStream(Base64.getDecoder().decode(encodedKeyStore));
		}

		InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(storeLocation);
		if (resourceAsStream != null) {
			log.debug("Loading crypto store from classpath resource: {}", storeLocation);
			return resourceAsStream;
		}
		throw new CaasConfigurationException("No crypto store found '" + storeLocation + "'");
	}

	private String getPassword(String password) {
		// the "password" can be the name of the environment variable with encoded password
		// or the password itself
		if (StringUtils.isNotBlank(getSystemEnv(password))) {
			log.debug("Keystore password taken from encoded environment variable: {}", password);
			return base64Decode(getSystemEnv(password));
		} else {
			return password;
		}
	}

	protected String getSystemEnv(String name) {
		return System.getenv(name);
	}
}
