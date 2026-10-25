package com.mastercard.commons.mango4j.crypto.caas.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercard.commons.mango.crypto.core.factories.ConfigurableObjectMapperFactory;
import com.mastercard.commons.mango4j.crypto.caas.delegate.exceptions.CaasBatchItemException;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.DecryptRequest;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.DecryptResponse;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.EncryptRequest;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.EncryptResponse;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.HashPanRequest;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.HashPanResponse;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.BatchRequest;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.BatchResponse;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.BatchResponseItem;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.DecryptBatchRequest;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.DecryptBatchResponse;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch.DecryptBatchResponseItem;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.wrapped.WrappedDecryptRequest;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.wrapped.WrappedDecryptResponse;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.wrapped.WrappedEncryptRequest;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.wrapped.WrappedEncryptResponse;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasConstants.BATCH_URI;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasConstants.DECRYPT_ADVANCED_AES_URI;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasConstants.DECRYPT_AES_URI;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasConstants.ENCRYPT_ADVANCED_AES_URI;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasConstants.ENCRYPT_AES_URI;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasConstants.WRAPPED_DECRYPT_URI;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasConstants.WRAPPED_ENCRYPT_URI;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasUtils.base64Decode;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasUtils.base64Encode;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasUtils.toJsonString;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.ERROR;

public class CaasCryptoGateway {

	private static System.Logger logger = System.getLogger(CaasCryptoGateway.class.getName());

	private final ProcessCaasService processCaasService;
	private final String caasEndpointBase;

	public CaasCryptoGateway(ProcessCaasService processCaasService, String caasEndpointBase) {
		this.caasEndpointBase = caasEndpointBase;
		this.processCaasService = processCaasService;
	}

	public DecryptResponse decryptAes(String encryptedString) {
		return decryptInternal(encryptedString, DECRYPT_AES_URI, null, null);
	}

	public DecryptResponse decryptAdvancedAes(String encryptedString, String keyAlias, String iv) {
		return decryptInternal(encryptedString, DECRYPT_ADVANCED_AES_URI, keyAlias, iv);
	}

	private DecryptResponse decryptInternal(String encryptedString, String uri, String keyAlias, String iv) {
		DecryptRequest decryptRequest = new DecryptRequest().setCipher(encryptedString);
		if (keyAlias != null) {
			decryptRequest.setAlias(keyAlias);
		}
		if (iv != null) {
			decryptRequest.setIv(iv);
		}

		logger.log(DEBUG, "CaaS request to decrypt data: url={0} request={1}", caasEndpointBase + uri, decryptRequest);

		DecryptResponse decryptResponse = processCaasService.exchange(
				caasEndpointBase + uri,
				decryptRequest,
				DecryptResponse.class
		);

		decryptResponse.setMessage(base64Decode(decryptResponse.getMessage()));
		logger.log(DEBUG, "CaaS decrypt data response: requestId={0}", decryptResponse.getRequestId());

		return decryptResponse;
	}

	public Map<String, DecryptBatchResponseItem> decryptAes(DecryptBatchRequest decryptBatchRequest) {
		logger.log(DEBUG, "CaaS request to batch decrypt data: url={0} batchSize={1}:", caasEndpointBase + BATCH_URI, decryptBatchRequest.getRequests().size());
		DecryptBatchResponse decryptResponse = processCaasService.exchange(caasEndpointBase + BATCH_URI,
				decryptBatchRequest, DecryptBatchResponse.class);
		logger.log(DEBUG, "CaaS batch decrypt response: requestId={0}", decryptResponse.getRequestId());
		Map<String, DecryptBatchResponseItem> responseItemMap = new HashMap<>();
		decryptResponse.getResponses().forEach(responseItem -> {
			if (HttpStatus.valueOf(responseItem.getStatusCode()).is2xxSuccessful()) {
				// only decode if successful, otherwise won't be a base64 encoded string
				responseItem.getPayload().setMessage(base64Decode(responseItem.getPayload().getMessage()));
				responseItemMap.put(responseItem.getBatchRequestId(), responseItem);
			} else {
				String responseItemStr = toJsonString(objectMapper(), responseItem);
				logger.log(ERROR, "CaaS batch decrypt item failure: requestId={0}, error={1}", decryptResponse.getRequestId(), responseItemStr);
				throw CaasBatchItemException.createWithRequestIdAndBatchItem(decryptResponse.getRequestId(), responseItemStr);
			}
		});
		return responseItemMap;
	}

	public EncryptResponse encryptAes(String stringToEncrypt, String keyAlias) {
		return encryptInternal(stringToEncrypt, keyAlias, ENCRYPT_AES_URI);
	}

	public EncryptResponse encryptAdvancedAes(String stringToEncrypt, String keyAlias) {
		return encryptInternal(stringToEncrypt, keyAlias, ENCRYPT_ADVANCED_AES_URI);
	}

	private EncryptResponse encryptInternal(String stringToEncrypt, String keyAlias, String uri) {
		EncryptRequest encryptRequest = new EncryptRequest()
				.setAlias(keyAlias)
				.setMessage(base64Encode(stringToEncrypt));

		return processCaasService.exchange(
				caasEndpointBase + uri,
				encryptRequest,
				EncryptResponse.class
		);
	}

	public String hashPan(String unHashedString, String keyAlias) {
		return invokeHash(unHashedString, keyAlias, HashAlgorithm.PAN);
	}

	public String hash(String unHashedString, String keyAlias, HashAlgorithm hashAlgorithm) {
		return invokeHash(unHashedString, keyAlias, hashAlgorithm);
	}

	public Map<String, BatchResponseItem> caasBatch(BatchRequest batchRequest) {
		logger.log(DEBUG, "CaaS batch request: url={0} batchSize={1}", caasEndpointBase + BATCH_URI, batchRequest.requests().size());
		BatchResponse batchResponse = processCaasService.exchange(caasEndpointBase + BATCH_URI, batchRequest, BatchResponse.class);
		logger.log(DEBUG, "CaaS batch response: requestId={0}", batchResponse.getRequestId());
		Map<String, BatchResponseItem> responseItemMap = new HashMap<>();
		batchResponse.getResponses().forEach(responseItem -> {
			if (HttpStatus.valueOf(responseItem.getStatusCode()).is2xxSuccessful()) {
				responseItemMap.put(responseItem.getBatchRequestId(), responseItem);
			} else {
				String responseItemStr = toJsonString(objectMapper(), responseItem);
				logger.log(ERROR, "CaaS batch item failure: requestId={0}, error={1}", batchResponse.getRequestId(), responseItemStr);
				throw CaasBatchItemException.createWithRequestIdAndBatchItem(batchResponse.getRequestId(), responseItemStr);
			}
		});
		return responseItemMap;
	}

	public WrappedDecryptResponse wrappedDecrypt(WrappedDecryptRequest request) {
		WrappedDecryptResponse response = processCaasService.exchange(caasEndpointBase + WRAPPED_DECRYPT_URI,
				request, WrappedDecryptResponse.class);
		response.setMessage(base64Decode(response.getMessage()));
		return response;
	}

	public WrappedEncryptResponse wrappedEncrypt(WrappedEncryptRequest request) {
		request.setMessage(base64Encode(request.getMessage()));
		WrappedEncryptResponse response = processCaasService.exchange(caasEndpointBase + WRAPPED_ENCRYPT_URI,
				request, WrappedEncryptResponse.class);
		logger.log(DEBUG, "CaaS wrapped-encrypt data response: requestId={0}", response.getRequestId());
		return response;
	}

	private String invokeHash(String unHashedString, String keyAlias, HashAlgorithm hashAlgorithm) {
		HashPanRequest hashPanRequest = new HashPanRequest()
				.setAlias(keyAlias)
				.setMessage(base64Encode(unHashedString));
		HashPanResponse hashPanResponse = processCaasService.exchange(caasEndpointBase + hashAlgorithm.getUri(),
				hashPanRequest, HashPanResponse.class);
		logger.log(DEBUG, "CaaS hash response: requestId={0} hash={1}", hashPanResponse.getRequestId(), hashPanResponse.getHash());
		return hashPanResponse.getHash();
	}

	private ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ConfigurableObjectMapperFactory().objectMapper();
		objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
		return objectMapper;
	}
}