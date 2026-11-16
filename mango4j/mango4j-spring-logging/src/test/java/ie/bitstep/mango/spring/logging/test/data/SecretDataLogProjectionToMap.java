package ie.bitstep.mango.spring.logging.test.data;

import ie.bitstep.mango.spring.logging.LogProjection;
import ie.bitstep.mango.spring.logging.LogProjectionToMap;
import ie.bitstep.mango.spring.logging.annotations.LoggableClass;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
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
