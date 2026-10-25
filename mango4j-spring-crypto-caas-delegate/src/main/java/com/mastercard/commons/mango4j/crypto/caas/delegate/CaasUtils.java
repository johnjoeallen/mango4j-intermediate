package com.mastercard.commons.mango4j.crypto.caas.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasConstants.REQUEST_ID_HEADER;
import static com.mastercard.commons.mango4j.crypto.caas.delegate.CaasConstants.RESPONSE_TIME_HEADER;

public class CaasUtils {

	private CaasUtils() {
	}

	public static String getRequestId(HttpHeaders httpHeaders) {
		return getHeaderValue(httpHeaders, REQUEST_ID_HEADER);
	}

	public static String getResponseTime(HttpHeaders httpHeaders) {
		return getHeaderValue(httpHeaders, RESPONSE_TIME_HEADER);
	}

	private static String getHeaderValue(HttpHeaders httpHeaders, String header) {
		return Optional.ofNullable(httpHeaders).map(headers -> headers.getFirst(header)).orElse("");
	}

	public static String toJsonString(ObjectMapper objectMapper, Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static String base64Encode(String value) {
		if (StringUtils.isEmpty(value)) {
			return value;
		}
		return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	public static String base64Decode(String value) {
		if (StringUtils.isEmpty(value)) {
			return value;
		}
		return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
	}

}
