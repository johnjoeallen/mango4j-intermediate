package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.annotations.OnException;
import ie.bitstep.mango.spring.aop.MethodInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CAUSE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CLASS_NAME;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.EXCEPTION;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.MESSAGE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.REASON;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.STACK_TRACE;
import static org.assertj.core.api.Assertions.assertThat;

class ExceptionDetailResolverTest {

	@SuppressWarnings("unused")
	static class Fixture {
		public void notAnnotated() {
		}

		@LoggableMethod
		public void defaults() {
		}

		@LoggableMethod(logException = {})
		public void none() {
		}

		@LoggableMethod(logException = {REASON, CLASS_NAME})
		public void classOnly() {
		}

		@LoggableMethod(logException = {MESSAGE})
		@OnException(type = IOException.class, detail = {CLASS_NAME})
		@OnException(type = IllegalStateException.class, detail = {})
		public void rules() {
		}
	}

	private static MethodInfo mi(String name) throws NoSuchMethodException {
		return new MethodInfo(Fixture.class, Fixture.class.getMethod(name));
	}

	private final RuntimeException ex = new RuntimeException("outer", new IllegalArgumentException("inner"));

	@Test
	void resolve_notAnnotatedOrNullMethodInfo_usesDefault() throws Exception {
		assertThat(ExceptionDetailResolver.resolve(mi("notAnnotated"), ex)).containsExactlyInAnyOrder(REASON, MESSAGE, CAUSE, EXCEPTION, STACK_TRACE);
		assertThat(ExceptionDetailResolver.resolve(null, ex)).containsExactlyInAnyOrder(REASON, MESSAGE, CAUSE, EXCEPTION, STACK_TRACE);
	}

	@Test
	void resolve_loggableMethodDefault() throws Exception {
		assertThat(ExceptionDetailResolver.resolve(mi("defaults"), ex)).containsExactlyInAnyOrder(REASON, MESSAGE, CAUSE, EXCEPTION, STACK_TRACE);
	}

	@Test
	void resolve_emptyLogException_isEmpty() throws Exception {
		assertThat(ExceptionDetailResolver.resolve(mi("none"), ex)).isEmpty();
	}

	@Test
	void resolve_logExceptionArray() throws Exception {
		assertThat(ExceptionDetailResolver.resolve(mi("classOnly"), ex)).containsExactlyInAnyOrder(REASON, CLASS_NAME);
	}

	@Test
	void resolve_onExceptionRuleWinsOverLogException() throws Exception {
		assertThat(ExceptionDetailResolver.resolve(mi("rules"), new IOException("x"))).containsExactly(CLASS_NAME);
		assertThat(ExceptionDetailResolver.resolve(mi("rules"), new IllegalStateException("x"))).isEmpty();
	}

	@Test
	void resolve_noMatchingRule_fallsBackToLogException() throws Exception {
		assertThat(ExceptionDetailResolver.resolve(mi("rules"), ex)).containsExactly(MESSAGE);
	}

	@Test
	void describe_variants() {
		assertThat(ExceptionDetailResolver.describe(ex, Set.of())).isEmpty();
		assertThat(ExceptionDetailResolver.describe(ex, Set.of(STACK_TRACE, EXCEPTION))).isEmpty();
		assertThat(ExceptionDetailResolver.describe(ex, Set.of(REASON, CLASS_NAME))).isEqualTo("java.lang.RuntimeException");
		assertThat(ExceptionDetailResolver.describe(ex, Set.of(REASON, MESSAGE))).isEqualTo("outer");
		assertThat(ExceptionDetailResolver.describe(ex, Set.of(REASON, MESSAGE, CAUSE))).isEqualTo("outer");
		assertThat(ExceptionDetailResolver.describe(ex, Set.of(REASON, CAUSE))).isEqualTo("outer");
		assertThat(ExceptionDetailResolver.describe(ex, Set.of(REASON, CLASS_NAME, MESSAGE))).isEqualTo("java.lang.RuntimeException: outer");
	}

	@Test
	void describe_cause_causesAreNotPartOfTheText() {
		RuntimeException chain = new RuntimeException("outer",
			new IllegalStateException("middle", new IllegalArgumentException("inner")));

		assertThat(ExceptionDetailResolver.describe(chain, Set.of(REASON, MESSAGE, CAUSE))).isEqualTo("outer");
		assertThat(ExceptionDetailResolver.describe(chain, Set.of(REASON, MESSAGE))).isEqualTo("outer");
	}

	@Test
	void describe_wrapperWhoseMessageIsOnlyTheCause_showsClassNameNotTheCauseText() {
		RuntimeException wrapper = new RuntimeException(new IllegalStateException("inner-MSG"));
		assertThat(wrapper.getMessage()).contains("inner-MSG");    // the JDK derives it from the cause

		assertThat(ExceptionDetailResolver.describe(wrapper, Set.of(REASON, MESSAGE))).isEqualTo("java.lang.RuntimeException");
	}

	@Test
	void describe_cause_selectedButThereIsNoCause_noSuffix() {
		assertThat(ExceptionDetailResolver.describe(new RuntimeException("outer"), Set.of(REASON, MESSAGE, CAUSE)))
			.isEqualTo("outer");
	}

	@Test
	void describe_cause_withClassNameButNoMessage_classNameOnly() {
		RuntimeException ex = new RuntimeException("outer", new IllegalArgumentException("inner"));

		assertThat(ExceptionDetailResolver.describe(ex, Set.of(REASON, CLASS_NAME, CAUSE))).isEqualTo("java.lang.RuntimeException");
	}

	@Test
	void describe_withoutReason_isEmptyWhateverElseIsSelected() {
		assertThat(ExceptionDetailResolver.describe(ex, Set.of(CLASS_NAME, MESSAGE, CAUSE))).isEmpty();
		assertThat(ExceptionDetailResolver.describe(ex, Set.of(MESSAGE))).isEmpty();
	}

	@Test
	void describe_reasonAlone_usesTheMessage() {
		assertThat(ExceptionDetailResolver.describe(ex, Set.of(REASON))).isEqualTo("outer");
	}

	@Test
	void item_toLoggable_honoursDetail() throws Exception {
		assertThat(loggableFor("none", ex)).doesNotContainKeys("reason", "stacktrace", "exception");
		assertThat(loggableFor("classOnly", ex)).containsEntry("reason", "java.lang.RuntimeException").doesNotContainKey("stacktrace");
		assertThat(loggableFor("defaults", ex)).containsEntry("reason", "outer").containsKey("stacktrace").containsKey("cause");
	}

	private Map<String, Object> loggableFor(String method, Throwable t) throws Exception {
		DiagnosticContextItem item = new DiagnosticContextItem(mi(method), null);
		item.putThrowable(t);
		return item.toLoggable();
	}
}
