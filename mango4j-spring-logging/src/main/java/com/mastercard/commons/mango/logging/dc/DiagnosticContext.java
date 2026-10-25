package com.mastercard.commons.mango.logging.dc;

import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.aop.ThrowableInfo;
import com.mastercard.commons.mango.logging.adapter.AbstractLoggerAdapter;
import com.mastercard.commons.mango.logging.config.DiagnosticLifecycleLogger;
import com.mastercard.commons.mango.logging.internal.DiagnosticContextItem;
import com.mastercard.commons.mango.logging.internal.LoggerMapper;
import com.mastercard.commons.mango.logging.internal.ToLoggable;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mastercard.commons.mango.logging.web.populators.base.PopulatorConstants.CORRELATION_ID;
import static com.mastercard.commons.mango.logging.web.populators.base.PopulatorConstants.REQUEST_ID_ATTRIBUTE;

/**
 * Diagnostic Context.
 */
public class DiagnosticContext implements ToLoggable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticContext.class);

	public static final String LOG_KEY_CORRELATION_ID = CORRELATION_ID;
	public static final String LOG_KEY_ID = "logId";
	public static final String LOG_KEY_URL = "url";
	public static final String KEY_REQUESTOR_ID = "requestorid";

	public static final String LOG_KEY_TIMESTAMP = "ts";

	static Clock clock = Clock.systemUTC();

	private static List<String> topLevelKeys = new ArrayList<>(Arrays.asList(LOG_KEY_CORRELATION_ID, LOG_KEY_URL, KEY_REQUESTOR_ID, REQUEST_ID_ATTRIBUTE));
	private static final List<String> TOP_LEVEL_KEYS_TO_REMOVE_FROM_MDC = new ArrayList<>(Arrays.asList(LOG_KEY_URL, KEY_REQUESTOR_ID));

	private final Map<String, Object> topValues = new LinkedHashMap<>();
	private final Map<String, Object> statValues = new LinkedHashMap<>();

	private DiagnosticContextItem rootItem;
	private DiagnosticContextItem currentItem;

	private final List<ThrowableInfo> thrown = new ArrayList<>();

	public DiagnosticContext() {
		TOP_LEVEL_KEYS_TO_REMOVE_FROM_MDC.forEach(MDC::remove);
		// NOTE don't clear the whole MDC as other libraries use it as well (eg Synapse or Sleuth)
		DiagnosticLifecycleLogger.logContextCreated(LOGGER, this);
	}

	void clear() {
		DiagnosticLifecycleLogger.logContextClearStart(LOGGER, this, rootItem, currentItem);

		// Break circular references in the diagnostic context item tree to prevent memory leaks
		if (rootItem != null) {
			DiagnosticLifecycleLogger.logContextClearingRootItem(LOGGER, rootItem);
			rootItem.clearReferences();
		}

		// Defensive: Also clear currentItem if it's not null and not already cleared by rootItem
		// This handles edge cases where currentItem might be detached or in an inconsistent state
		if (currentItem != null && currentItem != rootItem) {
			DiagnosticLifecycleLogger.logContextClearingDetachedItem(LOGGER, currentItem);
			currentItem.clearReferences();
		}

		rootItem = null;
		currentItem = null;
		topValues.clear();
		statValues.clear();
		thrown.clear();

		// Clear MDC entries
		TOP_LEVEL_KEYS_TO_REMOVE_FROM_MDC.forEach(MDC::remove);

		// NOTE: DiagnosticContextThreadLocal.unset() should be called by the caller (interceptor/aspect)
		// after up() completes, to ensure proper cleanup in thread pools

		DiagnosticLifecycleLogger.logContextClearComplete(LOGGER, this);
	}

	public static List<String> getTopLevelKeys() {
		return topLevelKeys;
	}

	public static void setTopLevelKeys(List<String> topLevelKeys) {
		DiagnosticContext.topLevelKeys = topLevelKeys;
	}

	public DiagnosticContext copy() {
		return new DiagnosticContext().copy(this);
	}

	public DiagnosticContext copy(DiagnosticContext diagnosticContext) {
		topValues.putAll(diagnosticContext.topValues);
		statValues.putAll(diagnosticContext.statValues);

		return this;
	}

	public DiagnosticContext getRootContext() {
		DiagnosticContext newContext = new DiagnosticContext();
		DiagnosticContext currentDiagnosticContext = DiagnosticContextThreadLocal.get();

		return newContext.setCurrentItem(currentDiagnosticContext.getRootItem());
	}


	public DiagnosticContextItem getRootItem() {
		return rootItem;
	}

	public DiagnosticContextItem getCurrentItem() {
		return currentItem;
	}

	protected DiagnosticContext setCurrentItem(DiagnosticContextItem dci) {
		currentItem = dci;
		return this;
	}

	public List<ThrowableInfo> getThrown() {
		return thrown;
	}

	public Map<String, Object> getStatValues() {
		return statValues;
	}

	public Object getStatValue(String key) {
		return statValues.get(key);
	}

	public DiagnosticContextItem push(MethodInfo mi) {
		DiagnosticLifecycleLogger.logContextPushMethodInfo(LOGGER,
			this,
			mi != null ? mi.getSimpleClassName() + "." + mi.getMethodName() : "null",
			currentItem,
			currentItem != null ? currentItem.isFinalMethodInfo() : "N/A");

		if (currentItem != null && !currentItem.isFinalMethodInfo()) {
			DiagnosticLifecycleLogger.logContextPushReusingItem(LOGGER, currentItem);
			currentItem.setMethodInfo(mi)
				.setFinalMethodInfo(true);
			return currentItem;
		} else {
			DiagnosticLifecycleLogger.logContextPushCreatingNewItem(LOGGER, currentItem);
			return push(new DiagnosticContextItem(mi, currentItem));
		}
	}

	private DiagnosticContextItem push(DiagnosticContextItem dci) {
		DiagnosticLifecycleLogger.logContextPushItem(LOGGER, this, dci, rootItem, currentItem);

		if (rootItem == null) {
			rootItem = dci;
			DiagnosticLifecycleLogger.logContextSetRoot(LOGGER, this, rootItem);
		} else {
			DiagnosticLifecycleLogger.logContextAddChild(LOGGER, currentItem, dci);
			currentItem.addChild(dci);
		}
		currentItem = dci;
		DiagnosticLifecycleLogger.logContextSetCurrent(LOGGER, this, currentItem);
		return dci;
	}

	public void up() {
		DiagnosticLifecycleLogger.logContextUpBefore(LOGGER,
			this,
			currentItem,
			currentItem != null ? currentItem.getParent() : null);

		if (currentItem != null) {
			DiagnosticContextItem itemToLeave = currentItem;
			currentItem = currentItem.getParent();

			DiagnosticLifecycleLogger.logContextUpAfter(LOGGER, this, itemToLeave, currentItem);

			if (currentItem == null) {
				DiagnosticLifecycleLogger.logContextUpReachedRoot(LOGGER, this);
				clear();
			}
		}
	}

	public void put(String key, Object value) {
		putValue(key, value);
	}

	public Object get(String key) {
		return getValue(key);
	}

	public boolean contains(String key) {
		return containsKey(key);
	}

	public Object getStat(String key) {
		return getStatValue(key);
	}

	public void add(String key, Object... value) {
		addValues(key, value, getCurrentItem());
	}

	public void addMap(String key, Object... value) {
		addMap(key, value, getCurrentItem());
	}

	public void putValue(String key, Object value) {
		if (topLevelKeys.contains(key)) {
			topValues.put(key, value);
			MDC.put(key, String.valueOf(value));
		} else {
			DiagnosticContextItem dci = getCurrentItem();
			if (dci != null) {
				dci.putValue(key, value);
			}
		}
	}

	public Object getValue(String key) {
		if (topLevelKeys.contains(key)) {
			return topValues.get(key);
		} else {
			DiagnosticContextItem dci = getCurrentItem();
			return dci != null ? dci.getValue(key) : null;
		}
	}

	public boolean containsKey(String key) {
		if (topLevelKeys.contains(key)) {
			return true;
		} else {
			DiagnosticContextItem dci = getCurrentItem();
			return dci != null && dci.containsKey(key);
		}
	}

	public void putStatValue(String key, Object value) {
		statValues.put(key, value);
	}

	public void addValue(String key, Object value) {
		DiagnosticContextItem dci = getCurrentItem();
		if (dci != null) {
			dci.addValue(key, value);
		}
	}

	public void addValues(String key, Object... values) {
		DiagnosticContextItem dci = getCurrentItem();
		addValues(key, values, dci);
	}

	private static void addValues(String key, Object[] values, DiagnosticContextItem dci) {
		if (dci != null) {
			for (Object v : values) {
				dci.addValue(key, v);
			}
		}
	}

	private static void addMap(String key, Object[] values, DiagnosticContextItem dci) {
		if (dci != null) {
			int i = 0;
			Map<String, Object> map = new LinkedHashMap<>();

			while (i < values.length) {
				map.put((String) values[i++], values[i++]);
			}

			dci.addValue(key, map);
		}
	}

	public void mapValue(String key, String name, Object value) {
		DiagnosticContextItem dci = getCurrentItem();
		mapValue(key, name, value, dci);
	}

	private static void mapValue(String key, String name, Object value, DiagnosticContextItem dci) {
		if (dci != null) {
			dci.mapValue(key, name, value);
		}
	}

	public void map(String key, Object... values) {
		DiagnosticContextItem dci = getCurrentItem();
		mapValues(key, values, dci);
	}

	private static void mapValues(String key, Object[] values, DiagnosticContextItem dci) {
		if (dci != null) {
			int i = 0;

			while (i < values.length) {
				dci.mapValue(key, (String) values[i++], values[i++]);
			}
		}
	}

	public void putThrowable(Throwable t) {
		DiagnosticContextItem dci = getCurrentItem();
		if (dci != null && dci.getThrowable() != t) {    // ignore if the very same exception is put again (might happen in exception handler)
			dci.putThrowable(t);
			thrown.add(new ThrowableInfo(t, dci.getMethodInfo()));
		}
	}

	public Throwable getThrowable() {
		return getCurrentItem().getThrowable();
	}

	public void putPreviousCallThrowable(Throwable t) {
		DiagnosticContextItem dci = getCurrentItem();
		if (dci != null) {
			addThrowable(t, this, dci);
		}
	}

	private static void addThrowable(Throwable t, DiagnosticContext dc, DiagnosticContextItem dci) {
		if (!CollectionUtils.isEmpty(dci.getChildren())) {
			DiagnosticContextItem last = dci.getChildren().get(dci.getChildren().size() - 1);
			last.putThrowable(t);
			dc.thrown.add(new ThrowableInfo(t, last.getMethodInfo()));
		}
	}

	@Override
	public Map<String, Object> toLoggable() {
		Map<String, Object> loggable = new LinkedHashMap<>();
		loggable.put(LOG_KEY_TIMESTAMP, clock.instant().toString());

		loggable.putAll(LoggerMapper.toLoggable(topValues));

		if (getRootItem() != null) {
			loggable.putAll(getRootItem().toLoggable());
		}

		loggable.putAll(LoggerMapper.toLoggable(statValues));

		return AbstractLoggerAdapter.loggableToMap(loggable);
	}
}
