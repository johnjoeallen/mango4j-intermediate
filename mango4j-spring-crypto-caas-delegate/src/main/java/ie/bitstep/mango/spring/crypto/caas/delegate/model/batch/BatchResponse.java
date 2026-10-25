package ie.bitstep.mango.spring.crypto.caas.delegate.model.batch;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.CaasResponse;

import java.util.List;

@DoNotMutate
public class BatchResponse implements CaasResponse {
	private List<BatchResponseItem> responses;
	private String requestId;

	public List<BatchResponseItem> getResponses() {
		return responses;
	}

	public BatchResponse setResponses(List<BatchResponseItem> responses) {
		this.responses = responses;
		return this;
	}

	@Override
	public String getRequestId() {
		return requestId;
	}

	public BatchResponse setRequestId(String requestId) {
		this.requestId = requestId;
		return this;
	}
}
