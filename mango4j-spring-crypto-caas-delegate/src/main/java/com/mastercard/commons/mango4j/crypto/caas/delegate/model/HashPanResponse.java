package com.mastercard.commons.mango4j.crypto.caas.delegate.model;

import com.mastercard.commons.mango4j.crypto.caas.delegate.config.DoNotMutate;

@DoNotMutate
public class HashPanResponse extends CaasBaseResponse {

	private String hash;

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}
}
