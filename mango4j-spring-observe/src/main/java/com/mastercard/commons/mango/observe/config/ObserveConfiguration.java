package com.mastercard.commons.mango.observe.config;

import com.mastercard.commons.mango.observe.web.ObservationsWebInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
@AutoConfigureOrder(value = HIGHEST_PRECEDENCE)
@ComponentScan(basePackages = {"com.mastercard.commons.mango"})
@RequiredArgsConstructor
public class ObserveConfiguration {
	public static final int PRECEDENCE_ORDER = -101;

	@Bean
	WebMvcConfigurer observerConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(new ObservationsWebInterceptor()).order(PRECEDENCE_ORDER);
			}
		};
	}

}
