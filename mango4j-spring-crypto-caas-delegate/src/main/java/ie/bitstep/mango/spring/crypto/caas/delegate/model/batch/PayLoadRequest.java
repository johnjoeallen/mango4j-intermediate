package ie.bitstep.mango.spring.crypto.caas.delegate.model.batch;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

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
