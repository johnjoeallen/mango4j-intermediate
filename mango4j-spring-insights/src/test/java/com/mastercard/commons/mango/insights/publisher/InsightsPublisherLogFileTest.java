package com.mastercard.commons.mango.insights.publisher;

import com.mastercard.commons.mango.insights.consts.InsightType;
import com.mastercard.commons.mango.insights.internal.AppInsight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class InsightsPublisherLogFileTest {

	private final InsightsPublisherLogFile insightsPublisher = new InsightsPublisherLogFile();
	private final AppInsight testInsight = new AppInsight(InsightType.API);

	@BeforeEach
	public void setup() {
		// BasicConfigurator.configure(); // TODO: figure out why this is needed (not needed in enigma-insights)
	}

	@Test
	void publish(CapturedOutput output) {
		// when
		insightsPublisher.publish(testInsight);

		// then
		// assertThat(output.getAll()).contains("API");
	}
}