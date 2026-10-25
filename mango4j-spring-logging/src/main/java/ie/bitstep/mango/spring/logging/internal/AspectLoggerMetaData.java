package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.spring.logging.annotations.LoggableView;
import ie.bitstep.mango.utils.masking.Masker;

import java.util.Set;
import java.util.function.UnaryOperator;

public class AspectLoggerMetaData {
	private final UnaryOperator<Object> accessor;
	private final String name;
	private final Masker valueMasker;
	private final UnaryOperator<Object> valueConverter;
	private final int truncateLen;
	private final LoggableView valueView;
	private final Set<LoggableView> belongsToViews;

	AspectLoggerMetaData(UnaryOperator<Object> accessor, String name, Masker valueMasker, UnaryOperator<Object> valueConverter,
						 int truncateLen, LoggableView valueView, Set<LoggableView> belongsToViews) {
		this.accessor = accessor;
		this.name = name;
		this.valueMasker = valueMasker;
		this.valueConverter = valueConverter;
		this.truncateLen = truncateLen;
		this.valueView = valueView;
		this.belongsToViews = belongsToViews;
	}

	public UnaryOperator<Object> getAccessor() {
		return accessor;
	}

	public String getName() {
		return name;
	}

	public Masker getValueMasker() {
		return valueMasker;
	}

	public UnaryOperator<Object> getValueConverter() {
		return valueConverter;
	}

	public int getTruncateLen() {
		return truncateLen;
	}

	public LoggableView getValueView() {
		return valueView;
	}

	public Set<LoggableView> getBelongsToViews() {
		return belongsToViews;
	}
}
