package ie.bitstep.mango.spring.insights;

import ie.bitstep.mango.collections.MapBuilder;
import ie.bitstep.mango.spring.insights.annotations.Insight;
import ie.bitstep.mango.spring.insights.consts.InsightType;
import ie.bitstep.mango.spring.insights.initialiser.DefaultInsightsContextInitialiser;
import ie.bitstep.mango.spring.insights.internal.AppInsight;
import ie.bitstep.mango.spring.insights.internal.Insights;
import ie.bitstep.mango.spring.insights.internal.InsightsProvider;
import ie.bitstep.mango.spring.insights.internal.InsightsThreadContext;
import ie.bitstep.mango.spring.insights.publisher.InsightsPublisherLogFile;
import ie.bitstep.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InsightsTest {

	private static final String KEY = "key";
	private static final String KEY2 = "key2";
	private static final String VALUE = "value";
	private static final String VALUE2 = "value2";
	private final InsightsProvider insightsProvider = new InsightsProvider(Collections.singletonList(new DefaultInsightsContextInitialiser()), Collections.singletonList(new InsightsPublisherLogFile()), null);

	@BeforeEach
	void setUp() {
		MDCTestUtils.init();
		InsightsThreadContext.reset();
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void current_whenAvailable() {
		// given
		pushInsight();

		// when
		Optional<AppInsight> actual = Insights.current();

		// then
		assertThat(actual).isPresent();
	}

	@Test
	void current_whenNotAvailable() {
		// when
		Optional<AppInsight> actual = Insights.current();

		// then
		assertThat(actual).isNotPresent();
	}

	@Test
	void setContextValue_whenCurrentAvailable() {
		// given
		pushInsight();

		// when
		Insights.setContextValue(KEY, VALUE);

		// then
		assertThat(InsightsThreadContext.get().getStack().getFirst().getContext()).containsEntry(KEY, VALUE);
	}

	@Test
	void setTag_whenCurrentAvailable() {
		// given
		pushInsight();

		// when
		Insights.setTag(KEY, VALUE);

		// then
		assertThat(InsightsThreadContext.get().getStack().getFirst().getTags()).containsEntry(KEY, VALUE);
	}

	@Test
	void setContextValue_whenCurrentNotAvailable() {
		assertDoesNotThrow(() -> Insights.setContextValue(KEY, VALUE));
	}

	@Test
	void setContextValues() {
		// given
		pushInsight();

		// when
		Insights.setContextValues(MapBuilder.<String, Object>map().with(KEY, VALUE).with(KEY2, VALUE2).build());

		// then
		assertThat(InsightsThreadContext.get().getStack().getFirst().getContext()).containsEntry(KEY, VALUE);
		assertThat(InsightsThreadContext.get().getStack().getFirst().getContext()).containsEntry(KEY2, VALUE2);
	}


	@Insight(type = InsightType.API, action = "")
	public void pushInsight() {
		try {
			InsightsThreadContext.get().push(getClass(), getClass().getMethod("pushInsight"), insightsProvider);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException();
		}
	}

	@Test
	void getContextValue() {
		// given
		pushInsight();

		// when
		Insights.setContextValue(KEY, VALUE);

		// then
		assertThat(Insights.getContextValue(KEY)).isEqualTo(VALUE);
	}

	@Test
	void setTag() {
		assertDoesNotThrow(() -> Insights.setTag(KEY, VALUE));
	}
}