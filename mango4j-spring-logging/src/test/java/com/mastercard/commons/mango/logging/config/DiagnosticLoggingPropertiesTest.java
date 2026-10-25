package com.mastercard.commons.mango.logging.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for DiagnosticLoggingProperties configuration class.
 */
class DiagnosticLoggingPropertiesTest {

	private DiagnosticLoggingProperties properties;

	@BeforeEach
	void setUp() {
		properties = new DiagnosticLoggingProperties();
	}

	@Test
	void testLifecycleLoggingEnabled_defaultIsFalse() {
		assertFalse(properties.isEnabled());
	}

	@Test
	void testLifecycleLoggingEnabled_canBeSetToTrue() {
		properties.setEnabled(true);
		assertTrue(properties.isEnabled());
	}

	@Test
	void testLifecycleLoggingEnabled_canBeSetToFalse() {
		properties.setEnabled(true);
		properties.setEnabled(false);
		assertFalse(properties.isEnabled());
	}

	@Test
	void testThreadlocalLoggingEnabled_defaultIsFalse() {
		assertFalse(properties.isThreadlocalEnabled());
	}

	@Test
	void testThreadlocalLoggingEnabled_canBeSetToTrue() {
		properties.setThreadlocalEnabled(true);
		assertTrue(properties.isThreadlocalEnabled());
	}

	@Test
	void testThreadlocalLoggingEnabled_canBeSetToFalse() {
		properties.setThreadlocalEnabled(true);
		properties.setThreadlocalEnabled(false);
		assertFalse(properties.isThreadlocalEnabled());
	}

	@Test
	void testBothProperties_areIndependent() {
		// Both disabled by default
		assertFalse(properties.isEnabled());
		assertFalse(properties.isThreadlocalEnabled());

		// Enable lifecycle only
		properties.setEnabled(true);
		assertTrue(properties.isEnabled());
		assertFalse(properties.isThreadlocalEnabled());

		// Enable threadlocal only
		properties.setEnabled(false);
		properties.setThreadlocalEnabled(true);
		assertFalse(properties.isEnabled());
		assertTrue(properties.isThreadlocalEnabled());

		// Enable both
		properties.setEnabled(true);
		assertTrue(properties.isEnabled());
		assertTrue(properties.isThreadlocalEnabled());

		// Disable both
		properties.setEnabled(false);
		properties.setThreadlocalEnabled(false);
		assertFalse(properties.isEnabled());
		assertFalse(properties.isThreadlocalEnabled());
	}

	@Test
	void testSetLifecycleLoggingEnabled_multipleToggles() {
		properties.setEnabled(true);
		assertTrue(properties.isEnabled());

		properties.setEnabled(false);
		assertFalse(properties.isEnabled());

		properties.setEnabled(true);
		assertTrue(properties.isEnabled());

		properties.setEnabled(true);
		assertTrue(properties.isEnabled());
	}

	@Test
	void testSetThreadlocalLoggingEnabled_multipleToggles() {
		properties.setThreadlocalEnabled(true);
		assertTrue(properties.isThreadlocalEnabled());

		properties.setThreadlocalEnabled(false);
		assertFalse(properties.isThreadlocalEnabled());

		properties.setThreadlocalEnabled(true);
		assertTrue(properties.isThreadlocalEnabled());

		properties.setThreadlocalEnabled(true);
		assertTrue(properties.isThreadlocalEnabled());
	}

	@Test
	void testMaxChildren_defaultIs100() {
		assertThat(properties.getMaxChildren()).isEqualTo(100);
	}

	@Test
	void testMaxChildren_canBeSet() {
		properties.setMaxChildren(50);
		assertThat(properties.getMaxChildren()).isEqualTo(50);
	}

	@Test
	void testMaxChildren_canBeSetToZero() {
		properties.setMaxChildren(0);
		assertThat(properties.getMaxChildren()).isEqualTo(0);
	}
}

