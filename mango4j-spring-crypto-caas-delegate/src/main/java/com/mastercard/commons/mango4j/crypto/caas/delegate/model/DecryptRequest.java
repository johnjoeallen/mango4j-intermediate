package com.mastercard.commons.mango4j.crypto.caas.delegate.model;

import com.mastercard.commons.mango4j.crypto.caas.delegate.config.DoNotMutate;

import java.util.Objects;

@DoNotMutate
public class DecryptRequest {

	private String cipher;
	private String alias;
	private String iv;

	public String getCipher() {
		return cipher;
	}

	public DecryptRequest setCipher(String cipher) {
		this.cipher = cipher;
		return this;
	}

	public String getAlias() {
		return alias;
	}

	public DecryptRequest setAlias(String alias) {
		this.alias = alias;
		return this;
	}

	public String getIv() {
		return iv;
	}

	public DecryptRequest setIv(String iv) {
		this.iv = iv;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DecryptRequest that = (DecryptRequest) o;
		return Objects.equals(alias, that.alias) && Objects.equals(cipher, that.cipher) && Objects.equals(iv, that.iv);
	}

	@Override
	public int hashCode() {
		return Objects.hash(alias, cipher, iv);
	}
}
