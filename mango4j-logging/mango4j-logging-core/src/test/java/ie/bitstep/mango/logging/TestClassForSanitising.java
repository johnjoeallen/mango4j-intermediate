package ie.bitstep.mango.logging;

import ie.bitstep.mango.logging.annotations.LoggableMethod;

public class TestClassForSanitising {

	@LoggableMethod
	public void first() {
		second();
	}

	public void second() {
		throw new RuntimeException("oops");
	}
}
