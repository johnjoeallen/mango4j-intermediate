package com.mastercard.commons.mango.logging;

public interface LogProjection<T> {
	Class<T> getProjectedType();
}
