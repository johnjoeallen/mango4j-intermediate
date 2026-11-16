package ie.bitstep.mango.spring.logging.web;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.aop.MethodInfoFactory;
import ie.bitstep.mango.spring.logging.LogUtils;
import ie.bitstep.mango.spring.logging.annotations.LoggableMethod;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContextThreadLocal;
import ie.bitstep.mango.spring.logging.internal.DiagnosticContextLogger;
import ie.bitstep.mango.spring.logging.web.populators.WebContextPostPopulator;
import ie.bitstep.mango.spring.logging.web.populators.WebContextPrePopulator;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LoggingWebInterceptor implements HandlerInterceptor {

	public static final String DUMMY_METHOD_FOR_LOGGING = "handle";

	private final DiagnosticContextLogger diagnosticContextLogger;
	private final List<WebContextPrePopulator> webContextPrePopulators;
	private final List<WebContextPostPopulator> webContextPostPopulators;

	@PostConstruct
	public void setup() {
		webContextPrePopulators.sort(AnnotationAwareOrderComparator.INSTANCE);
		webContextPostPopulators.sort(AnnotationAwareOrderComparator.INSTANCE);
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws NoSuchMethodException {
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();
		dc.push(MethodInfoFactory.getMethodInfo(getClass(), getClass().getMethod(DUMMY_METHOD_FOR_LOGGING)))
			.setFinalMethodInfo(false)
			.setWithExternalExceptionHandler(true);

		for (WebContextPrePopulator populator : webContextPrePopulators) {
			populator.populate(request);
		}

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws NoSuchMethodException {
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();
		MethodInfo methodInfo = MethodInfoFactory.getMethodInfo(getClass(), getClass().getMethod(DUMMY_METHOD_FOR_LOGGING));
		LoggableMethod loggableMethod = LogUtils.getLoggableMethod(methodInfo);

		for (WebContextPostPopulator populator : webContextPostPopulators) {
			populator.populate(request, response);
		}

		diagnosticContextLogger.logExitOrException(loggableMethod, dc);
		dc.up();
		DiagnosticContextThreadLocal.unset();
	}

	@LoggableMethod
	public void handle() {
		// pseudo-method just to appear in the logs
	}
}
