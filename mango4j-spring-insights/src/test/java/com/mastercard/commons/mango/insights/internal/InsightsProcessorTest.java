package com.mastercard.commons.mango.insights.internal;

import com.mastercard.commons.mango.insights.annotations.Insight;
import com.mastercard.commons.mango.insights.annotations.InsightTag;
import com.mastercard.commons.mango.insights.annotations.InsightVariable;
import com.mastercard.commons.mango.insights.consts.InsightType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightsProcessorTest {

	private static final Object RESULT = "Response";

	@Mock
	private InsightsProvider mockInsightsProvider;

	@InjectMocks
	private InsightsProcessor insightsProcessor;

	@Captor
	private ArgumentCaptor<AppInsight> appInsightCaptor;

	@Mock
	private ProceedingJoinPoint mockProceedingJoinPoint;
	@Mock
	private MethodSignature methodSignature;

	@Captor
	private ArgumentCaptor<AppInsight> insightArgumentCaptor;
	private Method methodToTest;

	@Insight(type = InsightType.API, action = "api")
	@SuppressWarnings("java:S5786")
	public void api(
			@InsightVariable(name = "name")
			@InsightTag(name = "name")
			String name,
			@InsightVariable(name = "")
			@InsightTag(name = "")
			String value
	) {

	}

	@BeforeEach
	void setup() throws Throwable {
		InsightsThreadContext.unset();
		methodToTest = getClass().getMethod("api", String.class, String.class);
	}

	@Test
	void proceedWithNullParameter() throws Throwable {
		// given
		when(mockProceedingJoinPoint.getTarget()).thenReturn(this);
		when(mockProceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);
		when(methodSignature.getMethod()).thenReturn(methodToTest);
		when(mockProceedingJoinPoint.getArgs()).thenReturn(new Object[]{null, null});
		when(mockInsightsProvider.newInsight(methodToTest)).thenReturn(new AppInsight().setTimestamp(Instant.now()));

		// when
		Object result = insightsProcessor.proceed(mockProceedingJoinPoint);

		// then
		assertThat(result).isEqualTo(RESULT);
		then(mockInsightsProvider).should().publish(appInsightCaptor.capture());
		assertThat(appInsightCaptor.getValue().getMethod()).isEqualTo("InsightsProcessorTest.api");
	}

	@Test
	void proceed() throws Throwable {
		// given
		when(mockProceedingJoinPoint.getTarget()).thenReturn(this);
		when(mockProceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);
		when(methodSignature.getMethod()).thenReturn(methodToTest);
		when(mockProceedingJoinPoint.getArgs()).thenReturn(new Object[]{"Name", "Me"});
		when(mockInsightsProvider.newInsight(methodToTest)).thenReturn(new AppInsight().setTimestamp(Instant.now()));

		// when
		Object result = insightsProcessor.proceed(mockProceedingJoinPoint);

		// then
		assertThat(result).isEqualTo(RESULT);
		then(mockInsightsProvider).should().publish(appInsightCaptor.capture());
		assertThat(appInsightCaptor.getValue().getMethod()).isEqualTo("InsightsProcessorTest.api");
		verify(mockInsightsProvider, times(1)).publish(insightArgumentCaptor.capture());
		assertThat(insightArgumentCaptor.getValue().getContext()).hasSize(2).containsEntry("name", "Name");
	}

	@Test
	void proceedArgumentInContextExceptionTest() throws Throwable {
		// given
		when(mockProceedingJoinPoint.getTarget()).thenReturn(this);
		when(mockProceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);
		when(methodSignature.getMethod()).thenReturn(methodToTest);
		when(mockProceedingJoinPoint.getArgs()).thenReturn(new Object[]{"Name", "Me"});
		when(mockInsightsProvider.newInsight(methodToTest)).thenReturn(new AppInsight().setTimestamp(Instant.now()));

		try (MockedStatic<Insights> insightsMocked = Mockito.mockStatic(Insights.class)) {
			insightsMocked.when(() -> Insights.setContextValue(any(), any())).thenThrow(new RuntimeException());

			// when
			Object result = insightsProcessor.proceed(mockProceedingJoinPoint);

			// then
			assertThat(result).isEqualTo(RESULT);
			then(mockInsightsProvider).should().publish(appInsightCaptor.capture());
			assertThat(appInsightCaptor.getValue().getMethod()).isEqualTo("InsightsProcessorTest.api");
			verify(mockInsightsProvider, times(1)).publish(insightArgumentCaptor.capture());
			assertThat(insightArgumentCaptor.getValue().getContext()).hasSize(1);
		}
	}

	@Test
	void proceedWithException() throws Throwable {
		Exception expectedException = new Exception("failure");

		when(mockProceedingJoinPoint.getTarget()).thenReturn(this);
		when(mockProceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);
		when(methodSignature.getMethod()).thenReturn(methodToTest);
		when(mockInsightsProvider.newInsight(methodToTest)).thenReturn(new AppInsight().setTimestamp(Instant.now()));
		when(mockProceedingJoinPoint.proceed()).thenThrow(expectedException);

		try {
			insightsProcessor.proceed(mockProceedingJoinPoint);

			fail("Exception expected");
		} catch (Exception e) {
			assertThat(e).isEqualTo(expectedException);

			then(mockInsightsProvider).should().publish(appInsightCaptor.capture());
			assertThat(appInsightCaptor.getValue().getMethod()).isEqualTo("InsightsProcessorTest.api");
		}
	}

	@Test
	void proceedNested() throws Throwable {
		// given
		when(mockInsightsProvider.newInsight(methodToTest)).thenReturn(new AppInsight().setTimestamp(Instant.now()));
		when(mockProceedingJoinPoint.getTarget()).thenReturn(this);
		when(mockProceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);
		when(methodSignature.getMethod()).thenReturn(methodToTest);
		InsightsThreadContext.get().push(getClass(), methodToTest, mockInsightsProvider);

		// when
		Object result = insightsProcessor.proceed(mockProceedingJoinPoint);

		// then
		assertThat(result).isEqualTo(RESULT);
		then(mockInsightsProvider).should(times(0)).publish(appInsightCaptor.capture());
	}

	@Test
	@Insight(type = InsightType.API, action = "")
	@SuppressWarnings("java:S5786")
	public void exceptionHandlerWithExceptionArgs() throws Throwable {
		InsightsContext insightsContext = InsightsThreadContext.get();

		Exception exception = new Exception("failure");
		ExceptionDetail exceptionDetail = ExceptionDetail.builder().name(exception.getClass().getSimpleName()).message(exception.getMessage()).build();
		List<ExceptionDetail> expected = new ArrayList<ExceptionDetail>();
		expected.add(exceptionDetail);

		when(mockProceedingJoinPoint.getArgs()).thenReturn(new Object[]{exception});
		methodToTest = getClass().getMethod("exceptionHandlerWithExceptionArgs");
		when(mockInsightsProvider.newInsight(methodToTest)).thenReturn(new AppInsight().setTimestamp(Instant.now()));
		when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);

		InsightsThreadContext.get().push(getClass(), methodToTest, mockInsightsProvider);

		insightsProcessor.interceptExceptionHandler(mockProceedingJoinPoint);

		assertThat(insightsContext.current().get().getException()).isEqualTo(expected);
	}

	@Test
	@Insight(type = InsightType.API, action = "")
	@SuppressWarnings("java:S5786")
	public void exceptionHandlerWithExceptionCausedByArgs() throws Throwable {
		InsightsContext insightsContext = InsightsThreadContext.get();

		List<ExceptionDetail> expected = new ArrayList<ExceptionDetail>();
		Exception causedBy = new Exception("causedby");
		Exception exception = new Exception("failure", causedBy);
		expected.add(ExceptionDetail.builder().name(exception.getClass().getSimpleName()).message(exception.getMessage()).build());
		expected.add(ExceptionDetail.builder().name(causedBy.getClass().getSimpleName()).message(causedBy.getMessage()).build());

		when(mockProceedingJoinPoint.getArgs()).thenReturn(new Object[]{exception});
		methodToTest = getClass().getMethod("exceptionHandlerWithExceptionCausedByArgs");
		when(mockInsightsProvider.newInsight(methodToTest)).thenReturn(new AppInsight().setTimestamp(Instant.now()));
		when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);

		InsightsThreadContext.get().push(getClass(), methodToTest, mockInsightsProvider);

		insightsProcessor.interceptExceptionHandler(mockProceedingJoinPoint);

		assertThat(insightsContext.current().get().getException()).isEqualTo(expected);
	}

	@Test
	@Insight(type = InsightType.API, action = "")
	public void exceptionHandlerWithNonExceptionArgs() throws Throwable { // NOSONAR - required public as used within test
		InsightsContext insightsContext = InsightsThreadContext.get();
		when(mockProceedingJoinPoint.getArgs()).thenReturn(new Object[]{""});
		methodToTest = getClass().getMethod("exceptionHandlerWithNonExceptionArgs");
		when(mockInsightsProvider.newInsight(methodToTest)).thenReturn(new AppInsight().setTimestamp(Instant.now()));
		when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);

		InsightsThreadContext.get().push(getClass(), methodToTest, mockInsightsProvider);

		insightsProcessor.interceptExceptionHandler(mockProceedingJoinPoint);

		assertThat(insightsContext.current().get().getException()).isEmpty();
	}

	@Test
	@Insight(type = InsightType.API, action = "")
	@SuppressWarnings("java:S5786")
	public void exceptionHandlerNoArgs() throws Throwable {
		InsightsContext insightsContext = InsightsThreadContext.get();
		when(mockProceedingJoinPoint.getArgs()).thenReturn(new Object[]{});
		methodToTest = getClass().getMethod("exceptionHandlerNoArgs");
		when(mockInsightsProvider.newInsight(methodToTest)).thenReturn(new AppInsight().setTimestamp(Instant.now()));

		InsightsThreadContext.get().push(getClass(), methodToTest, mockInsightsProvider);

		insightsProcessor.interceptExceptionHandler(mockProceedingJoinPoint);

		assertThat(insightsContext.current().get().getException()).isEmpty();
	}
}
