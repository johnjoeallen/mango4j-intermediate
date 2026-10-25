package ie.bitstep.mango.spring.insights.test;

import ie.bitstep.mango.spring.insights.annotations.Insight;
import ie.bitstep.mango.spring.insights.consts.InsightType;
import ie.bitstep.mango.spring.insights.internal.Insights;
import ie.bitstep.mango.spring.insights.internal.InsightsThreadContext;
import ie.bitstep.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static ie.bitstep.mango.spring.insights.test.TestUtils.fakeInsight;
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