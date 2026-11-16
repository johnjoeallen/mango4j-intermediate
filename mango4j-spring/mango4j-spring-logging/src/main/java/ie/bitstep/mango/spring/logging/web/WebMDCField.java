package ie.bitstep.mango.spring.logging.web;

import java.util.Arrays;
import java.util.List;

public enum WebMDCField {
	APPLICATION("application"),
	AUDIT_ID("auditid"),
	CHANNEL("channel"),
	CUSTOMER("customer"),
	TYPE("type"),
	OPERATION("operation"),
	SUB_OPERATION("suboperation"),
	MC_CUSTOMER("mccustomer"),
	MC_PRODUCT("mcproduct"),
	MC_REQUEST_DOMICILE("mcrequestdomicile"),
	CUSTOM("custom"),
	USER_ID("userid"),
	CORRELATION_ID("correlationid"),
	REMOTE_IP("remoteip"),
	PORT("port"),
	SESSION_ID("sessionid"),
	REQUEST_ID("requestid"),
	URL("url"),
	REQUEST_PARAMS("requestparams"),
	HTTP_METHOD("httpmethod"),
	TOTAL_RESPONSE_TIME("responsetime");

	private final String fieldName;

	WebMDCField(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getFieldName() {
		return fieldName;
	}

	public static List<String> getFieldNames() {
		return Arrays.stream(WebMDCField.values()).map(WebMDCField::getFieldName)
			.toList();
	}
}
