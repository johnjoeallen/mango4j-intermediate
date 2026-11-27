package ie.bitstep.mango.spring.logging.detail;

import ie.bitstep.mango.logging.annotations.ExceptionDetail;
import ie.bitstep.mango.spring.aop.MethodInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Ordered exception-type rules; the first rule whose type matches (isInstance) wins.
 * Exceptions matching no rule defer to the method's {@code logException}.
 *
 * <pre>
 * ExceptionDetailRules.builder()
 *     .on(NotFoundException.class)                         // nothing
 *     .on(ValidationException.class, CLASS_NAME)
 *     .on(DataAccessException.class, MESSAGE, CAUSE)
 *     .build();
 * </pre>
 */
public final class ExceptionDetailRules implements ExceptionDetailStrategy {

	private record Rule(Class<? extends Throwable> type, Set<ExceptionDetail> detail) {
	}

	private final List<Rule> rules;

	private ExceptionDetailRules(List<Rule> rules) {
		this.rules = List.copyOf(rules);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public Optional<Set<ExceptionDetail>> getDetail(Throwable t, MethodInfo methodInfo) {
		for (Rule rule : rules) {
			if (rule.type().isInstance(t)) {
				return Optional.of(rule.detail());
			}
		}
		return Optional.empty();
	}

	public static final class Builder {
		private final List<Rule> rules = new ArrayList<>();

		/**
		 * @param detail what to log; none means log nothing for matching exceptions
		 */
		public Builder on(Class<? extends Throwable> type, ExceptionDetail... detail) {
			Set<ExceptionDetail> set = detail.length == 0
				? EnumSet.noneOf(ExceptionDetail.class)
				: EnumSet.copyOf(Arrays.asList(detail));
			rules.add(new Rule(type, set));
			return this;
		}

		public ExceptionDetailRules build() {
			return new ExceptionDetailRules(rules);
		}
	}
}
