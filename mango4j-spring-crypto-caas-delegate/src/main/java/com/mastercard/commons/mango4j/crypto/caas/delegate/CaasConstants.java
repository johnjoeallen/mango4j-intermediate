package com.mastercard.commons.mango4j.crypto.caas.delegate;

public class CaasConstants {
	private CaasConstants() {
	}

	public static final String SERVICE_NAME = "CaaS";
	public static final String DECRYPT_AES_URI = "/core/decrypt/aes";
	public static final String DECRYPT_ADVANCED_AES_URI = "/core/decrypt/aes/advanced";
	public static final String HASH_PAN_URI = "/core/hash/pan";
	public static final String ENCRYPT_AES_URI = "/core/encrypt/aes";
	public static final String ENCRYPT_ADVANCED_AES_URI = "/core/encrypt/aes/advanced";
	public static final String BATCH_URI = "/common/batch";
	public static final String WRAPPED_DECRYPT_URI = "/core/wrapped-decrypt";
	public static final String WRAPPED_ENCRYPT_URI = "/core/wrapped-encrypt/aes-256";
	public static final String REQUEST_ID_HEADER = "request-id";
	public static final String RESPONSE_TIME_HEADER = "response-time";
}