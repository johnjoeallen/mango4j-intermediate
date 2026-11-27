package ie.bitstep.mango.spring.logging.detail;

import ie.bitstep.mango.spring.logging.internal.ExceptionDetailResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Hands the application's {@link ExceptionDetailStrategy} bean (if any) to the resolver,
 * which is used from places that are not Spring beans.
 */
@Component
@RequiredArgsConstructor
public class ExceptionDetailStrategyRegistrar {

	private final ObjectProvider<ExceptionDetailStrategy> strategy;

	@PostConstruct
	void register() {
		ExceptionDetailResolver.setStrategy(strategy.getIfUnique());
	}
}
