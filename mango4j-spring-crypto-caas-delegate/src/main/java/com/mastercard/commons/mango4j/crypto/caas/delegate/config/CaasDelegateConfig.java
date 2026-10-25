package com.mastercard.commons.mango4j.crypto.caas.delegate.config;

import java.time.Duration;

public class CaasDelegateConfig {

	private int maxConnections = 32;
	private int maxPerRouteConnections = 32;
	private Duration validateAfterInactivity = Duration.ofMillis(200);
	private Duration connectionTimeout = Duration.ofMillis(5000);
	private Duration maxKeepAlive = Duration.ofMillis(30000);
	private Duration poolStats = Duration.ofMillis(30000);

	private String keyStorePassword = System.getProperty("javax.net.ssl.keyStorePassword", "KEYSTORE_PASSWORD");
	private String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword", "");

	private String protocol = System.getProperty("server.ssl.protocol", "TLSv1.2");
	private String clientAlias = System.getProperty("server.ssl.clientAlias", "client-common");

	private String keyStoreLocation = System.getProperty("javax.net.ssl.keyStore", "KEYSTORE");
	private String trustStoreLocation = System.getProperty("javax.net.ssl.trustStore", "");

	private String caasEndpointBase;

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public int getMaxPerRouteConnections() {
		return maxPerRouteConnections;
	}

	public void setMaxPerRouteConnections(int maxPerRouteConnections) {
		this.maxPerRouteConnections = maxPerRouteConnections;
	}

	public Duration getValidateAfterInactivity() {
		return validateAfterInactivity;
	}

	public void setValidateAfterInactivity(Duration validateAfterInactivity) {
		this.validateAfterInactivity = validateAfterInactivity;
	}

	public Duration getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Duration getMaxKeepAlive() {
		return maxKeepAlive;
	}

	public void setMaxKeepAlive(Duration maxKeepAlive) {
		this.maxKeepAlive = maxKeepAlive;
	}

	public Duration getPoolStats() {
		return poolStats;
	}

	public void setPoolStats(Duration poolStats) {
		this.poolStats = poolStats;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getClientAlias() {
		return clientAlias;
	}

	public void setClientAlias(String clientAlias) {
		this.clientAlias = clientAlias;
	}

	public String getKeyStoreLocation() {
		return keyStoreLocation;
	}

	public void setKeyStoreLocation(String keyStoreLocation) {
		this.keyStoreLocation = keyStoreLocation;
	}

	public String getTrustStoreLocation() {
		return trustStoreLocation;
	}

	public void setTrustStoreLocation(String trustStoreLocation) {
		this.trustStoreLocation = trustStoreLocation;
	}

	public String getCaasEndpointBase() {
		return caasEndpointBase;
	}

	public void setCaasEndpointBase(String caasEndpointBase) {
		this.caasEndpointBase = caasEndpointBase;
	}
}
