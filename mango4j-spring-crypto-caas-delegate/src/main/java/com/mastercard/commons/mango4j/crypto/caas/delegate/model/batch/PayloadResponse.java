package com.mastercard.commons.mango4j.crypto.caas.delegate.model.batch;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mastercard.commons.mango4j.crypto.caas.delegate.config.DoNotMutate;
import com.mastercard.commons.mango4j.crypto.caas.delegate.model.CaasBaseResponse;

@JsonIgnoreProperties(
		ignoreUnknown = true
)
@DoNotMutate
public class PayloadResponse extends CaasBaseResponse {

	@JsonProperty("message")
	@JsonAlias({"cipher", "hash"})
	private String message;

	public String getMessage() {
		return message;
	}

	public PayloadResponse setMessage(String message) {
		this.message = message;
		return this;
	}
}
