package com.mastercard.commons.mango4j.crypto.caas.delegate;

public enum HashAlgorithm {

	PAN("/core/hash/pan"),
	SHA256("/core/hash/sha256"),
	SHA512("/core/hash/sha512");

	HashAlgorithm(String uri) {
		this.uri = uri;
	}

	private String uri;

	public String getUri() {
		return uri;
	}
}
