package com.mastercard.commons.mango4j.crypto.caas.delegate.net;

import org.apache.hc.client5.http.impl.DefaultConnectionKeepAliveStrategy;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.lang.System.Logger.Level.DEBUG;

public class CappedKeepAliveStrategy extends DefaultConnectionKeepAliveStrategy {

	private System.Logger logger = System.getLogger(CappedKeepAliveStrategy.class.getName());

	private final Duration maxKeepAlive;

	public CappedKeepAliveStrategy(Duration maxKeepAlive) {
		this.maxKeepAlive = maxKeepAlive;
	}

	@Override
	public TimeValue getKeepAliveDuration(HttpResponse response, HttpContext context) {
		TimeValue keepAliveDuration = super.getKeepAliveDuration(response, context);
		long maxKeepAliveMs = maxKeepAlive.toMillis();
		if (!response.containsHeader(HttpHeaders.KEEP_ALIVE)) {
			logger.log(DEBUG, "Keep-Alive header not present - setting to max {0}ms", maxKeepAliveMs);
			keepAliveDuration = TimeValue.ofMilliseconds(maxKeepAliveMs);
		} else if (keepAliveDuration.convert(TimeUnit.MILLISECONDS) > maxKeepAliveMs) {
			logger.log(DEBUG, "Keep-Alive header value of {0}ms was above the cap of {1}ms - Setting it to the cap", keepAliveDuration, maxKeepAliveMs);
			keepAliveDuration = TimeValue.ofMilliseconds(maxKeepAliveMs);
		} else {
			logger.log(DEBUG, "keep alive duration {0}ms", keepAliveDuration);
		}
		return keepAliveDuration;
	}
}