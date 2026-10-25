package com.mastercard.commons.mango.observe.web;

import com.mastercard.commons.mango.collections.ListBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebMDCFieldTest {

	@Test
	void getFieldNames() {
		String[] fields = {
			"application",
			"auditid",
			"channel",
			"customer",
			"type",
			"operation",
			"suboperation",
			"mccustomer",
			"mcproduct",
			"mcrequestdomicile",
			"custom",
			"userid",
			"correlationid",
			"remoteip",
			"port",
			"sessionid",
			"requestid",
			"url",
			"requestparams",
			"httpmethod",
			"responsetime"};

		List<String> fieldNames = ListBuilder.<String>list()
			.add(fields)
			.build();

		assertThat(WebMDCField.getFieldNames()).isEqualTo(fieldNames);
	}
}