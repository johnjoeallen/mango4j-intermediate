package ie.bitstep.mango.spring.insights.initialiser;

import ie.bitstep.mango.spring.insights.consts.InsightType;
import ie.bitstep.mango.spring.insights.internal.AppInsight;
import ie.bitstep.mango.spring.logging.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.CORRELATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultInsightsContextInitialiserTest {
	public static final String UNKNOWN = "*unknown*";
	public InsightsContextInitialiser insightsContextInitialiser = new DefaultInsightsContextInitialiser();

	@BeforeEach
	void setup() {
		MDCTestUtils.init();
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void testCorrelationIdNotInMDC() {
		AppInsight appInsight = new AppInsight(InsightType.API);
		insightsContextInitialiser.initialise(appInsight);
		assertThat(appInsight.getCorrelationId()).isNotEmpty();
	}


	@Test
	void testCorrelationIdInMDC() {
		MDC.put(CORRELATION_ID, UUID.randomUUID().toString());
		AppInsight appInsight = new AppInsight(InsightType.API);
		insightsContextInitialiser.initialise(appInsight);
		assertThat(appInsight.getCorrelationId()).isEqualTo(MDC.get(CORRELATION_ID));
	}
}