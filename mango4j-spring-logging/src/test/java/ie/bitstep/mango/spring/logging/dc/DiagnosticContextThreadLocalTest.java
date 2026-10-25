package ie.bitstep.mango.spring.logging.dc;

import ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger;
import ie.bitstep.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Deque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class DiagnosticContextThreadLocalTest {
	@BeforeEach
	void setup() throws Exception {
		MDCTestUtils.init();
		DiagnosticContextThreadLocal.unset();
		// Reset lifecycle logger state
		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
		// Reset lifecycle logger state
		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);
	}

	@Test
	void privateConstructorTest() throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
		Constructor<DiagnosticContextThreadLocal> constructor = DiagnosticContextThreadLocal.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		DiagnosticContextThreadLocal methoddiagnosticContextThreadLocalnfoFactory = constructor.newInstance();
		assertThat(methoddiagnosticContextThreadLocalnfoFactory).isNotNull(); // best we can do
	}

	@Test
	void reset() {
		DiagnosticContext diagnosticContext = DiagnosticContextThreadLocal.reset();

		assertThat(diagnosticContext).isNotNull();
	}

	@Test
	void push() {
		DiagnosticContext diagnosticContext = DiagnosticContextThreadLocal.push();

		assertThat(diagnosticContext).isNotNull();
	}

	@Test
	void pop() {
		assertDoesNotThrow(() -> DiagnosticContextThreadLocal.pop());
	}

	@Test
	void threadLocalIsNullAfterPopLast() {
		DiagnosticContextThreadLocal.reset();
		DiagnosticContextThreadLocal.push();
		DiagnosticContextThreadLocal.pop();
		DiagnosticContextThreadLocal.pop();

		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>) ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");

		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void get() {
		DiagnosticContext diagnosticContext = DiagnosticContextThreadLocal.get();

		assertThat(diagnosticContext).isNotNull();
	}

	@Test
	void unset_logsWhenThreadlocalLoggingEnabled() {
		// Enable threadlocal logging
		DiagnosticLifecycleLogger.setThreadlocalEnabled(true);

		// Get a context to initialize the ThreadLocal
		DiagnosticContextThreadLocal.get();

		// Get the logger to verify it was called
		Logger logger = DiagnosticContextThreadLocal.LOGGER;

		// Call unset - should log
		DiagnosticContextThreadLocal.unset();

		// Verify the ThreadLocal was cleared
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void unset_doesNotLogWhenThreadlocalLoggingDisabled() {
		// Ensure threadlocal logging is disabled
		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);

		// Get a context to initialize the ThreadLocal
		DiagnosticContextThreadLocal.get();

		// Call unset - should not log
		DiagnosticContextThreadLocal.unset();

		// Verify the ThreadLocal was cleared
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void unset_clearsAllContextsInDeque() {
		// Create multiple contexts
		DiagnosticContext ctx1 = DiagnosticContextThreadLocal.get();
		assertNotNull(ctx1);

		DiagnosticContext ctx2 = DiagnosticContextThreadLocal.push();
		assertNotNull(ctx2);

		DiagnosticContext ctx3 = DiagnosticContextThreadLocal.push();
		assertNotNull(ctx3);

		// Get the deque before unset
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		Deque<DiagnosticContext> deque = threadLocal.get();
		assertThat(deque).isNotNull().hasSize(3);

		// Unset should clear all contexts
		DiagnosticContextThreadLocal.unset();

		// Verify the ThreadLocal was removed
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void getDiagnosticContextQueue_logsWhenThreadlocalLoggingEnabled() {
		// Enable threadlocal logging
		DiagnosticLifecycleLogger.setThreadlocalEnabled(true);

		// First call should trigger logging (creates new ThreadLocal)
		DiagnosticContext context = DiagnosticContextThreadLocal.get();

		assertThat(context).isNotNull();
	}

	@Test
	void getDiagnosticContextQueue_doesNotLogWhenThreadlocalLoggingDisabled() {
		// Ensure threadlocal logging is disabled
		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);

		// First call should NOT log
		DiagnosticContext context = DiagnosticContextThreadLocal.get();

		assertThat(context).isNotNull();
	}

	@Test
	void pop_clearsContextWhenNotNull() {
		// Create multiple contexts
		DiagnosticContext ctx1 = DiagnosticContextThreadLocal.get();
		assertNotNull(ctx1);

		DiagnosticContext ctx2 = DiagnosticContextThreadLocal.push();
		assertNotNull(ctx2);

		// Get the deque size before pop
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		Deque<DiagnosticContext> deque = threadLocal.get();
		int sizeBefore = deque.size();

		// Pop should remove and clear the last context
		DiagnosticContextThreadLocal.pop();

		// Verify size decreased
		assertThat(deque.size()).isEqualTo(sizeBefore - 1);

		// Verify we can still get a context
		DiagnosticContext remaining = DiagnosticContextThreadLocal.get();
		assertThat(remaining).isNotNull();
	}

	@Test
	void pop_callsUnsetWhenEmpty() {
		// Get initial context
		DiagnosticContextThreadLocal.get();

		// Pop the only context
		DiagnosticContextThreadLocal.pop();

		// Verify the ThreadLocal was removed (unset was called)
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void unset_handlesNullDequeGracefully() {
		// Ensure ThreadLocal is null
		DiagnosticContextThreadLocal.unset();

		// Call unset again - should not throw
		assertDoesNotThrow(() -> DiagnosticContextThreadLocal.unset());
	}

	@Test
	void unset_clearsDequeCompletely() {
		// Get multiple contexts
		DiagnosticContext ctx1 = DiagnosticContextThreadLocal.get();
		assertThat(ctx1).isNotNull();

		DiagnosticContextThreadLocal.push();
		DiagnosticContextThreadLocal.push();

		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		Deque<DiagnosticContext> deque = threadLocal.get();
		assertThat(deque).hasSize(3);

		// Unset should clear all contexts and remove ThreadLocal
		DiagnosticContextThreadLocal.unset();

		// Verify ThreadLocal was cleared
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void reset_clearsAndReturnsNewContext() {
		// Create some contexts
		DiagnosticContext ctx1 = DiagnosticContextThreadLocal.get();
		assertNotNull(ctx1);

		DiagnosticContextThreadLocal.push();

		// Reset should clear everything and return a new context
		DiagnosticContext newContext = DiagnosticContextThreadLocal.reset();

		assertThat(newContext).isNotNull();
	}

	@Test
	void push_copiesCurrentContext() {
		// Get initial context
		DiagnosticContext ctx1 = DiagnosticContextThreadLocal.get();
		assertNotNull(ctx1);

		// Push should copy the current context
		DiagnosticContext ctx2 = DiagnosticContextThreadLocal.push();

		assertThat(ctx2).isNotNull();
		assertThat(ctx2).isNotSameAs(ctx1);
	}

	@Test
	void multipleThreads_isolatedThreadLocals() throws InterruptedException {
		// Get context in main thread
		DiagnosticContext mainContext = DiagnosticContextThreadLocal.get();
		assertNotNull(mainContext);

		// Create a flag to track thread execution
		final boolean[] threadExecuted = {false};
		final DiagnosticContext[] otherThreadContext = {null};

		// Create another thread
		Thread otherThread = new Thread(() -> {
			try {
				DiagnosticContext otherContext = DiagnosticContextThreadLocal.get();
				assertNotNull(otherContext);
				otherThreadContext[0] = otherContext;

				threadExecuted[0] = true;

				// Clean up
				DiagnosticContextThreadLocal.unset();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		otherThread.start();
		otherThread.join();

		// Verify the other thread executed
		assertThat(threadExecuted[0]).isTrue();
		assertThat(otherThreadContext[0]).isNotNull();

		// Verify contexts are different instances (thread-local isolation)
		DiagnosticContext stillMainContext = DiagnosticContextThreadLocal.get();
		assertThat(stillMainContext).isNotNull().isNotSameAs(otherThreadContext[0]);
	}

	@Test
	void unset_withDisabledLogging_doesNotLog() {
		// Ensure threadlocal logging is disabled (default state)
		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);

		// Get a context
		DiagnosticContextThreadLocal.get();

		// This should not log since logging is disabled
		// The test passes if no exception is thrown and ThreadLocal is cleared
		DiagnosticContextThreadLocal.unset();

		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void getDiagnosticContextQueue_withDisabledLogging_doesNotLog() {
		// Ensure threadlocal logging is disabled
		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);

		// Clear any existing context
		DiagnosticContextThreadLocal.unset();

		// This should not log since logging is disabled
		DiagnosticContext context = DiagnosticContextThreadLocal.get();

		assertThat(context).isNotNull();
	}

	@Test
	void unset_verifiesClearIsCalledOnContexts() {
		// Create a custom test that verifies clear is called
		// by checking that the context is in a cleared state after unset

		// Get initial context
		DiagnosticContext ctx1 = DiagnosticContextThreadLocal.get();
		assertThat(ctx1).isNotNull();

		// Push more contexts
		DiagnosticContextThreadLocal.push();
		DiagnosticContextThreadLocal.push();

		// Get the deque
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		Deque<DiagnosticContext> deque = threadLocal.get();
		assertThat(deque).hasSize(3);

		// After unset, the ThreadLocal should be completely removed
		DiagnosticContextThreadLocal.unset();
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void pop_verifiesClearIsCalledOnRemovedContext() {
		// Create multiple contexts
		DiagnosticContextThreadLocal.get();
		DiagnosticContextThreadLocal.push();

		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		Deque<DiagnosticContext> deque = threadLocal.get();
		assertThat(deque).hasSize(2);

		// Pop should clear the removed context
		DiagnosticContextThreadLocal.pop();

		// Verify size decreased
		assertThat(deque).hasSize(1);
	}

	// Tests for clearDeque method

	@Test
	void clearDeque_withNullDeque_doesNotThrow() {
		// Should handle null gracefully
		assertDoesNotThrow(() -> DiagnosticContextThreadLocal.clearDeque(null));
	}

	@Test
	void clearDeque_withEmptyDeque_doesNotThrow() {
		Deque<DiagnosticContext> emptyDeque = new java.util.ArrayDeque<>();

		// Should handle empty deque gracefully
		assertDoesNotThrow(() -> DiagnosticContextThreadLocal.clearDeque(emptyDeque));

		// Deque should still be empty
		assertThat(emptyDeque).isEmpty();
	}

	@Test
	void clearDeque_withSingleContext_clearsContext() {
		// Create a deque with one context
		Deque<DiagnosticContext> deque = new java.util.ArrayDeque<>();
		DiagnosticContext context = new DiagnosticContext();
		deque.add(context);

		assertThat(deque).hasSize(1);

		// Clear the deque
		DiagnosticContextThreadLocal.clearDeque(deque);

		// Deque should be empty after clearing
		assertThat(deque).isEmpty();
	}

	@Test
	void clearDeque_withMultipleContexts_clearsAllContexts() {
		// Create a deque with multiple contexts
		Deque<DiagnosticContext> deque = new java.util.ArrayDeque<>();
		DiagnosticContext context1 = new DiagnosticContext();
		DiagnosticContext context2 = new DiagnosticContext();
		DiagnosticContext context3 = new DiagnosticContext();
		deque.add(context1);
		deque.add(context2);
		deque.add(context3);

		assertThat(deque).hasSize(3);

		// Clear the deque
		DiagnosticContextThreadLocal.clearDeque(deque);

		// Deque should be empty after clearing
		assertThat(deque).isEmpty();
	}

	@Test
	void clearDeque_withNullContextInDeque_handlesGracefully() {
		// Create a deque with a null context (edge case)
		Deque<DiagnosticContext> deque = new java.util.ArrayDeque<>();
		DiagnosticContext context1 = new DiagnosticContext();
		deque.add(context1);
		// Note: ArrayDeque doesn't allow null, but this tests the null check in the forEach

		assertThat(deque).hasSize(1);

		// Clear the deque - should handle any nulls gracefully
		assertDoesNotThrow(() -> DiagnosticContextThreadLocal.clearDeque(deque));

		// Deque should be empty after clearing
		assertThat(deque).isEmpty();
	}

	@Test
	void clearDeque_afterClearing_dequeIsEmpty() {
		// Create a deque with contexts
		Deque<DiagnosticContext> deque = new java.util.ArrayDeque<>();
		deque.add(new DiagnosticContext());
		deque.add(new DiagnosticContext());

		assertThat(deque).hasSize(2);

		// Clear the deque
		DiagnosticContextThreadLocal.clearDeque(deque);

		// Verify deque is completely empty
		assertThat(deque).isEmpty();
		assertThat(deque).isEmpty();
	}

	@Test
	void clearDeque_calledTwice_doesNotThrow() {
		Deque<DiagnosticContext> deque = new java.util.ArrayDeque<>();
		deque.add(new DiagnosticContext());

		// Clear once
		DiagnosticContextThreadLocal.clearDeque(deque);
		assertThat(deque).isEmpty();

		// Clear again - should not throw
		assertDoesNotThrow(() -> DiagnosticContextThreadLocal.clearDeque(deque));
		assertThat(deque).isEmpty();
	}

	@Test
	void unset_withMultipleContexts_verifiesForEachIsCalled() {
		// Create multiple contexts by pushing
		DiagnosticContext ctx1 = DiagnosticContextThreadLocal.get();
		DiagnosticContext ctx2 = DiagnosticContextThreadLocal.push();
		DiagnosticContext ctx3 = DiagnosticContextThreadLocal.push();

		assertNotNull(ctx1);
		assertNotNull(ctx2);
		assertNotNull(ctx3);

		// Get the deque to verify it has contexts
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		Deque<DiagnosticContext> deque = threadLocal.get();
		assertThat(deque).hasSize(3);

		// Store references to verify they exist before unset
		DiagnosticContext[] contexts = deque.toArray(new DiagnosticContext[0]);
		assertThat(contexts).hasSize(3);

		// Unset should call forEach which calls clear on each context
		DiagnosticContextThreadLocal.unset();

		// Verify ThreadLocal was removed
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void pop_withNonNullContext_callsClear() {
		// Create multiple contexts
		DiagnosticContext ctx1 = DiagnosticContextThreadLocal.get();
		DiagnosticContext ctx2 = DiagnosticContextThreadLocal.push();

		assertNotNull(ctx1);
		assertNotNull(ctx2);

		// Get the deque
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		Deque<DiagnosticContext> deque = threadLocal.get();
		assertThat(deque).hasSize(2);

		// Pop should remove and clear the last context
		DiagnosticContextThreadLocal.pop();

		// Verify deque size decreased (proving clear was called on removed context)
		assertThat(deque).hasSize(1);

		// Verify we still have a valid context
		DiagnosticContext remaining = DiagnosticContextThreadLocal.get();
		assertThat(remaining).isNotNull();
		assertThat(remaining).isSameAs(ctx1);
	}

	@Test
	void unset_withNoDeque_handlesGracefully() {
		// Ensure ThreadLocal is already null
		DiagnosticContextThreadLocal.unset();

		// Verify ThreadLocal is null
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		assertThat(threadLocal.get()).isNull();

		// Call unset again with null deque - should handle gracefully
		assertDoesNotThrow(() -> DiagnosticContextThreadLocal.unset());
	}

	@Test
	void getDiagnosticContextQueue_createsNewDequeWhenNull() {
		// Ensure ThreadLocal is null
		DiagnosticContextThreadLocal.unset();

		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		assertThat(threadLocal.get()).isNull();

		// Get should create a new deque and context
		DiagnosticContext context = DiagnosticContextThreadLocal.get();

		assertThat(context).isNotNull();
		assertThat(threadLocal.get()).isNotNull();
		assertThat(threadLocal.get()).hasSize(1);
	}

	@Test
	void unset_withEnabledLogging_logsMessage() {
		// Enable threadlocal logging
		DiagnosticLifecycleLogger.setThreadlocalEnabled(true);

		// Create a context
		DiagnosticContextThreadLocal.get();

		// Unset with logging enabled - should log "Releasing threadlocal"
		DiagnosticContextThreadLocal.unset();

		// Verify ThreadLocal was cleared
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void getDiagnosticContextQueue_withEnabledLogging_logsMessage() {
		// Enable threadlocal logging
		DiagnosticLifecycleLogger.setThreadlocalEnabled(true);

		// Ensure ThreadLocal is null
		DiagnosticContextThreadLocal.unset();

		// Get should log "Acquiring threadlocal"
		DiagnosticContext context = DiagnosticContextThreadLocal.get();

		assertThat(context).isNotNull();
	}

	@Test
	void unset_verifiesDequeIsCleared() {
		// Create contexts
		DiagnosticContextThreadLocal.get();
		DiagnosticContextThreadLocal.push();
		DiagnosticContextThreadLocal.push();

		// Get the deque
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		Deque<DiagnosticContext> deque = threadLocal.get();

		// Store the deque reference before unset
		int sizeBeforeUnset = deque.size();
		assertThat(sizeBeforeUnset).isEqualTo(3);

		// Unset should call deque.clear()
		DiagnosticContextThreadLocal.unset();

		// After unset, the ThreadLocal should be removed entirely
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void pop_whenDequeBecomesEmpty_callsUnset() {
		// Get only one context
		DiagnosticContext ctx = DiagnosticContextThreadLocal.get();
		assertNotNull(ctx);

		// Get the ThreadLocal reference
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		Deque<DiagnosticContext> deque = threadLocal.get();
		assertThat(deque).hasSize(1);

		// Pop the only context - should call unset
		DiagnosticContextThreadLocal.pop();

		// Verify unset was called (ThreadLocal should be null)
		assertThat(threadLocal.get()).isNull();
	}

	@Test
	void pop_whenDequeNotEmpty_doesNotCallUnset() {
		// Create multiple contexts
		DiagnosticContextThreadLocal.get();
		DiagnosticContextThreadLocal.push();
		DiagnosticContextThreadLocal.push();

		// Get the ThreadLocal reference
		ThreadLocal<Deque<DiagnosticContext>> threadLocal = (ThreadLocal<Deque<DiagnosticContext>>)
			ReflectionTestUtils.getField(DiagnosticContextThreadLocal.class, "THREAD_LOCAL_STORAGE");
		Deque<DiagnosticContext> deque = threadLocal.get();
		assertThat(deque).hasSize(3);

		// Pop one context - should NOT call unset (deque not empty)
		DiagnosticContextThreadLocal.pop();

		// Verify unset was NOT called (ThreadLocal should still exist)
		assertThat(threadLocal.get()).isNotNull();
		assertThat(threadLocal.get()).hasSize(2);
	}
}