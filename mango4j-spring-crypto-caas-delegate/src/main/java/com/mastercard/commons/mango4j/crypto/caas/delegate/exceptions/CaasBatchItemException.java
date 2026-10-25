package com.mastercard.commons.mango4j.crypto.caas.delegate.exceptions;

/**
 * Failure of an item in a CaaS batch operation.
 */
public class CaasBatchItemException extends CaasCryptoException {

	public CaasBatchItemException(String message, Throwable cause) {
		super(message, cause);
	}

	public static CaasBatchItemException createWithRequestIdAndBatchItem(String requestId, String batchItemStr) {
		return new CaasBatchItemException(
				String.format("CaaS batch item failure: requestId=%s, error=%s", requestId, batchItemStr),
				null);
	}
}
