package ie.bitstep.mango.spring.logging.web.populators.base;

import ie.bitstep.mango.spring.logging.LogContext;
import ie.bitstep.mango.spring.logging.web.populators.WebContextPostPopulator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.REQUEST_STATS_ATTRIBUTE;


@Component
@Order(value = 0)
public class DefaultLoggingPostPopulator implements WebContextPostPopulator {

	@Override
	public void populate(HttpServletRequest request, HttpServletResponse response) {
		if (request.getAttribute(REQUEST_STATS_ATTRIBUTE) != null) {
			// the service can use "requestStats" attribute map to add various request stats for logging
			LogContext.putStat(REQUEST_STATS_ATTRIBUTE, request.getAttribute(REQUEST_STATS_ATTRIBUTE));
		}
	}
}
