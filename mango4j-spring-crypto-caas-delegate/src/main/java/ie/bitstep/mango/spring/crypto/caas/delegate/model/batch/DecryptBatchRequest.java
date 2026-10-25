package ie.bitstep.mango.spring.crypto.caas.delegate.model.batch;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

import java.util.ArrayList;
import java.util.List;

@DoNotMutate
public class DecryptBatchRequest {

	private List<DecryptBatchRequestItem> requests = new ArrayList<>();

	public List<DecryptBatchRequestItem> getRequests() {
		return requests;
	}

	public DecryptBatchRequest setRequests(List<DecryptBatchRequestItem> requests) {
		this.requests = requests;
		return this;
	}
}
