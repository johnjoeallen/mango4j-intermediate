package com.mastercard.commons.mango.observe.observers;

import com.mastercard.commons.mango.observe.context.Observation;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.function.Consumer;

@AllArgsConstructor
public abstract class Observer<T> {
	private final List<Consumer<T>> consumers;

	public abstract void onEntry(Observation observation);

	public abstract void onExit(Observation observation);

	public void consume(T t) {
		for (Consumer<T> consumer : consumers) {
			consumer.accept(t);
		}
	}
}
