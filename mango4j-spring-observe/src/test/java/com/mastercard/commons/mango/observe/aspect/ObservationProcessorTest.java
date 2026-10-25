package com.mastercard.commons.mango.observe.aspect;

import com.mastercard.commons.mango.collections.ListBuilder;
import com.mastercard.commons.mango.collections.MapBuilder;
import com.mastercard.commons.mango.logging.annotations.LoggableClass;
import com.mastercard.commons.mango.logging.annotations.LoggableField;
import com.mastercard.commons.mango.observe.annotations.Independent;
import com.mastercard.commons.mango.observe.annotations.ObservationTags;
import com.mastercard.commons.mango.observe.annotations.Observe;
import com.mastercard.commons.mango.observe.annotations.Segregate;
import com.mastercard.commons.mango.observe.annotations.Tag;
import com.mastercard.commons.mango.observe.context.Observation;
import com.mastercard.commons.mango.observe.context.ObservationContext;
import com.mastercard.commons.mango.observe.context.ObservationContextThreadLocal;
import com.mastercard.commons.mango.observe.factory.ObservationFactory;
import com.mastercard.commons.mango.observe.initialisers.DefaultObservationInitializer;
import com.mastercard.commons.mango.observe.initialisers.ObservationInitializer;
import com.mastercard.commons.mango.observe.observers.Observer;
import com.mastercard.commons.mango.observe.observers.ObserverService;
import com.mastercard.commons.mango.reflection.utils.test.MDCTestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservationProcessorTest {
	private static final Object RESULT = "Response";

	@Mock
	private ProceedingJoinPoint proceedingJoinPoint;

	@Mock
	private MethodSignature methodSignature;

	@Mock
	private Observer observer;

	@Mock
	private Logger logger;

	@InjectMocks
	private ObservationProcessor observationProcessor;

	@InjectMocks
	private ObservationFactory observationFactory;

	@InjectMocks
	DefaultObservationInitializer defaultObservationInitializer;

	@InjectMocks
	@Spy
	private ObserverService observerService;

	@Captor
	private ArgumentCaptor<Observation> observationCaptor;

	@LoggableClass // replace with new serialiser when ready
	static class Request {
		@Tag(name = "firstName")
		@LoggableField // replace with new serialiser when ready
		public String firstName;

		@LoggableField // replace with new serialiser when ready
		public String lastName;

		public Request(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}
	}

	static class RequestEmptyTag {
		@Tag(name = "")
		public String firstName;

		public String lastName;

		public RequestEmptyTag(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}
	}

	static class BadRequest {
		@Tag(name = "")
		public String firstName;

		public String lastName;

		public BadRequest(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String toString() {
			throw new RuntimeException();
		}
	}

	static class PrivateRequest {
		@Tag(name = "")
		private String firstName;

		private String lastName;

		public PrivateRequest(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String toString() {
			throw new RuntimeException();
		}
	}

	ObservationProcessorTest() throws NoSuchMethodException {
	}

	@Observe(name = "testMethodNoArgs")
	public void testMethodNoArgs() {
	}

	@Observe(name = "testMethodWithObjectArg")
	@ObservationTags(base = "firstName")
	@Segregate
	public void testMethodWithObjectArg(@Observe(name = "req") Request req) {
	}

	@Observe(name = "testMethodWithObjectArg")
	@Segregate
	public void testMethodWithObjectArg(@Observe(name = "") RequestEmptyTag req) {
	}

	@Observe(name = "testMethodWithObjectArg")
	@Segregate
	public void testMethodWithObjectArg(@Observe(name = "") BadRequest req) {
	}

	@Observe(name = "testMethodNoArgsSegregate")
	@Segregate
	public void testMethodNoArgsSegregate() {
	}

	@Observe(name = "testMethodNoArgsIndependent")
	@Independent
	public void testMethodNoArgsIndependent() {
	}

	@Observe(name = "testMethodWithArgsNoTags")
	@ObservationTags(base = "ts", permutations = {"a", "b"})
	public void testMethodWithArgsNoTags(@Observe(name = "s") String s) {
	}

	@Observe(name = "testMethodWithArgs")
	@ObservationTags(base = "ts", permutations = {"a", "b"})
	@Segregate
	public void testMethodWithArgs(@Tag(name = "s") String s) {
	}

	@BeforeEach
	void setUp() {
		MDCTestUtils.init();
		ObservationContextThreadLocal.reset();

		observationFactory = new ObservationFactory(
			ListBuilder.<ObservationInitializer>list().add(new DefaultObservationInitializer()).build()
		);

		ReflectionTestUtils.setField(observationProcessor, "observationFactory", observationFactory);
		ReflectionTestUtils.setField(observationProcessor, "observerService", observerService);
		ReflectionTestUtils.setField(observerService, "observers",
			ListBuilder.<Observer>list().add(observer).build());
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void observeNoArgs() throws Throwable {
		// GIVEN
		ObservationContext instance = ObservationContextThreadLocal.get();

		// WHEN
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethodNoArgs"));
		when(proceedingJoinPoint.proceed()).thenReturn(RESULT);

		// WHEN
		Object result = observationProcessor.observe(proceedingJoinPoint);

		// THEN
		assertThat(result).isEqualTo(RESULT);
		assertThat(instance.getRoot()).isNull();
		assertThat(instance.getObservation()).isNull();
		assertThat(instance.getTopValues()).isEmpty();
		assertThat(instance.getStatValues()).isEmpty();
	}

	@Test
	void observeWithSegregation() throws Throwable {
		// GIVEN
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethodNoArgsSegregate"));
		when(proceedingJoinPoint.proceed()).thenReturn(RESULT);

		// WHEN
		Object result = observationProcessor.observe(proceedingJoinPoint);

		// THEN
		assertThat(result).isEqualTo(RESULT);
		verify(observerService, times(1)).observeOnEntry(observationCaptor.capture());
		Observation observation = observationCaptor.getValue();

		verify(observerService, times(1)).observeOnExit(observation);
		assertThat(observation.roundTripTime()).isEqualTo(
			observation.endTimeMillis() - observation.startTimeMillis()
		);
	}

	@Test
	void observeWithIndependent() throws Throwable {
		// GIVEN
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethodNoArgsIndependent"));
		when(proceedingJoinPoint.proceed()).thenReturn(RESULT);

		// WHEN
		Object result = observationProcessor.observe(proceedingJoinPoint);

		// THEN
		assertThat(result).isEqualTo(RESULT);
	}

	@Test
	void observeWithArgs() throws Throwable {
		// GIVEN
		Request request = new Request("John", "Allen");
		Map<String, Object> expectedParams = MapBuilder.<String, Object>map()
			.with("req",
				MapBuilder.<String, Object>map()
					.with("firstName", "John")
					.with("lastName", "Allen")
					.build())
			.build();

		Map<String, Object> expectedTags = MapBuilder.<String, Object>map()
			.with("firstName", "John")
			.build();

		// WHEN
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{request});
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethodWithObjectArg", Request.class));
		when(proceedingJoinPoint.proceed()).thenReturn(RESULT);

		Object result = observationProcessor.observe(proceedingJoinPoint);

		// THEN
		assertThat(result).isEqualTo(RESULT);
		verify(observerService, times(1)).observeOnEntry(observationCaptor.capture());

		assertThat(observationCaptor.getValue().params()).isEqualTo(expectedParams);
		assertThat(observationCaptor.getValue().tags()).isEqualTo(expectedTags);
	}

	@Test
	void observeWithRequestEmptyTag() throws Throwable {
		// GIVEN

		// WHEN
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{new RequestEmptyTag("John", "Allen")});
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethodWithObjectArg", RequestEmptyTag.class));
		when(proceedingJoinPoint.proceed()).thenReturn(RESULT);

		Object result = observationProcessor.observe(proceedingJoinPoint);

		// THEN
		assertThat(result).isEqualTo(RESULT);
	}

	@Test
	void observeWithBadRequest() throws Throwable {
		// GIVEN
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{new BadRequest("John", "Allen")});
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethodWithObjectArg", BadRequest.class));
		when(proceedingJoinPoint.proceed()).thenReturn(RESULT);

		// WHEN
		Object result = observationProcessor.observe(proceedingJoinPoint);

		// THEN
		assertThat(result).isEqualTo(RESULT);
	}

	@Test
	void observeWithNullRequest() throws Throwable {
		// GIVEN
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{null});
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethodWithObjectArg", Request.class));
		when(proceedingJoinPoint.proceed()).thenReturn(RESULT);

		// WHEN
		Object result = observationProcessor.observe(proceedingJoinPoint);

		// THEN
		assertThat(result).isEqualTo(RESULT);
	}

	@Test
	void observeWithObjectArgs() throws Throwable {
		// GIVEN
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"Hello"});
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethodWithArgs", String.class));
		when(proceedingJoinPoint.proceed()).thenReturn(RESULT);

		// WHEN
		Object result = observationProcessor.observe(proceedingJoinPoint);

		// THEN
		assertThat(result).isEqualTo(RESULT);
	}

	@Test
	void observeWithArgsNoTags() throws Throwable {
		// GIVEN
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"Hello"});
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethodWithArgsNoTags", String.class));
		when(proceedingJoinPoint.proceed()).thenReturn(RESULT);

		// WHEN
		Object result = observationProcessor.observe(proceedingJoinPoint);

		// THEN
		assertThat(result).isEqualTo(RESULT);
	}

	@Test
	void observeWithException() throws Throwable {
		// GIVEN
		RuntimeException expected = new RuntimeException();
		when(proceedingJoinPoint.getTarget()).thenReturn(this);
		when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("testMethodNoArgs"));
		when(proceedingJoinPoint.proceed()).thenThrow(expected);

		// WHEN
		Exception thrown = assertThrows(RuntimeException.class, () -> observationProcessor.observe(proceedingJoinPoint));

		// THEN
		assertThat(thrown).isEqualTo(expected);
	}

	@Test
	void getObjectFieldAllNulls() {
		// GIVEN
		ReflectionTestUtils.setField(ObservationProcessor.class, "logger", logger);

		// WHEN
		String s = (String) ObservationProcessor.getObjectField(null, null);

		// THEN
		verify(logger, times(1)).error(eq("Failed to argument field value"), any(Exception.class));
		assertThat(s).isEqualTo("Failed to get field value: NullPointerException");
	}

	@Test
	void getObjectFieldRequest() throws NoSuchFieldException {
		// GIVEN
		ReflectionTestUtils.setField(ObservationProcessor.class, "logger", logger);
		PrivateRequest request = new PrivateRequest("John", "Allen");

		// WHEN
		String s = (String) ObservationProcessor.getObjectField(request, request.getClass().getDeclaredField("firstName"));

		// THEN
		verify(logger, never()).error(eq("Failed to argument field value"), any(Exception.class));
		assertThat(s).isEqualTo("John");
	}
}