package com.mastercard.commons.mango.logging.test.data;

import com.mastercard.commons.mango.logging.LogProjection;
import com.mastercard.commons.mango.logging.annotations.LoggableClass;
import com.mastercard.commons.mango.logging.annotations.LoggableField;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@LoggableClass
@Data
public class SecretDataLogProjection implements LogProjection<SecretData> {
	@LoggableField
	private String cardNumber;

	@Override
	public Class<SecretData> getProjectedType() {
		return SecretData.class;
	}
}
