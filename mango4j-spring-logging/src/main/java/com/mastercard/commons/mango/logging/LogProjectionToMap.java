package com.mastercard.commons.mango.logging;

import java.util.Map;

public interface LogProjectionToMap<T> {
	Map<String, Object> toMap(T object);
}
