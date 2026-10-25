package com.mastercard.commons.mango.logging.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticLoggingAutoConfigurationTest {

	@BeforeEach
	void setUp() {
		// Reset to default state before each test
		DiagnosticLifecycleLogger.setEnabled(false);
		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);
	}

	@AfterEach
	void tearDown() {
		// Reset to default state after each test
		DiagnosticLifecycleLogger.setEnabled(false);
		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);
	}

	@Test
	void testInitializer_withDefaultProperties() throws Exception {
		// Given
		DiagnosticLoggingProperties properties = new DiagnosticLoggingProperties();
		DiagnosticLoggingAutoConfiguration.DiagnosticLoggingInitializer initializer =
			new DiagnosticLoggingAutoConfiguration.DiagnosticLoggingInitializer(properties);

		ApplicationArguments args = new DefaultApplicationArguments();

		// When
		initializer.run(args);

		// Then
		assertFalse(DiagnosticLifecycleLogger.isEnabled());
		assertFalse(DiagnosticLifecycleLogger.isThreadlocalEnabled());
	}

	@Test
	void testInitializer_withEnabledProperties() throws Exception {
		// Given
		DiagnosticLoggingProperties properties = new DiagnosticLoggingProperties();
		properties.setEnabled(true);
		properties.setThreadlocalEnabled(true);

		DiagnosticLoggingAutoConfiguration.DiagnosticLoggingInitializer initializer =
			new DiagnosticLoggingAutoConfiguration.DiagnosticLoggingInitializer(properties);

		ApplicationArguments args = new DefaultApplicationArguments();

		// When
		initializer.run(args);

		// Then
		assertTrue(DiagnosticLifecycleLogger.isEnabled());
		assertTrue(DiagnosticLifecycleLogger.isThreadlocalEnabled());
	}

	@Test
	void testInitializer_withPartiallyEnabledProperties() throws Exception {
		// Given
		DiagnosticLoggingProperties properties = new DiagnosticLoggingProperties();
		properties.setEnabled(true);
		properties.setThreadlocalEnabled(false);

		DiagnosticLoggingAutoConfiguration.DiagnosticLoggingInitializer initializer =
			new DiagnosticLoggingAutoConfiguration.DiagnosticLoggingInitializer(properties);

		ApplicationArguments args = new DefaultApplicationArguments();

		// When
		initializer.run(args);

		// Then
		assertTrue(DiagnosticLifecycleLogger.isEnabled());
		assertFalse(DiagnosticLifecycleLogger.isThreadlocalEnabled());
	}
}

