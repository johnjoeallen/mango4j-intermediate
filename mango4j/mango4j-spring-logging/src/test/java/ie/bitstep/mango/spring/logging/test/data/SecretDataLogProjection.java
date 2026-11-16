package ie.bitstep.mango.spring.logging.test.data;

import ie.bitstep.mango.spring.logging.LogProjection;
import ie.bitstep.mango.spring.logging.annotations.LoggableClass;
import ie.bitstep.mango.spring.logging.annotations.LoggableField;
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
