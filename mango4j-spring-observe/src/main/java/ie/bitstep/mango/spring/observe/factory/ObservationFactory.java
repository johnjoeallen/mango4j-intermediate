package ie.bitstep.mango.spring.observe.factory;

import ie.bitstep.mango.spring.aop.MethodInfo;
import ie.bitstep.mango.spring.observe.annotations.Observe;
import ie.bitstep.mango.spring.observe.context.Observation;
import ie.bitstep.mango.spring.observe.initialisers.ObservationInitializer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;


@RequiredArgsConstructor
@Component
@Getter
@Slf4j
public class ObservationFactory {

	private final List<ObservationInitializer> initializers;

	public Observation create(MethodInfo methodInfo) {
		Observe observe = methodInfo.getDeclaredAnnotation(Observe.class);
		Observation observation = Observation.builder()
			.methodInfo(methodInfo)
			.name(observe.name())
			.build();

		if (initializers != null) {
			initializers.forEach(initializer -> {
					try {
						initializer.initialise(observation);
					} catch (Exception e) {
						log.error("ObservationInitializer->" + initializer.getClass().getSimpleName(), e);
					}
				}
			);
		}

		return observation;
	}

}

