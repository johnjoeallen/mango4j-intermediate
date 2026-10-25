package ie.bitstep.mango.spring.logging;

import ie.bitstep.mango.spring.logging.annotations.LoggableMethod;

public class TestClassForSanitising {

	@LoggableMethod
	public void first() {
		second();
	}

	public void second() {
		throw new RuntimeException("oops");
	}
}
