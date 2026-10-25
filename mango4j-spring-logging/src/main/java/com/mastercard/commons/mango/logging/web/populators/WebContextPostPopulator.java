package com.mastercard.commons.mango.logging.web.populators;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface WebContextPostPopulator {

	void populate(HttpServletRequest request, HttpServletResponse response);

}
