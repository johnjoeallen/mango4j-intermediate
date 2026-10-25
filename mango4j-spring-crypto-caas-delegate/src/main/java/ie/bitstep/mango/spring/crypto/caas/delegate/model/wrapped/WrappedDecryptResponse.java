package ie.bitstep.mango.spring.crypto.caas.delegate.model.wrapped;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@DoNotMutate
public class WrappedDecryptResponse {

	@JsonProperty("requestId")
	private String requestId;

	@JsonProperty("message")
	private String message;     // plain (although base64 when received from caas)

	public String getRequestId() {
		return requestId;
	}

	public WrappedDecryptResponse setRequestId(String requestId) {
		this.requestId = requestId;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public WrappedDecryptResponse setMessage(String message) {
		this.message = message;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WrappedDecryptResponse that = (WrappedDecryptResponse) o;
		return Objects.equals(requestId, that.requestId) && Objects.equals(message, that.message);
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestId, message);
	}
}
