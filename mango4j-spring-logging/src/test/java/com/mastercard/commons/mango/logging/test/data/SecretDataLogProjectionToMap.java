package com.mastercard.commons.mango.logging.test.data;

import com.mastercard.commons.mango.logging.LogProjection;
import com.mastercard.commons.mango.logging.LogProjectionToMap;
import com.mastercard.commons.mango.logging.annotations.LoggableClass;
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
