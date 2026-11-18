package ie.bitstep.mango.logging;

public interface LogProjection<T> {
	Class<T> getProjectedType();
}
