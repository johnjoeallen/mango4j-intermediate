package com.mastercard.commons.mango.logging;

import com.mastercard.commons.mango.logging.dc.DiagnosticContextThreadLocal;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class RootLogContext {
	public static void put(String key, Object value) {
		DiagnosticContextThreadLocal.get().getRootContext().put(key, value);
	}

	public static Object get(String key) {
		return DiagnosticContextThreadLocal.get().getRootContext().get(key);
	}

	public static boolean contains(String key) {
		return DiagnosticContextThreadLocal.get().getRootContext().contains(key);
	}

	public static void map(String key, Object... values) {
		DiagnosticContextThreadLocal.get().getRootContext().map(key, values);
	}

	public static void add(String key, Object... values) {
		DiagnosticContextThreadLocal.get().getRootContext().add(key, values);
	}

	public static void addMap(String key, Object... values) {
		DiagnosticContextThreadLocal.get().getRootContext().addMap(key, values);
	}

	public static Map<String, Object> toLoggable() {
		return DiagnosticContextThreadLocal.get().getRootContext().toLoggable();
	}
}
