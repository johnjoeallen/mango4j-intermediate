package ie.bitstep.mango.spring.crypto.caas.delegate.basic;

import ie.bitstep.mango.spring.crypto.caas.delegate.CaasCryptoGateway;
import ie.bitstep.mango.spring.crypto.caas.delegate.ProcessCaasService;
import ie.bitstep.mango.spring.crypto.caas.delegate.config.CaasDelegateConfig;
import ie.bitstep.mango.spring.crypto.caas.delegate.config.RestTemplateBuilder;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.BatchRequest;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.BatchRequestItem;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.BatchResponseItem;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.batch.PayLoadRequest;
import ie.bitstep.mango.crypto.core.domain.CiphertextContainer;
import ie.bitstep.mango.crypto.core.domain.CryptoKey;
import ie.bitstep.mango.crypto.core.domain.HmacHolder;
import ie.bitstep.mango.crypto.core.encryption.EncryptionServiceDelegate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static ie.bitstep.mango.spring.crypto.caas.delegate.CaasConstants.HASH_PAN_URI;
import static java.util.stream.Collectors.toMap;

public class CaasBasicEncryptionServiceDelegate extends EncryptionServiceDelegate {

	public static final String CAAS_BASIC_KEY_TYPE = "CAAS_BASIC";

	protected static final String CIPHERTEXT_FIELD_NAME = "cipherText";
	protected static final String CRYPTO_KEY_CONFIG_CAAS_KEY_ALIAS_FIELD_NAME = "keyAlias";

	protected final CaasCryptoGateway caasCryptoGateway;

	public CaasBasicEncryptionServiceDelegate(CaasDelegateConfig caasDelegateConfig) {
		ProcessCaasService processCaasService = new ProcessCaasService(RestTemplateBuilder.build(caasDelegateConfig));
		this.caasCryptoGateway = new CaasCryptoGateway(processCaasService, caasDelegateConfig.getCaasEndpointBase());
	}

	@Override
	public String supportedCryptoKeyType() {
		return CAAS_BASIC_KEY_TYPE;
	}

	@Override
	public CiphertextContainer encrypt(CryptoKey cryptoKey, String data) {
		return new CiphertextContainer(cryptoKey, Map.of(CIPHERTEXT_FIELD_NAME, caasCryptoGateway.encryptAes(data, caasKeyAlias(cryptoKey)).getCipher()));
	}

	@Override
	public String decrypt(CiphertextContainer ciphertextContainer) {
		return caasCryptoGateway.decryptAes(String.valueOf(ciphertextContainer.getData().get(CIPHERTEXT_FIELD_NAME))).getMessage();
	}

	@Override
	public void hmac(Collection<HmacHolder> hmacHolders) {
		Map<String, HmacHolder> requestIdToHmacHolderMap = hmacHolders.stream()
				.collect(toMap(hmacHolder -> UUID.randomUUID().toString(), Function.identity()));

		List<BatchRequestItem> batchRequestItems = requestIdToHmacHolderMap.entrySet().stream()
				.map(hmacHolder -> new BatchRequestItem()
						.setBatchRequestId(hmacHolder.getKey())
						.setPayload(new PayLoadRequest()
								.setAlias(caasKeyAlias(hmacHolder.getValue().getCryptoKey()))
								.setMessage(hmacHolder.getValue().getValue()))
						.setPath(HASH_PAN_URI))
				.toList();

		BatchRequest batchRequest = new BatchRequest(batchRequestItems);
		Map<String, BatchResponseItem> batchResponse = caasCryptoGateway.caasBatch(batchRequest);
		requestIdToHmacHolderMap.forEach((requestId, hmacHolder) -> hmacHolder.setValue(batchResponse.get(requestId).getPayload().getMessage()));
	}

	private static String caasKeyAlias(CryptoKey cryptoKey) {
		return String.valueOf(cryptoKey.getConfiguration().get(CRYPTO_KEY_CONFIG_CAAS_KEY_ALIAS_FIELD_NAME));
	}

}