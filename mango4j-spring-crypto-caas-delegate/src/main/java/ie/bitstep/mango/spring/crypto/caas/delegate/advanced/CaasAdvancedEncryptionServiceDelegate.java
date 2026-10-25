package ie.bitstep.mango.spring.crypto.caas.delegate.advanced;

import ie.bitstep.mango.crypto.core.domain.CiphertextContainer;
import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.spring.crypto.caas.delegate.basic.CaasBasicEncryptionServiceDelegate;
import ie.bitstep.mango.spring.crypto.caas.delegate.config.CaasDelegateConfig;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.EncryptResponse;

import java.util.Map;

public class CaasAdvancedEncryptionServiceDelegate extends CaasBasicEncryptionServiceDelegate {

	public static final String CAAS_ADVANCE_KEY_TYPE = "CAAS_ADVANCED";
	private static final String IV = "iv";

	public CaasAdvancedEncryptionServiceDelegate(CaasDelegateConfig caasDelegateConfig) {
		super(caasDelegateConfig);
	}

	@Override
	public String supportedCryptoKeyType() {
		return CAAS_ADVANCE_KEY_TYPE;
	}

	@Override
	public CiphertextContainer encrypt(CryptoKey cryptoKey, String data) {
		EncryptResponse encryptResponse = caasCryptoGateway.encryptAdvancedAes(data, caasKeyAlias(cryptoKey));
		return new CiphertextContainer(cryptoKey, getData(encryptResponse));
	}

	@Override
	public String decrypt(CiphertextContainer ciphertextContainer) {
		var data = ciphertextContainer.getData();
		var cipherText = String.valueOf(data.get(CIPHERTEXT_FIELD_NAME));
		var iv = String.valueOf(data.get(IV));
		var keyAlias = caasKeyAlias(ciphertextContainer.getCryptoKey());

		return caasCryptoGateway.decryptAdvancedAes(cipherText, keyAlias, iv).getMessage();
	}

	private static String caasKeyAlias(CryptoKey cryptoKey) {
		return String.valueOf(cryptoKey.getConfiguration().get(CRYPTO_KEY_CONFIG_CAAS_KEY_ALIAS_FIELD_NAME));
	}

	private Map<String, Object> getData(EncryptResponse encryptResponse) {
		return Map.of(CIPHERTEXT_FIELD_NAME, encryptResponse.getCipher(), IV, encryptResponse.getIv());
	}

}