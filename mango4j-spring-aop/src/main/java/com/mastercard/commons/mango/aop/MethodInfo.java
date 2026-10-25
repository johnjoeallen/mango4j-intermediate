package com.mastercard.commons.mango.aop;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 *
 */
public class MethodInfo {
	@JsonIgnore
	private final Method method;

	@JsonIgnore
	private final Class<?> targetClass;

	@JsonProperty("class")
	private String canonicalClassName;

	@JsonProperty("name")
	private String methodName;

	@JsonIgnore
	private Logger logger;

	public MethodInfo(ProceedingJoinPoint joinPoint) {
		this(joinPoint.getTarget().getClass(), ((MethodSignature) joinPoint.getSignature()).getMethod());
	}

	/**
	 * @param targetClass
	 * @param method
	 */
	public MethodInfo(Class<?> targetClass, Method method) {
		this.method = method;
		this.targetClass = targetClass;

		if (method != null) {
			this.canonicalClassName = this.targetClass.getCanonicalName();
			this.logger = LoggerFactory.getLogger(this.targetClass);
			methodName = method.getName();
		}
	}

	/**
	 * @return
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * @param annotationClass
	 * @param <T>
	 * @return
	 */
	public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
		return method != null ? AnnotationUtils.findAnnotation(method, annotationClass) : null;
	}

	public <T extends Annotation> boolean hasDeclaredAnnotation(Class<T> annotationClass) {
		return method != null && AnnotationUtils.findAnnotation(method, annotationClass) != null;
	}

	/**
	 * @return
	 */
	@JsonIgnore
	public Annotation[][] getParameterAnnotations() {
		return method.getParameterAnnotations();
	}

	/**
	 * @return
	 */
	@JsonIgnore
	public Annotation[] getAnnotations() {
		return method.getAnnotations();
	}

	/**
	 * @return
	 */
	public Class<?> getTargetClass() {
		return targetClass;
	}

	/**
	 * @return
	 */
	public String getCanonicalClassName() {
		return canonicalClassName;
	}


	@JsonIgnore
	public String getSimpleClassName() {
		return targetClass.getSimpleName();
	}

	/**
	 * @return
	 */
	public String getMethodName() {
		return methodName;
	}

	public Logger logger() {
		return logger;
	}

	@Override
	public String toString() {
		return getSimpleClassName() + "." + getMethodName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MethodInfo that = (MethodInfo) o;
		return Objects.equals(method, that.method) &&
			Objects.equals(targetClass, that.targetClass);
	}

	@Override
	public int hashCode() {
		return Objects.hash(method, targetClass);
	}
}
