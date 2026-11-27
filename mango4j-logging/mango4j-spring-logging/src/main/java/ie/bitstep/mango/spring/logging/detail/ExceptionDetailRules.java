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
 * Exception-type rules; the rule with the most specific matching type wins (the exact class, else the nearest
 * superclass), whatever order the rules were declared in. Exceptions matching no rule defer to the method's
 * {@code logException}.
 *
 * <pre>
 * ExceptionDetailRules.builder()
 *     .on(NotFoundException.class)                         // nothing
 *     .on(ValidationException.class, REASON, CLASS_NAME)
 *     .on(DataAccessException.class, REASON, MESSAGE, CAUSE)
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
		Rule best = null;
		int bestDistance = Integer.MAX_VALUE;

		for (Rule rule : rules) {
			int distance = ExceptionTypes.distance(t.getClass(), rule.type());
			if (distance >= 0 && distance < bestDistance) {    // strictly closer: for equal rules the first declared is kept
				best = rule;
				bestDistance = distance;
			}
		}
		return best == null ? Optional.empty() : Optional.of(best.detail());
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
