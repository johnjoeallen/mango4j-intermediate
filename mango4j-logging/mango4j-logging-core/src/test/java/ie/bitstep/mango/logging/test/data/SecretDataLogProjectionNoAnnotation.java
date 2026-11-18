package ie.bitstep.mango.logging.test.data;

import ie.bitstep.mango.logging.LogProjection;
import ie.bitstep.mango.logging.annotations.LoggableField;

public class SecretDataLogProjectionNoAnnotation implements LogProjection<SecretData> {
	@Override
	public Class<SecretData> getProjectedType() {
		return SecretData.class;
	}

	@LoggableField
	public String getCardNumber(SecretData secretData) {
		return secretData.getCardNumber();
	}
}
