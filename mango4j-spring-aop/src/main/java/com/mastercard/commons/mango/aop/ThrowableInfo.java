package com.mastercard.commons.mango.aop;

import lombok.EqualsAndHashCode;

/**
 *
 */
@EqualsAndHashCode
public class ThrowableInfo {

	private Throwable throwable;
	private MethodInfo methodInfo;

	/**
	 * @param throwable
	 * @param methodInfo
	 */
	public ThrowableInfo(Throwable throwable, MethodInfo methodInfo) {
		this.throwable = throwable;
		this.methodInfo = methodInfo;
	}

	/**
	 * @return
	 */
	public Throwable getThrowable() {
		return throwable;
	}

	/**
	 * |
	 *
	 * @return
	 */
	public MethodInfo getMethodInfo() {
		return methodInfo;
	}
}
