package ie.bitstep.mango.spring.crypto.caas.delegate.advanced;

import ie.bitstep.mango.crypto.core.domain.CiphertextContainer;
import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.spring.crypto.caas.delegate.CaasCryptoGateway;
import ie.bitstep.mango.spring.crypto.caas.delegate.config.CaasDelegateConfig;
import ie.bitstep.mango.spring.crypto.caas.delegate.config.RestTemplateBuilder;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.EncryptResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static ie.bitstep.mango.spring.crypto.caas.delegate.testdata.TestData.TEST_CAAS_KEY_ALIAS;
import static ie.bitstep.mango.spring.crypto.caas.delegate.testdata.TestData.TEST_IV;
import static ie.bitstep.mango.spring.crypto.caas.delegate.testdata.TestData.TEST_CIPHERTEXT;
import static ie.bitstep.mango.spring.crypto.caas.delegate.testdata.TestData.TEST_SOURCE_VALUE;
import static ie.bitstep.mango.spring.crypto.caas.delegate.testdata.TestData.testAdvancedEncryptResponse;
import static ie.bitstep.mango.spring.crypto.caas.delegate.testdata.TestData.testCryptoKey;
import static ie.bitstep.mango.spring.crypto.caas.delegate.testdata.TestData.testDecryptResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CaasAdvancedEncryptionServiceDelegateTest {

	private CaasAdvancedEncryptionServiceDelegate caasAdvancedEncryptionServiceDelegate;

	@Mock
	private CaasCryptoGateway mockCaasCryptoGateway;

	private final CaasDelegateConfig caasDelegateConfig = new CaasDelegateConfig();
	private CryptoKey testCryptoKey;

	@BeforeEach
	void setup() {
		try (MockedStatic<RestTemplateBuilder> restTemplateBuilder = Mockito.mockStatic(RestTemplateBuilder.class)) {
			restTemplateBuilder.when(() -> RestTemplateBuilder.build(any())).thenReturn(null);
			caasAdvancedEncryptionServiceDelegate = new CaasAdvancedEncryptionServiceDelegate(caasDelegateConfig);
		}
		ReflectionTestUtils.setField(caasAdvancedEncryptionServiceDelegate, "caasCryptoGateway", mockCaasCryptoGateway);
		testCryptoKey = testCryptoKey();
	}

	@Test
	void supportedCryptoKeyType() {
		assertThat(caasAdvancedEncryptionServiceDelegate.supportedCryptoKeyType()).isEqualTo("CAAS_ADVANCED");
	}

	@Test
	void encrypt() {
		EncryptResponse encryptResponse = testAdvancedEncryptResponse();
		given(mockCaasCryptoGateway.encryptAdvancedAes(TEST_SOURCE_VALUE, TEST_CAAS_KEY_ALIAS)).willReturn(encryptResponse);

		CiphertextContainer result = caasAdvancedEncryptionServiceDelegate.encrypt(testCryptoKey, TEST_SOURCE_VALUE);

		assertThat(result.getCryptoKey()).isEqualTo(testCryptoKey);
		assertThat(result.getData()).containsEntry("cipherText", encryptResponse.getCipher());
		assertThat(result.getData()).containsEntry("iv", encryptResponse.getIv());
	}

	@Test
	void decrypt() {
		Map<String, Object> encryptedData = Map.of(
				"cipherText", TEST_CIPHERTEXT,
				"iv", TEST_IV
		);

		given(mockCaasCryptoGateway.decryptAdvancedAes(TEST_CIPHERTEXT, TEST_CAAS_KEY_ALIAS, TEST_IV))
				.willReturn(testDecryptResponse(TEST_SOURCE_VALUE));

		String result = caasAdvancedEncryptionServiceDelegate.decrypt(new CiphertextContainer(testCryptoKey, encryptedData));

		assertThat(result).isEqualTo(TEST_SOURCE_VALUE);
	}
}