package ie.bitstep.mango.spring.crypto.caas.delegate.model.wrapped;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@DoNotMutate
public class WrappedEncryptRequest {

	@JsonProperty("oaepHashingAlgorithm")
	private String oaepHashingAlgorithm;    // eg "SHA256" or "SHA512"

	@JsonProperty("message")
	private String message;     // plain (although base64 when sent to caas)

	@JsonProperty("certificate")
	private String certificate;      // pem

	public String getOaepHashingAlgorithm() {
		return oaepHashingAlgorithm;
	}

	public WrappedEncryptRequest setOaepHashingAlgorithm(String oaepHashingAlgorithm) {
		this.oaepHashingAlgorithm = oaepHashingAlgorithm;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public WrappedEncryptRequest setMessage(String message) {
		this.message = message;
		return this;
	}

	public String getCertificate() {
		return certificate;
	}

	public WrappedEncryptRequest setCertificate(String certificate) {
		this.certificate = certificate;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WrappedEncryptRequest that = (WrappedEncryptRequest) o;
		return Objects.equals(oaepHashingAlgorithm, that.oaepHashingAlgorithm) && Objects.equals(message, that.message) && Objects.equals(certificate, that.certificate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(oaepHashingAlgorithm, message, certificate);
	}
}
