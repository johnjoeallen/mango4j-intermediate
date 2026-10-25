package com.mastercard.commons.mango.logging;

import com.mastercard.commons.mango.logging.adapter.LoggerAdapter;
import com.mastercard.commons.mango.logging.adapter.MapLoggerAdapter;
import com.mastercard.commons.mango.logging.config.DiagnosticLifecycleLogger;
import com.mastercard.commons.mango.logging.config.DiagnosticLoggingProperties;
import com.mastercard.commons.mango.logging.web.LoggingWebInterceptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;


@AutoConfiguration
@AutoConfigureOrder(value = HIGHEST_PRECEDENCE)
@ComponentScan(basePackages = {"com.mastercard.commons.mango"})
@EnableConfigurationProperties(DiagnosticLoggingProperties.class)
@RequiredArgsConstructor
public class LoggingConfiguration {

	private final DiagnosticLoggingProperties diagnosticLoggingProperties;

	/**
	 * Initialize DiagnosticLifecycleLogger static fields from configuration properties.
	 * Called after dependency injection completes.
	 */
	@PostConstruct
	public void initializeDiagnosticLifecycleLogger() {
		DiagnosticLifecycleLogger.setEnabled(diagnosticLoggingProperties.isEnabled());
		DiagnosticLifecycleLogger.setThreadlocalEnabled(diagnosticLoggingProperties.isThreadlocalEnabled());
		DiagnosticLifecycleLogger.setMaxChildren(diagnosticLoggingProperties.getMaxChildren());
	}

	@Bean
	@ConditionalOnMissingBean
	public LoggerAdapter baseLoggerAdapter() {
		return new MapLoggerAdapter();
	}

	@Bean
	@ConditionalOnBean(LoggingWebInterceptor.class)
	WebMvcConfigurer loggingWebInterceptorConfigurer(@Autowired LoggingWebInterceptor loggingWebInterceptor) {
		return new WebMvcConfigurer() {
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(loggingWebInterceptor);
			}
		};
	}


}
