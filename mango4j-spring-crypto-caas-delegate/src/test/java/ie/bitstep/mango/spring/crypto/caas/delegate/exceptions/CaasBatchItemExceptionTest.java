package ie.bitstep.mango.spring.crypto.caas.delegate.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaasBatchItemExceptionTest {

	@Test
	void constructor() {
		RuntimeException cause = new RuntimeException("Test cause Exception Message");
		String testErrorMessage = "Test error message";

		CaasBatchItemException caasBatchItemException = new CaasBatchItemException(testErrorMessage, cause);

		assertThat(caasBatchItemException.getCause()).isSameAs(cause);
		assertThat(caasBatchItemException.getMessage()).isEqualTo(testErrorMessage);
	}

	@Test
	void createWithRequestIdAndBatchItem() {
		String testRequestId = "Test request ID";
		String testBatchItemResponse = "Test batch item response";

		CaasBatchItemException caasBatchItemException = CaasBatchItemException.createWithRequestIdAndBatchItem(testRequestId, testBatchItemResponse);

		assertThat(caasBatchItemException.getCause()).isNull();
		assertThat(caasBatchItemException.getMessage()).isEqualTo(String.format("CaaS batch item failure: requestId=%s, error=%s", testRequestId, testBatchItemResponse));
	}
}