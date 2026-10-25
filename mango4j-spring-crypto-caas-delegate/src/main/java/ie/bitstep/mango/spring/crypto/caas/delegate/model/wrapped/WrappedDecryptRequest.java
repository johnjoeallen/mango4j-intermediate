package ie.bitstep.mango.spring.crypto.caas.delegate.model.wrapped;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@DoNotMutate
public class WrappedDecryptRequest {

	@JsonProperty("oaepHashingAlgorithm")
	private String oaepHashingAlgorithm;    // eg "SHA256" or "SHA512"

	@JsonProperty("cipher")
	private String cipher;      // base64

	@JsonProperty("iv")
	private String iv;          // base64

	@JsonProperty("wrappedKey")
	private String wrappedKey;              // base64

	@JsonProperty("encryptedPrivateKey")
	private String encryptedPrivateKey;     // base64

	public String getOaepHashingAlgorithm() {
		return oaepHashingAlgorithm;
	}

	public WrappedDecryptRequest setOaepHashingAlgorithm(String oaepHashingAlgorithm) {
		this.oaepHashingAlgorithm = oaepHashingAlgorithm;
		return this;
	}

	public String getCipher() {
		return cipher;
	}

	public WrappedDecryptRequest setCipher(String cipher) {
		this.cipher = cipher;
		return this;
	}

	public String getIv() {
		return iv;
	}

	public WrappedDecryptRequest setIv(String iv) {
		this.iv = iv;
		return this;
	}

	public String getWrappedKey() {
		return wrappedKey;
	}

	public WrappedDecryptRequest setWrappedKey(String wrappedKey) {
		this.wrappedKey = wrappedKey;
		return this;
	}

	public String getEncryptedPrivateKey() {
		return encryptedPrivateKey;
	}

	public WrappedDecryptRequest setEncryptedPrivateKey(String encryptedPrivateKey) {
		this.encryptedPrivateKey = encryptedPrivateKey;
		return this;
	}
}
