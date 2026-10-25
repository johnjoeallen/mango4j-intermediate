package com.mastercard.commons.mango.observe.web;

import com.mastercard.commons.mango.observe.context.ObservationContext;
import com.mastercard.commons.mango.observe.context.ObservationContextThreadLocal;
import com.mastercard.commons.mango.observe.context.WebContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
public class ObservationsWebInterceptor implements HandlerInterceptor {
	public final Logger logger = LoggerFactory.getLogger(ObservationsWebInterceptor.class);

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handlerTarget) {
		ObservationContext observationContext = ObservationContextThreadLocal.get();

		observationContext.setWebContext(new WebContext(request));

		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handlerTarget, @Nullable Exception ex) { // NOSONAR
		// NOSONAR: Keeping this here till v1 in-case we want to do something on exit
		// Do we want to keep the support for HTTP requests stats?
		// If so we will need to the core logic to see that there is a WebContext, and not emit an
		// observation, and instead allow this method to add the stats and emit the observation
	}
}
