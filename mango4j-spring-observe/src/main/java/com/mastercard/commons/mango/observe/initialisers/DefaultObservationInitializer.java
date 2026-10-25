package com.mastercard.commons.mango.observe.initialisers;

import com.mastercard.commons.mango.observe.annotations.ObservationTags;
import com.mastercard.commons.mango.observe.context.Observation;
import com.mastercard.commons.mango.observe.context.ObservationContext;
import com.mastercard.commons.mango.observe.context.ObservationContextThreadLocal;
import com.mastercard.commons.mango.observe.web.WebMDCField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Slf4j
@Component
@Order(0)
public class DefaultObservationInitializer implements ObservationInitializer {
	public static final String CORRELATION_ID = WebMDCField.CORRELATION_ID.getFieldName();

	public static final String DEFAULT_SERVICE_ID = "*unknown*";

	@Override
	public void initialise(Observation observation) {
		initCorrelationId();

		// Q: I wonder should this be the APP name that APPF expects to be in the MDC or spring props
		observation.serviceId(DEFAULT_SERVICE_ID);
		observation.correlationid(MDC.get(CORRELATION_ID));

		ObservationTags observationTags = observation.methodInfo().getDeclaredAnnotation(ObservationTags.class);

		if (observationTags != null) {
			observation.setAllowedTags(observationTags);
			observation.setTagPermutations(observationTags);
		}

		// default tags
		observation
			.tag("service.id", observation.serviceId());
	}

	private void initCorrelationId() {
		ObservationContext observationContext = ObservationContextThreadLocal.get();

		if (observationContext.getWebContext() != null) {
			String correlationId = observationContext.getWebContext().getHeader(CORRELATION_ID);

			if (!StringUtils.isEmpty(correlationId)) {
				MDC.put(CORRELATION_ID, correlationId);
			}
		}

		if (StringUtils.isEmpty(MDC.get(CORRELATION_ID))) {
			MDC.put(CORRELATION_ID, UUID.randomUUID().toString());
		}
	}
}
