package ie.bitstep.mango.spring.crypto.caas.delegate.model.batch;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.DecryptResponse;

import java.util.Objects;

@DoNotMutate
public class DecryptBatchResponseItem {

	private String batchRequestId;
	private String path;
	private int statusCode;
	private DecryptResponse payload;

	public String getBatchRequestId() {
		return batchRequestId;
	}

	public DecryptBatchResponseItem setBatchRequestId(String batchRequestId) {
		this.batchRequestId = batchRequestId;
		return this;
	}

	public String getPath() {
		return path;
	}

	public DecryptBatchResponseItem setPath(String path) {
		this.path = path;
		return this;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public DecryptBatchResponseItem setStatusCode(int statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	public DecryptResponse getPayload() {
		return payload;
	}

	public DecryptBatchResponseItem setPayload(DecryptResponse payload) {
		this.payload = payload;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DecryptBatchResponseItem that = (DecryptBatchResponseItem) o;
		return statusCode == that.statusCode && Objects.equals(batchRequestId, that.batchRequestId) && Objects.equals(path, that.path) && Objects.equals(payload, that.payload);
	}

	@Override
	public int hashCode() {
		return Objects.hash(batchRequestId, path, statusCode, payload);
	}
}
