package ie.bitstep.mango.spring.crypto.caas.delegate.model;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

@DoNotMutate
public class HashPanResponse extends CaasBaseResponse {

	private String hash;

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}
}
