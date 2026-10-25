package com.mastercard.commons.mango.logging.web.populators;

import jakarta.servlet.http.HttpServletRequest;

public interface WebContextPrePopulator {

	void populate(HttpServletRequest request);

}
