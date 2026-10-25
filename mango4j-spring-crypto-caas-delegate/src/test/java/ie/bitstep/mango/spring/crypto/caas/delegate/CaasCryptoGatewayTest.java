package ie.bitstep.mango.spring.crypto.caas.delegate;

import ie.bitstep.mango.spring.crypto.caas.delegate.exceptions.CaasBatchItemException;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.DecryptRequest;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.DecryptResponse;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.EncryptRequest;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.EncryptResponse;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.HashPanRequest;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.HashPanResponse;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.BatchRequest;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.BatchRequestItem;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.BatchResponse;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.BatchResponseItem;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.DecryptBatchRequest;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.DecryptBatchRequestItem;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.DecryptBatchResponse;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.DecryptBatchResponseItem;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.PayloadResponse;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.wrapped.WrappedDecryptRequest;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.wrapped.WrappedDecryptResponse;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.wrapped.WrappedEncryptRequest;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.wrapped.WrappedEncryptResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

import static ie.bitstep.mango.spring.crypto.caas.delegate.CaasConstants.BATCH_URI;
import static ie.bitstep.mango.spring.crypto.caas.delegate.CaasConstants.WRAPPED_DECRYPT_URI;
import static ie.bitstep.mango.spring.crypto.caas.delegate.CaasUtils.base64Encode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith({MockitoExtension.class})
class CaasCryptoGatewayTest {

	private static final String CAAS_BASE_URL = "https://caas/";
	private static final String HASH_PAN_URI = "/core/hash/pan";
	private static final String ENCRYPT_AES_URI = "/core/encrypt/aes";
	private static final String DECRYPT_AES_URI = "/core/decrypt/aes";
	private static final String HASH_SHA512_URI = "/core/hash/sha512";

	private static final String TEST_KEY_ALIAS = "TEST_KEY_ALIAS";
	private static final String TEST_REQUEST_ID = "TEST_REQUEST_ID";
	private static final String TEST_BATCH_ITEM_ID = "TEST_BATCH_ITEM_ID";
	private static final String TEST_ERROR = "TEST_ERROR";

	private static final String TEST_MESSAGE = "TEST_MESSAGE";
	private static final String TEST_MESSAGE_B64 = "VEVTVF9NRVNTQUdF";
	private static final String TEST_ENCRYPTED_MESSAGE = "TEST_ENCRYPTED_MESSAGE";
	private static final String TEST_HASH = "TEST_HASH";

	private static final String OAEP_HASHING_ALG = "SHA512";
	private static final String TEST_IV = "TEST_IV";
	private static final String TEST_IV_B64 = base64Encode(TEST_IV);
	private static final String TEST_WRAPPED_KEY = "TEST_WRAPPED_KEY";
	private static final String TEST_ENCRYPTED_PRIVATE_KEY = "TEST_ENCRYPTED_PRIVATE_KEY";
	private static final String TEST_CERTIFICATE = "TEST_CERTIFICATE";

	@InjectMocks
	private CaasCryptoGateway caasGateway;

	@Mock
	private ProcessCaasService mockProcessCaasService;

	@Mock
	System.Logger mockLogger;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(caasGateway, "caasEndpointBase", CAAS_BASE_URL);
		ReflectionTestUtils.setField(caasGateway, "logger", mockLogger);
	}

	@Test
	void decryptAes() {
		DecryptRequest decryptRequest = new DecryptRequest().setCipher(TEST_ENCRYPTED_MESSAGE);
		DecryptResponse decryptResponse = new DecryptResponse();
		decryptResponse.setMessage(TEST_MESSAGE_B64);
		decryptResponse.setRequestId(TEST_REQUEST_ID);
		given(mockProcessCaasService.exchange(CAAS_BASE_URL + DECRYPT_AES_URI, decryptRequest, DecryptResponse.class))
				.willReturn(decryptResponse);

		DecryptResponse result = caasGateway.decryptAes(TEST_ENCRYPTED_MESSAGE);

		assertThat(result.getMessage()).isEqualTo(TEST_MESSAGE);
		assertThat(result.getRequestId()).isEqualTo(TEST_REQUEST_ID);
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS request to decrypt data: url={0} request={1}", CAAS_BASE_URL + DECRYPT_AES_URI, decryptRequest);
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS decrypt data response: requestId={0}", TEST_REQUEST_ID);
	}

	@Test
	void decryptAesWithBatch() {
		DecryptBatchRequest decryptBatchRequest = new DecryptBatchRequest()
				.setRequests(Collections.singletonList(new DecryptBatchRequestItem()
						.setBatchRequestId(TEST_BATCH_ITEM_ID)));
		DecryptBatchResponse decryptBatchResponse = new DecryptBatchResponse()
				.setRequestId(TEST_REQUEST_ID)
				.setResponses(Collections.singletonList(new DecryptBatchResponseItem()
						.setBatchRequestId(TEST_BATCH_ITEM_ID)
						.setStatusCode(200)
						.setPayload(new DecryptResponse().setMessage(TEST_MESSAGE_B64))));
		given(mockProcessCaasService.exchange(CAAS_BASE_URL + BATCH_URI, decryptBatchRequest, DecryptBatchResponse.class)).willReturn(decryptBatchResponse);

		Map<String, DecryptBatchResponseItem> result = caasGateway.decryptAes(decryptBatchRequest);

		assertThat(result).hasSize(1)
				.containsEntry(TEST_BATCH_ITEM_ID, new DecryptBatchResponseItem()
						.setBatchRequestId(TEST_BATCH_ITEM_ID)
						.setStatusCode(200)
						.setPayload(new DecryptResponse().setMessage(TEST_MESSAGE)));
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS request to batch decrypt data: url={0} batchSize={1}:", CAAS_BASE_URL + BATCH_URI, 1);
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS batch decrypt response: requestId={0}", TEST_REQUEST_ID);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void decryptAesWithBatchWhenItemFailure() {
		DecryptBatchRequest decryptBatchRequest = new DecryptBatchRequest()
				.setRequests(Collections.singletonList(new DecryptBatchRequestItem()
						.setBatchRequestId(TEST_BATCH_ITEM_ID)));
		DecryptBatchResponse decryptBatchResponse = new DecryptBatchResponse()
				.setRequestId(TEST_REQUEST_ID)
				.setResponses(Collections.singletonList(new DecryptBatchResponseItem()
						.setBatchRequestId(TEST_BATCH_ITEM_ID)
						.setStatusCode(400)
						.setPayload(new DecryptResponse()
								.setMessage(TEST_ERROR))));
		given(mockProcessCaasService.exchange(CAAS_BASE_URL + BATCH_URI, decryptBatchRequest, DecryptBatchResponse.class)).willReturn(decryptBatchResponse);

		assertThatThrownBy(() -> caasGateway.decryptAes(decryptBatchRequest))
				.isInstanceOf(CaasBatchItemException.class)
				.hasMessage("CaaS batch item failure: requestId=TEST_REQUEST_ID, error={\"batchRequestId\":\"TEST_BATCH_ITEM_ID\",\"path\":null,\"statusCode\":400,\"payload\":{\"requestId\":null,\"message\":\"TEST_ERROR\",\"rotatedCipher\":null,\"rotatedKeyAlias\":null}}");
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS request to batch decrypt data: url={0} batchSize={1}:", CAAS_BASE_URL + BATCH_URI, 1);
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS batch decrypt response: requestId={0}", TEST_REQUEST_ID);
		then(mockLogger).should().log(System.Logger.Level.ERROR,
				"CaaS batch decrypt item failure: requestId={0}, error={1}", TEST_REQUEST_ID, "{\"batchRequestId\":\"TEST_BATCH_ITEM_ID\",\"path\":null,\"statusCode\":400,\"payload\":{\"requestId\":null,\"message\":\"TEST_ERROR\",\"rotatedCipher\":null,\"rotatedKeyAlias\":null}}");
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void encryptAes() {
		EncryptRequest encryptRequest = new EncryptRequest()
				.setAlias(TEST_KEY_ALIAS)
				.setMessage(TEST_MESSAGE_B64);
		EncryptResponse encryptResponse = new EncryptResponse();
		encryptResponse.setRequestId(TEST_REQUEST_ID);
		encryptResponse.setCipher(TEST_ENCRYPTED_MESSAGE);
		given(mockProcessCaasService.exchange(CAAS_BASE_URL + ENCRYPT_AES_URI, encryptRequest, EncryptResponse.class))
				.willReturn(encryptResponse);

		String result = caasGateway.encryptAes(TEST_MESSAGE, TEST_KEY_ALIAS).getCipher();

		assertThat(result).isEqualTo(TEST_ENCRYPTED_MESSAGE);
	}

	@Test
	void hashPan() {
		HashPanRequest hashRequest = new HashPanRequest()
				.setAlias(TEST_KEY_ALIAS)
				.setMessage(TEST_MESSAGE_B64);
		HashPanResponse hashResponse = new HashPanResponse();
		hashResponse.setRequestId(TEST_REQUEST_ID);
		hashResponse.setHash(TEST_HASH);
		given(mockProcessCaasService.exchange(CAAS_BASE_URL + HASH_PAN_URI, hashRequest, HashPanResponse.class)).willReturn(hashResponse);

		String result = caasGateway.hashPan(TEST_MESSAGE, TEST_KEY_ALIAS);

		assertThat(result).isEqualTo(TEST_HASH);
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS hash response: requestId={0} hash={1}", TEST_REQUEST_ID, TEST_HASH);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void hash() {
		HashPanRequest hashRequest = new HashPanRequest()
				.setAlias(TEST_KEY_ALIAS)
				.setMessage(TEST_MESSAGE_B64);
		HashPanResponse hashResponse = new HashPanResponse();
		hashResponse.setRequestId(TEST_REQUEST_ID);
		hashResponse.setHash(TEST_HASH);
		given(mockProcessCaasService.exchange(CAAS_BASE_URL + HASH_SHA512_URI, hashRequest, HashPanResponse.class)).willReturn(hashResponse);

		String result = caasGateway.hash(TEST_MESSAGE, TEST_KEY_ALIAS, HashAlgorithm.SHA512);

		assertThat(result).isEqualTo(TEST_HASH);
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS hash response: requestId={0} hash={1}", TEST_REQUEST_ID, TEST_HASH);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void caasBatch() {
		BatchRequest batchRequest = new BatchRequest(Collections.singletonList(new BatchRequestItem()
				.setBatchRequestId(TEST_BATCH_ITEM_ID)));
		BatchResponse batchResponse = new BatchResponse()
				.setRequestId(TEST_REQUEST_ID)
				.setResponses(Collections.singletonList(new BatchResponseItem()
						.setBatchRequestId(TEST_BATCH_ITEM_ID)
						.setStatusCode(200)));
		given(mockProcessCaasService.exchange(CAAS_BASE_URL + BATCH_URI, batchRequest, BatchResponse.class)).willReturn(batchResponse);

		Map<String, BatchResponseItem> actual = caasGateway.caasBatch(batchRequest);

		assertThat(actual).hasSize(1)
				.containsEntry(TEST_BATCH_ITEM_ID, new BatchResponseItem()
						.setBatchRequestId(TEST_BATCH_ITEM_ID)
						.setStatusCode(200));
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS batch request: url={0} batchSize={1}", CAAS_BASE_URL + BATCH_URI, 1);
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS batch response: requestId={0}", TEST_REQUEST_ID);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void caasBatchWhenItemFailure() {
		BatchRequest batchRequest = new BatchRequest(Collections.singletonList(new BatchRequestItem()
				.setBatchRequestId(TEST_BATCH_ITEM_ID)));
		BatchResponse batchResponse = new BatchResponse()
				.setRequestId(TEST_REQUEST_ID)
				.setResponses(Collections.singletonList(new BatchResponseItem()
						.setBatchRequestId(TEST_BATCH_ITEM_ID)
						.setStatusCode(400)
						.setPayload(new PayloadResponse().setMessage(TEST_ERROR))));
		given(mockProcessCaasService.exchange(CAAS_BASE_URL + BATCH_URI, batchRequest, BatchResponse.class)).willReturn(batchResponse);

		assertThatThrownBy(() -> caasGateway.caasBatch(batchRequest))
				.isInstanceOf(CaasBatchItemException.class)
				.hasMessage("CaaS batch item failure: requestId=TEST_REQUEST_ID, error={\"batchRequestId\":\"TEST_BATCH_ITEM_ID\",\"path\":null,\"statusCode\":400,\"payload\":{\"requestId\":null,\"message\":\"TEST_ERROR\"}}");
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS batch request: url={0} batchSize={1}", CAAS_BASE_URL + BATCH_URI, 1);
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS batch response: requestId={0}", TEST_REQUEST_ID);
		then(mockLogger).should().log(System.Logger.Level.ERROR,
				"CaaS batch item failure: requestId={0}, error={1}", TEST_REQUEST_ID, "{\"batchRequestId\":\"TEST_BATCH_ITEM_ID\",\"path\":null,\"statusCode\":400,\"payload\":{\"requestId\":null,\"message\":\"TEST_ERROR\"}}");
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void wrappedDecrypt() {
		WrappedDecryptRequest decryptRequest = new WrappedDecryptRequest()
				.setOaepHashingAlgorithm(OAEP_HASHING_ALG)
				.setCipher(TEST_ENCRYPTED_MESSAGE)
				.setIv(TEST_IV_B64)
				.setWrappedKey(TEST_WRAPPED_KEY)
				.setEncryptedPrivateKey(TEST_ENCRYPTED_PRIVATE_KEY);
		WrappedDecryptResponse decryptResponse = new WrappedDecryptResponse()
				.setRequestId(TEST_REQUEST_ID)
				.setMessage(TEST_MESSAGE_B64);
		given(mockProcessCaasService.exchange(CAAS_BASE_URL + WRAPPED_DECRYPT_URI, decryptRequest, WrappedDecryptResponse.class))
				.willReturn(decryptResponse);

		WrappedDecryptResponse actual = caasGateway.wrappedDecrypt(decryptRequest);

		WrappedDecryptResponse expected = new WrappedDecryptResponse()
				.setRequestId(TEST_REQUEST_ID)
				.setMessage(TEST_MESSAGE);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void wrappedEncrypt() {
		WrappedEncryptRequest encryptRequest = new WrappedEncryptRequest()
				.setOaepHashingAlgorithm(OAEP_HASHING_ALG)
				.setMessage(TEST_MESSAGE)
				.setCertificate(TEST_CERTIFICATE);
		WrappedEncryptRequest expectedEncryptRequest = new WrappedEncryptRequest()
				.setOaepHashingAlgorithm(OAEP_HASHING_ALG)
				.setMessage(TEST_MESSAGE_B64)
				.setCertificate(TEST_CERTIFICATE);
		WrappedEncryptResponse encryptResponse = new WrappedEncryptResponse()
				.setRequestId(TEST_REQUEST_ID)
				.setCipher(TEST_MESSAGE_B64)
				.setIv(TEST_IV_B64)
				.setWrappedKey(TEST_WRAPPED_KEY);
		given(mockProcessCaasService.exchange(CAAS_BASE_URL + CaasConstants.WRAPPED_ENCRYPT_URI, expectedEncryptRequest, WrappedEncryptResponse.class))
				.willReturn(encryptResponse);

		WrappedEncryptResponse actual = caasGateway.wrappedEncrypt(encryptRequest);

		assertThat(actual).isEqualTo(encryptResponse);
		assertThat(actual.getRequestId()).isEqualTo(TEST_REQUEST_ID);
		then(mockLogger).should().log(System.Logger.Level.DEBUG,
				"CaaS wrapped-encrypt data response: requestId={0}", TEST_REQUEST_ID);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}
}