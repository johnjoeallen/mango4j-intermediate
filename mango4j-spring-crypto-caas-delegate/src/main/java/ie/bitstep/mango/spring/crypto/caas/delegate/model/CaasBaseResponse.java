package ie.bitstep.mango.spring.crypto.caas.delegate.model;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

@DoNotMutate
public abstract class CaasBaseResponse implements CaasResponse {

	private String requestId;

	@Override
	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}
}
