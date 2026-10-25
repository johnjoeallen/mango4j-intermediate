package ie.bitstep.mango.spring.logging;

public interface LogProjection<T> {
	Class<T> getProjectedType();
}
