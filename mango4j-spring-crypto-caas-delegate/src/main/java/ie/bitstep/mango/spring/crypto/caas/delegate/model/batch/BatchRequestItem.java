package ie.bitstep.mango.spring.crypto.caas.delegate.model.batch;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;
import org.springframework.http.HttpMethod;

@DoNotMutate
public class BatchRequestItem {
	private String path;
	private String method = HttpMethod.POST.toString().toLowerCase();
	private String batchRequestId;
	private PayLoadRequest payload;

	public String getPath() {
		return path;
	}

	public BatchRequestItem setPath(String path) {
		this.path = path;
		return this;
	}

	public String getMethod() {
		return method;
	}

	public BatchRequestItem setMethod(String method) {
		this.method = method;
		return this;
	}

	public String getBatchRequestId() {
		return batchRequestId;
	}

	public BatchRequestItem setBatchRequestId(String batchRequestId) {
		this.batchRequestId = batchRequestId;
		return this;
	}

	public PayLoadRequest getPayload() {
		return payload;
	}

	public BatchRequestItem setPayload(PayLoadRequest payload) {
		this.payload = payload;
		return this;
	}
}