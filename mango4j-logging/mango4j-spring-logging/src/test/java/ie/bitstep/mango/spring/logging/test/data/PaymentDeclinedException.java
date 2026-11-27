package ie.bitstep.mango.spring.logging.test.data;

import ie.bitstep.mango.logging.annotations.LoggableClass;
import ie.bitstep.mango.logging.annotations.LoggableField;
import ie.bitstep.mango.masking.Mask;

/**
 * Test exception with loggable fields; the card number is masked (first and last four digits kept).
 */
@LoggableClass
public class PaymentDeclinedException extends RuntimeException {

	@LoggableField(name = "orderId")
	private final String orderId;

	@LoggableField(name = "cardNumber")
	@Mask(maskingChars = "*", prefix = 4, postfix = 4)
	private final String cardNumber;

	@LoggableField(name = "declineCode")
	private final String declineCode;

	public PaymentDeclinedException(String message, String orderId, String cardNumber, String declineCode, Throwable cause) {
		super(message, cause);
		this.orderId = orderId;
		this.cardNumber = cardNumber;
		this.declineCode = declineCode;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public String getDeclineCode() {
		return declineCode;
	}
}
