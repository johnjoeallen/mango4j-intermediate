package com.mastercard.commons.mango.aop;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public final class MethodInfoFactory {

	private static final ConcurrentMap<Pair<Class<?>, Method>, MethodInfo> CACHE = new ConcurrentHashMap<>();

	/**
	 *
	 */
	private MethodInfoFactory() {
	}

	/**
	 * @param targetClass
	 * @param method
	 * @return
	 */
	public static MethodInfo getMethodInfo(Class<?> targetClass, Method method) {
		return CACHE.computeIfAbsent(ImmutablePair.of(targetClass, method),
			key -> new MethodInfo(key.getLeft(), key.getRight()));
	}

	/**
	 * @param joinPoint
	 * @return
	 */
	public static MethodInfo getMethodInfo(ProceedingJoinPoint joinPoint) {
		Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
		Class<?> targetClass = joinPoint.getTarget().getClass();

		return getMethodInfo(targetClass, method);
	}

}
