package ie.bitstep.mango.logging.test.data;

import ie.bitstep.mango.logging.LogProjection;
import ie.bitstep.mango.logging.LogProjectionToMap;
import ie.bitstep.mango.logging.annotations.LoggableClass;

import java.util.Map;

@LoggableClass
public class SecretDataLogProjectionToMap implements LogProjection<SecretData>, LogProjectionToMap {
	@Override
	public Class<SecretData> getProjectedType() {
		return SecretData.class;
	}

	@Override
	public Map<String, Object> toMap(Object object) {
		SecretData secretData = (SecretData) object;
		return Map.of("cardNumber", secretData.getCardNumber());
	}
}
