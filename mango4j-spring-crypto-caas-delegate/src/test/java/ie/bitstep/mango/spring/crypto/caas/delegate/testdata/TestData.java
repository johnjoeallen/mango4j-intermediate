package ie.bitstep.mango.spring.crypto.caas.delegate.testdata;

import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.DecryptResponse;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.EncryptResponse;

import java.util.HashMap;
import java.util.Map;

public class TestData {
	public static final String TEST_CAAS_KEY_ALIAS = "TestCaasKeyAlias";
	public static final String CRYPTO_KEY_CONFIGURATION_CAAS_KEY_ALIAS_KEY = "keyAlias";
	public static final String TEST_CRYPTO_KEY_ID = "testCryptoKeyId";
	public static final String TEST_CIPHERTEXT = "TestCiphertext";
	public static final String TEST_IV = "TestIV";
	public static final String TEST_SOURCE_VALUE = "TestValue";

	public static CryptoKey testCryptoKey() {
		CryptoKey cryptoKey = new CryptoKey();
		cryptoKey.setId(TEST_CRYPTO_KEY_ID);
		Map<String, Object> keyConfiguration = new HashMap<>();
		keyConfiguration.put(CRYPTO_KEY_CONFIGURATION_CAAS_KEY_ALIAS_KEY, TEST_CAAS_KEY_ALIAS);
		cryptoKey.setConfiguration(keyConfiguration);
		return cryptoKey;
	}

	public static EncryptResponse testEncryptResponse() {
		EncryptResponse encryptResponse = new EncryptResponse();
		encryptResponse.setCipher(TEST_CIPHERTEXT);
		return encryptResponse;
	}

	public static EncryptResponse testAdvancedEncryptResponse() {
		EncryptResponse encryptResponse = new EncryptResponse();
		encryptResponse.setCipher(TEST_CIPHERTEXT);
		encryptResponse.setIv(TEST_IV);
		return encryptResponse;
	}

	public static DecryptResponse testDecryptResponse(String message) {
		DecryptResponse decryptResponse = new DecryptResponse();
		decryptResponse.setMessage(message);
		return decryptResponse;
	}
}
