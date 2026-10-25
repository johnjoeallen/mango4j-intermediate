package com.mastercard.commons.mango.observe.web;

import com.mastercard.commons.mango.observe.context.ObservationContext;
import com.mastercard.commons.mango.observe.context.ObservationContextThreadLocal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservationsWebInterceptorTest {

	private final ObservationsWebInterceptor observationsWebInterceptor = new ObservationsWebInterceptor();

	@Mock
	private HttpServletRequest httpServletRequest;

	@Mock
	private HttpServletResponse httpServletResponse;

	@Test
	void preHandle() {
		// GIVEN
		ObservationContext observationContext = ObservationContextThreadLocal.get();

		// WHEN
		when(httpServletRequest.getHeader("Host")).thenReturn("www.ireland.com");
		when(httpServletRequest.getHeaders("Host")).thenReturn(
			Collections.enumeration(List.of("www.ireland.com")));
		when(httpServletRequest.getHeaderNames()).thenReturn(
			Collections.enumeration(List.of("Host", "Location"))
		);
		when(httpServletRequest.getRequestURI()).thenReturn("https://www.ireland.com");
		when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET.name());
		when(httpServletRequest.getContextPath()).thenReturn("/broker/v2");

		boolean result = observationsWebInterceptor.preHandle(httpServletRequest, httpServletResponse, null);

		// THEN
		assertThat(result).isTrue();
		assertThat(observationContext.getWebContext()).isNotNull();

		assertThat(observationContext.getWebContext().getHeader("Host")).isEqualTo(httpServletRequest.getHeader("Host"));
		assertThat(observationContext.getWebContext().getHeaders("Host")).isEqualTo(httpServletRequest.getHeaders("Host"));
		assertThat(observationContext.getWebContext().getHeaderNames()).isEqualTo(httpServletRequest.getHeaderNames());
		assertThat(observationContext.getWebContext().getRequestURI()).isEqualTo(httpServletRequest.getRequestURI());
		assertThat(observationContext.getWebContext().getMethod()).isEqualTo(httpServletRequest.getMethod());
		assertThat(observationContext.getWebContext().getContextPath()).isEqualTo(httpServletRequest.getContextPath());
	}

	@Test
	void afterCompletion() {
		// GIVEN
		ObservationContext observationContext = ObservationContextThreadLocal.get();

		// WHEN
		when(httpServletRequest.getHeader("Host")).thenReturn("www.ireland.com");
		when(httpServletRequest.getHeaders("Host")).thenReturn(
			Collections.enumeration(List.of("www.ireland.com")));
		when(httpServletRequest.getHeaderNames()).thenReturn(
			Collections.enumeration(List.of("Host", "Location"))
		);
		when(httpServletRequest.getRequestURI()).thenReturn("https://www.ireland.com");
		when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET.name());
		when(httpServletRequest.getContextPath()).thenReturn("/broker/v2");

		observationsWebInterceptor.preHandle(httpServletRequest, httpServletResponse, null);
		observationsWebInterceptor.afterCompletion(httpServletRequest, httpServletResponse, null, null);

		// THEN
		assertThat(observationContext.getWebContext()).isNotNull();

		assertThat(observationContext.getWebContext().getHeader("Host")).isEqualTo(httpServletRequest.getHeader("Host"));
		assertThat(observationContext.getWebContext().getHeaders("Host")).isEqualTo(httpServletRequest.getHeaders("Host"));
		assertThat(observationContext.getWebContext().getHeaderNames()).isEqualTo(httpServletRequest.getHeaderNames());
		assertThat(observationContext.getWebContext().getRequestURI()).isEqualTo(httpServletRequest.getRequestURI());
		assertThat(observationContext.getWebContext().getMethod()).isEqualTo(httpServletRequest.getMethod());
		assertThat(observationContext.getWebContext().getContextPath()).isEqualTo(httpServletRequest.getContextPath());
	}
}
