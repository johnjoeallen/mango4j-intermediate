package com.mastercard.commons.mango.logging.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class for DiagnosticLifecycleLogger utility class.
 */
@ExtendWith(MockitoExtension.class)
class DiagnosticLifecycleLoggerTest {

	@Mock
	private Logger mockLogger;

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
	void testIsEnabled_defaultIsFalse() {
		assertFalse(DiagnosticLifecycleLogger.isEnabled());
	}

	@Test
	void testIsEnabled_returnsTrue_whenEnabled() {
		DiagnosticLifecycleLogger.setEnabled(true);
		assertTrue(DiagnosticLifecycleLogger.isEnabled());
	}

	@Test
	void testIsEnabled_returnsFalse_whenDisabled() {
		DiagnosticLifecycleLogger.setEnabled(true);
		DiagnosticLifecycleLogger.setEnabled(false);
		assertFalse(DiagnosticLifecycleLogger.isEnabled());
	}

	@Test
	void testIsThreadlocalEnabled_defaultIsFalse() {
		assertFalse(DiagnosticLifecycleLogger.isThreadlocalEnabled());
	}

	@Test
	void testIsThreadlocalEnabled_returnsTrue_whenEnabled() {
		DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
		assertTrue(DiagnosticLifecycleLogger.isThreadlocalEnabled());
	}

	@Test
	void testIsThreadlocalEnabled_returnsFalse_whenDisabled() {
		DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);
		assertFalse(DiagnosticLifecycleLogger.isThreadlocalEnabled());
	}

	@Test
	void testLogInfo_noArguments_whenDisabled() {
		DiagnosticLifecycleLogger.setEnabled(false);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test message");
		// Verify logger was NOT called when disabled
		verify(mockLogger, never()).info("Test message");
	}

	@Test
	void testLogInfo_noArguments_whenEnabled() {
		DiagnosticLifecycleLogger.setEnabled(true);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test message");
		// Verify logger WAS called when enabled
		verify(mockLogger, times(1)).info("Test message");
	}

	@Test
	void testLogInfo_oneArgument_whenDisabled() {
		DiagnosticLifecycleLogger.setEnabled(false);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test message: {}", "arg1");
		// Verify logger was NOT called when disabled
		verify(mockLogger, never()).info("Test message: {}", "arg1");
	}

	@Test
	void testLogInfo_oneArgument_whenEnabled() {
		DiagnosticLifecycleLogger.setEnabled(true);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test message: {}", "arg1");
		// Verify logger WAS called when enabled
		verify(mockLogger, times(1)).info("Test message: {}", "arg1");
	}

	@Test
	void testLogInfo_twoArguments_whenDisabled() {
		DiagnosticLifecycleLogger.setEnabled(false);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test: {} {}", "arg1", "arg2");
		// Verify logger was NOT called when disabled
		verify(mockLogger, never()).info("Test: {} {}", "arg1", "arg2");
	}

	@Test
	void testLogInfo_twoArguments_whenEnabled() {
		DiagnosticLifecycleLogger.setEnabled(true);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test: {} {}", "arg1", "arg2");
		// Verify logger WAS called when enabled
		verify(mockLogger, times(1)).info("Test: {} {}", "arg1", "arg2");
	}

	@Test
	void testLogInfo_threeArguments_whenDisabled() {
		DiagnosticLifecycleLogger.setEnabled(false);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test: {} {} {}", "arg1", "arg2", "arg3");
		// Verify logger was NOT called when disabled
		verify(mockLogger, never()).info("Test: {} {} {}", "arg1", "arg2", "arg3");
	}

	@Test
	void testLogInfo_threeArguments_whenEnabled() {
		DiagnosticLifecycleLogger.setEnabled(true);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test: {} {} {}", "arg1", "arg2", "arg3");
		// Verify logger WAS called when enabled
		verify(mockLogger, times(1)).info("Test: {} {} {}", "arg1", "arg2", "arg3");
	}

	@Test
	void testLogInfo_fourArguments_whenDisabled() {
		DiagnosticLifecycleLogger.setEnabled(false);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test: {} {} {} {}", "arg1", "arg2", "arg3", "arg4");
		// Verify logger was NOT called when disabled
		verify(mockLogger, never()).info("Test: {} {} {} {}", "arg1", "arg2", "arg3", "arg4");
	}

	@Test
	void testLogInfo_fourArguments_whenEnabled() {
		DiagnosticLifecycleLogger.setEnabled(true);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test: {} {} {} {}", "arg1", "arg2", "arg3", "arg4");
		// Verify logger WAS called when enabled
		verify(mockLogger, times(1)).info("Test: {} {} {} {}", "arg1", "arg2", "arg3", "arg4");
	}

	@Test
	void testLogInfo_fiveArguments_whenDisabled() {
		DiagnosticLifecycleLogger.setEnabled(false);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test: {} {} {} {} {}", "arg1", "arg2", "arg3", "arg4", "arg5");
		// Verify logger was NOT called when disabled
		verify(mockLogger, never()).info("Test: {} {} {} {} {}", "arg1", "arg2", "arg3", "arg4", "arg5");
	}

	@Test
	void testLogInfo_fiveArguments_whenEnabled() {
		DiagnosticLifecycleLogger.setEnabled(true);
		DiagnosticLifecycleLogger.logInfo(mockLogger, "Test: {} {} {} {} {}", "arg1", "arg2", "arg3", "arg4", "arg5");
		// Verify logger WAS called when enabled
		verify(mockLogger, times(1)).info("Test: {} {} {} {} {}", "arg1", "arg2", "arg3", "arg4", "arg5");
	}

	@Test
	void testSetEnabled_togglesState() {
		assertFalse(DiagnosticLifecycleLogger.isEnabled());

		DiagnosticLifecycleLogger.setEnabled(true);
		assertTrue(DiagnosticLifecycleLogger.isEnabled());

		DiagnosticLifecycleLogger.setEnabled(false);
		assertFalse(DiagnosticLifecycleLogger.isEnabled());

		DiagnosticLifecycleLogger.setEnabled(true);
		assertTrue(DiagnosticLifecycleLogger.isEnabled());
	}

	@Test
	void testSetThreadlocalEnabled_togglesState() {
		assertFalse(DiagnosticLifecycleLogger.isThreadlocalEnabled());

		DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
		assertTrue(DiagnosticLifecycleLogger.isThreadlocalEnabled());

		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);
		assertFalse(DiagnosticLifecycleLogger.isThreadlocalEnabled());

		DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
		assertTrue(DiagnosticLifecycleLogger.isThreadlocalEnabled());
	}

	@Test
	void testBothFlags_independentOfEachOther() {
		// Both disabled by default
		assertFalse(DiagnosticLifecycleLogger.isEnabled());
		assertFalse(DiagnosticLifecycleLogger.isThreadlocalEnabled());

		// Enable lifecycle only
		DiagnosticLifecycleLogger.setEnabled(true);
		assertTrue(DiagnosticLifecycleLogger.isEnabled());
		assertFalse(DiagnosticLifecycleLogger.isThreadlocalEnabled());

		// Enable threadlocal only
		DiagnosticLifecycleLogger.setEnabled(false);
		DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
		assertFalse(DiagnosticLifecycleLogger.isEnabled());
		assertTrue(DiagnosticLifecycleLogger.isThreadlocalEnabled());

		// Enable both
		DiagnosticLifecycleLogger.setEnabled(true);
		assertTrue(DiagnosticLifecycleLogger.isEnabled());
		assertTrue(DiagnosticLifecycleLogger.isThreadlocalEnabled());

		// Disable both
		DiagnosticLifecycleLogger.setEnabled(false);
		DiagnosticLifecycleLogger.setThreadlocalEnabled(false);
		assertFalse(DiagnosticLifecycleLogger.isEnabled());
		assertFalse(DiagnosticLifecycleLogger.isThreadlocalEnabled());
	}

	@Test
	void testLogInfo_withNullLogger_whenEnabled() {
		DiagnosticLifecycleLogger.setEnabled(true);
		// Should throw NullPointerException when trying to log with null logger
		assertThrows(NullPointerException.class, () ->
			DiagnosticLifecycleLogger.logInfo(null, "Test message"));
	}

	@Test
	void testLogInfo_withNullLogger_whenDisabled() {
		DiagnosticLifecycleLogger.setEnabled(false);
		// Should not throw exception when disabled (doesn't try to log)
		assertDoesNotThrow(() ->
			DiagnosticLifecycleLogger.logInfo(null, "Test message"));
	}
}

