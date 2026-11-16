package ie.bitstep.mango.spring.aop;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Parameter;
import java.util.Objects;

@Getter
@Setter
public class ParameterValue {

	private final Parameter parameter;
	private Object value;

	public ParameterValue(Parameter parameter, Object value) {
		this.parameter = Objects.requireNonNull(parameter, "parameter must not be null");
		this.value = value;
	}

	public String getName() {
		return parameter.getName();
	}

	public Class<?> getType() {
		return parameter.getType();
	}

	public <T extends java.lang.annotation.Annotation> T getAnnotation(Class<T> annotationClass) {
		return parameter.getAnnotation(annotationClass);
	}
}
