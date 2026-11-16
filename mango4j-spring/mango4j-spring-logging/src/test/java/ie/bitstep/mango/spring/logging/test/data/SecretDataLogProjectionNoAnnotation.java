package ie.bitstep.mango.spring.logging.test.data;

import ie.bitstep.mango.spring.logging.LogProjection;
import ie.bitstep.mango.spring.logging.annotations.LoggableField;
import org.springframework.stereotype.Component;

@Component
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
