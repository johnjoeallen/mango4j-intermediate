package ie.bitstep.mango.spring.insights.internal;

import ie.bitstep.mango.spring.insights.annotations.Insight;
import ie.bitstep.mango.spring.insights.consts.InsightType;
import ie.bitstep.mango.spring.insights.initialiser.DefaultInsightsContextInitialiser;
import ie.bitstep.mango.spring.insights.initialiser.InsightsContextInitialiser;
import ie.bitstep.mango.spring.insights.publisher.InsightsPublisher;
import ie.bitstep.mango.spring.insights.publisher.InsightsPublisherLogFile;
import ie.bitstep.mango.reflection.utils.test.MDCTestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightsInterceptorTest {

	private static final Object RESULT = "Response";
	private final InsightsPublisher insightsPublisher = new InsightsPublisherLogFile();
	private final InsightsContextInitialiser insightsContextInitialiser = new DefaultInsightsContextInitialiser();
	private final InsightsProvider insightsProvider = new InsightsProvider(Collections.singletonList(insightsContextInitialiser), Collections.singletonList(insightsPublisher), null);
	private final InsightsProcessor insightsProcessor = new InsightsProcessor(insightsProvider);
	private final InsightsInterceptor insightsInterceptor = new InsightsInterceptor(insightsProcessor);
	public static final String MDC_ADAPTER = "MDC_ADAPTER";

	@Mock
	private ProceedingJoinPoint mockProceedingJoinPoint;

	@Mock
	private MethodSignature methodSignature;

	@BeforeEach
	@Insight(type = InsightType.API, action = "setup")
	public void setup() throws Throwable {
		MDCTestUtils.init();
		InsightsThreadContext.unset();
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void interceptor() throws Throwable {
		when(mockProceedingJoinPoint.getTarget()).thenReturn(this);
		when(mockProceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("setup"));

		Object result = insightsInterceptor.interceptor(mockProceedingJoinPoint);

		assertThat(result).isEqualTo(RESULT);
	}

	@Test
	void interceptorWithException() throws Throwable {
		Exception expectedException = new Exception("failure");

		when(mockProceedingJoinPoint.getTarget()).thenReturn(this);
		when(mockProceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(mockProceedingJoinPoint.proceed()).thenThrow(expectedException);
		when(methodSignature.getMethod()).thenReturn(getClass().getMethod("setup"));

		try {
			insightsInterceptor.interceptor(mockProceedingJoinPoint);
			fail("Exception expected");
		} catch (Exception e) {
			assertThat(e).isEqualTo(expectedException);
		}
	}

	@Test
	@Insight(type = InsightType.API, action = "")
	void exceptionHandlerWithoutException() throws Throwable {
		when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);
		InsightsContext insightsContext = InsightsThreadContext.get();

		InsightsThreadContext.get().push(getClass(), getClass().getMethod("exceptionHandlerWithException"), insightsProvider);

		assertThat(insightsInterceptor.interceptExceptionHandler(mockProceedingJoinPoint)).isEqualTo(RESULT);

		assertThat(insightsContext.current().get().getException()).isEmpty();
	}

	@Test
	@Insight(type = InsightType.API, action = "")
	public void exceptionHandlerWithException() throws Throwable { // NOSONAR - required public as used within test
		InsightsContext insightsContext = InsightsThreadContext.get();

		Exception exception = new Exception("failure");
		ExceptionDetail exceptionDetail = ExceptionDetail.builder().name(exception.getClass().getSimpleName()).message(exception.getMessage()).build();
		List<ExceptionDetail> expected = new ArrayList<ExceptionDetail>();
		expected.add(exceptionDetail);

		when(mockProceedingJoinPoint.getArgs()).thenReturn(new Object[]{exception});

		InsightsThreadContext.get().push(getClass(), getClass().getMethod("exceptionHandlerWithException"), insightsProvider);

		insightsInterceptor.interceptExceptionHandler(mockProceedingJoinPoint);

		assertThat(insightsContext.current().get().getException()).isEqualTo(expected);
	}
}