package com.mastercard.commons.mango.insights.test;

import com.mastercard.commons.mango.insights.annotations.Insight;
import com.mastercard.commons.mango.insights.consts.InsightType;
import com.mastercard.commons.mango.insights.internal.Insights;
import com.mastercard.commons.mango.insights.internal.InsightsThreadContext;
import com.mastercard.commons.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mastercard.commons.mango.insights.test.TestUtils.fakeInsight;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TestUtilsTest {

	@Insight(type = InsightType.API, action = "methodWithoutParams")
	public void methodWithoutParams() {
	}

	@Insight(type = InsightType.API, action = "methodWithParams")
	public void methodWithParams(String s) {
	}

	@BeforeEach
	public void setup() throws Throwable {
		MDCTestUtils.init();
		InsightsThreadContext.unset();
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	@SuppressWarnings("java:S5786")
	public void testFakeInsight() throws NoSuchMethodException {
		fakeInsight(this, "methodWithoutParams");
		assertThat(Insights.current().get().getAction()).isEqualTo("methodWithoutParams");
	}

	@Test
	@SuppressWarnings("java:S5786")
	public void testFakeInsightWithParams() throws NoSuchMethodException {
		fakeInsight(this, "methodWithParams", String.class);
		assertThat(Insights.current().get().getAction()).isEqualTo("methodWithParams");
	}
}