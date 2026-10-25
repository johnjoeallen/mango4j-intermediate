package ie.bitstep.mango.spring.insights.internal;

import ie.bitstep.mango.spring.insights.annotations.Insight;
import ie.bitstep.mango.spring.insights.consts.InsightStatus;
import ie.bitstep.mango.spring.insights.consts.InsightType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static ie.bitstep.mango.spring.insights.internal.InsightsContext.HTTP_KEY;
import static ie.bitstep.mango.spring.insights.internal.InsightsContext.HTTP_STATUS_CODE_KEY;

@Slf4j
@RequiredArgsConstructor
public class InsightWebInterceptor implements HandlerInterceptor {
	private final InsightsProvider insightsProvider;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handlerTarget) {
		InsightsThreadContext.reset(); // ensure context is clean for entry
		Method method = handler(handlerTarget);

		if (method.getAnnotation(Insight.class) != null) {
			InsightsContext insightsContext = InsightsThreadContext.get();

			AppInsight appInsight = insightsContext.push(method.getDeclaringClass(), method, insightsProvider);

			appInsight.setContextValue(HTTP_KEY, getRequestHTTPInfo(request));
		}

		return true;
	}

	private Map<String, Object> getRequestHTTPInfo(HttpServletRequest request) {
		Map<String, Object> info = new LinkedHashMap<>();
		info.put(InsightsContext.HTTP_METHOD_KEY, request.getMethod());
		info.put(InsightsContext.HTTP_URL_KEY, request.getRequestURI());
		info.put(InsightsContext.HTTP_MAPPING_KEY, request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
		return info;
	}

	@Insight(type = InsightType.INTERCEPTOR, action = "InsightWebInterceptor")
	public Method handler(Object targetHandler) {
		try {
			Class<?> clazz = getClass();
			return targetHandler instanceof HandlerMethod handler ? handler.getMethod() : clazz.getMethod("handler", Object.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Failed to get handler method"); // NOSONAR
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handlerTarget, @Nullable Exception ex) {
		try {
			Method method = handler(handlerTarget);

			if (method.getAnnotation(Insight.class) != null) {
				InsightsContext insightsContext = InsightsThreadContext.get();

				insightsContext.current().ifPresent(appInsight -> {
					((Map<String, String>) appInsight.getContext().get(HTTP_KEY)).put(HTTP_STATUS_CODE_KEY, HttpStatus.valueOf(response.getStatus()).toString());

					if (ex != null) {
						appInsight.setException(ex);
					}

					if (HttpStatus.valueOf(response.getStatus()).is4xxClientError() || HttpStatus.valueOf(response.getStatus()).is5xxServerError()) {
						appInsight.setStatus(InsightStatus.FAILURE);
					}
					else {
						appInsight.setStatus(InsightStatus.SUCCESS);
					}
				});

				insightsContext.publish(insightsProvider);
			}
		} catch (Throwable t) { // NOSONAR
			log.error("afterCompletion failed", t);
		}

		InsightsThreadContext.unset(); // clear the ThreadLocal
	}
}
