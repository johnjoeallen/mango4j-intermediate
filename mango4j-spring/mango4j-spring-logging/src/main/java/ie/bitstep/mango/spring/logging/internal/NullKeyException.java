package ie.bitstep.mango.spring.logging.internal;

public class NullKeyException extends Exception {
	public NullKeyException(Object v) {
		super(String.format("Null key for value[%s]", v.toString()));
	}
}
