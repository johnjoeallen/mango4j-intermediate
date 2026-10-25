package ie.bitstep.mango.spring.crypto.caas.delegate.model.wrapped;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@DoNotMutate
public class WrappedEncryptResponse {

	@JsonProperty("requestId")
	private String requestId;

	@JsonProperty("cipher")
	private String cipher;      // base64

	@JsonProperty("iv")
	private String iv;          // base64

	@JsonProperty("wrappedKey")
	private String wrappedKey;  // base64

	public String getRequestId() {
		return requestId;
	}

	public WrappedEncryptResponse setRequestId(String requestId) {
		this.requestId = requestId;
		return this;
	}

	public String getCipher() {
		return cipher;
	}

	public WrappedEncryptResponse setCipher(String cipher) {
		this.cipher = cipher;
		return this;
	}

	public String getIv() {
		return iv;
	}

	public WrappedEncryptResponse setIv(String iv) {
		this.iv = iv;
		return this;
	}

	public String getWrappedKey() {
		return wrappedKey;
	}

	public WrappedEncryptResponse setWrappedKey(String wrappedKey) {
		this.wrappedKey = wrappedKey;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WrappedEncryptResponse that = (WrappedEncryptResponse) o;
		return Objects.equals(requestId, that.requestId) && Objects.equals(cipher, that.cipher) && Objects.equals(iv, that.iv) && Objects.equals(wrappedKey, that.wrappedKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestId, cipher, iv, wrappedKey);
	}
}
