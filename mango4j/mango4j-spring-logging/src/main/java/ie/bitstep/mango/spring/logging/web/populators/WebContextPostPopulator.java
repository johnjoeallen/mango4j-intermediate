package ie.bitstep.mango.spring.logging.web.populators;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface WebContextPostPopulator {

	void populate(HttpServletRequest request, HttpServletResponse response);

}
