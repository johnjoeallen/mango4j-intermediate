package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.logging.LogProjection;
import ie.bitstep.mango.logging.internal.LogProjectionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Registers every {@link LogProjection} bean with the framework-free {@link LogProjectionRegistry}.
 */
@Component
public class LogProjectionRegistrar {

	@Autowired
	@SuppressWarnings({"unchecked", "rawtypes"})
	public LogProjectionRegistrar(ApplicationContext context) {
		LogProjectionRegistry.register((java.util.Collection) context.getBeansOfType(LogProjection.class).values());
	}
}
