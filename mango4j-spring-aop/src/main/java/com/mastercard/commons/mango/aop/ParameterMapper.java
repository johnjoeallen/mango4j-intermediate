package com.mastercard.commons.mango.aop;

import lombok.experimental.UtilityClass;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public final class ParameterMapper {

	// SoftReference allows GC to reclaim metadata if memory is low
	private static final Map<Method, SoftReference<MethodParameterMetadata>> METHOD_CACHE = new ConcurrentHashMap<>();

	public static MappedMethodParameters map(JoinPoint joinPoint) {
		if (!(joinPoint.getSignature() instanceof MethodSignature methodSignature)) {
			throw new IllegalArgumentException("JoinPoint signature is not a MethodSignature");
		}

		Method method = methodSignature.getMethod();
		MethodParameterMetadata metadata = getOrLoadMetadata(method);

		return new MappedMethodParameters(metadata.getMethod(), joinPoint.getArgs());
	}

	static void clear() {
		METHOD_CACHE.clear();
	}

	static MethodParameterMetadata getOrLoadMetadata(Method method) {
		MethodParameterMetadata metadata = getFromCache(method);

		if (metadata == null) {
			metadata = new MethodParameterMetadata(method);
			METHOD_CACHE.put(method, new SoftReference<>(metadata));
		}

		return metadata;
	}

	static MethodParameterMetadata getFromCache(Method method) {
		SoftReference<MethodParameterMetadata> ref = METHOD_CACHE.get(method);
		return (ref != null) ? ref.get() : null;
	}
}
