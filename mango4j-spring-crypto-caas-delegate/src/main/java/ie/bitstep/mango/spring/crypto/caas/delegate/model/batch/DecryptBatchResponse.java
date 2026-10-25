package ie.bitstep.mango.spring.crypto.caas.delegate.model.batch;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.CaasResponse;

import java.util.List;

@DoNotMutate
public class DecryptBatchResponse implements CaasResponse {

	private List<DecryptBatchResponseItem> responses;
	private String requestId;

	public List<DecryptBatchResponseItem> getResponses() {
		return responses;
	}

	public DecryptBatchResponse setResponses(List<DecryptBatchResponseItem> responses) {
		this.responses = responses;
		return this;
	}

	@Override
	public String getRequestId() {
		return requestId;
	}

	public DecryptBatchResponse setRequestId(String requestId) {
		this.requestId = requestId;
		return this;
	}
}
