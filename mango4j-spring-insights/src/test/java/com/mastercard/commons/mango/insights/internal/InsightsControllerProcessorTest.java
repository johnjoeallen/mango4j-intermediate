package com.mastercard.commons.mango.insights.internal;

import com.mastercard.commons.mango.insights.annotations.Insight;
import com.mastercard.commons.mango.insights.consts.InsightType;
import com.mastercard.commons.mango.insights.initialiser.DefaultInsightsContextInitialiser;
import com.mastercard.commons.mango.insights.initialiser.InsightsContextInitialiser;
import com.mastercard.commons.mango.insights.publisher.InsightsPublisher;
import com.mastercard.commons.mango.insights.publisher.InsightsPublisherLogFile;
import com.mastercard.commons.mango.reflection.utils.test.MDCTestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Controller
class InsightsControllerProcessorTest {

	private static final Object RESULT = "Response";
	private final InsightsPublisher insightsPublisher = new InsightsPublisherLogFile();
	private final InsightsContextInitialiser insightsContextInitialiser = new DefaultInsightsContextInitialiser();
	private final InsightsProvider insightsProvider = new InsightsProvider(Collections.singletonList(insightsContextInitialiser), Collections.singletonList(insightsPublisher), null);
	private final InsightsProcessor insightsProcessor = new InsightsProcessor(insightsProvider);

	@Mock
	private ProceedingJoinPoint mockProceedingJoinPoint;

	@Mock
	private MethodSignature methodSignature;

	@BeforeEach
	@Insight(type = InsightType.API, action = "setup")
	public void setup() throws Throwable {
		MDCTestUtils.init();
		InsightsThreadContext.unset();
		InsightsContext insightsContext = InsightsThreadContext.get();
		insightsContext.push(getClass(), getClass().getMethod("setup"), insightsProvider);
		lenient().when(mockProceedingJoinPoint.getTarget()).thenReturn(this);
		lenient().when(mockProceedingJoinPoint.getSignature()).thenReturn(methodSignature);
		lenient().when(mockProceedingJoinPoint.proceed()).thenReturn(RESULT);
		lenient().when(methodSignature.getMethod()).thenReturn(getClass().getMethod("setup"));
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void proceed() throws Throwable {
		Object result = insightsProcessor.proceed(mockProceedingJoinPoint);

		assertThat(result).isEqualTo(RESULT);
	}

	@Test
	void proceedFailsDueToEmptyCurrentInsightsContext() throws Throwable {
		InsightsContext insightsContext = InsightsThreadContext.get();
		insightsContext.reset();

		IllegalStateException thrown = Assertions.assertThrows(IllegalStateException.class, () -> {
			insightsProcessor.proceed(mockProceedingJoinPoint);
			Assertions.fail();
		});

		assertThat(thrown.getMessage()).isEqualTo("Insights should have been initialised by InsightWebInterceptor");
	}

	@Test
	void proceedWithException() throws Throwable {
		Exception expectedException = new Exception("failure");
		when(mockProceedingJoinPoint.proceed()).thenThrow(expectedException);

		try {
			insightsProcessor.proceed(mockProceedingJoinPoint);
			fail("Exception expected");
		} catch (Exception e) {
			assertThat(e).isEqualTo(expectedException);
			AppInsight appInsight = InsightsThreadContext.get().current().get();
			assertThat(appInsight.getStatus()).isEqualTo("FAILURE");
			assertThat(appInsight.getExecutionTimeMillis()).isLessThan(100L);
		}
	}

	@Test
	void exceptionHandler() throws Throwable {
		InsightsContext insightsContext = InsightsThreadContext.get();

		Exception exception = new Exception("failure");
		ExceptionDetail exceptionDetail = ExceptionDetail.builder().name(exception.getClass().getSimpleName()).message(exception.getMessage()).build();
		List<ExceptionDetail> expected = new ArrayList<ExceptionDetail>();
		expected.add(exceptionDetail);

		when(mockProceedingJoinPoint.getArgs()).thenReturn(new Object[]{exception});

		insightsProcessor.interceptExceptionHandler(mockProceedingJoinPoint);

		assertThat(insightsContext.current().get().getException()).isEqualTo(expected);
	}

	@Test
	void exceptionHandlerEmptyStack() throws Throwable {
		InsightsThreadContext.get().reset();

		assertTrue(InsightsThreadContext.get().isStackEmpty());

		insightsProcessor.interceptExceptionHandler(mockProceedingJoinPoint);
	}
}