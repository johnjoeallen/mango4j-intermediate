package com.mastercard.commons.mango4j.crypto.caas.delegate;

import com.mastercard.commons.mango4j.crypto.caas.delegate.exceptions.CaasCryptoException;
import com.mastercard.commons.mango4j.crypto.caas.delegate.exceptions.CaasServiceTransientException;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.BatchRequest;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.BatchRequestItem;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.DecryptBatchRequest;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.DecryptBatchRequestItem;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasUtils.getRequestId;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasUtils.getResponseTime;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;

public class ProcessCaasService {

	private final System.Logger logger = System.getLogger(ProcessCaasService.class.getName());

	private final RestTemplate restTemplate;

	private Clock clock = Clock.systemUTC();

	public ProcessCaasService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public <T, R> R caasFallback(RuntimeException ex) {     //NOSONAR
		logger.log(ERROR, "An error occurred calling CaaS, all retries failed, splunkEvent=caasRetryFailure:", ex);
		throw ex;
	}

	public <T, R> R exchange(String url, T request, Class<R> responseType) {
		return executeHttpRequest(url, request, responseType).getBody();
	}

	public <T, R> ResponseEntity<R> executeHttpRequest(String url, T request, Class<R> responseType) {
		logger.log(DEBUG, "Initiating CaaS service call for url={0}", url);
		Instant start = clock.instant();
		try {
			HttpEntity<T> httpEntity = buildHttpEntity(request);
			ResponseEntity<R> responseEntity = restTemplate
					.exchange(url, HttpMethod.POST, httpEntity, responseType);
			logSuccess(url, request, responseEntity, since(start));
			return responseEntity;
		} catch (HttpClientErrorException ex) {
			logHttpFailure(url, ex, since(start));
			throw CaasCryptoException.createWithUrlAndHttpCause(url, ex);
		} catch (HttpStatusCodeException ex) {
			logHttpFailure(url, ex, since(start));
			throw CaasServiceTransientException.createWithUrlAndHttpCause(url, ex);
		} catch (Exception ex) {
			logGenericFailure(url, ex, since(start));
			throw CaasServiceTransientException.createWithUrlAndGenericCause(url, ex);
		}
	}

	private <T> HttpEntity<T> buildHttpEntity(T request) {
		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		headers.add("Content-Type", MediaType.APPLICATION_JSON.toString());
		headers.add("Accept", MediaType.APPLICATION_JSON.toString());
		return new HttpEntity<>(request, headers);
	}

	private <T, R> void logSuccess(String url, T request, ResponseEntity<R> responseEntity, Duration responseTime) {
		String requestId = getRequestId(responseEntity.getHeaders());
		String caasResponseTime = getResponseTime(responseEntity.getHeaders());

		// NOTE that this log record is used by Splunk dashboards and alerts
		if (request instanceof BatchRequest batchRequest) {
			String firstPath = batchRequest.requests().stream().findFirst().map(BatchRequestItem::getPath).orElse("");
			logger.log(DEBUG, "CaaS response for url={0}, batchSize={1}, batchPath={2}: statusCode={3}, requestId={4}, responseTime={5}, caasResponseTime={6}, splunkEvent=caasSuccess",
					url, batchRequest.requests().size(), firstPath, responseEntity.getStatusCode(), requestId, responseTime.toMillis(), caasResponseTime);
		} else if (request instanceof DecryptBatchRequest batchRequest) {
			String firstPath = batchRequest.getRequests().stream().findFirst().map(DecryptBatchRequestItem::getPath).orElse("");
			logger.log(DEBUG, "CaaS response for url={0}, batchSize={1}, batchPath={2}: statusCode={3}, requestId={4}, responseTime={5}, caasResponseTime={6}, splunkEvent=caasSuccess",
					url, batchRequest.getRequests().size(), firstPath, responseEntity.getStatusCode(), requestId, responseTime.toMillis(), caasResponseTime);
		} else {
			logger.log(DEBUG, "CaaS response for url={0}: statusCode={1}, requestId={2}, responseTime={3}, caasResponseTime={4}, splunkEvent=caasSuccess",
					url, responseEntity.getStatusCode(), requestId, responseTime.toMillis(), caasResponseTime);
		}
	}

	/**
	 * NOTE that this log record is used by Splunk dashboards and alerts (we use "warn" level here because this operation might be retried)
	 * @param url
	 * @param ex
	 * @param responseTime
	 */
	private void logHttpFailure(String url, HttpStatusCodeException ex, Duration responseTime) {
		String requestId = getRequestId(ex.getResponseHeaders());
		String caasResponseTime = getResponseTime(ex.getResponseHeaders());
		logger.log(WARNING, "CaaS error response for url={0}: statusCode={1}, requestId={2}, error={3}, responseTime={4}, caasResponseTime={5}, splunkEvent=caasFailure",
				url, ex.getStatusCode(), requestId, ex.getResponseBodyAsString(), responseTime.toMillis(), caasResponseTime);
	}

	private void logGenericFailure(String url, Exception ex, Duration responseTime) {
		logger.log(WARNING, "CaaS failure for url={0}: responseTime={1}, ex={2}, splunkEvent=caasFailure", url, responseTime.toMillis(), ex);
	}

	private Duration since(Instant start) {
		return Duration.between(start, clock.instant());
	}
}
