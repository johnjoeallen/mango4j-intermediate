package com.mastercard.commons.mango4j.crypto.caas.delegate.basic;

import com.mastercard.commons.mango.crypto.core.domain.CiphertextContainer;
import com.mastercard.commons.mango.crypto.core.domain.CryptoKey;
import com.mastercard.commons.mango.crypto.core.domain.HmacHolder;
import com.mastercard.commons.mango4j.crypto.caas.delegate.CaasCryptoGateway;
import com.mastercard.commons.mango4j.crypto.caas.delegate.config.CaasDelegateConfig;
import com.mastercard.commons.mango4j.crypto.caas.delegate.config.RestTemplateBuilder;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.EncryptResponse;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.BatchRequest;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.BatchRequestItem;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.BatchResponseItem;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.PayloadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mastercard.commons.mango4j.crypto.caas.delegate.testdata.TestData.CRYPTO_KEY_CONFIGURATION_CAAS_KEY_ALIAS_KEY;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.testdata.TestData.TEST_CAAS_KEY_ALIAS;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.testdata.TestData.TEST_CIPHERTEXT;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.testdata.TestData.TEST_SOURCE_VALUE;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.testdata.TestData.testCryptoKey;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.testdata.TestData.testDecryptResponse;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.testdata.TestData.testEncryptResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CaasBasicEncryptionServiceDelegateTest {

	private static final String HMAC_BATCH_PATH = "/core/hash/pan";
	private static final String HMAC_BATCH_METHOD = "post";

	private CaasBasicEncryptionServiceDelegate caasBasicEncryptionServiceDelegate;

	@Mock
	private CaasCryptoGateway mockCaasCryptoGateway;

	@Captor
	private ArgumentCaptor<BatchRequest> batchRequestArgumentCaptor;

	private final CaasDelegateConfig caasDelegateConfig = new CaasDelegateConfig();
	private CryptoKey testCryptoKey;

	@BeforeEach
	void setup() {
		try (MockedStatic<RestTemplateBuilder> restTemplateBuilder = Mockito.mockStatic(RestTemplateBuilder.class)) {
			restTemplateBuilder.when(() -> RestTemplateBuilder.build(any())).thenReturn(null);
			caasBasicEncryptionServiceDelegate = new CaasBasicEncryptionServiceDelegate(caasDelegateConfig);
		}
		ReflectionTestUtils.setField(caasBasicEncryptionServiceDelegate, "caasCryptoGateway", mockCaasCryptoGateway);
		testCryptoKey = testCryptoKey();
	}

	@Test
	void supportedCryptoKeyType() {
		assertThat(caasBasicEncryptionServiceDelegate.supportedCryptoKeyType()).isEqualTo("CAAS_BASIC");
	}

	@Test
	void encrypt() {
		EncryptResponse encryptResponse = testEncryptResponse();
		given(mockCaasCryptoGateway.encryptAes(TEST_SOURCE_VALUE, TEST_CAAS_KEY_ALIAS)).willReturn(encryptResponse);

		CiphertextContainer ciphertextContainer = caasBasicEncryptionServiceDelegate.encrypt(testCryptoKey, TEST_SOURCE_VALUE);

		assertThat(ciphertextContainer.getCryptoKey()).isEqualTo(testCryptoKey);
		assertThat(ciphertextContainer.getData()).hasSize(1);
		assertThat(ciphertextContainer.getData()).containsEntry("cipherText", encryptResponse.getCipher());
	}

	@Test
	void decrypt() {
		given(mockCaasCryptoGateway.decryptAes(TEST_CIPHERTEXT)).willReturn(testDecryptResponse(TEST_SOURCE_VALUE));

		String result = caasBasicEncryptionServiceDelegate.decrypt(new CiphertextContainer(testCryptoKey, Map.of("cipherText", TEST_CIPHERTEXT)));

		assertThat(result).isEqualTo(TEST_SOURCE_VALUE);
	}

	@Test
	void hmac() {
		String testHmacAlias = "pan";
		HmacHolder hmacHolder1 = new HmacHolder(testCryptoKey, TEST_SOURCE_VALUE, testHmacAlias);

		CryptoKey testCryptoKey2 = testCryptoKey();
		testCryptoKey2.setId("Different Key");
		String someOtherCaasKeyAlias = "SomeOtherCaasKeyAlias";
		testCryptoKey2.getConfiguration().put(CRYPTO_KEY_CONFIGURATION_CAAS_KEY_ALIAS_KEY, someOtherCaasKeyAlias);
		HmacHolder hmacHolder2 = new HmacHolder(testCryptoKey2, TEST_SOURCE_VALUE, testHmacAlias);

		UUID requestId1 = UUID.randomUUID();
		UUID requestId2 = UUID.randomUUID();
		Map<String, BatchResponseItem> batchResponseMap = new HashMap<>();
		String testBatchResponse1 = "TestBatchResponse1Message";
		String testBatchResponse2 = "TestBatchResponse2Message";
		BatchResponseItem batchResponseItem1 = new BatchResponseItem().setPayload(new PayloadResponse().setMessage(testBatchResponse1));
		BatchResponseItem batchResponseItem2 = new BatchResponseItem().setPayload(new PayloadResponse().setMessage(testBatchResponse2));
		batchResponseMap.put(requestId1.toString(), batchResponseItem1);
		batchResponseMap.put(requestId2.toString(), batchResponseItem2);

		try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
			uuid.when(UUID::randomUUID).thenReturn(requestId1, requestId2);

			given(mockCaasCryptoGateway.caasBatch(batchRequestArgumentCaptor.capture())).willReturn(batchResponseMap);


			caasBasicEncryptionServiceDelegate.hmac(List.of(hmacHolder1, hmacHolder2));

			BatchRequest capturedBatchRequest = batchRequestArgumentCaptor.getValue();
			assertThat(capturedBatchRequest.requests()).hasSize(2);

			BatchRequestItem firstHmacHolderBatchRequestItem = getBatchRequest(capturedBatchRequest, requestId1);
			BatchRequestItem secondHmacHolderBatchRequestItem = getBatchRequest(capturedBatchRequest, requestId2);

			assertThat(firstHmacHolderBatchRequestItem.getPath()).isEqualTo(HMAC_BATCH_PATH);
			assertThat(firstHmacHolderBatchRequestItem.getBatchRequestId()).isEqualTo(requestId1.toString());
			assertThat(firstHmacHolderBatchRequestItem.getMethod()).isEqualTo(HMAC_BATCH_METHOD);
			assertThat(firstHmacHolderBatchRequestItem.getPayload().getAlias()).isEqualTo(TEST_CAAS_KEY_ALIAS);
			assertThat(firstHmacHolderBatchRequestItem.getPayload().getMessage()).isEqualTo(TEST_SOURCE_VALUE);
			assertThat(secondHmacHolderBatchRequestItem.getPath()).isEqualTo(HMAC_BATCH_PATH);
			assertThat(secondHmacHolderBatchRequestItem.getBatchRequestId()).isEqualTo(requestId2.toString());
			assertThat(secondHmacHolderBatchRequestItem.getMethod()).isEqualTo(HMAC_BATCH_METHOD);
			assertThat(secondHmacHolderBatchRequestItem.getPayload().getAlias()).isEqualTo(someOtherCaasKeyAlias);
			assertThat(secondHmacHolderBatchRequestItem.getPayload().getMessage()).isEqualTo(TEST_SOURCE_VALUE);

			assertThat(hmacHolder1.getValue()).isEqualTo(testBatchResponse1);
			assertThat(hmacHolder2.getValue()).isEqualTo(testBatchResponse2);
		}
	}

	private static BatchRequestItem getBatchRequest(BatchRequest capturedBatchRequest, UUID requestId1) {
		return capturedBatchRequest.requests().stream()
				.filter(batchRequestItem -> batchRequestItem.getBatchRequestId().equals(requestId1.toString()))
				.findFirst().orElseThrow();
	}
}