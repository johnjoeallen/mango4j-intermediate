package com.mastercard.commons.mango.logging.test;

import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.logging.dc.DiagnosticContext;
import com.mastercard.commons.mango.logging.dc.DiagnosticContextThreadLocal;

import java.lang.reflect.Method;

/**
 * Helper methods for testing logging, and use of logging in applications.
 */
public class TestUtils {

	/**
	 * This class is to be removed in future as there should be other
	 * ways of enabling testing
	 */
	private TestUtils() {
	}

	public static void fakeLoggable() {
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();

		dc.push(new MethodInfo(TestUtils.class, getFakeLoggableMethod("fakeLoggable")));
	}

	public static void fakeLoggable(Object o, String method) throws NoSuchMethodException {
		DiagnosticContext dc = new DiagnosticContext();
		dc.push(new MethodInfo(o.getClass(), o.getClass().getMethod(method)));
	}

	static Method getFakeLoggableMethod(String methodName) {
		try {
			return TestUtils.class.getMethod(methodName);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
}
