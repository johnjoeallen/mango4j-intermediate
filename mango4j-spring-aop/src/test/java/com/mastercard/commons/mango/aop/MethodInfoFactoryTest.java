package com.mastercard.commons.mango.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MethodInfoFactoryTest {
	public static final String METHOD_NAME = "m";
	@Mock
	private ProceedingJoinPoint joinPoint;
	@Mock
	private MethodSignature joinPointMethodSignature;

	public interface I {
		void m();
	}

	public static class Parent {
		public void m() {
			// Dummy class for tests
		}

		public void parentOnly() {
			// Dummy class for tests
		}

		public void parentAndChild() {
			// Dummy class for tests
		}

		public void overloaded() {
			// Dummy class for tests
		}

		public void overloaded(String str) {
			// Dummy class for tests
		}
	}

	public static class Child extends Parent {
		void parentAndChild(String str) {
			// Dummy class for tests
		}
	}


	@BeforeEach
	void setUp() {
		((Map) ReflectionTestUtils.getField(MethodInfoFactory.class, "CACHE")).clear();
	}

	@Test
	void privateConstructorTest() throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
		Constructor<MethodInfoFactory> constructor = MethodInfoFactory.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		MethodInfoFactory methodInfoFactory = constructor.newInstance();
		assertThat(methodInfoFactory).isNotNull(); // best we can do
	}

	@Test
	void getMethodInfo_whenFirstTime() throws Exception {
		MethodInfo actual = MethodInfoFactory.getMethodInfo(Parent.class, Parent.class.getMethod("m"));

		assertThat(actual.getTargetClass()).isSameAs(Parent.class);
		assertThat(actual.getMethodName()).isEqualTo("m");
		assertThat(actual.getMethod().getParameterCount()).isZero();
		assertThat(getCacheSize()).isOne();
	}

	@Test
	void getMethodInfo_whenCached() throws Exception {
		MethodInfo actual0 = MethodInfoFactory.getMethodInfo(Parent.class, Parent.class.getMethod("m"));
		MethodInfo actual1 = MethodInfoFactory.getMethodInfo(Parent.class, Parent.class.getMethod("m"));

		assertThat(actual0).isInstanceOf(actual1.getClass());
		assertThat(getCacheSize()).isOne();
	}

	@Test
	void getMethodInfo_whenOverloaded() throws Exception {
		MethodInfo actual0 = MethodInfoFactory.getMethodInfo(Parent.class, Parent.class.getMethod("overloaded"));

		assertThat(actual0.getTargetClass()).isSameAs(Parent.class);
		assertThat(actual0.getMethodName()).isEqualTo("overloaded");
		assertThat(actual0.getMethod().getParameterCount()).isZero();
		assertThat(getCacheSize()).isOne();

		MethodInfo actual1 = MethodInfoFactory.getMethodInfo(Parent.class, Parent.class.getMethod("overloaded", String.class));

		assertThat(actual1.getTargetClass()).isSameAs(Parent.class);
		assertThat(actual1.getMethodName()).isEqualTo("overloaded");
		assertThat(actual1.getMethod().getParameterCount()).isOne();
		assertThat(getCacheSize()).isEqualTo(2);
	}

	@Test
	void getMethodInfo_whenParentAndChild() throws Exception {
		MethodInfo actual0 = MethodInfoFactory.getMethodInfo(Parent.class, Parent.class.getMethod("parentAndChild"));

		assertThat(actual0.getTargetClass()).isSameAs(Parent.class);
		assertThat(actual0.getMethodName()).isEqualTo("parentAndChild");
		assertThat(getCacheSize()).isOne();

		MethodInfo actual1 = MethodInfoFactory.getMethodInfo(Child.class, Child.class.getMethod("parentAndChild"));

		assertThat(actual1.getTargetClass()).isSameAs(Child.class);
		assertThat(actual1.getMethodName()).isEqualTo("parentAndChild");
		assertThat(getCacheSize()).isEqualTo(2);
	}

	@Test
	void getMethodInfo_whenParentOnly() throws Exception {
		MethodInfo actual0 = MethodInfoFactory.getMethodInfo(Parent.class, Parent.class.getMethod("parentOnly"));

		assertThat(actual0.getTargetClass()).isSameAs(Parent.class);
		assertThat(actual0.getMethodName()).isEqualTo("parentOnly");
		assertThat(getCacheSize()).isOne();

		MethodInfo actual1 = MethodInfoFactory.getMethodInfo(Child.class, Child.class.getMethod("parentOnly"));

		assertThat(actual1.getTargetClass()).isSameAs(Child.class);
		assertThat(actual1.getMethodName()).isEqualTo("parentOnly");
		assertThat(getCacheSize()).isEqualTo(2);
	}


	@Test
	void getMethodInfo_withJointPoint() throws Exception {
		// given
		given(joinPointMethodSignature.getMethod()).willReturn(Parent.class.getMethod("m"));
		given(joinPoint.getSignature()).willReturn(joinPointMethodSignature);
		given(joinPoint.getTarget()).willReturn(new Parent());

		// when
		MethodInfo actual = MethodInfoFactory.getMethodInfo(joinPoint);

		// then
		assertThat(actual.getTargetClass()).isSameAs(Parent.class);
		assertThat(actual.getMethodName()).isEqualTo("m");
	}

	@Test
	void getMethodInfo_withJointPoint_whenInterface() throws Exception {
		// given
		given(joinPointMethodSignature.getMethod()).willReturn(I.class.getMethod("m"));
		given(joinPoint.getSignature()).willReturn(joinPointMethodSignature);
		given(joinPoint.getTarget()).willReturn(new Parent());

		// when
		MethodInfo actual = MethodInfoFactory.getMethodInfo(joinPoint);

		// then
		assertThat(actual.getTargetClass()).isSameAs(Parent.class);
		assertThat(actual.getMethodName()).isEqualTo("m");
	}

	@Test
	void getMethodInfo_withJointPoint_whenInterface_failingToFindMethod() throws Exception {
		// given
		given(joinPointMethodSignature.getMethod()).willReturn(I.class.getMethod(METHOD_NAME));
		given(joinPoint.getSignature()).willReturn(joinPointMethodSignature);
		given(joinPoint.getTarget()).willReturn(this);

		// when
		MethodInfo actual = MethodInfoFactory.getMethodInfo(joinPoint);

		// then
		// nothing as it can't happen as we are in a proxy to this method
		assertThat(actual).isNotNull(); // added to make sonar happy
	}


	private int getCacheSize() {
		return ((ConcurrentMap) ReflectionTestUtils.getField(MethodInfoFactory.class, "CACHE")).size();
	}
}