package ie.bitstep.mango.spring.insights.internal;

import ie.bitstep.mango.spring.insights.annotations.Insight;
import ie.bitstep.mango.spring.insights.consts.InsightType;
import ie.bitstep.mango.spring.insights.initialiser.DefaultInsightsContextInitialiser;
import ie.bitstep.mango.spring.insights.publisher.InsightsPublisherLogFile;
import ie.bitstep.mango.spring.logging.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightsThreadContextTest {
	private final String key = "key";
	private final String value = UUID.randomUUID().toString();
	private final InsightsProvider insightsProvider = new InsightsProvider(Collections.singletonList(new DefaultInsightsContextInitialiser()), Collections.singletonList(new InsightsPublisherLogFile()), null);

	@BeforeEach
	public void setup() {
		MDCTestUtils.init();
		InsightsThreadContext.unset();
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void privateConstructorTest() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		Constructor<InsightsThreadContext> constructor = InsightsThreadContext.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		InsightsThreadContext esm = constructor.newInstance();
		assertThat(esm).isNotNull(); // best we can do
	}

	@Test
	void get() {
		InsightsContext insightsContext = InsightsThreadContext.get();

		assertThat(insightsContext).isNotNull();
	}

	@Test
	@Insight(type = InsightType.API, action = "unset")
	public void unset() throws NoSuchMethodException { // NOSONAR - required public as used within test
		Method method = getClass().getMethod("unset");
		AppInsight appInsight1 = InsightsThreadContext.get().push(getClass(), method, insightsProvider);
		appInsight1.setContextValue(key, value).setContextValue(key, value);
		appInsight1.setTag(key, value).setContextValue(key, value);

		InsightsThreadContext.unset();
		assertTrue(InsightsThreadContext.get().isStackEmpty());
	}

	@Test
	@Insight(type = InsightType.API, action = "reset")
	public void reset() throws NoSuchMethodException { // NOSONAR - required public as used within test
		Method method = getClass().getMethod("reset");
		AppInsight appInsight1 = InsightsThreadContext.get().push(getClass(), method, insightsProvider);
		appInsight1.setContextValue(key, value);

		assertThat(InsightsThreadContext.reset()).isNotNull();
		assertTrue(InsightsThreadContext.get().isStackEmpty());
	}
}