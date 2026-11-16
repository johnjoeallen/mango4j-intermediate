package ie.bitstep.mango.spring.aop;

import org.springframework.aop.framework.AopContext;

public class WithSpringAOP {
	private WithSpringAOP() {
		// SONAR
	}

	public static <T> T proxy() {
		try {
			return (T) AopContext.currentProxy();
		} catch (Exception e) {
			throw new IllegalStateException(
				"Cannot find current proxy: Set 'exposeProxy' property on @EnableAspectJAutoProxy to 'true' to make it available, and " +
					"ensure that AopContext.currentProxy() is invoked in the same thread as the AOP invocation context.");
		}

	}
}
