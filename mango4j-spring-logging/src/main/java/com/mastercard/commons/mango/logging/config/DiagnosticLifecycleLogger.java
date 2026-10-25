package com.mastercard.commons.mango.logging.config;

import org.slf4j.Logger;

/**
 * Utility class for conditional diagnostic lifecycle logging.
 * Wraps SLF4J logging calls and only executes them if lifecycle logging is enabled.
 */
public final class DiagnosticLifecycleLogger {

	private static boolean enabled = false;
	private static boolean threadlocalEnabled = false;
	private static int maxChildren = 100; // Default maximum children per DiagnosticContextItem

	private DiagnosticLifecycleLogger() {
		// Utility class - prevent instantiation
	}

	/**
	 * Set whether lifecycle logging is enabled.
	 *
	 * @param enabled true to enable lifecycle logging, false to disable
	 */
	public static void setEnabled(boolean enabled) {
		DiagnosticLifecycleLogger.enabled = enabled;
	}

	/**
	 * Check if lifecycle logging is enabled.
	 *
	 * @return true if lifecycle logging is enabled
	 */
	public static boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set whether threadlocal logging is enabled.
	 *
	 * @param enabled true to enable threadlocal logging, false to disable
	 */
	public static void setThreadlocalEnabled(boolean enabled) {
		DiagnosticLifecycleLogger.threadlocalEnabled = enabled;
	}

	/**
	 * Check if threadlocal logging is enabled.
	 *
	 * @return true if threadlocal logging is enabled
	 */
	public static boolean isThreadlocalEnabled() {
		return threadlocalEnabled;
	}

	/**
	 * Set the maximum number of children allowed per DiagnosticContextItem.
	 *
	 * @param max maximum number of children (must be positive)
	 */
	public static void setMaxChildren(int max) {
		if (max <= 0) {
			throw new IllegalArgumentException("maxChildren must be positive, got: " + max);
		}
		DiagnosticLifecycleLogger.maxChildren = max;
	}

	/**
	 * Get the maximum number of children allowed per DiagnosticContextItem.
	 *
	 * @return maximum number of children
	 */
	public static int getMaxChildren() {
		return maxChildren;
	}

	/**
	 * Log at INFO level if lifecycle logging is enabled.
	 *
	 * @param logger  the SLF4J logger to use
	 * @param message the message to log
	 */
	public static void logInfo(Logger logger, String message) {
		if (enabled) {
			logger.info(message);
		}
	}

	/**
	 * Log at INFO level with one argument if lifecycle logging is enabled.
	 *
	 * @param logger  the SLF4J logger to use
	 * @param message the message to log
	 * @param arg     the argument
	 */
	public static void logInfo(Logger logger, String message, Object arg) {
		if (enabled) {
			logger.info(message, arg);
		}
	}

	/**
	 * Log at INFO level with two arguments if lifecycle logging is enabled.
	 *
	 * @param logger  the SLF4J logger to use
	 * @param message the message to log
	 * @param arg1    the first argument
	 * @param arg2    the second argument
	 */
	public static void logInfo(Logger logger, String message, Object arg1, Object arg2) {
		if (enabled) {
			logger.info(message, arg1, arg2);
		}
	}

	/**
	 * Log at INFO level with three arguments if lifecycle logging is enabled.
	 *
	 * @param logger  the SLF4J logger to use
	 * @param message the message to log
	 * @param arg1    the first argument
	 * @param arg2    the second argument
	 * @param arg3    the third argument
	 */
	public static void logInfo(Logger logger, String message, Object arg1, Object arg2, Object arg3) {
		if (enabled) {
			logger.info(message, arg1, arg2, arg3);
		}
	}

	/**
	 * Log at INFO level with four arguments if lifecycle logging is enabled.
	 *
	 * @param logger  the SLF4J logger to use
	 * @param message the message to log
	 * @param arg1    the first argument
	 * @param arg2    the second argument
	 * @param arg3    the third argument
	 * @param arg4    the fourth argument
	 */
	public static void logInfo(Logger logger, String message, Object arg1, Object arg2, Object arg3, Object arg4) {
		if (enabled) {
			logger.info(message, arg1, arg2, arg3, arg4);
		}
	}

	/**
	 * Log at INFO level with five arguments if lifecycle logging is enabled.
	 *
	 * @param logger  the SLF4J logger to use
	 * @param message the message to log
	 * @param arg1    the first argument
	 * @param arg2    the second argument
	 * @param arg3    the third argument
	 * @param arg4    the fourth argument
	 * @param arg5    the fifth argument
	 */
	public static void logInfo(Logger logger, String message, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
		if (enabled) {
			logger.info(message, arg1, arg2, arg3, arg4, arg5);
		}
	}

	// Custom methods for DiagnosticContext lifecycle logging

	/**
	 * Format object identity for logging.
	 * Returns "@hashcode" if object is not null, "null" otherwise.
	 */
	private static String formatIdentity(Object obj) {
		return obj != null ? "@" + System.identityHashCode(obj) : "null";
	}

	/**
	 * Log DiagnosticContext creation.
	 */
	public static void logContextCreated(Logger logger, Object context) {
		if (enabled) {
			logger.info("DiagnosticContext CREATED: {}", formatIdentity(context));
		}
	}

	/**
	 * Log DiagnosticContext clear start.
	 */
	public static void logContextClearStart(Logger logger, Object context, Object rootItem, Object currentItem) {
		if (enabled) {
			logger.info("DiagnosticContext CLEAR (start): {} | Root: {} | Current: {}",
				formatIdentity(context), formatIdentity(rootItem), formatIdentity(currentItem));
		}
	}

	/**
	 * Log DiagnosticContext clearing root item tree.
	 */
	public static void logContextClearingRootItem(Logger logger, Object rootItem) {
		if (enabled) {
			logger.info("DiagnosticContext clearing rootItem tree starting from: {}", formatIdentity(rootItem));
		}
	}

	/**
	 * Log DiagnosticContext clearing detached current item.
	 */
	public static void logContextClearingDetachedItem(Logger logger, Object currentItem) {
		if (enabled) {
			logger.info("DiagnosticContext clearing detached currentItem: {}", formatIdentity(currentItem));
		}
	}

	/**
	 * Log DiagnosticContext clear complete.
	 */
	public static void logContextClearComplete(Logger logger, Object context) {
		if (enabled) {
			logger.info("DiagnosticContext CLEAR (complete): {}", formatIdentity(context));
		}
	}

	/**
	 * Log DiagnosticContext push with method info.
	 */
	public static void logContextPushMethodInfo(Logger logger, Object context, Object methodInfo, Object currentItem, Object isFinalValue) {
		if (enabled) {
			logger.info("DiagnosticContext PUSH (MethodInfo): {} | Method: {} | Current: {} | isFinal: {}",
				formatIdentity(context), methodInfo, formatIdentity(currentItem), isFinalValue);
		}
	}

	/**
	 * Log DiagnosticContext push reusing existing item.
	 */
	public static void logContextPushReusingItem(Logger logger, Object currentItem) {
		if (enabled) {
			logger.info("DiagnosticContext PUSH reusing existing item: {}", formatIdentity(currentItem));
		}
	}

	/**
	 * Log DiagnosticContext push creating new item.
	 */
	public static void logContextPushCreatingNewItem(Logger logger, Object parent) {
		if (enabled) {
			logger.info("DiagnosticContext PUSH creating new item with parent: {}", formatIdentity(parent));
		}
	}

	/**
	 * Log DiagnosticContext push with item details.
	 */
	public static void logContextPushItem(Logger logger, Object context, Object item, Object rootItem, Object currentItem) {
		if (enabled) {
			logger.info("DiagnosticContext PUSH (Item): {} | Item: {} | Root: {} | CurrentBefore: {}",
				formatIdentity(context), formatIdentity(item), formatIdentity(rootItem), formatIdentity(currentItem));
		}
	}

	/**
	 * Log DiagnosticContext set root.
	 */
	public static void logContextSetRoot(Logger logger, Object context, Object root) {
		if (enabled) {
			logger.info("DiagnosticContext SET ROOT: {} | Root: {}", formatIdentity(context), formatIdentity(root));
		}
	}

	/**
	 * Log DiagnosticContext add child.
	 */
	public static void logContextAddChild(Logger logger, Object currentItem, Object child) {
		if (enabled) {
			logger.info("DiagnosticContext ADD CHILD: {} adding {} as child", formatIdentity(currentItem), formatIdentity(child));
		}
	}

	/**
	 * Log DiagnosticContext set current.
	 */
	public static void logContextSetCurrent(Logger logger, Object context, Object current) {
		if (enabled) {
			logger.info("DiagnosticContext SET CURRENT: {} | Current: {}", formatIdentity(context), formatIdentity(current));
		}
	}

	/**
	 * Log DiagnosticContext up before.
	 */
	public static void logContextUpBefore(Logger logger, Object context, Object currentItem, Object parent) {
		if (enabled) {
			logger.info("DiagnosticContext UP (before): {} | Current: {} | Parent: {}",
				formatIdentity(context), formatIdentity(currentItem), formatIdentity(parent));
		}
	}

	/**
	 * Log DiagnosticContext up after.
	 */
	public static void logContextUpAfter(Logger logger, Object context, Object leftItem, Object currentItem) {
		if (enabled) {
			logger.info("DiagnosticContext UP (after): {} | Left: {} | Current: {}",
				formatIdentity(context), formatIdentity(leftItem), formatIdentity(currentItem));
		}
	}

	/**
	 * Log DiagnosticContext up reached root.
	 */
	public static void logContextUpReachedRoot(Logger logger, Object context) {
		if (enabled) {
			logger.info("DiagnosticContext UP reached root, calling clear(): {}", formatIdentity(context));
		}
	}

	/**
	 * Log at ERROR level - always logs regardless of enabled flag.
	 * Used for critical errors like exceeding max children limit.
	 *
	 * @param logger  the SLF4J logger to use
	 * @param message the message to log
	 * @param args    the arguments
	 */
	public static void logError(Logger logger, String message, Object... args) {
		logger.error(message, args);
	}

	/**
	 * Log error when max children limit is exceeded.
	 *
	 * @param logger      the SLF4J logger to use
	 * @param item        the diagnostic context item
	 * @param currentSize current number of children
	 * @param maxChildren maximum allowed children
	 * @param parent      the parent item (may be null)
	 * @param methodInfo  the method info (may be null)
	 */
	public static void logMaxChildrenExceeded(Logger logger, Object item, int currentSize, int maxChildren,
											  Object parent, Object methodInfo) {
		String parentStr = parent != null ? formatIdentity(parent) : "null";
		String methodStr = methodInfo != null ? methodInfo.toString() : "null";

		logger.error("DiagnosticContextItem max children limit exceeded: {} | Current: {} | Max: {} | Parent: {} | Method: {}",
			formatIdentity(item), currentSize, maxChildren, parentStr, methodStr);
	}

}

