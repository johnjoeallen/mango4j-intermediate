package com.mastercard.commons.mango.logging.internal;

/**
 * Classes that can convert themselves to a form suitable for logging (usually a map).
 */
@FunctionalInterface
public interface ToLoggable {

	Object toLoggable();
}
