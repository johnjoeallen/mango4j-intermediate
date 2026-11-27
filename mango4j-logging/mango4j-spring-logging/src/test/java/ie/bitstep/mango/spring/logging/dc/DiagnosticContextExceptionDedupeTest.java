package ie.bitstep.mango.spring.logging.dc;

import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.logging.annotations.OnException;
import ie.bitstep.mango.logging.test.MDCTestUtils;
import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.aop.ThrowableInfo;
import ie.bitstep.mango.spring.logging.detail.ExceptionDetailRules;
import ie.bitstep.mango.spring.logging.internal.DiagnosticContextItem;
import ie.bitstep.mango.spring.logging.internal.ExceptionDetailResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CAUSE;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.CLASS_NAME;
import static ie.bitstep.mango.logging.annotations.ExceptionDetail.MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A parent method must not record an exception that is the very same instance its last child threw,
 * unless the parent has a matching {@link OnException}.
 */
class DiagnosticContextExceptionDedupeTest {

	private DiagnosticContext dc;

	@BeforeEach
	void setup() {
		MDCTestUtils.init();
		DiagnosticContextThreadLocal.reset();
		dc = new DiagnosticContext();
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	// ---- fixtures -------------------------------------------------------------------------

	@SuppressWarnings("unused")
	static class Fixture {
		@LoggableMethod
		public void child() {
		}

		@LoggableMethod
		public void otherChild() {
		}

		@LoggableMethod
		public void parentNoRules() {
		}

		@LoggableMethod
		@OnException(detail = {MESSAGE})
		public void parentCatchAll() {
		}

		@LoggableMethod
		@OnException(type = IllegalStateException.class, detail = {MESSAGE})
		public void parentMatchesIllegalState() {
		}

		@LoggableMethod
		@OnException(type = {IOException.class, IllegalStateException.class}, detail = {CLASS_NAME})
		public void parentMatchesAnyOfSeveral() {
		}

		@LoggableMethod
		@OnException(type = IOException.class, detail = {MESSAGE})
		@OnException(type = IllegalStateException.class, detail = {CLASS_NAME})
		public void parentRepeatable() {
		}

		@LoggableMethod
		@OnException(type = IOException.class, detail = {})
		public void parentMatchesOtherType() {
		}

		@LoggableMethod
		@OnException(type = IllegalStateException.class, detail = {})
		public void parentMatchesWithEmptyDetail() {
		}

		public void parentNotLoggable() {
		}
	}

	private static MethodInfo mi(String name) throws NoSuchMethodException {
		Method m = Fixture.class.getMethod(name);
		return new MethodInfo(Fixture.class, m);
	}

	/** Parent calls child; child throws t; the parent then rethrows the same instance. */
	private List<ThrowableInfo> parentRethrowsChild(String parent, Throwable t) throws Exception {
		dc.push(mi(parent));
		dc.push(mi("child"));
		dc.putThrowable(t);
		dc.up();
		dc.putThrowable(t);
		return dc.getThrown();
	}

	private static DiagnosticContextItem child(DiagnosticContext dc) {
		return dc.getRootItem().getChildren().get(0);
	}

	// ---- no @OnException: same instance as last child is skipped -----------------------------

	@Test
	void sameInstanceAsLastChild_noOnException_parentDoesNotRecord() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		List<ThrowableInfo> thrown = parentRethrowsChild("parentNoRules", ex);

		assertThat(dc.getRootItem().getThrowable()).isNull();
		assertThat(child(dc).getThrowable()).isSameAs(ex);
		assertThat(thrown).hasSize(1);
		assertThat(thrown.get(0).getThrowable()).isSameAs(ex);
		assertThat(thrown.get(0).getMethodInfo().getMethodName()).isEqualTo("child");
	}

	@Test
	void sameInstanceAsLastChild_parentWithoutLoggableAnnotation_parentDoesNotRecord() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		List<ThrowableInfo> thrown = parentRethrowsChild("parentNotLoggable", ex);

		assertThat(dc.getRootItem().getThrowable()).isNull();
		assertThat(thrown).hasSize(1);
	}

	// ---- matching @OnException: parent opts in -------------------------------------------------

	@Test
	void sameInstanceAsLastChild_catchAllOnException_parentRecords() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		List<ThrowableInfo> thrown = parentRethrowsChild("parentCatchAll", ex);

		assertThat(dc.getRootItem().getThrowable()).isSameAs(ex);
		assertThat(thrown).hasSize(2);
		assertThat(thrown.get(1).getMethodInfo().getMethodName()).isEqualTo("parentCatchAll");
	}

	@Test
	void sameInstanceAsLastChild_onExceptionMatchesExactType_parentRecords() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		parentRethrowsChild("parentMatchesIllegalState", ex);

		assertThat(dc.getRootItem().getThrowable()).isSameAs(ex);
	}

	@Test
	void sameInstanceAsLastChild_onExceptionMatchesSubclass_parentRecords() throws Exception {
		// rule is for RuntimeException-family via IllegalStateException; use a subclass of it
		class CustomIllegalState extends IllegalStateException {
		}
		CustomIllegalState ex = new CustomIllegalState();

		parentRethrowsChild("parentMatchesIllegalState", ex);

		assertThat(dc.getRootItem().getThrowable()).isSameAs(ex);
	}

	@Test
	void sameInstanceAsLastChild_typeArrayMatchesAnyEntry_parentRecords() throws Exception {
		IOException ex = new IOException("io");

		parentRethrowsChild("parentMatchesAnyOfSeveral", ex);

		assertThat(dc.getRootItem().getThrowable()).isSameAs(ex);
	}

	@Test
	void sameInstanceAsLastChild_repeatedOnExceptionSecondRuleMatches_parentRecords() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		parentRethrowsChild("parentRepeatable", ex);

		assertThat(dc.getRootItem().getThrowable()).isSameAs(ex);
	}

	@Test
	void sameInstanceAsLastChild_onExceptionForOtherType_parentDoesNotRecord() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		List<ThrowableInfo> thrown = parentRethrowsChild("parentMatchesOtherType", ex);

		assertThat(dc.getRootItem().getThrowable()).isNull();
		assertThat(thrown).hasSize(1);
	}

	@Test
	void sameInstanceAsLastChild_onExceptionWithEmptyDetail_parentStillRecordsInContext() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		List<ThrowableInfo> thrown = parentRethrowsChild("parentMatchesWithEmptyDetail", ex);

		// recorded (opted in); whether anything is logged for it is decided later by its (empty) detail
		assertThat(dc.getRootItem().getThrowable()).isSameAs(ex);
		assertThat(thrown).hasSize(2);
		assertThat(ExceptionDetailResolver.resolve(thrown.get(1).getMethodInfo(), ex)).isEmpty();
	}

	@Test
	void sameInstanceAsLastChild_globalRuleButNoOnException_parentStillDoesNotRecord() throws Exception {
		ExceptionDetailResolver.setStrategy(ExceptionDetailRules.builder()
			.on(IllegalStateException.class, MESSAGE, CAUSE)
			.build());
		try {
			IllegalStateException ex = new IllegalStateException("boom");

			List<ThrowableInfo> thrown = parentRethrowsChild("parentNoRules", ex);

			assertThat(dc.getRootItem().getThrowable()).isNull();
			assertThat(thrown).hasSize(1);
		} finally {
			ExceptionDetailResolver.setStrategy(null);
		}
	}

	// ---- not the same instance as the last child -------------------------------------------------

	@Test
	void wrapperAroundChildException_noOnException_parentRecords() throws Exception {
		IllegalStateException cause = new IllegalStateException("inner");
		RuntimeException wrapper = new RuntimeException("outer", cause);

		dc.push(mi("parentNoRules"));
		dc.push(mi("child"));
		dc.putThrowable(cause);
		dc.up();
		dc.putThrowable(wrapper);

		assertThat(dc.getRootItem().getThrowable()).isSameAs(wrapper);
		assertThat(dc.getThrown()).hasSize(2);
	}

	@Test
	void equalButDifferentInstance_noOnException_parentRecords() throws Exception {
		dc.push(mi("parentNoRules"));
		dc.push(mi("child"));
		dc.putThrowable(new IllegalStateException("boom"));
		dc.up();
		IllegalStateException other = new IllegalStateException("boom");
		dc.putThrowable(other);

		assertThat(dc.getRootItem().getThrowable()).isSameAs(other);
		assertThat(dc.getThrown()).hasSize(2);
	}

	@Test
	void parentThrowsOwnExceptionWithNoChildren_parentRecords() throws Exception {
		IllegalStateException ex = new IllegalStateException("own");

		dc.push(mi("parentNoRules"));
		dc.putThrowable(ex);

		assertThat(dc.getRootItem().getThrowable()).isSameAs(ex);
		assertThat(dc.getThrown()).hasSize(1);
	}

	@Test
	void parentThrowsOwnExceptionAfterChildSucceeded_parentRecords() throws Exception {
		IllegalStateException ex = new IllegalStateException("own");

		dc.push(mi("parentNoRules"));
		dc.push(mi("child"));
		dc.up();
		dc.putThrowable(ex);

		assertThat(child(dc).getThrowable()).isNull();
		assertThat(dc.getRootItem().getThrowable()).isSameAs(ex);
	}

	// ---- only the LAST child counts ------------------------------------------------------------

	@Test
	void sameInstanceAsEarlierChildButNotLast_parentRecords() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		dc.push(mi("parentNoRules"));
		dc.push(mi("child"));
		dc.putThrowable(ex);          // first child threw it (and the parent swallowed it)
		dc.up();
		dc.push(mi("otherChild"));    // last child succeeded
		dc.up();
		dc.putThrowable(ex);          // parent later throws the same instance itself

		assertThat(dc.getRootItem().getThrowable()).isSameAs(ex);
		assertThat(dc.getThrown()).hasSize(2);
	}

	@Test
	void lastChildThrewDifferentExceptionThanEarlierChild_sameAsLast_parentDoesNotRecord() throws Exception {
		IllegalStateException first = new IllegalStateException("first");
		IOException last = new IOException("last");

		dc.push(mi("parentNoRules"));
		dc.push(mi("child"));
		dc.putThrowable(first);
		dc.up();
		dc.push(mi("otherChild"));
		dc.putThrowable(last);
		dc.up();
		dc.putThrowable(last);

		assertThat(dc.getRootItem().getThrowable()).isNull();
		assertThat(dc.getThrown()).extracting(ThrowableInfo::getThrowable).containsExactly(first, last);
	}

	// ---- deeper nesting -------------------------------------------------------------------------

	@Test
	void propagatesThroughTwoParents_onlyOriginatingMethodRecords() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		dc.push(mi("parentNoRules"));       // grandparent
		dc.push(mi("parentNoRules"));       // parent
		dc.push(mi("child"));
		dc.putThrowable(ex);
		dc.up();
		dc.putThrowable(ex);                // parent rethrows
		dc.up();
		dc.putThrowable(ex);                // grandparent rethrows

		assertThat(dc.getThrown()).hasSize(1);
		assertThat(dc.getRootItem().getThrowable()).isNull();
	}

	@Test
	void propagatesThroughTwoParents_middleOptsIn_middleRecordsGrandparentDoesNot() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		dc.push(mi("parentNoRules"));       // grandparent
		dc.push(mi("parentCatchAll"));      // parent opts in
		dc.push(mi("child"));
		dc.putThrowable(ex);
		dc.up();
		dc.putThrowable(ex);                // parent records
		dc.up();
		dc.putThrowable(ex);                // grandparent: last child (parent) has it, no rule -> skipped

		assertThat(dc.getThrown()).hasSize(2);
		assertThat(dc.getRootItem().getThrowable()).isNull();
	}

	// ---- existing behaviour that must be kept ---------------------------------------------------

	@Test
	void samePutTwiceOnSameItem_stillIgnored() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		dc.push(mi("parentNoRules"));
		dc.putThrowable(ex);
		dc.putThrowable(ex);

		assertThat(dc.getThrown()).hasSize(1);
	}

	@Test
	void putPreviousCallThrowable_stillRecordsAgainstLastChild() throws Exception {
		IllegalStateException ex = new IllegalStateException("boom");

		dc.push(mi("parentNoRules"));
		dc.push(mi("child"));
		dc.up();
		dc.putPreviousCallThrowable(ex);

		assertThat(child(dc).getThrowable()).isSameAs(ex);
		assertThat(dc.getThrown()).hasSize(1);
		assertThat(dc.getThrown().get(0).getMethodInfo().getMethodName()).isEqualTo("child");
	}
}
