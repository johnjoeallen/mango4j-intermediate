package com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch;

import com.mastercard.commons.mango4j.crypto.caas.delegate.config.DoNotMutate;

@DoNotMutate
public class PayLoadRequest {
	private String alias;
	private String message;

	public String getAlias() {
		return alias;
	}

	public PayLoadRequest setAlias(String alias) {
		this.alias = alias;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public PayLoadRequest setMessage(String message) {
		this.message = message;
		return this;
	}
}
