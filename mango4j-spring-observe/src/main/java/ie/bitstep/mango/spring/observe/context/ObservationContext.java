package ie.bitstep.mango.spring.observe.context;

import ie.bitstep.mango.spring.aop.ThrowableInfo;
import ie.bitstep.mango.spring.observe.annotations.Segregate;
import lombok.EqualsAndHashCode;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.CORRELATION_ID;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.REQUEST_ID_ATTRIBUTE;

/**
 * Diagnostic Context.
 */
@EqualsAndHashCode
public class ObservationContext {

	public static final String LOG_KEY_CORRELATION_ID = CORRELATION_ID;
	public static final String KEY_REQUESTOR_ID = "requestorid"; // ???: What exactly is this supposed to be

	public static final String LOG_KEY_TIMESTAMP = "ts";

	private static final List<String> DEFAULT_TOP_LEVEL_KEYS = List.of(LOG_KEY_CORRELATION_ID, KEY_REQUESTOR_ID, REQUEST_ID_ATTRIBUTE);
	private static List<String> topLevelKeys = DEFAULT_TOP_LEVEL_KEYS;
	private static final List<String> TOP_LEVEL_KEYS_TO_REMOVE_FROM_MDC = List.of(KEY_REQUESTOR_ID);

	private final Map<String, Object> topValues = new LinkedHashMap<>();
	private final Map<String, Object> statValues = new LinkedHashMap<>();

	private Observation root; // the root observation
	private Observation observation; // the current observation

	private List<ThrowableInfo> thrown = new ArrayList<>();

	private WebContext webContext;

	public ObservationContext() {
		// NOTE don't clear the entire MDC as other libraries use it as well (eg Synapse or Sleuth)
		TOP_LEVEL_KEYS_TO_REMOVE_FROM_MDC.forEach(MDC::remove);
	}

	public static ObservationContext getInstance() {
		return ObservationContextThreadLocal.get();
	}

	public static Observation getCurrentObservation() {
		return getInstance().getObservation();
	}

	public static WebContext getCurrentWebcontext() {
		return getInstance().getWebContext();
	}

	public WebContext getWebContext() {
		return webContext;
	}

	public void setWebContext(WebContext webContext) {
		this.webContext = webContext;
	}

	void clear() {
		root = null;
		observation = null;
		topValues.clear();
		statValues.clear();
		thrown.clear();
	}

	public static List<String> getTopLevelKeys() {
		return topLevelKeys;
	}

	public static void setTopLevelKeys(List<String> topLevelKeys) {
		ObservationContext.topLevelKeys = topLevelKeys;
	}

	public ObservationContext fresh() {
		return new ObservationContext().copyFrom(this);
	}

	private ObservationContext copyFrom(ObservationContext observationContext) {
		topValues.putAll(observationContext.topValues);
		statValues.putAll(observationContext.statValues);

		return this;
	}

	public Observation getRoot() {
		return root;
	}

	public Observation getObservation() {
		return observation;
	}

	public List<ThrowableInfo> getThrown() {
		return thrown;
	}

	public Map<String, Object> getTopValues() {
		return topValues;
	}

	public Map<String, Object> getStatValues() {
		return statValues;
	}

	public Object getStatValue(String key) {
		return statValues.get(key);
	}

	public Observation push(Observation observation) {
		if (this.root == null) {
			this.root = observation;
		} else {
			this.observation.addChild(observation);
		}

		this.observation = observation;

		return observation;
	}

	public void pop() {
		if (observation != null) {
			Segregate segregate = AnnotationUtils.findAnnotation(observation.methodInfo().getMethod(), Segregate.class);
			if (segregate != null) {
				Observation current = observation;
				observation = current.parent().get();

				if (current.parent().get() != null) {
					current.parent().get().removeChild(current);
					current.parent(null);
				}
			}
			else {
				observation = observation.parent().get();
			}

			if (observation == null) {
				clear();
			}
		}
	}

	public static void put(String key, Object value) {
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		observationContext.putValue(key, value);
	}

	public static Object get(String key) {
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		return observationContext.getValue(key);
	}

	public static boolean contains(String key) {
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		return observationContext.containsKey(key);
	}

	public static void putStat(String key, Object value) {
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		observationContext.putStatValue(key, value);
	}

	public static Object getStat(String key) {
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		return observationContext.getStatValue(key);
	}

	public static void add(String key, Object value) {
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		observationContext.addValue(key, value);
	}

	public static void add(String key, Object... value) {
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		observationContext.addValues(key, value);
	}

	public static void map(String key, Object... values) {
		ObservationContext observationContext = ObservationContextThreadLocal.get();
		observationContext.mapValues(key, values);
	}


	public void putValue(String key, Object value) {
		if (topLevelKeys.contains(key)) {
			topValues.put(key, value);
			MDC.put(key, String.valueOf(value));
		} else {
			if (observation != null) {
				observation.putValue(key, value);
			}
		}
	}

	public Object getValue(String key) {
		if (topLevelKeys.contains(key)) {
			return topValues.get(key);
		} else {
			return observation != null ? observation.getValue(key) : null;
		}
	}

	public boolean containsKey(String key) {
		if (topLevelKeys.contains(key)) {
			return topValues.containsKey(key);
		} else {
			return observation != null && observation.containsKey(key);
		}
	}

	public void putStatValue(String key, Object value) {
		statValues.put(key, value);
	}

	public void addValue(String key, Object value) {
		if (observation != null) {
			observation.addValue(key, value);
		}
	}

	public void addValues(String key, Object... values) {
		if (observation != null) {
			int i = 0;
			Map<String, Object> map = new LinkedHashMap<>();

			while (i < values.length) {
				map.put((String) values[i++], values[i++]);
			}

			observation.addValue(key, map);
		}
	}

	public void mapValue(String key, String name, Object value) {
		observation.mapValue(key, name, value);
	}

	public void mapValues(String key, Object... values) {
		if (observation != null) {
			int i = 0;

			while (i + 1 < values.length) {
				mapValue(key, (String) values[i++], values[i++]);
			}
		}
	}

	public static void putThrowable(Throwable t) {
		Observation observation = getCurrentObservation();
		if (observation != null && observation.getThrowable() != t) {    // ignore if the very same exception is put again (might happen in exception handler)
			observation.putThrowable(t);
			getInstance().thrown.add(new ThrowableInfo(t, observation.methodInfo()));
		}
	}

	public static Throwable getThrowable() {
		return getCurrentObservation() != null ? getCurrentObservation().getThrowable() : null;
	}
}
