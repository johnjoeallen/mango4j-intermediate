package ie.bitstep.mango.spring.logging.web.populators.base;

import ie.bitstep.mango.spring.logging.LogContext;
import ie.bitstep.mango.spring.logging.LogUtils;
import ie.bitstep.mango.spring.logging.dc.DiagnosticContext;
import ie.bitstep.mango.spring.logging.web.WebMDCField;
import ie.bitstep.mango.spring.logging.web.populators.WebContextPrePopulator;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.CORRELATION_ID;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.HEADER_REQUEST_ID;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.OPERATION;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.PCF_HEADER_REQUEST_ID;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.REQUEST_ID_ATTRIBUTE;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.REQUEST_START_TIME_ATTRIBUTE;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.TYPE;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.TYPE_BUSINESS;

@Component
@Order(value = 0)
public class DefaultLoggingPrePopulator implements WebContextPrePopulator {

	private final Clock clock = Clock.systemUTC();
	protected Optional<Set<String>> toPropagate = Optional.empty();

	@Override
	public void populate(HttpServletRequest request) {
		LogContext.put(DiagnosticContext.LOG_KEY_URL, request.getRequestURI());

		removeFieldsFromMDC(WebMDCField.CORRELATION_ID.getFieldName());

		setRequestId(request);
		setStartTime(request);

		MDC.put(WebMDCField.TYPE.getFieldName(), TYPE_BUSINESS);

		propagate(CORRELATION_ID).propagate(TYPE).propagate(OPERATION);
		LogUtils.propagateToMDC(request, toPropagate);
	}

	protected void removeFieldsFromMDC(String... fields) {
		Set<String> excludes = new HashSet<>();
		CollectionUtils.addAll(excludes, fields);

		for (String field : WebMDCField.getFieldNames()) {
			if (!excludes.contains(field)) {
				MDC.remove(field);
			}
		}
	}

	protected void setRequestId(HttpServletRequest request) {
		String requestId = StringUtils.firstNonEmpty(
			request.getHeader(HEADER_REQUEST_ID),
			request.getHeader(PCF_HEADER_REQUEST_ID),
			UUID.randomUUID().toString());

		request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
		MDC.put(REQUEST_ID_ATTRIBUTE, requestId);
		LogContext.put(REQUEST_ID_ATTRIBUTE, requestId);
	}

	protected void setStartTime(HttpServletRequest request) {
		Long millis = clock.millis();
		request.setAttribute(REQUEST_START_TIME_ATTRIBUTE, millis);
		MDC.put(REQUEST_START_TIME_ATTRIBUTE, millis.toString());
	}

	public DefaultLoggingPrePopulator propagate(String key) {
		if (toPropagate.isEmpty()) {
			toPropagate = Optional.of(new HashSet<String>());
		}

		toPropagate.get().add(key);

		return this;
	}
}
