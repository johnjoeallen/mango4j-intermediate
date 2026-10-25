package com.mastercard.commons.mango.logging.test.data;

import com.mastercard.commons.mango.logging.LogProjection;
import com.mastercard.commons.mango.logging.annotations.LoggableField;
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
