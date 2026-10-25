package com.mastercard.commons.mango.logging.sanitiser;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.core.MethodParameter;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MethodArgumentNotValidExceptionSanitiserTest {
	private static final String TEST_OBJECT_NAME = "TEST_OBJECT_NAME";
	private static final String TEST_FIELD = "TEST_FIELD";
	private static final String TEST_REJECTED_VALUE = "TEST_REJECTED_VALUE";
	private static final String TEST_MESSAGE = "TEST_MESSAGE";
	private static final String TEST_MESSAGE_2 = "TEST_MESSAGE_2";
	private static final String[] TEST_CODES = new String[]{"code1", "code2"};
	private static final Object[] TEST_ARGS = new Object[]{"arg1", "arg2"};

	@Mock
	private MethodParameter mockMethodParameter;

	private MethodArgumentNotValidExceptionSanitiser sanitiser = new MethodArgumentNotValidExceptionSanitiser();

	@Test
	void getSanitisedMessage_whenFieldError() {
		MapBindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), TEST_OBJECT_NAME);
		bindingResult.addError(
			new FieldError(
				TEST_OBJECT_NAME, TEST_FIELD, TEST_REJECTED_VALUE, true, TEST_CODES, TEST_ARGS, TEST_MESSAGE));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mockMethodParameter, bindingResult);

		assertThat(sanitiser.getSanitisedMessage(ex)).isEqualTo("Field error in object 'TEST_OBJECT_NAME' on field 'TEST_FIELD': TEST_MESSAGE");
	}

	@Test
	void getSanitisedMessage_whenObjectError() {
		MapBindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), TEST_OBJECT_NAME);
		bindingResult.addError(
			new ObjectError(TEST_OBJECT_NAME, TEST_CODES, TEST_ARGS, TEST_MESSAGE));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mockMethodParameter, bindingResult);

		assertThat(sanitiser.getSanitisedMessage(ex)).isEqualTo("Error in object 'TEST_OBJECT_NAME': TEST_MESSAGE");
	}

	@Test
	void getSanitisedMessage_whenSeveralErrors() {
		MapBindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), TEST_OBJECT_NAME);
		bindingResult.addError(
			new ObjectError(TEST_OBJECT_NAME, TEST_CODES, TEST_ARGS, TEST_MESSAGE));
		bindingResult.addError(
			new ObjectError(TEST_OBJECT_NAME, TEST_CODES, TEST_ARGS, TEST_MESSAGE_2));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mockMethodParameter, bindingResult);

		assertThat(sanitiser.getSanitisedMessage(ex)).isEqualTo("Error in object 'TEST_OBJECT_NAME': TEST_MESSAGE; " +
			"Error in object 'TEST_OBJECT_NAME': TEST_MESSAGE_2");
	}

	@Test
	void getSanitisedMessage_whenOtherException() {
		RuntimeException ex = new RuntimeException("Oops");
		assertThat(sanitiser.getSanitisedMessage(ex)).isEqualTo("Oops");
	}


	@Test
	void sanitiseStackTrace_whenFieldError() {
		MapBindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), TEST_OBJECT_NAME);
		bindingResult.addError(
			new FieldError(
				TEST_OBJECT_NAME, TEST_FIELD, TEST_REJECTED_VALUE, true, TEST_CODES, TEST_ARGS, TEST_MESSAGE));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mockMethodParameter, bindingResult);

		List<String> stackTrace = Arrays.asList(
			"org.springframework.web.bind.MethodArgumentNotValidException: Validation failed for argument [8] in public org.springframework.http.ResponseEntity<com.mastercard.consumer.lifecycle.user.dto.UserInfo> com.mastercard.consumer.lifecycle.business.UserBusinessApi.createUser(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,boolean,com.mastercard.consumer.lifecycle.user.dto.UserInfo): [Field error in object 'userInfo' on field 'emails': rejected value [[Email(operation=null, value=robert, display=robert.d.tester@mastercard.com, ref=null, unrecognizedFields={})]]; codes [EmailValid.userInfo.emails,EmailValid.emails,EmailValid.java.util.List,EmailValid]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [userInfo.emails,emails]; arguments []; default message [emails]]; default message [Email format was invalid]]",
			"at org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor.resolveArgument(RequestResponseBodyMethodProcessor.java:139)",
			"at com.mastercard.consumer.correlation.NetworkTimingsFilter.doFilter(NetworkTimingsFilter.java:43)"
		);

		assertThat(sanitiser.sanitiseStackTrace(ex, stackTrace)).contains(
			"org.springframework.web.bind.MethodArgumentNotValidException: Field error in object 'TEST_OBJECT_NAME' on field 'TEST_FIELD': TEST_MESSAGE",
			"at org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor.resolveArgument(RequestResponseBodyMethodProcessor.java:139)",
			"at com.mastercard.consumer.correlation.NetworkTimingsFilter.doFilter(NetworkTimingsFilter.java:43)");
	}

	@Test
	void sanitiseStackTrace_whenObjectError() {
		MapBindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), TEST_OBJECT_NAME);
		bindingResult.addError(
			new ObjectError(TEST_OBJECT_NAME, TEST_CODES, TEST_ARGS, TEST_MESSAGE));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mockMethodParameter, bindingResult);

		List<String> stackTrace = Arrays.asList(
			"org.springframework.web.bind.MethodArgumentNotValidException: Validation failed for argument [8] in public org.springframework.http.ResponseEntity<com.mastercard.consumer.lifecycle.user.dto.UserInfo> com.mastercard.consumer.lifecycle.business.UserBusinessApi.createUser(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,boolean,com.mastercard.consumer.lifecycle.user.dto.UserInfo): [Field error in object 'userInfo' on field 'emails': rejected value [[Email(operation=null, value=robert, display=robert.d.tester@mastercard.com, ref=null, unrecognizedFields={})]]; codes [EmailValid.userInfo.emails,EmailValid.emails,EmailValid.java.util.List,EmailValid]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [userInfo.emails,emails]; arguments []; default message [emails]]; default message [Email format was invalid]]",
			"at org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor.resolveArgument(RequestResponseBodyMethodProcessor.java:139)",
			"at com.mastercard.consumer.correlation.NetworkTimingsFilter.doFilter(NetworkTimingsFilter.java:43)"
		);

		assertThat(sanitiser.sanitiseStackTrace(ex, stackTrace)).contains(
			"org.springframework.web.bind.MethodArgumentNotValidException: Error in object 'TEST_OBJECT_NAME': TEST_MESSAGE",
			"at org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor.resolveArgument(RequestResponseBodyMethodProcessor.java:139)",
			"at com.mastercard.consumer.correlation.NetworkTimingsFilter.doFilter(NetworkTimingsFilter.java:43)");
	}

	@Test
	void sanitiseStackTrace_whenOtherException() {
		RuntimeException ex = new RuntimeException("Oops");
		List<String> stackTrace = Arrays.asList("first", "second");
		assertThat(sanitiser.sanitiseStackTrace(ex, stackTrace)).isEqualTo(stackTrace);
	}
}