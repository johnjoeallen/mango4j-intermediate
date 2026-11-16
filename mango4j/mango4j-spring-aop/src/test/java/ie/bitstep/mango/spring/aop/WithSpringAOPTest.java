package ie.bitstep.mango.spring.aop;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.AopContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
class WithSpringAOPTest {

	@Mock
	private MockedStatic<AopContext> aopContextMockedStatic;

	@Test
	void privateConstructorTest() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		Constructor<WithSpringAOP> constructor = WithSpringAOP.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		WithSpringAOP cp = constructor.newInstance();
		assertThat(cp).isNotNull(); // best we can do
	}

	@Test
	void currentProxyInvokeFailsTest() {
		aopContextMockedStatic.when(AopContext::currentProxy).thenThrow(new IllegalStateException());
		IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> {
			WithSpringAOP.proxy();
			fail();
		});

		assertThat(thrown).isNotNull();
		assertThat(thrown.getMessage()).isEqualTo("Cannot find current proxy: Set 'exposeProxy' property on @EnableAspectJAutoProxy to 'true' to make it available, and ensure that AopContext.currentProxy() is invoked in the same thread as the AOP invocation context.");
	}

	@Test
	void currentProxyInvokeSuccessTest() {
		Object context = new Object();
		aopContextMockedStatic.when(AopContext::currentProxy).thenReturn(context);
		Object invoke = WithSpringAOP.proxy();

		assertThat(invoke).isEqualTo(context);
	}

}