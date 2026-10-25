package com.mastercard.commons.mango.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MethodInfoTest {
	@Mock
	private ProceedingJoinPoint proceedingJoinPoint;

	@Mock
	private MethodSignature methodSignature;

	@Tag("testMethod1")
	public void testMethod1() {
		// Dummy class for tests
	}

	@Tag("testMethod2")
	public void testMethod2() {
		// Dummy class for tests
	}

	@Test
	void testMethodInfo() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);
		String s = mi.toString();
		assertEquals("MethodInfoTest.testMethod1", s);
		assertNotNull(mi.getMethod());
		assertNotNull(mi.getTargetClass());
		assertNotNull(mi.getCanonicalClassName());
		assertNotNull(mi.getMethodName());
		assertNotNull(mi.getParameterAnnotations());
		assertNotNull(mi.logger());
	}

	@Test
	void testMethodInfoJoinPoint() throws NoSuchMethodException {
		// GIVEN

		// WHEN
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethod1"));
		MethodInfo mi = new MethodInfo(proceedingJoinPoint);
		String s = mi.toString();

		// THEN
		assertEquals("MethodInfoTest.testMethod1", s);
		assertNotNull(mi.getMethod());
		assertNotNull(mi.getTargetClass());
		assertNotNull(mi.getCanonicalClassName());
		assertNotNull(mi.getMethodName());
		assertNotNull(mi.getParameterAnnotations());
		assertNotNull(mi.logger());
	}

	@Test
	void getMethodMethodIsNull() {
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, null);

		assertThat(mi.getMethod()).isNull();
	}

	@Test
	void getMethod() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.getMethod()).isInstanceOf(method.getClass());
	}

	@Test
	void hasDeclaredAnnotationMethodExistsAndHasAnnotation() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.hasDeclaredAnnotation(Tag.class)).isTrue();
	}

	@Test
	void doesNotHaveDeclaredAnnotation() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.hasDeclaredAnnotation(Override.class)).isFalse();
	}

	@Test
	void hasDeclaredAnnotationNullMethod() {
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, null);

		assertThat(mi.hasDeclaredAnnotation(Test.class)).isFalse();
	}

	@Test
	void getDeclaredAnnotation() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.getDeclaredAnnotation(Tag.class)).withFailMessage("Has expected annotation").isInstanceOf(Tag.class);
	}

	@Test
	void getDeclaredAnnotationMethodIsNull() {
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, null);

		assertThat(mi.getDeclaredAnnotation(Test.class)).withFailMessage("Is not null").isNull();
	}

	@Test
	void getParameterAnnotations() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);
		Annotation[][] parameterAnnotations = mi.getParameterAnnotations();

		assertThat(parameterAnnotations).withFailMessage("Has expected annotation").isEmpty();
	}

	@Test
	void getAnnotations() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.getAnnotations()).withFailMessage("Has expected annotation").hasSize(1);
		assertThat(mi.getAnnotations()[0]).withFailMessage("Has expected annotation").isInstanceOf(Tag.class);
	}

	@Test
	void getTargetClass() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.getTargetClass()).withFailMessage("Target class is expected class").isEqualTo(MethodInfoTest.class);
	}

	@Test
	void getClassName() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.getCanonicalClassName()).withFailMessage("Class name is expected class name").isEqualTo(MethodInfoTest.class.getCanonicalName());
	}

	@Test
	void getMethodName() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.getMethodName()).withFailMessage("Method name is expected method name").isEqualTo(method.getName());
	}

	@Test
	void logger() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.logger()).withFailMessage("Logger is not expected logger").isInstanceOf(ch.qos.logback.classic.Logger.class);
	}

	@Test
	void testToString() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi).withFailMessage("toString() expected output").hasToString("MethodInfoTest.testMethod1");
	}

	@Test
	void testEqualsIdentity() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.equals(mi)).withFailMessage("equals() expected output").isTrue();
	}

	@Test
	void testEqualsWrongClass() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.equals(new String("hello"))).withFailMessage("equals() expected output").isFalse();
	}

	@Test
	void testEqualsNull() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.equals(null)).withFailMessage("equals() expected output").isFalse();
	}

	@Test
	void testEqualsSame() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);

		assertThat(mi.equals(new MethodInfo(MethodInfoTest.class, method))).withFailMessage("equals() expected output").isTrue();
	}

	@Test
	void testEqualsMethodSameTargetDifferent() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(String.class, method);

		assertThat(mi.equals(new MethodInfo(MethodInfoTest.class, method))).withFailMessage("equals() expected output").isFalse();
	}

	@Test
	void testEqualsMethodDifferentTargetSame() throws NoSuchMethodException {
		Method method1 = MethodInfoTest.class.getMethod("testMethod1");
		Method method2 = MethodInfoTest.class.getMethod("testMethod2");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method1);

		assertThat(mi.equals(new MethodInfo(MethodInfoTest.class, method2))).withFailMessage("equals() expected output").isFalse();
	}

	@Test
	void testHashCode() throws NoSuchMethodException {
		Method method = MethodInfoTest.class.getMethod("testMethod1");
		MethodInfo mi = new MethodInfo(MethodInfoTest.class, method);
		int expectedHash = Objects.hash(method, MethodInfoTest.class);

		assertThat(mi.hashCode()).isEqualTo(expectedHash);
	}
}
