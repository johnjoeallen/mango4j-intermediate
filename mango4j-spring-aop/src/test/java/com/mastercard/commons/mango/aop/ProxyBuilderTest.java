package com.mastercard.commons.mango.aop;

import org.aopalliance.aop.Advice;
import org.aspectj.lang.annotation.Aspect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ProxyBuilderTest {
	public static interface Interface {
		void hello();
	}

	public static interface AnotherInterface {
		void hello();
	}

	public static class Concrete implements Interface {

		@Override
		public void hello() {
			// NOSONAR: DUMMY METHOD
		}
	}

	public static class Standalone {
		public void hello() {
			// NOSONAR: DUMMY METHOD
		}
	}

	void dummy(@Deprecated String s, int x) {
		// dummy method for use in tests
	}

	@Aspect
	public static class DummyAspect {

	}

	@Aspect
	public static class AspectAdviceClass implements Advice {
	}

	public static class AdviceClass implements MethodBeforeAdvice {
		public Object target;

		@Override
		public void before(Method method, Object[] args, Object target) throws Throwable {
			this.target = target;
		}
	}

	@Test
	void testWithAspects() {
		// GIVEN
		DummyAspect dummyAspect = new DummyAspect();
		ProxyBuilder<ProxyBuilderTest> proxyBuilder =
			ProxyBuilder.withTarget(this).withAspects(dummyAspect).withAspects(dummyAspect);

		// WHEN

		// THEN
		assertThat(proxyBuilder.containsAspectAnnotation()).isTrue();
		assertThat(proxyBuilder.getAspects()).isEqualTo(Set.of(dummyAspect));
	}

	@Test
	void testWithInterface() {
		// GIVEN
		ProxyBuilder<Interface> proxyBuilder =
			ProxyBuilder.<Interface>withTarget(new Concrete()).withAspects(new DummyAspect());

		// WHEN

		// THEN
		assertDoesNotThrow(() -> proxyBuilder.build().hello());
	}

	@Test
	void testBuildAspectAdvice() {
		// GIVEN
		ProxyBuilder<ProxyBuilderTest> proxyBuilder =
			ProxyBuilder.withTarget(this).withAspects(new AspectAdviceClass());

		// WHEN

		// THEN
		assertThat(proxyBuilder.containsAspectAnnotation()).isTrue();
		assertThat(proxyBuilder.isAdvice()).isTrue();

		assertThrows(IllegalStateException.class, proxyBuilder::build);
	}

	@Test
	void testBuildAdviceInterface() {
		// GIVEN
		AdviceClass adviceClass = new AdviceClass();
		Concrete c = new Concrete();
		ProxyBuilder<Interface> proxyBuilder =
			ProxyBuilder.<Interface>withTarget(c).withAspects(adviceClass);

		// WHEN
		proxyBuilder.build().hello();

		// THEN
		assertThat(proxyBuilder.isAdvice()).isTrue();
		assertThat(adviceClass.target).isEqualTo(c);
	}

	@Test
	void testBuildAdviceStandalone() {
		// GIVEN
		AdviceClass adviceClass = new AdviceClass();
		Standalone c = new Standalone();
		ProxyBuilder<Standalone> proxyBuilder = ProxyBuilder.withTarget(c).withAspects(adviceClass);

		// WHEN
		Standalone s = proxyBuilder.build();

		s.hello();

		// THEN
		assertThat(proxyBuilder.isAdvice()).isTrue();
		assertThat(adviceClass.target).isEqualTo(c);
	}
}
