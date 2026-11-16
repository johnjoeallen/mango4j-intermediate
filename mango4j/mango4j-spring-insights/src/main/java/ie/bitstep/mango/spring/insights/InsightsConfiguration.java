package ie.bitstep.mango.spring.insights;

import ie.bitstep.mango.spring.insights.internal.InsightWebInterceptor;
import ie.bitstep.mango.spring.insights.internal.InsightsProvider;
import ie.bitstep.mango.spring.insights.publisher.InsightsPublisher;
import ie.bitstep.mango.spring.insights.publisher.InsightsPublisherLogFile;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
@AutoConfigureOrder(value = HIGHEST_PRECEDENCE)
@ComponentScan(basePackages = {"ie.bitstep.mango.spring"})
@RequiredArgsConstructor
public class InsightsConfiguration {

	public static final int PRECEDENCE_ORDER = -101;

	@Bean
	@ConditionalOnProperty("mango.insights.publishers.log.enabled")
	InsightsPublisher logPublisher() {
		return new InsightsPublisherLogFile();
	}

	@Bean
	WebMvcConfigurer insightsWebInterceptorConfigurer(InsightsProvider insightsProvider) {
		return new WebMvcConfigurer() {
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(new InsightWebInterceptor(insightsProvider)).order(PRECEDENCE_ORDER);
			}
		};
	}

}
