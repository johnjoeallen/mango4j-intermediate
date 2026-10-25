package com.mastercard.commons.mango4j.crypto.caas.delegate.model;

import com.mastercard.commons.mango4j.crypto.caas.delegate.config.DoNotMutate;

import java.util.Objects;

@DoNotMutate
public class HashPanRequest {

	private String alias;
	private String message;

	public String getAlias() {
		return alias;
	}

	public HashPanRequest setAlias(String alias) {
		this.alias = alias;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public HashPanRequest setMessage(String message) {
		this.message = message;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HashPanRequest that = (HashPanRequest) o;
		return Objects.equals(alias, that.alias) && Objects.equals(message, that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(alias, message);
	}
}