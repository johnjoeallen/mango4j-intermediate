package ie.bitstep.mango.spring.logging.web.populators;

import jakarta.servlet.http.HttpServletRequest;

public interface WebContextPrePopulator {

	void populate(HttpServletRequest request);

}
