package ie.bitstep.mango.spring.logging.web.populators.base;

import ie.bitstep.mango.spring.logging.web.WebMDCField;

public class PopulatorConstants {

	private PopulatorConstants() {
		// NOSONAR
	}

	public static final String REQUEST_START_TIME_ATTRIBUTE = "requestStartTime";
	public static final String REQUEST_STATS_ATTRIBUTE = "requestStats";
	public static final String REQUEST_ID_ATTRIBUTE = "requestId";
	public static final String CORRELATION_ID = WebMDCField.CORRELATION_ID.getFieldName();
	public static final String OPERATION = "operation";
	public static final String TYPE = "type";

	public static final String HEADER_REQUEST_ID = "Request-Id";
	public static final String PCF_HEADER_REQUEST_ID = "X-Vcap-Request-Id";
	public static final String TYPE_BUSINESS = "Business";
	public static final String TYPE_API = "API";

}
