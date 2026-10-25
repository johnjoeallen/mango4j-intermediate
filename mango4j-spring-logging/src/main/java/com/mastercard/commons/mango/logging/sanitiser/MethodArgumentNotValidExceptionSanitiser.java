package com.mastercard.commons.mango.logging.sanitiser;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception sanitiser that removes field values from MethodArgumentNotValidException's message
 * and generally makes it smaller and more readable.
 */
public class MethodArgumentNotValidExceptionSanitiser implements ExceptionSanitiser {

	@Override
	public String getSanitisedMessage(Throwable throwable) {
		if (throwable instanceof MethodArgumentNotValidException manve) {
			return manve.getBindingResult().getAllErrors().stream()
				.map(this::errorToString)
				.collect(Collectors.joining("; "));
		} else {
			return throwable.getMessage();
		}
	}

	@Override
	public List<String> sanitiseStackTrace(Throwable throwable, List<String> stackTrace) {
		if (throwable instanceof MethodArgumentNotValidException manve) {
			List<String> newStackTrace = new ArrayList<>(stackTrace);
			newStackTrace.set(0, manve.getClass().getName() + ": " + getSanitisedMessage(manve));
			return newStackTrace;
		} else {
			return stackTrace;
		}
	}

	private String errorToString(ObjectError error) {
		if (error instanceof FieldError fe) {
			return "Field error in object '" + fe.getObjectName() + "' on field '" + fe.getField() + "': " + fe.getDefaultMessage();
		} else {
			return "Error in object '" + error.getObjectName() + "': " + error.getDefaultMessage();
		}
	}
}
