package ie.bitstep.mango.spring.crypto.caas.delegate.model.batch;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;
import ie.bitstep.mango.spring.crypto.caas.delegate.model.DecryptRequest;
import org.springframework.http.HttpMethod;

import static ie.bitstep.mango.spring.crypto.caas.delegate.CaasConstants.DECRYPT_AES_URI;

@DoNotMutate
public class DecryptBatchRequestItem {
	private String path = DECRYPT_AES_URI;
	private String method = HttpMethod.POST.toString().toLowerCase();
	private String batchRequestId;
	private DecryptRequest payload;

	public String getPath() {
		return path;
	}

	public DecryptBatchRequestItem setPath(String path) {
		this.path = path;
		return this;
	}

	public String getMethod() {
		return method;
	}

	public DecryptBatchRequestItem setMethod(String method) {
		this.method = method;
		return this;
	}

	public String getBatchRequestId() {
		return batchRequestId;
	}

	public DecryptBatchRequestItem setBatchRequestId(String batchRequestId) {
		this.batchRequestId = batchRequestId;
		return this;
	}

	public DecryptRequest getPayload() {
		return payload;
	}

	public DecryptBatchRequestItem setPayload(DecryptRequest payload) {
		this.payload = payload;
		return this;
	}
}
