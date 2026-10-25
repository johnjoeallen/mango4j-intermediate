package ie.bitstep.mango.spring.crypto.caas.delegate.exceptions;

/**
 * CaaS Gateway configuration failure.
 */
public class CaasConfigurationException extends RuntimeException {

	public CaasConfigurationException(String message) {
		super(message);
	}

	public CaasConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

}
