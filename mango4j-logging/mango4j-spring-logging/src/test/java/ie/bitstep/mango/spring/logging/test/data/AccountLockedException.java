package ie.bitstep.mango.spring.logging.test.data;

import ie.bitstep.mango.logging.annotations.LoggableClass;
import ie.bitstep.mango.logging.annotations.LoggableField;
import ie.bitstep.mango.masking.IdMasker;
import ie.bitstep.mango.masking.Mask;

/**
 * Test exception with loggable fields; the account id is masked with {@link IdMasker}.
 */
@LoggableClass
public class AccountLockedException extends RuntimeException {

	@LoggableField(name = "accountId")
	@Mask(masker = IdMasker.class)
	private final String accountId;

	@LoggableField(name = "failedAttempts")
	private final int failedAttempts;

	public AccountLockedException(String message, String accountId, int failedAttempts, Throwable cause) {
		super(message, cause);
		this.accountId = accountId;
		this.failedAttempts = failedAttempts;
	}

	public String getAccountId() {
		return accountId;
	}

	public int getFailedAttempts() {
		return failedAttempts;
	}
}
