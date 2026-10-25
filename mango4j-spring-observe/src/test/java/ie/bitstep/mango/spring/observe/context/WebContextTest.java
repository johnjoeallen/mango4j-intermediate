package ie.bitstep.mango.spring.observe.context;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
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
class WebContextTest {
	private WebContext webContext;

	@Mock
	private HttpServletRequest httpServletRequest;

	@BeforeEach
	void setUp() {
		webContext = new WebContext(httpServletRequest);
	}

	@Test
	void getHeader() {
		// GIVEN

		// WHEN
		when(httpServletRequest.getHeader("Host")).thenReturn("www.ireland.com");

		// THEN
		assertThat(webContext.getHeader("Host")).isEqualTo(httpServletRequest.getHeader("Host"));
	}

	@Test
	void getHeaders() {
		// GIVEN

		// WHEN
		when(httpServletRequest.getHeaders("Host")).thenReturn(
			Collections.enumeration(List.of("www.ireland.com")));

		// THEN
		assertThat(webContext.getHeaders("Host")).isEqualTo(httpServletRequest.getHeaders("Host"));
	}

	@Test
	void getHeaderNames() {
		// GIVEN

		// WHEN
		when(httpServletRequest.getHeaderNames()).thenReturn(
			Collections.enumeration(List.of("Host", "Location"))
		);

		// THEN
		assertThat(webContext.getHeaderNames()).isEqualTo(httpServletRequest.getHeaderNames());
	}

	@Test
	void getRequestURI() {
		// GIVEN

		// WHEN
		when(httpServletRequest.getRequestURI()).thenReturn("https://www.ireland.com");

		// THEN
		assertThat(webContext.getRequestURI()).isEqualTo(httpServletRequest.getRequestURI());
	}

	@Test
	void getMethod() {
		// GIVEN

		// WHEN
		when(httpServletRequest.getMethod()).thenReturn(HttpMethod.GET.name());

		// THEN
		assertThat(webContext.getMethod()).isEqualTo(httpServletRequest.getMethod());
	}

	@Test
	void getContextPath() {
		// GIVEN

		// WHEN
		when(httpServletRequest.getContextPath()).thenReturn("/broker/v2");

		// THEN
		assertThat(webContext.getContextPath()).isEqualTo(httpServletRequest.getContextPath());
	}
}