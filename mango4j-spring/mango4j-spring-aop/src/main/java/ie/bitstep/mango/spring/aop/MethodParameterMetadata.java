package ie.bitstep.mango.spring.aop;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class MethodParameterMetadata {

	private final Method method;
	private final Parameter[] parameters;

	public MethodParameterMetadata(Method method) {
		this.method = method;
		this.parameters = method.getParameters();
	}

	public Method getMethod() {
		return method;
	}

	public Parameter[] getParameters() {
		return parameters;
	}

	public Parameter getParameter(int index) {
		return parameters[index];
	}

	public int getParameterCount() {
		return parameters.length;
	}
}
