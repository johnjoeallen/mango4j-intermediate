package com.mastercard.commons.mango.logging;

import com.mastercard.commons.mango.logging.dc.DiagnosticContext;
import com.mastercard.commons.mango.logging.dc.DiagnosticContextThreadLocal;
import lombok.experimental.UtilityClass;

import java.util.Map;

@UtilityClass
public class LogContext {
	public static void put(String key, Object value) {
		DiagnosticContextThreadLocal.get().put(key, value);
	}

	public static Object get(String key) {
		return DiagnosticContextThreadLocal.get().get(key);
	}

	public static boolean contains(String key) {
		return DiagnosticContextThreadLocal.get().contains(key);
	}

	public static void map(String key, Object... values) {
		DiagnosticContextThreadLocal.get().map(key, values);
	}

	public static void add(String key, Object... values) {
		DiagnosticContextThreadLocal.get().add(key, values);
	}

	public static void addMap(String key, Object... values) {
		DiagnosticContextThreadLocal.get().addMap(key, values);
	}

	public static Map<String, Object> toLoggable() {
		return DiagnosticContextThreadLocal.get().toLoggable();
	}

	public static void putThrowable(Throwable t) {
		DiagnosticContextThreadLocal.get().putThrowable(t);
	}

	public static void putPreviousCallThrowable(Throwable t) {
		DiagnosticContextThreadLocal.get().putPreviousCallThrowable(t);
	}

	public static Throwable getThrowable() {
		return DiagnosticContextThreadLocal.get().getThrowable();
	}

	public static Object getStat(String key) {
		return DiagnosticContextThreadLocal.get().getStat(key);
	}

	public static void putStat(String key, Object value) {
		DiagnosticContextThreadLocal.get().putStatValue(key, value);
	}

	public DiagnosticContext copy(DiagnosticContext from) {
		return DiagnosticContextThreadLocal.get().copy(from);
	}
}