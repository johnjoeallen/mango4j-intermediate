package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.logging.LogUtils;
import ie.bitstep.mango.spring.logging.annotations.LoggableClass;
import ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Diagnostic context item related to a single method.
 */
public class DiagnosticContextItem implements ToLoggable {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticContextItem.class);

	public static final String KEY_METHOD = "method";
	public static final String KEY_PARAMS = "params";
	public static final String KEY_CALLED = "called";
	public static final String KEY_RESPONSE = "response";
	public static final String KEY_RESPONSE_STATUS = "responsestatus";
	public static final String KEY_REASON = "reason";
	public static final String KEY_STACK_TRACE = "stacktrace";
	public static final String KEY_RESPONSE_TIME = "responsetime";

	private MethodInfo methodInfo;
	private final Map<String, Object> values = new LinkedHashMap<>();
	private final Map<String, Object> resultValues = new LinkedHashMap<>();
	private Throwable throwable;
	private boolean isFinalMethodInfo = true;
	private boolean isWithExternalExceptionHandler;

	private final WeakReference<DiagnosticContextItem> parent;
	private final List<DiagnosticContextItem> children = new ArrayList<>();

	public DiagnosticContextItem(MethodInfo methodInfo, DiagnosticContextItem parent) {
		this.methodInfo = methodInfo;
		this.parent = parent != null ? new WeakReference<>(parent) : null;

		DiagnosticLifecycleLogger.logInfo(LOGGER, "DiagnosticContextItem CREATED: @{} | Parent: {} | Method: {}",
			System.identityHashCode(this),
			parent != null ? "@" + System.identityHashCode(parent) : "null",
			methodInfo != null ? methodInfo.getSimpleClassName() + "." + methodInfo.getMethodName() : "null");
	}

	public String getName() {
		return methodInfo.getSimpleClassName() + "." + methodInfo.getMethodName();
	}

	public MethodInfo getMethodInfo() {
		return methodInfo;
	}

	public DiagnosticContextItem setMethodInfo(MethodInfo methodInfo) {
		this.methodInfo = methodInfo;
		return this;
	}

	public boolean isFinalMethodInfo() {
		return isFinalMethodInfo;
	}

	public DiagnosticContextItem setFinalMethodInfo(boolean finalMethodInfo) {
		isFinalMethodInfo = finalMethodInfo;
		return this;
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public Object getValue(String key) {
		return values.get(key);
	}

	public boolean containsKey(String key) {
		return values.containsKey(key);
	}

	public DiagnosticContextItem putValue(String key, Object value) {
		// ideally, we need to clone the value to ensure its immutability,
		// but for now we'll just convert the value into the map which is mostly immutable
		values.put(key, LoggerMapper.toLoggable(value));
		return this;
	}

	public DiagnosticContextItem addValue(String key, Object value) {
		@SuppressWarnings("unchecked")
		List<Object> list = (List<Object>) values.computeIfAbsent(key, k -> new ArrayList<>());
		list.add(LoggerMapper.toLoggable(value));
		return this;
	}

	public DiagnosticContextItem mapValue(String key, String name, Object value) {
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) values.computeIfAbsent(key, k -> new LinkedHashMap<>());
		map.put(name, LoggerMapper.toLoggable(value));
		return this;
	}

	public Map<String, Object> getResultValues() {
		return resultValues;
	}

	public Object getResultValue(String key) {
		return resultValues.get(key);
	}

	public DiagnosticContextItem putResultValue(String key, Object value) {
		resultValues.put(key, LoggerMapper.toLoggable(value));
		return this;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	public DiagnosticContextItem putThrowable(Throwable throwable) {
		this.throwable = throwable;
		return this;
	}

	public boolean isWithExternalExceptionHandler() {
		return isWithExternalExceptionHandler;
	}

	public DiagnosticContextItem setWithExternalExceptionHandler(boolean withExternalExceptionHandler) {
		this.isWithExternalExceptionHandler = withExternalExceptionHandler;
		return this;
	}

	public DiagnosticContextItem getParent() {
		return parent != null ? parent.get() : null;
	}

	public boolean hasParent() {
		return parent != null && parent.get() != null;
	}


	public List<DiagnosticContextItem> getChildren() {
		return children;
	}

	public void addChild(DiagnosticContextItem child) {
		int maxChildren = DiagnosticLifecycleLogger.getMaxChildren();
		int currentSize = children.size();

		if (currentSize >= maxChildren) {
			DiagnosticContextItem parentItem = parent != null ? parent.get() : null;
			String methodName = methodInfo != null ? methodInfo.getSimpleClassName() + "." + methodInfo.getMethodName() : null;
			DiagnosticLifecycleLogger.logMaxChildrenExceeded(LOGGER, this, currentSize, maxChildren, parentItem, methodName);
		}

		children.add(child);
	}

	/**
	 * Breaks circular references between parent and children to allow proper garbage collection.
	 * This method recursively clears all children and their data to prevent memory leaks.
	 * Should be called before discarding the diagnostic context tree.
	 */
	public void clearReferences() {
		DiagnosticContextItem parentItem = parent != null ? parent.get() : null;
		DiagnosticLifecycleLogger.logInfo(LOGGER, "DiagnosticContextItem CLEARING: @{} | Parent: {} | Children: {} | Method: {}",
			System.identityHashCode(this),
			parentItem != null ? "@" + System.identityHashCode(parentItem) : "null",
			children.size(),
			methodInfo != null ? methodInfo.getSimpleClassName() + "." + methodInfo.getMethodName() : "null");

		// Clear children recursively (depth-first to avoid stack overflow on wide trees)
		for (DiagnosticContextItem child : children) {
			child.clearReferences();
		}
		children.clear();

		// Clear collections that may hold references
		values.clear();
		resultValues.clear();

		// Clear throwable reference
		throwable = null;

		// Note: parent is a WeakReference and will be automatically garbage collected
		// Clearing children breaks the strong reference chain from parent → child

		DiagnosticLifecycleLogger.logInfo(LOGGER, "DiagnosticContextItem CLEARED: @{}", System.identityHashCode(this));
	}


	@Override
	public Map<String, Object> toLoggable() {
		Map<String, Object> loggable = new LinkedHashMap<>();
		loggable.put(KEY_METHOD, getName());
		loggable.putAll(getValues());    // already converted to loggables
		if (!getChildren().isEmpty()) {
			loggable.put(KEY_CALLED, LoggerMapper.toLoggable(getChildren()));
		}
		if (throwable != null) {
			LoggableClass loggableClass = throwable.getClass().getAnnotation(LoggableClass.class);

			if (loggableClass != null) {
				setThrowableAsReason(loggable);
			} else {
				setMessageAsReason(loggable);
			}

			loggable.put(KEY_STACK_TRACE, LogUtils.getStackTrace(throwable));
		}

		loggable.putAll(getResultValues());        // already converted to loggables

		return loggable;
	}

	private void setThrowableAsReason(Map<String, Object> loggable) {
		Map<String, Object> throwableAsLoggable = new LinkedHashMap<>();
		throwableAsLoggable.put(throwable.getClass().getName(), LoggerMapper.toLoggable(throwable));
		loggable.put(KEY_REASON, throwableAsLoggable);
	}

	private void setMessageAsReason(Map<String, Object> loggable) {
		String message = LogUtils.exception2Message(throwable);
		if (!StringUtils.isEmpty(message)) {
			loggable.put(KEY_REASON, message);
		}
	}
}
