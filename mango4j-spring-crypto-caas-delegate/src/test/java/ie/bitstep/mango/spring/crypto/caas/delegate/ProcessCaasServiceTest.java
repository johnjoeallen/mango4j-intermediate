package ie.bitstep.mango.spring.crypto.caas.delegate;

import ie.bitstep.mango.spring.crypto.caas.delegate.exceptions.CaasCryptoException;
import ie.bitstep.mango.spring.crypto.caas.delegate.exceptions.CaasServiceTransientException;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.BatchRequest;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.BatchRequestItem;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.DecryptBatchRequest;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.DecryptBatchRequestItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ProcessCaasServiceTest {

	private static final String TEST_URL = "TEST_URL";
	private static final String TEST_REQUEST = "TEST_REQUEST";
	private static final String TEST_RESPONSE = "TEST_RESPONSE";
	private static final String TEST_REQUEST_ID = "TEST_REQUEST_ID";
	private static final String TEST_RESPONSE_TIME = "18ms";
	private static final Instant TEST_START_TIME = Instant.now();
	private static final Instant TEST_END_TIME = TEST_START_TIME.plusMillis(125);
	private static final String REQUEST_ID_HEADER_NAME = "request-id";
	private static final String RESPONSE_TIME_HEADER_NAME = "response-time";
	private static final String TEST_PATH = "/hash/pan";
	private static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";
	private static final String ACCEPT_HEADER_NAME = "Accept";

	@InjectMocks
	private ProcessCaasService processCaasService;

	@Mock
	private RestTemplate mockRestTemplate;

	@Mock
	private Clock mockClock;

	@Mock
	private System.Logger mockLogger;

	@Captor
	private ArgumentCaptor<HttpEntity<Object>> httpEntityCaptor;

	private ResponseEntity<String> testResponseEntity;
	private HttpHeaders testResponseHeaders;
	private BatchRequest testBatchRequest;
	private DecryptBatchRequest testDecryptBatchRequest;

	@BeforeEach
	void setUp() {
		testResponseHeaders = new HttpHeaders();
		testResponseHeaders.set(REQUEST_ID_HEADER_NAME, TEST_REQUEST_ID);
		testResponseHeaders.set(RESPONSE_TIME_HEADER_NAME, TEST_RESPONSE_TIME);
		testResponseEntity = new ResponseEntity<>(TEST_RESPONSE, testResponseHeaders, HttpStatus.OK);

		BatchRequestItem batchRequestItem = new BatchRequestItem();
		batchRequestItem.setPath(TEST_PATH);
		testBatchRequest = new BatchRequest(Collections.singletonList(batchRequestItem));

		DecryptBatchRequestItem decryptBatchRequestItem = new DecryptBatchRequestItem();
		testDecryptBatchRequest = new DecryptBatchRequest();
		testDecryptBatchRequest.setRequests(Collections.singletonList(decryptBatchRequestItem));

		ReflectionTestUtils.setField(processCaasService, "clock", mockClock);
		ReflectionTestUtils.setField(processCaasService, "logger", mockLogger);
	}

	@Test
	void caasServiceFallback() {
		RuntimeException cause = new RuntimeException("Oops");

		assertThatThrownBy(() -> processCaasService.caasFallback(cause)).isSameAs(cause);
		then(mockLogger).should().log(ERROR, "An error occurred calling CaaS, all retries failed, splunkEvent=caasRetryFailure:", cause);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void executeHttpRequestWhenSuccess() {
		given(mockClock.instant()).willReturn(TEST_START_TIME, TEST_END_TIME);
		given(mockRestTemplate.exchange(eq(TEST_URL), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(String.class)))
				.willReturn(testResponseEntity);

		ResponseEntity<String> result = processCaasService.executeHttpRequest(TEST_URL, TEST_REQUEST, String.class);

		assertThat(result).isEqualTo(testResponseEntity);
		HttpEntity<Object> httpEntity = httpEntityCaptor.getValue();
		assertThat(httpEntity.getBody()).isEqualTo(TEST_REQUEST);
		assertThat(httpEntity.getHeaders().getFirst(CONTENT_TYPE_HEADER_NAME)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(httpEntity.getHeaders().getFirst(ACCEPT_HEADER_NAME)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		then(mockLogger).should().log(DEBUG, "Initiating CaaS service call for url={0}", TEST_URL);
		then(mockLogger).should().log(DEBUG, "CaaS response for url={0}: statusCode={1}, requestId={2}, responseTime={3}, caasResponseTime={4}, splunkEvent=caasSuccess",
				TEST_URL, HttpStatus.OK, TEST_REQUEST_ID, Duration.between(TEST_START_TIME, TEST_END_TIME).toMillis(), TEST_RESPONSE_TIME);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void exchangeWhenSuccess() {
		given(mockClock.instant()).willReturn(TEST_START_TIME, TEST_END_TIME);
		given(mockRestTemplate.exchange(eq(TEST_URL), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(String.class)))
				.willReturn(testResponseEntity);

		String result = processCaasService.exchange(TEST_URL, TEST_REQUEST, String.class);

		assertThat(result).isEqualTo(TEST_RESPONSE);
		HttpEntity<Object> httpEntity = httpEntityCaptor.getValue();
		assertThat(httpEntity.getBody()).isEqualTo(TEST_REQUEST);
		assertThat(httpEntity.getHeaders().getFirst(CONTENT_TYPE_HEADER_NAME)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(httpEntity.getHeaders().getFirst(ACCEPT_HEADER_NAME)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		then(mockLogger).should().log(DEBUG, "Initiating CaaS service call for url={0}", TEST_URL);
		then(mockLogger).should().log(DEBUG, "CaaS response for url={0}: statusCode={1}, requestId={2}, responseTime={3}, caasResponseTime={4}, splunkEvent=caasSuccess",
				TEST_URL, HttpStatus.OK, TEST_REQUEST_ID, Duration.between(TEST_START_TIME, TEST_END_TIME).toMillis(), TEST_RESPONSE_TIME);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void exchangeWhenBatchRequest() {
		given(mockClock.instant()).willReturn(TEST_START_TIME, TEST_END_TIME);
		given(mockRestTemplate.exchange(eq(TEST_URL), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(String.class)))
				.willReturn(testResponseEntity);

		String result = processCaasService.exchange(TEST_URL, testBatchRequest, String.class);

		assertThat(result).isEqualTo(TEST_RESPONSE);
		HttpEntity<Object> httpEntity = httpEntityCaptor.getValue();
		assertThat(httpEntity.getBody()).isEqualTo(testBatchRequest);
		assertThat(httpEntity.getHeaders().getFirst(CONTENT_TYPE_HEADER_NAME)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(httpEntity.getHeaders().getFirst(ACCEPT_HEADER_NAME)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		then(mockLogger).should().log(DEBUG, "Initiating CaaS service call for url={0}", TEST_URL);
		then(mockLogger).should().log(DEBUG, "CaaS response for url={0}, batchSize={1}, batchPath={2}: statusCode={3}, requestId={4}, responseTime={5}, caasResponseTime={6}, splunkEvent=caasSuccess",
				TEST_URL, 1, TEST_PATH, HttpStatus.OK, TEST_REQUEST_ID, Duration.between(TEST_START_TIME, TEST_END_TIME).toMillis(), TEST_RESPONSE_TIME);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void exchangeWhenDecryptBatchRequest() {
		given(mockClock.instant()).willReturn(TEST_START_TIME, TEST_END_TIME);
		given(mockRestTemplate.exchange(eq(TEST_URL), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(String.class)))
				.willReturn(testResponseEntity);

		String result = processCaasService.exchange(TEST_URL, testDecryptBatchRequest, String.class);

		assertThat(result).isEqualTo(TEST_RESPONSE);
		HttpEntity<Object> httpEntity = httpEntityCaptor.getValue();
		assertThat(httpEntity.getBody()).isEqualTo(testDecryptBatchRequest);
		assertThat(httpEntity.getHeaders().getFirst(CONTENT_TYPE_HEADER_NAME)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(httpEntity.getHeaders().getFirst(ACCEPT_HEADER_NAME)).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
		then(mockLogger).should().log(DEBUG, "Initiating CaaS service call for url={0}", TEST_URL);
		then(mockLogger).should().log(DEBUG, "CaaS response for url={0}, batchSize={1}, batchPath={2}: statusCode={3}, requestId={4}, responseTime={5}, caasResponseTime={6}, splunkEvent=caasSuccess",
				TEST_URL, 1, "/core/decrypt/aes", HttpStatus.OK, TEST_REQUEST_ID, Duration.between(TEST_START_TIME, TEST_END_TIME).toMillis(), TEST_RESPONSE_TIME);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void exchangeWhenHttpClientErrorException() {
		given(mockClock.instant()).willReturn(TEST_START_TIME, TEST_END_TIME);
		HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request", testResponseHeaders, TEST_RESPONSE.getBytes(StandardCharsets.UTF_8), null);
		given(mockRestTemplate.exchange(eq(TEST_URL), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(String.class)))
				.willThrow(cause);

		assertThatThrownBy(() -> processCaasService.exchange(TEST_URL, TEST_REQUEST, String.class))
				.isInstanceOf(CaasCryptoException.class)
				.hasMessage("Failed to perform CaaS operation for url=TEST_URL: statusCode=400 BAD_REQUEST, requestId=TEST_REQUEST_ID, error=TEST_RESPONSE")
				.hasCause(cause);
		then(mockLogger).should().log(DEBUG, "Initiating CaaS service call for url={0}", TEST_URL);
		then(mockLogger).should().log(WARNING, "CaaS error response for url={0}: statusCode={1}, requestId={2}, error={3}, responseTime={4}, caasResponseTime={5}, splunkEvent=caasFailure",
				TEST_URL, HttpStatus.BAD_REQUEST, TEST_REQUEST_ID, TEST_RESPONSE, Duration.between(TEST_START_TIME, TEST_END_TIME).toMillis(), TEST_RESPONSE_TIME);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void exchangeWhenHttpServerErrorException() {
		given(mockClock.instant()).willReturn(TEST_START_TIME, TEST_END_TIME);
		HttpServerErrorException cause = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", testResponseHeaders, TEST_RESPONSE.getBytes(StandardCharsets.UTF_8), null);
		given(mockRestTemplate.exchange(eq(TEST_URL), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(String.class)))
				.willThrow(cause);

		assertThatThrownBy(() -> processCaasService.exchange(TEST_URL, TEST_REQUEST, String.class))
				.isInstanceOf(CaasServiceTransientException.class)
				.hasMessage("CaaS service failure for url=TEST_URL: statusCode=503 SERVICE_UNAVAILABLE, requestId=TEST_REQUEST_ID, error=TEST_RESPONSE")
				.hasCause(cause);
		then(mockLogger).should().log(DEBUG, "Initiating CaaS service call for url={0}", TEST_URL);
		then(mockLogger).should().log(WARNING, "CaaS error response for url={0}: statusCode={1}, requestId={2}, error={3}, responseTime={4}, caasResponseTime={5}, splunkEvent=caasFailure",
				TEST_URL, HttpStatus.SERVICE_UNAVAILABLE, TEST_REQUEST_ID, TEST_RESPONSE, Duration.between(TEST_START_TIME, TEST_END_TIME).toMillis(), TEST_RESPONSE_TIME);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void exchangeWhenRestClientException() {
		given(mockClock.instant()).willReturn(TEST_START_TIME).willReturn(TEST_END_TIME);
		RestClientException cause = new RestClientException("Connection failure");
		given(mockRestTemplate.exchange(eq(TEST_URL), eq(HttpMethod.POST), httpEntityCaptor.capture(), eq(String.class)))
				.willThrow(cause);

		assertThatThrownBy(() -> processCaasService.exchange(TEST_URL, TEST_REQUEST, String.class))
				.isInstanceOf(CaasServiceTransientException.class)
				.hasMessage("CaaS service failure for url=TEST_URL")
				.hasCause(cause);
		then(mockLogger).should().log(DEBUG, "Initiating CaaS service call for url={0}", TEST_URL);
		then(mockLogger).should().log(WARNING, "CaaS failure for url={0}: responseTime={1}, ex={2}, splunkEvent=caasFailure",
				TEST_URL, Duration.between(TEST_START_TIME, TEST_END_TIME).toMillis(), cause);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}
}