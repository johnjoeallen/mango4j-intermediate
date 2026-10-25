package com.mastercard.commons.mango.logging.holders;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Parameter;

@Getter
@AllArgsConstructor
@Builder
public class ParameterHolder {
	private final String name;
	private final boolean namePresent;
	private final boolean synthetic;

	public ParameterHolder(Parameter parameter) {
		this.name = parameter.getName();
		this.namePresent = parameter.isNamePresent();
		this.synthetic = parameter.isSynthetic();
	}
}
