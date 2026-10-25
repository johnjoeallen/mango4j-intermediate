package ie.bitstep.mango.spring.logging.web;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.annotations.LoggableMethod;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContextThreadLocal;
import ie.bitstep.mango.spring.logging.internal.DiagnosticContextItem;
import ie.bitstep.mango.spring.logging.internal.DiagnosticContextLogger;
import ie.bitstep.mango.spring.logging.test.TestUtils;
import ie.bitstep.mango.spring.logging.web.populators.WebContextPostPopulator;
import ie.bitstep.mango.spring.logging.web.populators.WebContextPrePopulator;
import ie.bitstep.mango.reflection.utils.test.MDCTestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class LoggingWebInterceptorTest {
	@InjectMocks
	private LoggingWebInterceptor loggingWebInterceptor;

	@Mock
	private DiagnosticContextLogger diagnosticContextLogger;

	private List<WebContextPostPopulator> postPopulatorsList = new ArrayList<>();

	private List<WebContextPrePopulator> prePopulatorsList = new ArrayList<>();

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private Object handler;

	@Mock
	private WebContextPrePopulator pre;
	@Mock
	private WebContextPostPopulator post;

	@Captor
	private ArgumentCaptor<LoggableMethod> loggableMethodArgumentCaptor;

	@BeforeEach
	void beforeSetup() {
		MDCTestUtils.init();
		prePopulatorsList = new ArrayList<>();
		prePopulatorsList.add(pre);
		postPopulatorsList = new ArrayList<>();
		postPopulatorsList.add(post);
		ReflectionTestUtils.setField(loggingWebInterceptor, "webContextPrePopulators", prePopulatorsList);
		ReflectionTestUtils.setField(loggingWebInterceptor, "webContextPostPopulators", postPopulatorsList);
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void postConstructorTest() {
		ArrayList<WebContextPrePopulator> prePopulatorListMock = mock(ArrayList.class);
		ArrayList<WebContextPostPopulator> postPopulatorListMock = mock(ArrayList.class);
		ReflectionTestUtils.setField(loggingWebInterceptor, "webContextPrePopulators", prePopulatorListMock);
		ReflectionTestUtils.setField(loggingWebInterceptor, "webContextPostPopulators", postPopulatorListMock);
		loggingWebInterceptor.setup();

		verify(prePopulatorListMock, times(1)).sort(AnnotationAwareOrderComparator.INSTANCE);
		verify(postPopulatorListMock, times(1)).sort(AnnotationAwareOrderComparator.INSTANCE);
		verifyNoMoreInteractions(prePopulatorListMock);
		verifyNoMoreInteractions(postPopulatorListMock);
	}

	@Test
	void preHandleTest() throws NoSuchMethodException {
		DiagnosticContext dc = DiagnosticContextThreadLocal.reset();
		boolean actual = loggingWebInterceptor.preHandle(request, response, handler);

		assertThat(actual).isTrue();
		assertThat(dc.getCurrentItem()).isNotNull();
		verify(pre, times(1)).populate(request);
		verifyNoMoreInteractions(pre);
		verifyNoInteractions(post);
	}

	@Test
	void afterCompletionTest() throws NoSuchMethodException {
		try (MockedStatic<DiagnosticContextThreadLocal> diagnosticContextThreadLocalMockedStatic = Mockito.mockStatic(DiagnosticContextThreadLocal.class, Mockito.CALLS_REAL_METHODS)) {
			DiagnosticContext dc = DiagnosticContextThreadLocal.reset();
			DiagnosticContextItem preRunCurrentItem = dc.getCurrentItem();
			dc.push(new MethodInfo(TestUtils.class, TestUtils.class.getMethod("fakeLoggable")));

			loggingWebInterceptor.afterCompletion(request, response, handler, null);

			diagnosticContextThreadLocalMockedStatic.verify(
				DiagnosticContextThreadLocal::unset, times(2)
			);

			assertThat(dc).isNotNull();
			assertThat(dc.getCurrentItem()).isEqualTo(preRunCurrentItem);

			verify(diagnosticContextLogger, times(1)).logExitOrException(loggableMethodArgumentCaptor.capture(), any(DiagnosticContext.class));
			verify(post, times(1)).populate(request, response);
			verifyNoMoreInteractions(diagnosticContextLogger);
			verifyNoMoreInteractions(post);
			verifyNoInteractions(pre);
		}
	}

	@Test
	void pseudoHandleTest() {
		loggingWebInterceptor.handle();
		assertThat(true).isTrue(); // NOSONAR nothing else
	}


}