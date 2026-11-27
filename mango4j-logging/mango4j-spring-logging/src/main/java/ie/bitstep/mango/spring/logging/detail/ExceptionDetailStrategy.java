package ie.bitstep.mango.spring.logging.detail;

import ie.bitstep.mango.logging.annotations.ExceptionDetail;
import ie.bitstep.mango.spring.aop.MethodInfo;

import java.util.Optional;
import java.util.Set;

/**
 * Global rules for what to log about an exception. Register one as a Spring bean.
 * <p>
 * Precedence: a matching {@code @OnException} on the method, then this strategy,
 * then the method's {@code @LoggableMethod(logException = ...)}.
 */
@FunctionalInterface
public interface ExceptionDetailStrategy {

	/**
	 * @return the detail to log for the exception, or empty to defer to the method's {@code logException}.
	 * An empty set means log nothing for this exception.
	 */
	Optional<Set<ExceptionDetail>> getDetail(Throwable t, MethodInfo methodInfo);
}
