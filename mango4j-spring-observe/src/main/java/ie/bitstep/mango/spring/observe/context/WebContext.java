package ie.bitstep.mango.spring.observe.context;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;

public class WebContext {
	private final HttpServletRequest request;

	public WebContext(HttpServletRequest request) {
		this.request = request;
	}

	public String getHeader(String name) {
		return request.getHeader(name);
	}

	public Enumeration<String> getHeaders(String name) {
		return request.getHeaders(name);
	}

	public Enumeration<String> getHeaderNames() {
		return request.getHeaderNames();
	}

	public String getRequestURI() {
		return request.getRequestURI();
	}

	public String getMethod() {
		return request.getMethod();
	}

	public String getContextPath() {
		return request.getContextPath();
	}
}
