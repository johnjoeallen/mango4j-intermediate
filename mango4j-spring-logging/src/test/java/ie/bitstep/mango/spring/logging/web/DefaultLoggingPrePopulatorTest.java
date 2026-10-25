package ie.bitstep.mango.spring.logging.web;

import ie.bitstep.mango.spring.logging.LogContext;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContextThreadLocal;
import ie.bitstep.mango.spring.logging.web.populators.base.DefaultLoggingPrePopulator;
import ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants;
import ie.bitstep.mango.spring.logging.test.MDCTestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.UUID;

import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.CORRELATION_ID;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.HEADER_REQUEST_ID;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.OPERATION;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.REQUEST_ID_ATTRIBUTE;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.REQUEST_START_TIME_ATTRIBUTE;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.TYPE;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.TYPE_API;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.TYPE_BUSINESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultLoggingPrePopulatorTest {

	public static final String TRANSACT = "TRANSACT";
	private static final String REQUEST_URI = "URL";
	private static final String REQUEST_ID = "requestIdValue";

	@Mock
	private HttpServletRequest httpRequest;

	@Mock
	private HttpServletResponse httpResponse;

	@InjectMocks
	private DefaultLoggingPrePopulator defaultLoggingPrePopulator;

	@BeforeEach
	void setup() throws Throwable {
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
		String correlationid = UUID.randomUUID().toString();

		MDC.put(WebMDCField.CORRELATION_ID.getFieldName(), "random value");
		MDC.put(WebMDCField.OPERATION.getFieldName(), "random value");
		MDC.put(WebMDCField.TYPE.getFieldName(), TYPE_API);

		// when
		when(httpRequest.getAttribute(CORRELATION_ID)).thenReturn(correlationid);
		when(httpRequest.getAttribute(TYPE)).thenReturn(TYPE_BUSINESS);
		when(httpRequest.getAttribute(OPERATION)).thenReturn(TRANSACT);
		when(httpRequest.getRequestURI()).thenReturn(REQUEST_URI);
		when(httpRequest.getHeader(HEADER_REQUEST_ID)).thenReturn(REQUEST_ID);

		defaultLoggingPrePopulator.populate(httpRequest);

		// then
		assertThat(MDC.get(WebMDCField.TYPE.getFieldName())).isEqualTo(TYPE_BUSINESS);
		assertThat(MDC.get(WebMDCField.OPERATION.getFieldName())).isEqualTo(TRANSACT);
		assertThat(MDC.get(WebMDCField.CORRELATION_ID.getFieldName())).isEqualTo(correlationid);
		assertThat(MDC.getCopyOfContextMap()).doesNotContainKey(WebMDCField.URL.getFieldName());
		assertThat(MDC.get(REQUEST_ID_ATTRIBUTE)).isEqualTo(REQUEST_ID);
		assertThat(MDC.get(REQUEST_START_TIME_ATTRIBUTE)).isNotNull();
		assertThat(LogContext.get(DiagnosticContext.LOG_KEY_URL)).isEqualTo(REQUEST_URI);
		assertThat(LogContext.get(REQUEST_ID_ATTRIBUTE)).isEqualTo(REQUEST_ID);

		verify(httpRequest, times(1)).setAttribute(REQUEST_ID_ATTRIBUTE, REQUEST_ID);
		verify(httpRequest, times(1)).setAttribute(eq(PopulatorConstants.REQUEST_START_TIME_ATTRIBUTE), anyLong());
	}

	@Test
	void testPopulate_verifyMDC_TypeIsCalled() {
		// Target: Line 46 - MDC.put for TYPE (SURVIVED)
		// given
		when(httpRequest.getRequestURI()).thenReturn(REQUEST_URI);
		when(httpRequest.getHeader(HEADER_REQUEST_ID)).thenReturn(REQUEST_ID);

		// when
		try (org.mockito.MockedStatic<MDC> mockedMDC = org.mockito.Mockito.mockStatic(MDC.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			defaultLoggingPrePopulator.populate(httpRequest);

			// then - verify MDC.put was called for TYPE field
			// Note: May be called more than once due to propagateToMDC
			mockedMDC.verify(() ->
					MDC.put(WebMDCField.TYPE.getFieldName(), TYPE_BUSINESS),
				org.mockito.Mockito.atLeastOnce()
			);
		}
	}

	@Test
	void testPopulate_verifyMDC_RequestIdIsCalled() {
		// Target: Line 70 - MDC.put for REQUEST_ID (SURVIVED)
		// given
		when(httpRequest.getRequestURI()).thenReturn(REQUEST_URI);
		when(httpRequest.getHeader(HEADER_REQUEST_ID)).thenReturn(REQUEST_ID);

		// when
		try (org.mockito.MockedStatic<MDC> mockedMDC = org.mockito.Mockito.mockStatic(MDC.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			defaultLoggingPrePopulator.populate(httpRequest);

			// then - verify MDC.put was called for REQUEST_ID
			// Note: May be called more than once due to LogContext.put which also calls MDC
			mockedMDC.verify(() ->
					MDC.put(REQUEST_ID_ATTRIBUTE, REQUEST_ID),
				org.mockito.Mockito.atLeastOnce()
			);
		}
	}

	@Test
	void testPopulate_verifyAllMDCCalls() {
		// Comprehensive test to verify all MDC interactions
		// given
		String correlationid = UUID.randomUUID().toString();
		when(httpRequest.getAttribute(CORRELATION_ID)).thenReturn(correlationid);
		when(httpRequest.getAttribute(TYPE)).thenReturn(TYPE_BUSINESS);
		when(httpRequest.getAttribute(OPERATION)).thenReturn(TRANSACT);
		when(httpRequest.getRequestURI()).thenReturn(REQUEST_URI);
		when(httpRequest.getHeader(HEADER_REQUEST_ID)).thenReturn(REQUEST_ID);

		// when
		try (org.mockito.MockedStatic<MDC> mockedMDC = org.mockito.Mockito.mockStatic(MDC.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			defaultLoggingPrePopulator.populate(httpRequest);

			// then - verify TYPE was set
			mockedMDC.verify(() ->
					MDC.put(WebMDCField.TYPE.getFieldName(), TYPE_BUSINESS),
				org.mockito.Mockito.atLeastOnce()
			);

			// verify REQUEST_ID was set
			mockedMDC.verify(() ->
					MDC.put(REQUEST_ID_ATTRIBUTE, REQUEST_ID),
				org.mockito.Mockito.atLeastOnce()
			);

			// verify REQUEST_START_TIME was set
			mockedMDC.verify(() ->
					MDC.put(eq(REQUEST_START_TIME_ATTRIBUTE), org.mockito.ArgumentMatchers.anyString()),
				org.mockito.Mockito.atLeastOnce()
			);

			// verify removeFieldsFromMDC was effective (MDC.remove called for excluded fields)
			// This verifies the method execution path
			mockedMDC.verify(() ->
					MDC.remove(org.mockito.ArgumentMatchers.anyString()),
				org.mockito.Mockito.atLeastOnce()
			);
		}
	}

	@Test
	void testSetRequestId_viaPopulate_withHeaderValue() {
		// Test that setRequestId calls MDC.put through populate() method
		// Target: Verify line 70 - MDC.put(REQUEST_ID_ATTRIBUTE, requestId)
		// given
		when(httpRequest.getRequestURI()).thenReturn(REQUEST_URI);
		when(httpRequest.getHeader(HEADER_REQUEST_ID)).thenReturn(REQUEST_ID);

		// when
		try (org.mockito.MockedStatic<MDC> mockedMDC = org.mockito.Mockito.mockStatic(MDC.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			defaultLoggingPrePopulator.populate(httpRequest);

			// then - verify MDC.put was called for REQUEST_ID with exact value
			// Note: Called multiple times due to LogContext.put also using MDC
			mockedMDC.verify(() ->
					MDC.put(REQUEST_ID_ATTRIBUTE, REQUEST_ID),
				org.mockito.Mockito.atLeastOnce()
			);
		}
	}

	@Test
	void testSetRequestId_viaPopulate_withGeneratedUUID() {
		// Test setRequestId when no header is present (generates UUID) via populate()
		// Target: Verify line 70 - MDC.put is called even with generated UUID
		// given
		when(httpRequest.getRequestURI()).thenReturn(REQUEST_URI);
		when(httpRequest.getHeader(HEADER_REQUEST_ID)).thenReturn(null);
		when(httpRequest.getHeader(PopulatorConstants.PCF_HEADER_REQUEST_ID)).thenReturn(null);

		// when
		try (org.mockito.MockedStatic<MDC> mockedMDC = org.mockito.Mockito.mockStatic(MDC.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			defaultLoggingPrePopulator.populate(httpRequest);

			// then - should call MDC.put with REQUEST_ID_ATTRIBUTE and a generated UUID
			mockedMDC.verify(() ->
					MDC.put(eq(REQUEST_ID_ATTRIBUTE), org.mockito.ArgumentMatchers.anyString()),
				org.mockito.Mockito.atLeastOnce()
			);
		}
	}

	@Test
	void testSetStartTime_viaPopulate_callsMDC() {
		// Test that setStartTime calls MDC.put through populate() method
		// Target: Verify line 77 - MDC.put(REQUEST_START_TIME_ATTRIBUTE, millis.toString())
		// given
		when(httpRequest.getRequestURI()).thenReturn(REQUEST_URI);
		when(httpRequest.getHeader(HEADER_REQUEST_ID)).thenReturn(REQUEST_ID);

		// when
		try (org.mockito.MockedStatic<MDC> mockedMDC = org.mockito.Mockito.mockStatic(MDC.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			defaultLoggingPrePopulator.populate(httpRequest);

			// then - verify MDC.put was called for REQUEST_START_TIME
			mockedMDC.verify(() ->
					MDC.put(eq(REQUEST_START_TIME_ATTRIBUTE), org.mockito.ArgumentMatchers.anyString()),
				org.mockito.Mockito.atLeastOnce()
			);
		}
	}

	@Test
	void testRemoveFieldsFromMDC_viaPopulate_callsMDCRemove() {
		// Test that removeFieldsFromMDC calls MDC.remove through populate() method
		// Target: Verify line 58 - MDC.remove(field) is called
		// given - set some values in MDC first
		MDC.put(WebMDCField.URL.getFieldName(), "test-url");
		MDC.put(WebMDCField.TYPE.getFieldName(), "test-type");
		MDC.put(WebMDCField.OPERATION.getFieldName(), "test-operation");

		when(httpRequest.getRequestURI()).thenReturn(REQUEST_URI);
		when(httpRequest.getHeader(HEADER_REQUEST_ID)).thenReturn(REQUEST_ID);

		// when
		try (org.mockito.MockedStatic<MDC> mockedMDC = org.mockito.Mockito.mockStatic(MDC.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			defaultLoggingPrePopulator.populate(httpRequest);

			// then - MDC.remove should be called for fields that are not CORRELATION_ID
			// removeFieldsFromMDC is called with CORRELATION_ID as excluded, so other fields should be removed
			mockedMDC.verify(() ->
					MDC.remove(org.mockito.ArgumentMatchers.anyString()),
				org.mockito.Mockito.atLeastOnce()
			);
		}
	}
}