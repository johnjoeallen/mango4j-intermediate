package ie.bitstep.mango.spring.logging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Spring Boot auto-configuration for diagnostic lifecycle logging.
 *
 * <p>Configure via application.yml:
 * <pre>
 * mango:
 *   logging:
 *     diagnostic:
 *       enabled: false
 *       threadlocal-enabled: false
 * </pre>
 *
 * <p>Or via environment variables:
 * <pre>
 * MANGO_LOGGING_DIAGNOSTIC_ENABLED=true
 * MANGO_LOGGING_DIAGNOSTIC_THREADLOCAL_ENABLED=true
 * </pre>
 *
 * @since 2.17.0
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(DiagnosticLoggingProperties.class)
public class DiagnosticLoggingAutoConfiguration {

	/**
	 * Application runner that initializes diagnostic logging based on configuration properties.
	 */
	@Component
	public static class DiagnosticLoggingInitializer implements ApplicationRunner {

		private final DiagnosticLoggingProperties properties;

		public DiagnosticLoggingInitializer(DiagnosticLoggingProperties properties) {
			this.properties = properties;
		}

		@Override
		public void run(ApplicationArguments args) {
			// Initialize diagnostic lifecycle logging
			DiagnosticLifecycleLogger.setEnabled(properties.isEnabled());
			DiagnosticLifecycleLogger.setThreadlocalEnabled(properties.isThreadlocalEnabled());

			log.info("Diagnostic lifecycle logging initialized - enabled: {}, threadlocal-enabled: {}",
				properties.isEnabled(), properties.isThreadlocalEnabled());
		}
	}
}

