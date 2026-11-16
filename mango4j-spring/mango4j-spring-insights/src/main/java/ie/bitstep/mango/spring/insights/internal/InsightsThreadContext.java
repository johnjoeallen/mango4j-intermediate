package ie.bitstep.mango.spring.insights.internal;

public class InsightsThreadContext {

	private static final ThreadLocal<InsightsContext> INSIGHTS_CONTEXT_THREAD_LOCAL = ThreadLocal.withInitial(InsightsContext::new);

	private InsightsThreadContext() {
		// SONAR
	}

	public static InsightsContext get() {
		return INSIGHTS_CONTEXT_THREAD_LOCAL.get();
	}

	public static void unset() {
		INSIGHTS_CONTEXT_THREAD_LOCAL.remove();
	}

	public static InsightsContext reset() {
		unset();
		return get();
	}
}
