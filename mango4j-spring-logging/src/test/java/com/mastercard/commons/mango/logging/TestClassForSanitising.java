package com.mastercard.commons.mango.logging;

import com.mastercard.commons.mango.logging.annotations.LoggableMethod;

public class TestClassForSanitising {

	@LoggableMethod
	public void first() {
		second();
	}

	public void second() {
		throw new RuntimeException("oops");
	}
}
