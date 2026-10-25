package com.mastercard.commons.mango.insights.internal;

import com.mastercard.commons.mango.insights.annotations.Insight;
import com.mastercard.commons.mango.insights.consts.InsightType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightsContextTest {

	@Mock
	private InsightsProvider mockInsightsProvider;

	private final InsightsContext insightsContext = new InsightsContext();

	private final AppInsight testAppInsight0 = new AppInsight(InsightType.API, "0");
	private final AppInsight testAppInsight1 = new AppInsight(InsightType.AUDIT, "1");

	@Test
	@Insight(type = InsightType.API, action = "pushMethodAction")
	public void pushMethod() throws NoSuchMethodException { // NOSONAR - required public as used within test
		Method pushMethod = getClass().getMethod("pushMethod");
		AppInsight appInsight = new AppInsight();
		when(mockInsightsProvider.newInsight(pushMethod)).thenReturn(appInsight);

		// when
		insightsContext.push(getClass(), pushMethod, mockInsightsProvider);

		// then
		AppInsight actual = insightsContext.getStack().getFirst();
		assertThat(actual).isEqualTo(appInsight);
	}

	@Test
	void push() {
		// when
		insightsContext.push(testAppInsight0);

		// then
		assertThat(insightsContext.getStack()).contains(testAppInsight0);
		assertThat(insightsContext.getInsights()).contains(testAppInsight0);
	}

	@Test
	void pushNested() {
		// when
		insightsContext.push(testAppInsight0);
		insightsContext.push(testAppInsight1);

		// then
		assertThat(insightsContext.getStack()).contains(testAppInsight1, testAppInsight0);
		assertThat(insightsContext.getInsights()).contains(testAppInsight0, testAppInsight1);
	}

	@Test
	void pop() {
		// given
		insightsContext.push(testAppInsight0);

		// when
		insightsContext.pop();

		// then
		assertThat(insightsContext.getStack()).isEmpty();
		assertThat(insightsContext.getInsights()).contains(testAppInsight0);
	}

	@Test
	void popNested() {
		// given
		insightsContext.push(testAppInsight0);
		insightsContext.push(testAppInsight1);

		// when
		insightsContext.pop();

		// then
		assertThat(insightsContext.getStack()).contains(testAppInsight0);
		assertThat(insightsContext.getInsights()).contains(testAppInsight0, testAppInsight1);
	}

	@Test
	void popEmpty() {
		// given
		assertThat(insightsContext.getStack()).isEmpty();

		// when
		insightsContext.pop();

		// then - no exceptions
		assertThat(insightsContext.getStack()).isEmpty();
		assertThat(insightsContext.getInsights()).isEmpty();
	}

	@Test
	void current() {
		// given
		insightsContext.push(testAppInsight0);

		// when
		AppInsight actual = insightsContext.current().get();

		// then
		assertThat(actual).isEqualTo(testAppInsight0);
	}

	@Test
	void currentNested() {
		// given
		insightsContext.push(testAppInsight0);
		insightsContext.push(testAppInsight1);

		// when
		AppInsight actual = insightsContext.current().get();

		// then
		assertThat(actual).isEqualTo(testAppInsight1);
	}


	@Test
	void currentEmpty() {
		assertThat(insightsContext.current()).isNotPresent();
	}

	@Test
	void reset() {
		// given
		insightsContext.push(testAppInsight0);

		assertThat(insightsContext.getInsights()).isNotEmpty();
		assertThat(insightsContext.getStack()).isNotEmpty();

		// when
		insightsContext.reset();

		// then
		assertThat(insightsContext.getInsights()).isEmpty();
		assertThat(insightsContext.getStack()).isEmpty();
	}

	@Test
	void publish() {
		// given
		insightsContext.push(testAppInsight0);
		insightsContext.push(testAppInsight1);
		insightsContext.pop();
		insightsContext.pop();

		// when
		insightsContext.publish(mockInsightsProvider);

		// then
		then(mockInsightsProvider).should().publish(testAppInsight0);
		then(mockInsightsProvider).should().publish(testAppInsight1);
	}

	@Test
	void publishNoInsights() {
		// when
		insightsContext.publish(mockInsightsProvider);

		// then
		then(mockInsightsProvider).shouldHaveNoInteractions();
	}
}
