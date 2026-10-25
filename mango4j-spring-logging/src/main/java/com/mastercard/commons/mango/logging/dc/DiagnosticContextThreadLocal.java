package com.mastercard.commons.mango.logging.dc;

import com.mastercard.commons.mango.logging.config.DiagnosticLifecycleLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

public final class DiagnosticContextThreadLocal {

	private static final ThreadLocal<Deque<DiagnosticContext>> THREAD_LOCAL_STORAGE = new ThreadLocal<>();
	public static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticContextThreadLocal.class);

	private DiagnosticContextThreadLocal() {
	}

	public static void unset() {
		if (DiagnosticLifecycleLogger.isThreadlocalEnabled()) {
			LOGGER.info("Releasing threadlocal for thread {}", Thread.currentThread().getName());
		}
		Deque<DiagnosticContext> deque = THREAD_LOCAL_STORAGE.get();
		clearDeque(deque);
		THREAD_LOCAL_STORAGE.remove();
	}

	/**
	 * Clear all contexts in the deque to prevent memory leaks.
	 * Package-private for testing.
	 *
	 * @param deque the deque containing diagnostic contexts
	 */
	static void clearDeque(Deque<DiagnosticContext> deque) {
		if (deque != null) {
			// Clear all contexts in the deque to prevent memory leaks
			deque.forEach(context -> {
				if (context != null) {
					context.clear();
				}
			});
			deque.clear();
		}
	}

	public static DiagnosticContext reset() {
		// unset() already clears all contexts before removing ThreadLocal
		unset();
		return get();
	}

	public static DiagnosticContext push() {
		Deque<DiagnosticContext> diagnosticContextDeque = getDiagnosticContextQueue();

		diagnosticContextDeque.addLast(diagnosticContextDeque.peekLast().copy());

		return diagnosticContextDeque.peekLast();
	}

	public static void pop() {
		Deque<DiagnosticContext> diagnosticContextDeque = getDiagnosticContextQueue();

		DiagnosticContext context = diagnosticContextDeque.removeLast();
		if (context != null) {
			context.clear();  // Clear context to prevent memory leaks
		}
		if (diagnosticContextDeque.isEmpty()) {
			unset();
		}
	}

	public static DiagnosticContext get() {
		Deque<DiagnosticContext> diagnosticContextDeque = getDiagnosticContextQueue();

		return diagnosticContextDeque.peekLast();
	}

	private static Deque<DiagnosticContext> getDiagnosticContextQueue() {
		Deque<DiagnosticContext> diagnosticContextDeque = THREAD_LOCAL_STORAGE.get();

		if (diagnosticContextDeque == null) {
			if (DiagnosticLifecycleLogger.isThreadlocalEnabled()) {
				LOGGER.info("Acquiring threadlocal for thread {}", Thread.currentThread().getName());
			}
			diagnosticContextDeque = new ArrayDeque<>();
			diagnosticContextDeque.add(new DiagnosticContext());
			THREAD_LOCAL_STORAGE.set(diagnosticContextDeque);
		}

		return diagnosticContextDeque;
	}

}
