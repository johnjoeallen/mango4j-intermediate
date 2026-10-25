package ie.bitstep.mango.spring.crypto.caas.delegate.model;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

import java.util.Objects;

@DoNotMutate
public class EncryptRequest {

	private String alias;
	private String message;

	public String getAlias() {
		return alias;
	}

	public EncryptRequest setAlias(String alias) {
		this.alias = alias;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public EncryptRequest setMessage(String message) {
		this.message = message;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EncryptRequest that = (EncryptRequest) o;
		return Objects.equals(alias, that.alias) && Objects.equals(message, that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(alias, message);
	}
}
