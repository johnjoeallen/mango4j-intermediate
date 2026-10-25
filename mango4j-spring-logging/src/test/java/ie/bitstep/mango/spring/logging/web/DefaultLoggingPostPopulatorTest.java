package ie.bitstep.mango.spring.logging.web;

import ie.bitstep.mango.spring.logging.LogContext;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContextThreadLocal;
import ie.bitstep.mango.spring.logging.web.populators.base.DefaultLoggingPostPopulator;
import ie.bitstep.mango.reflection.utils.test.MDCTestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.REQUEST_STATS_ATTRIBUTE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultLoggingPostPopulatorTest {
	@Mock
	private HttpServletRequest httpRequest;

	@Mock
	private HttpServletResponse httpResponse;

	@BeforeEach
	public void setup() throws Throwable {
		MDCTestUtils.init();
		DiagnosticContextThreadLocal.unset();
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void testPopulate() {
		// given
		DefaultLoggingPostPopulator defaultLoggingPostPopulator = new DefaultLoggingPostPopulator();

		// when
		when(httpRequest.getAttribute(REQUEST_STATS_ATTRIBUTE)).thenReturn("true");

		try (MockedStatic<LogContext> logContextMockedStatic = Mockito.mockStatic(LogContext.class)) {
			defaultLoggingPostPopulator.populate(httpRequest, httpResponse);
			logContextMockedStatic.verify(
				() -> LogContext.putStat(REQUEST_STATS_ATTRIBUTE, "true"),
				times(1)
			);
		}

	}

	@Test
	void testNullPopulate() {
		// given
		DefaultLoggingPostPopulator defaultLoggingPostPopulator = new DefaultLoggingPostPopulator();

		// when
		when(httpRequest.getAttribute(REQUEST_STATS_ATTRIBUTE)).thenReturn(null);

		try (MockedStatic<LogContext> diagnosticConlogContextMockedStaticextMockedStatic = Mockito.mockStatic(LogContext.class)) {
			defaultLoggingPostPopulator.populate(httpRequest, httpResponse);
			diagnosticConlogContextMockedStaticextMockedStatic.verify(
				() -> LogContext.putStat(any(), any()),
				never()
			);
		}
	}

}