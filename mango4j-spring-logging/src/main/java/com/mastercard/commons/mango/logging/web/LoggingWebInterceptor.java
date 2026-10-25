package com.mastercard.commons.mango.logging.web;

import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.aop.MethodInfoFactory;
import com.mastercard.commons.mango.logging.LogUtils;
import com.mastercard.commons.mango.logging.annotations.LoggableMethod;
import com.mastercard.commons.mango.logging.dc.DiagnosticContext;
import com.mastercard.commons.mango.logging.dc.DiagnosticContextThreadLocal;
import com.mastercard.commons.mango.logging.internal.DiagnosticContextLogger;
import com.mastercard.commons.mango.logging.web.populators.WebContextPostPopulator;
import com.mastercard.commons.mango.logging.web.populators.WebContextPrePopulator;
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
