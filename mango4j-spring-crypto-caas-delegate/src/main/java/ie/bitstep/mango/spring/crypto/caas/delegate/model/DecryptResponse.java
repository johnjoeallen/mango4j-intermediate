package ie.bitstep.mango.spring.crypto.caas.delegate.model;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

import java.util.Objects;

@DoNotMutate
public class DecryptResponse extends CaasBaseResponse {

	private String message;
	private String rotatedCipher;
	private String rotatedKeyAlias;

	public String getMessage() {
		return message;
	}

	public DecryptResponse setMessage(String message) {
		this.message = message;
		return this;
	}

	public String getRotatedCipher() {
		return rotatedCipher;
	}

	public DecryptResponse setRotatedCipher(String rotatedCipher) {
		this.rotatedCipher = rotatedCipher;
		return this;
	}

	public String getRotatedKeyAlias() {
		return rotatedKeyAlias;
	}

	public DecryptResponse setRotatedKeyAlias(String rotatedKeyAlias) {
		this.rotatedKeyAlias = rotatedKeyAlias;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DecryptResponse that = (DecryptResponse) o;
		return Objects.equals(message, that.message) && Objects.equals(rotatedCipher, that.rotatedCipher) && Objects.equals(rotatedKeyAlias, that.rotatedKeyAlias);
	}

	@Override
	public int hashCode() {
		return Objects.hash(message, rotatedCipher, rotatedKeyAlias);
	}
}
