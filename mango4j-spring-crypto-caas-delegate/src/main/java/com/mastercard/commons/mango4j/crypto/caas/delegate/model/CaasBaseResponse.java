package com.mastercard.commons.mango4j.crypto.caas.delegate.model;

import com.mastercard.commons.mango4j.crypto.caas.delegate.config.DoNotMutate;

@DoNotMutate
public abstract class CaasBaseResponse implements CaasResponse {

	private String requestId;

	@Override
	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
}
