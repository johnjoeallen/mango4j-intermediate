package ie.bitstep.mango.spring.logging.task;

import ie.bitstep.mango.spring.logging.LogContext;
import ie.bitstep.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static ie.bitstep.mango.spring.logging.test.TestUtils.fakeLoggable;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExceptionSquashingTaskRunnerTest {
	public static class GreetingParams {
		private String greeting;

		public GreetingParams(String from) {
			this.greeting = from;
		}

		public String greeting() {
			return greeting;
		}
	}

	public static class GreetingService {
		void test(GreetingParams params) {
			LogContext.put("greeting", this.getClass().getCanonicalName() + ":" + params.greeting());
		}

		void test() {
			LogContext.put("greeting", this.getClass().getCanonicalName());
		}

		void testException(GreetingParams params) {
			throw new RuntimeException(this.getClass().getCanonicalName() + ":" + params.greeting());
		}

		void testException() {
			throw new RuntimeException(this.getClass().getCanonicalName());
		}
	}

	@BeforeEach
	void setup() {
		MDCTestUtils.init();
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void privateConstructorTest() throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
		Constructor<ExceptionSquashingTaskRunner> constructor = ExceptionSquashingTaskRunner.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		ExceptionSquashingTaskRunner estr = constructor.newInstance();
		assertThat(estr).isNotNull(); // best we can do
	}

	@Test
	void runSquashedParams() {
		fakeLoggable();

		ExceptionSquashingTaskRunner.run(new GreetingParams("Hello there"), params ->
			new GreetingService().test(params)
		);

		assertThat(LogContext.getThrowable()).isNull();
		assertThat(LogContext.get("greeting")).isEqualTo("ie.bitstep.mango.spring.logging.task.ExceptionSquashingTaskRunnerTest.GreetingService:Hello there");
	}

	@Test
	void runSquashed() {
		fakeLoggable();

		ExceptionSquashingTaskRunner.run(() ->
			new GreetingService().test()
		);

		assertThat(LogContext.getThrowable()).isNull();
		assertThat(LogContext.get("greeting")).isEqualTo("ie.bitstep.mango.spring.logging.task.ExceptionSquashingTaskRunnerTest.GreetingService");
	}

	@Test
	void runSquashedParamsException() {
		fakeLoggable();

		ExceptionSquashingTaskRunner.run(new GreetingParams("Hello there"), params ->
			new GreetingService().testException(params)
		);

		assertThat(LogContext.getThrowable().getLocalizedMessage()).isEqualTo("ie.bitstep.mango.spring.logging.task.ExceptionSquashingTaskRunnerTest.GreetingService:Hello there");
	}

	@Test
	void runSquashedException() {
		fakeLoggable();

		ExceptionSquashingTaskRunner.run(() ->
			new GreetingService().testException()
		);

		assertThat(LogContext.getThrowable().getLocalizedMessage()).isEqualTo("ie.bitstep.mango.spring.logging.task.ExceptionSquashingTaskRunnerTest.GreetingService");
	}
}