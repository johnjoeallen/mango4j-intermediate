package ie.bitstep.mango.logging.test.data;

import ie.bitstep.mango.logging.LogProjection;
import ie.bitstep.mango.logging.annotations.LoggableClass;
import ie.bitstep.mango.logging.annotations.LoggableField;
import lombok.Data;

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
