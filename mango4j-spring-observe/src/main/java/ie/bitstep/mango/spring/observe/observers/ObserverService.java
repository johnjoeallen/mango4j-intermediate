package ie.bitstep.mango.spring.observe.observers;

import ie.bitstep.mango.spring.observe.context.Observation;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ObserverService {
	public final Logger logger = LoggerFactory.getLogger(ObserverService.class);
	private final List<Observer> observers;

	public void observeOnExit(Observation observation) {
		observers.forEach(observer -> onExit(observation, observer));
	}

	private void onExit(Observation observation, Observer observer) {
		try {
			observer.onExit(observation);
		} catch (Exception e) {
			logger.error(observation.label(), e);
		}
	}

	public void observeOnEntry(Observation observation) {
		observers.forEach(observer -> onEntry(observation, observer));
	}

	private void onEntry(Observation observation, Observer observer) {
		try {
			observer.onEntry(observation);
		} catch (Exception e) {
			logger.error(observation.label(), e);
		}
	}
}
