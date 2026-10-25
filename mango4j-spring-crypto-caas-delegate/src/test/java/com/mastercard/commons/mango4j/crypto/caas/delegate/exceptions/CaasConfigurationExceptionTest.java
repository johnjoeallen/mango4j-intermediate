package com.mastercard.commons.mango4j.crypto.caas.delegate.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaasConfigurationExceptionTest {

	@Test
	void constructor() {
		String testErrorMessage = "Test error message";

		CaasConfigurationException caasBatchItemException = new CaasConfigurationException(testErrorMessage);

		assertThat(caasBatchItemException.getCause()).isNull();
		assertThat(caasBatchItemException.getMessage()).isEqualTo(testErrorMessage);
	}

	@Test
	void constructorWithCause() {
		RuntimeException cause = new RuntimeException("Test cause Exception Message");
		String testErrorMessage = "Test error message";

		CaasConfigurationException caasBatchItemException = new CaasConfigurationException(testErrorMessage, cause);

		assertThat(caasBatchItemException.getCause()).isSameAs(cause);
		assertThat(caasBatchItemException.getMessage()).isEqualTo(testErrorMessage);
	}
}