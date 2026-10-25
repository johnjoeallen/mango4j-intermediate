package com.mastercard.commons.mango.logging.web.populators.base;

import com.mastercard.commons.mango.logging.LogContext;
import com.mastercard.commons.mango.logging.web.populators.WebContextPostPopulator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.mastercard.commons.mango.logging.web.populators.base.PopulatorConstants.REQUEST_STATS_ATTRIBUTE;


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
