package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.spring.logging.annotations.LoggableView;
import ie.bitstep.mango.utils.masking.Masker;

import java.util.function.UnaryOperator;

public class LoggableValue {
	private final Object value;
	private final Masker masker;
	private final UnaryOperator<Object> converter;
	private final int truncateLen;
	private final LoggableView view;

	public LoggableValue(Object value, Masker masker, UnaryOperator<Object> converter, int truncateLen, LoggableView view) {
		this.value = value;
		this.masker = masker;
		this.converter = converter;
		this.truncateLen = truncateLen;
		this.view = view;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Object getValue() {
		return value;
	}

	public Masker getMasker() {
		return masker;
	}

	public UnaryOperator<Object> getConverter() {
		return converter;
	}

	public int getTruncateLen() {
		return truncateLen;
	}

	public LoggableView getView() {
		return view;
	}


	public static class Builder {
		private Object value;
		private Masker masker;
		private UnaryOperator<Object> converter;
		private int truncateLen;
		private LoggableView view;

		public Builder value(Object value) {
			this.value = value;
			return this;
		}

		public Builder masker(Masker masker) {
			this.masker = masker;
			return this;
		}

		public Builder converter(UnaryOperator<Object> converter) {
			this.converter = converter;
			return this;
		}

		public Builder truncateLen(int truncateLen) {
			this.truncateLen = truncateLen;
			return this;
		}

		public Builder view(LoggableView view) {
			this.view = view;
			return this;
		}

		public LoggableValue build() {
			return new LoggableValue(value, masker, converter, truncateLen, view);
		}
	}
}
