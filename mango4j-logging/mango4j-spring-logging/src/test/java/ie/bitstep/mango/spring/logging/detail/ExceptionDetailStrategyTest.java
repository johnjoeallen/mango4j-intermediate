package ie.bitstep.mango.spring.logging.detail;

import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.annotations.OnException;
import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.internal.ExceptionDetailResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.util.Optional;

import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CAUSE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CLASS_NAME;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.MESSAGE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.STACK_TRACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionDetailStrategyTest {

	@SuppressWarnings("unused")
	static class Fixture {
		@LoggableMethod(logException = {MESSAGE})
		public void plain() {
		}

		@LoggableMethod(logException = {MESSAGE})
		@OnException(type = IOException.class, detail = {STACK_TRACE})
		public void withOnException() {
		}
	}

	private static MethodInfo mi(String name) throws NoSuchMethodException {
		return new MethodInfo(Fixture.class, Fixture.class.getMethod(name));
	}

	private final ExceptionDetailRules rules = ExceptionDetailRules.builder()
		.on(IllegalStateException.class)                       // nothing
		.on(IOException.class, CLASS_NAME)
		.on(RuntimeException.class, MESSAGE, CAUSE)
		.build();

	@AfterEach
	void reset() {
		ExceptionDetailResolver.setStrategy(null);
	}

	@Test
	void rules_firstMatchWins_andSubclassesMatch() throws Exception {
		assertThat(rules.getDetail(new IllegalStateException(), null)).contains(java.util.Set.of());
		assertThat(rules.getDetail(new IOException(), null).orElseThrow()).containsExactly(CLASS_NAME);
		assertThat(rules.getDetail(new IllegalArgumentException(), null).orElseThrow()).containsExactlyInAnyOrder(MESSAGE, CAUSE);
	}

	@Test
	void rules_mostSpecificTypeWins_whateverOrderTheyAreDeclaredIn() {
		ExceptionDetailRules broadFirst = ExceptionDetailRules.builder()
			.on(RuntimeException.class, MESSAGE)
			.on(IllegalStateException.class, CLASS_NAME)
			.build();
		ExceptionDetailRules specificFirst = ExceptionDetailRules.builder()
			.on(IllegalStateException.class, CLASS_NAME)
			.on(RuntimeException.class, MESSAGE)
			.build();

		for (ExceptionDetailRules rules : new ExceptionDetailRules[]{broadFirst, specificFirst}) {
			assertThat(rules.getDetail(new IllegalStateException(), null).orElseThrow()).containsExactly(CLASS_NAME);   // exact class
			assertThat(rules.getDetail(new IllegalArgumentException(), null).orElseThrow()).containsExactly(MESSAGE);   // nearest superclass
		}
	}

	@Test
	void rules_nearestSuperclassWins() {
		ExceptionDetailRules rules = ExceptionDetailRules.builder()
			.on(Throwable.class, STACK_TRACE)
			.on(Exception.class, CAUSE)
			.on(RuntimeException.class, MESSAGE)
			.build();

		assertThat(rules.getDetail(new IllegalStateException(), null).orElseThrow()).containsExactly(MESSAGE);
		assertThat(rules.getDetail(new IOException(), null).orElseThrow()).containsExactly(CAUSE);
		assertThat(rules.getDetail(new Error(), null).orElseThrow()).containsExactly(STACK_TRACE);
	}

	@Test
	void rules_sameTypeDeclaredTwice_firstDeclaredIsKept() {
		ExceptionDetailRules rules = ExceptionDetailRules.builder()
			.on(IOException.class, MESSAGE)
			.on(IOException.class, CLASS_NAME)
			.build();

		assertThat(rules.getDetail(new IOException(), null).orElseThrow()).containsExactly(MESSAGE);
	}

	@Test
	void rules_noMatch_isEmpty() {
		assertThat(rules.getDetail(new Exception(), null)).isEqualTo(Optional.empty());
	}

	@Test
	void resolver_globalRuleBeatsLogException() throws Exception {
		ExceptionDetailResolver.setStrategy(rules);

		assertThat(ExceptionDetailResolver.resolve(mi("plain"), new IOException())).containsExactly(CLASS_NAME);
		assertThat(ExceptionDetailResolver.resolve(mi("plain"), new IllegalStateException())).isEmpty();
	}

	@Test
	void resolver_noGlobalRule_fallsBackToLogException() throws Exception {
		ExceptionDetailResolver.setStrategy(rules);

		assertThat(ExceptionDetailResolver.resolve(mi("plain"), new Exception())).containsExactly(MESSAGE);
	}

	@Test
	void resolver_onExceptionBeatsGlobalRule() throws Exception {
		ExceptionDetailResolver.setStrategy(rules);

		assertThat(ExceptionDetailResolver.resolve(mi("withOnException"), new IOException())).containsExactly(STACK_TRACE);
	}

	@Test
	void resolver_noStrategy_usesLogException() throws Exception {
		assertThat(ExceptionDetailResolver.resolve(mi("plain"), new IOException())).containsExactly(MESSAGE);
	}

	@Test
	void registrar_handsBeanToResolver() throws Exception {
		@SuppressWarnings("unchecked")
		ObjectProvider<ExceptionDetailStrategy> provider = mock(ObjectProvider.class);
		when(provider.getIfUnique()).thenReturn(rules);

		new ExceptionDetailStrategyRegistrar(provider).register();

		assertThat(ExceptionDetailResolver.resolve(mi("plain"), new IOException())).containsExactly(CLASS_NAME);
	}

	@Test
	void registrar_noBean_clearsStrategy() throws Exception {
		ExceptionDetailResolver.setStrategy(rules);
		@SuppressWarnings("unchecked")
		ObjectProvider<ExceptionDetailStrategy> provider = mock(ObjectProvider.class);

		new ExceptionDetailStrategyRegistrar(provider).register();

		assertThat(ExceptionDetailResolver.resolve(mi("plain"), new IOException())).containsExactly(MESSAGE);
	}
}
