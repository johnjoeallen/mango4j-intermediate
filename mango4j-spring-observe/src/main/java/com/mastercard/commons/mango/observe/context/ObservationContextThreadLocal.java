package com.mastercard.commons.mango.observe.context;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ObservationContextThreadLocal {

	private static final ThreadLocal<Deque<ObservationContext>> THREAD_LOCAL_STORAGE = ThreadLocal.withInitial(() -> {
		Deque<ObservationContext> observationContextDeque = new ArrayDeque<>();
		observationContextDeque.add(new ObservationContext());
		return observationContextDeque;
	});
	private ObservationContextThreadLocal() {
	}

	public static void unset() {
		THREAD_LOCAL_STORAGE.remove();
	}

	public static ObservationContext reset() {
		unset();
		return get();
	}

	public static ObservationContext get() {
		Deque<ObservationContext> observationContextDeque = THREAD_LOCAL_STORAGE.get();

		return observationContextDeque.peekLast();
	}
}
