package ie.bitstep.mango.spring.aop;

import lombok.Getter;
import org.aopalliance.aop.Advice;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Getter
public class ProxyBuilder<T> {

	private final T target;
	private final Set<Object> aspects = new LinkedHashSet<>();

	private ProxyBuilder(T target) {
		this.target = Objects.requireNonNull(target);
	}

	public static <T> ProxyBuilder<T> withTarget(T target) {
		return new ProxyBuilder<>(target);
	}

	public ProxyBuilder<T> withAspect(Object aspect) {
		this.aspects.add(Objects.requireNonNull(aspect));
		return this;
	}

	public ProxyBuilder<T> withAspects(Object... aspects) {
		Arrays.stream(aspects).forEach(this::withAspect);
		return this;
	}

	@SuppressWarnings("unchecked")
	public T build() {
		if (aspects.isEmpty()) {
			throw new IllegalStateException("At least one aspect or advice must be provided");
		}

		boolean hasAspect = containsAspectAnnotation();
		boolean hasAdvice = isAdvice();

		if (hasAspect && hasAdvice) {
			throw new IllegalStateException("Cannot mix @Aspect-based and Advice-based proxying");
		}

		if (hasAspect) {
			AspectJProxyFactory factory = new AspectJProxyFactory(target);
			aspects.forEach(factory::addAspect);
			return factory.getProxy();
		} else {
			ProxyFactory factory = new ProxyFactory();
			factory.setTarget(target);

			for (Object aspect : aspects) {
				if (!(aspect instanceof Advice)) {
					throw new IllegalArgumentException(
						"Only Advice instances can be used without @Aspect: " + aspect.getClass());
				}
				factory.addAdvice((Advice) aspect);
			}

			return (T) factory.getProxy();
		}
	}

	boolean containsAspectAnnotation() {
		return aspects.stream().anyMatch(a -> AnnotationUtils.findAnnotation(a.getClass(), Aspect.class) != null);
	}

	boolean isAdvice() {
		return aspects.stream().anyMatch(Advice.class::isInstance);
	}
}
