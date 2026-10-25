package ie.bitstep.mango.spring.crypto.caas.delegate.model;

import ie.bitstep.mango.spring.crypto.caas.delegate.config.DoNotMutate;

@DoNotMutate
public class EncryptResponse extends CaasBaseResponse {

	public String getCipher() {
		return cipher;
	}

	public void setCipher(String cipher) {
		this.cipher = cipher;
	}

	private String cipher;

	private String iv;

	public String getIv() {
		return iv;
	}

	public void setIv(String iv) {
		this.iv = iv;
	}
}
