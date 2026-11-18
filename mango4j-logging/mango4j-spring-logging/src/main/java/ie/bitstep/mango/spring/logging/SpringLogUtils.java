package ie.bitstep.mango.spring.logging;

import ie.bitstep.mango.logging.annotations.LoggableMethod;
import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.web.WebMDCField;
import ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Spring/servlet specific logging helpers. The framework-free helpers live in
 * {@link ie.bitstep.mango.logging.LogUtils}.
 */
public class SpringLogUtils {

	private SpringLogUtils() {
	}

	public static LoggableMethod getLoggableMethod(MethodInfo methodInfo) {
		return methodInfo.getMethod().getAnnotation(LoggableMethod.class);
	}

	public static void propagateToMDC(HttpServletRequest request, Optional<Set<String>> include) {
		toMDC(WebMDCField.REQUEST_ID.getFieldName(), include,
			() -> request.getAttribute(PopulatorConstants.REQUEST_ID_ATTRIBUTE));

		toMDC(WebMDCField.CORRELATION_ID.getFieldName(), include,
			() -> request.getAttribute(PopulatorConstants.CORRELATION_ID));

		toMDC(WebMDCField.TYPE.getFieldName(), include,
			() -> request.getAttribute(PopulatorConstants.TYPE));

		toMDC(WebMDCField.OPERATION.getFieldName(), include,
			() -> request.getAttribute(PopulatorConstants.OPERATION));

		toMDC(WebMDCField.USER_ID.getFieldName(), include, request::getRemoteUser);

		toMDC(WebMDCField.HTTP_METHOD.getFieldName(), include, request::getMethod);
		toMDC(WebMDCField.URL.getFieldName(), include, request::getRequestURI);
		toMDC(WebMDCField.REQUEST_PARAMS.getFieldName(), include, request::getQueryString);
		toMDC(WebMDCField.REMOTE_IP.getFieldName(), include, request::getRemoteAddr);
		toMDC(WebMDCField.PORT.getFieldName(), include, () -> Integer.toString(request.getRemotePort()));
	}

	private static void toMDC(String key, Optional<Set<String>> include, Supplier<Object> supplier) {
		if (!include.isPresent() || include.get().contains(key)) {
			Object value = supplier.get();
			if (!ObjectUtils.isEmpty(value)) {
				MDC.put(key, value.toString());
			}
		}
	}
}
