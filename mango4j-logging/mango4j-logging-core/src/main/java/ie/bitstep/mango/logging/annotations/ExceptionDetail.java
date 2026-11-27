package ie.bitstep.mango.logging.annotations;

/**
 * What to include when an exception is logged for a method. Used by {@link OnException#detail()}.
 */
public enum ExceptionDetail {
	CLASS_NAME,
	MESSAGE,
	CAUSE,
	STACK_TRACE
}
