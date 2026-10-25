package ie.bitstep.mango.spring.logging.task;

import ie.bitstep.mango.spring.logging.LogContext;

import java.util.function.Consumer;

public class ExceptionSquashingTaskRunner {
	private ExceptionSquashingTaskRunner() { // SONAR
	}

	static <T> void run(T s, Consumer<T> task) {
		try {
			task.accept(s);
		} catch (Exception e) {
			LogContext.putThrowable(e);
		}
	}

	static void run(Runnable task) {
		try {
			task.run();
		} catch (Exception e) {
			LogContext.putThrowable(e);
		}
	}
}
