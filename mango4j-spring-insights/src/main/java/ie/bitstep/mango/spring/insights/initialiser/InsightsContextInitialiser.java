package ie.bitstep.mango.spring.insights.initialiser;

import ie.bitstep.mango.spring.insights.internal.AppInsight;

public interface InsightsContextInitialiser {
	void initialise(AppInsight insight);
}
