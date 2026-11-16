package ie.bitstep.mango.spring.insights.initialiser;

import ie.bitstep.mango.spring.insights.internal.AppInsight;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.CORRELATION_ID;

@Slf4j
@Component
@Order(0)
public class DefaultInsightsContextInitialiser implements InsightsContextInitialiser {

	public static final String DEFAULT_VALUE = "*unknown*";

	@Override
	public void initialise(AppInsight insight) {
		initCorrelationId();

		insight.setServiceId(DEFAULT_VALUE);
		insight.setCorrelationId(MDC.get(CORRELATION_ID));
	}

	private void initCorrelationId() {
		if (StringUtils.isEmpty(MDC.get(CORRELATION_ID))) {
			MDC.put(CORRELATION_ID, UUID.randomUUID().toString());
		}
	}
}
