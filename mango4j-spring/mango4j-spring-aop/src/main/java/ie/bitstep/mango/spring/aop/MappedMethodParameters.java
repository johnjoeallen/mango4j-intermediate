package ie.bitstep.mango.spring.aop;

import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MappedMethodParameters {

	@Getter
	private final Method method;

	private final Parameter[] parameters;
	private final Object[] arguments;
	private final List<ParameterValue> parameterValues;

	public MappedMethodParameters(Method calledMethod, Object[] methodArguments) {
		this.method = Objects.requireNonNull(calledMethod, "method must not be null");
		this.parameters = calledMethod.getParameters();
		this.arguments = sanitiseArguments(methodArguments);

		if (parameters.length != arguments.length) {
			throw new IllegalArgumentException("Parameter count does not match argument count");
		}

		this.parameterValues = new ArrayList<>(parameters.length);
		for (int i = 0; i < parameters.length; i++) {
			parameterValues.add(new ParameterValue(parameters[i], arguments[i]));
		}
	}

	static Object[] sanitiseArguments(Object[] arguments) {
		if (arguments == null) {
			return new Object[0];
		}

		return arguments;
	}

	public Parameter[] getParameters() {
		return parameters.clone();
	}

	public Object[] getArguments() {
		return arguments.clone();
	}

	public List<ParameterValue> getParameterValues() {
		return Collections.unmodifiableList(parameterValues);
	}

	public Object[] getModifiedArguments() {
		return parameterValues.stream().map(ParameterValue::getValue).toArray(Object[]::new);
	}

	public Object getArgument(int index) {
		return arguments[index];
	}

	public Parameter getParameter(int index) {
		return parameters[index];
	}

	public ParameterValue getParameterValue(int index) {
		return parameterValues.get(index);
	}

	public int size() {
		return parameters.length;
	}
}
