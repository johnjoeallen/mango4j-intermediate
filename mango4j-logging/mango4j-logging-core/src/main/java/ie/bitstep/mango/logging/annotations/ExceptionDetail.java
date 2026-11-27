package ie.bitstep.mango.logging.annotations;

/**
 * What to include when an exception is logged for a method. Used by {@link OnException#detail()}
 * and {@link LoggableMethod#logException()}.
 * <ul>
 *   <li>{@link #REASON} switches the text {@code reason} on; {@link #CLASS_NAME}, {@link #MESSAGE} and
 *   {@link #CAUSE} shape what that text contains (with none of them, the message is used).</li>
 *   <li>{@link #EXCEPTION} logs the exception's {@code @LoggableField}s under {@code exception}
 *   (only for exception classes annotated {@code @LoggableClass}). Independent of {@code reason}.</li>
 *   <li>{@link #STACK_TRACE} logs the stack trace.</li>
 * </ul>
 */
public enum ExceptionDetail {
	REASON,
	CLASS_NAME,
	MESSAGE,
	CAUSE,
	EXCEPTION,
	STACK_TRACE
}
