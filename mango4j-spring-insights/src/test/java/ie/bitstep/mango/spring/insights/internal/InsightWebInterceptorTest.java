package ie.bitstep.mango.spring.insights.internal;

import ie.bitstep.mango.spring.logging.test.MapBuilder;
import ie.bitstep.mango.spring.insights.consts.InsightStatus;
import ie.bitstep.mango.spring.insights.initialiser.DefaultInsightsContextInitialiser;
import ie.bitstep.mango.spring.logging.test.MDCTestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightWebInterceptorTest {

	@Mock
	private HttpServletRequest mockRequest;

	@Mock
	private HttpServletResponse mockResponse;

	@Spy
	private final InsightsProvider mockInsightsProvider = new InsightsProvider(Collections.singletonList(new DefaultInsightsContextInitialiser()), null, null);

	@Captor
	private ArgumentCaptor<Method> methodCaptor;

	@Captor
	private ArgumentCaptor<AppInsight> appInsightCaptor;

	private InsightWebInterceptor interceptor;
	private Map<String, Object> expected;

	@BeforeEach
	void setup() {
		MDCTestUtils.init();
		InsightsThreadContext.unset();
		interceptor = new InsightWebInterceptor(mockInsightsProvider);
		expected = MapBuilder.<String, Object>map()
				.with(InsightsContext.HTTP_METHOD_KEY, HttpMethod.GET.name())
				.with(InsightsContext.HTTP_URL_KEY, "")
				.with(InsightsContext.HTTP_MAPPING_KEY, "")
				.build();

		lenient().when(mockRequest.getMethod()).thenReturn(HttpMethod.GET.name());
		lenient().when(mockRequest.getRequestURI()).thenReturn("");
		lenient().when(mockRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).thenReturn("");
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void preHandle() {
		// when
		assertThat(interceptor.preHandle(mockRequest, mockResponse, null)).isTrue();

		// then
		AppInsight current = InsightsThreadContext.get().current().get();

		assertThat(current.getType()).isEqualTo("INTERCEPTOR");
		assertThat(current.getAction()).isEqualTo("InsightWebInterceptor");

		assertThat(InsightsThreadContext.get().current().get().getContext())
				.containsEntry(InsightsContext.HTTP_KEY, expected);
	}

	@Test
	public void preHandleNotAnnotated() throws NoSuchMethodException { // NOSONAR - required public as used within test
		// given
		HandlerMethod handlerMethod = new HandlerMethod(this, getClass().getMethod("preHandleNotAnnotated"));

		// when
		interceptor.preHandle(mockRequest, mockResponse, handlerMethod);

		// then
		assertThat(InsightsThreadContext.get().isStackEmpty()).isTrue();
	}

	@Test
	void handlerNull() {
		Method method = interceptor.handler(null);

		assertThat(method).isNotNull();
	}

	@Test
	public void handlerMethod() throws NoSuchMethodException { // NOSONAR - required public as used within test
		HandlerMethod handlerMethod = new HandlerMethod(this, getClass().getMethod("handlerMethod"));
		Method method = interceptor.handler(handlerMethod);

		assertThat(method).isEqualTo(handlerMethod.getMethod());
	}

	@Test
	public void handlerNoSuchMethodException() throws NoSuchMethodException { // NOSONAR - required public as used within test
		HandlerMethod handlerMethod = Mockito.spy(new HandlerMethod(this, getClass().getMethod("handlerNoSuchMethodException")));

		when(handlerMethod.getMethod()).then(invocation -> {
			throw new NoSuchMethodException();
		});

		try {
			Method method = interceptor.handler(handlerMethod);
			fail("Exception not thrown");
		} catch (RuntimeException e) {

		}
	}

	@Test
	void afterCompletionWithException() {
		// given
		when(mockResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());

		assertThat(interceptor.preHandle(mockRequest, mockResponse, null)).isTrue();

		AppInsight appInsight = InsightsThreadContext.get().current().get();

		// when
		interceptor.afterCompletion(mockRequest, mockResponse, null, new RuntimeException("Test exception"));


		// then
		assertThat(appInsight.getException()).isNotEmpty();
		assertThat(appInsight.getStatus()).isEqualTo(InsightStatus.FAILURE);
		assertThat(InsightsThreadContext.get().current()).isNotPresent();
		verify(mockInsightsProvider, times(1)).newInsight(methodCaptor.capture());
		verify(mockInsightsProvider).publish(appInsight);

	}

	@Test
	void afterCompletionWithoutException() {
		// given
		when(mockResponse.getStatus()).thenReturn(HttpStatus.OK.value());

		assertThat(interceptor.preHandle(mockRequest, mockResponse, null)).isTrue();

		AppInsight appInsight = InsightsThreadContext.get().current().get();

		// when
		interceptor.afterCompletion(mockRequest, mockResponse, null, null);

		// then
		assertThat(appInsight.getException()).isEmpty();
		assertThat(appInsight.getStatus()).isEqualTo(InsightStatus.SUCCESS);
		assertThat(InsightsThreadContext.get().current()).isNotPresent();
		verify(mockInsightsProvider, times(1)).newInsight(methodCaptor.capture());
		verify(mockInsightsProvider).publish(appInsight);
	}

	@Test
	public void afterCompletionNotAnnotated() throws NoSuchMethodException { // NOSONAR - required public due to usage within test
		// given
		HandlerMethod handlerMethod = new HandlerMethod(this, getClass().getMethod("afterCompletionNotAnnotated"));

		interceptor.preHandle(mockRequest, mockResponse, null);

		// when
		interceptor.afterCompletion(mockRequest, mockResponse, handlerMethod, null);

		then(mockInsightsProvider).should(times(0)).publish(appInsightCaptor.capture());
	}

	@Test
	void afterCompletionInternalException() {
		// given
		when(mockResponse.getStatus()).thenThrow(new RuntimeException());

		interceptor.preHandle(mockRequest, mockResponse, null);

		AppInsight appInsight = InsightsThreadContext.get().current().get();

		// when
		interceptor.afterCompletion(mockRequest, mockResponse, null, null);

		// then
		then(mockInsightsProvider).should(times(0)).publish(appInsightCaptor.capture());
	}

	@Test
	void afterCompletionWithResponseCode400() {
		// given
		when(mockResponse.getStatus()).thenReturn(HttpStatus.NOT_FOUND.value());

		interceptor.preHandle(mockRequest, mockResponse, null);

		AppInsight appInsight = InsightsThreadContext.get().current().get();

		// when
		interceptor.afterCompletion(mockRequest, mockResponse, null, null);

		// then
		verify(mockInsightsProvider, times(1)).newInsight(methodCaptor.capture());
		verify(mockInsightsProvider).publish(appInsight);
	}


	@Test
	void afterCompletionWithResponseCode500() {
		// given
		when(mockResponse.getStatus()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());

		interceptor.preHandle(mockRequest, mockResponse, null);

		AppInsight appInsight = InsightsThreadContext.get().current().get();

		// when
		interceptor.afterCompletion(mockRequest, mockResponse, null, null);

		// then
		verify(mockInsightsProvider, times(1)).newInsight(methodCaptor.capture());
		verify(mockInsightsProvider).publish(appInsight);
	}

	@Test
	void afterCompletionWithEmptyStack() {
		// when
		assertDoesNotThrow(() -> interceptor.afterCompletion(mockRequest, mockResponse, null, null));
	}

}
