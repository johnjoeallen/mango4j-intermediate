package ie.bitstep.mango.spring.logging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for diagnostic lifecycle logging.
 *
 * <p>Example configuration in application.yml:
 * <pre>
 * mango:
 *   logging:
 *     diagnostic:
 *       enabled: false              # Enable general lifecycle logging
 *       threadlocal-enabled: false  # Enable thread-local specific logging
 *       max-children: 100           # Max children per context item before warning
 * </pre>
 *
 * <p>Example configuration via environment variables:
 * <pre>
 * MANGO_LOGGING_DIAGNOSTIC_ENABLED=true
 * MANGO_LOGGING_DIAGNOSTIC_THREADLOCAL_ENABLED=true
 * MANGO_LOGGING_DIAGNOSTIC_MAX_CHILDREN=100
 * </pre>
 *
 * @since 2.17.0
 */
@Data
@ConfigurationProperties(prefix = "mango.logging.diagnostic")
public class DiagnosticLoggingProperties {

	/**
	 * Enable general diagnostic lifecycle logging.
	 * <p>
	 * When enabled, logs diagnostic context lifecycle events including:
	 * <ul>
	 *   <li>Context creation and cleanup</li>
	 *   <li>Push/pop operations</li>
	 *   <li>Up/down navigation</li>
	 *   <li>DiagnosticContextItem lifecycle</li>
	 * </ul>
	 * <p>
	 * Default: false (disabled for zero overhead)
	 */
	private boolean enabled = false;

	/**
	 * Enable thread-local specific logging.
	 * <p>
	 * When enabled, logs thread-local acquisition and release events.
	 * <p>
	 * Default: false (disabled for zero overhead)
	 */
	private boolean threadlocalEnabled = false;

	/**
	 * Maximum number of children allowed per DiagnosticContextItem before logging a warning.
	 * <p>
	 * This helps detect potential issues with excessive nested method calls that could
	 * indicate infinite recursion or performance problems.
	 * <p>
	 * Default: 100
	 */
	private int maxChildren = 100;
}

