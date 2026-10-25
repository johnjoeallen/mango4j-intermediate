package com.mastercard.commons.mango.logging.level;

import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.logging.annotations.LoggableLevel;
import com.mastercard.commons.mango.logging.annotations.LoggableMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DefaultExceptionLogLevelStrategyTest {

	private DefaultExceptionLogLevelStrategy strategy = new DefaultExceptionLogLevelStrategy();

	@Test
	@LoggableMethod(forceErrorOnException = true)
	public void testErrorLevelForced() throws Exception { // NOSONAR - public required used in test
		assertThat(strategy.isErrorLevelForced(new RuntimeException(""), new MethodInfo(getClass(), getClass().getMethod("testErrorLevelForced"))))
			.isTrue();
	}

	@Test
	@LoggableMethod(forceErrorOnException = false)
	public void testErrorLevelNotForced() throws Exception { // NOSONAR - public required used in test
		assertThat(strategy.isErrorLevelForced(new RuntimeException(""), new MethodInfo(getClass(), getClass().getMethod("testErrorLevelNotForced"))))
			.isFalse();
	}

	@Test
	public void testErrorLevelNotForcedNotLoggableMethod() throws Exception { // NOSONAR - public required used in test
		assertThat(strategy.isErrorLevelForced(new RuntimeException(""), new MethodInfo(getClass(), getClass().getMethod("testErrorLevelNotForcedNotLoggableMethod"))))
			.isFalse();
	}

	@Test
	public void getLogLevel_whenRuntimeException_thenError() throws Exception { // NOSONAR - public required used in test
		Level level = strategy.getLogLevel(
			new RuntimeException(),
			new MethodInfo(getClass(), getClass().getMethod("getLogLevel_whenRuntimeException_thenError")));
		assertThat(level).isEqualTo(Level.ERROR);
	}

	@Test
	public void getLogLevel_whenCheckedException_thenWarn() throws Exception { // NOSONAR - public required used in test
		Level level = strategy.getLogLevel(
			new IOException(""),
			new MethodInfo(getClass(), getClass().getMethod("getLogLevel_whenCheckedException_thenWarn")));
		assertThat(level).isEqualTo(Level.WARN);
	}

	@Test
	@LoggableMethod(forceErrorOnException = true)
	public void getLogLevel_whenForcedError_thenError() throws Exception { // NOSONAR - public required used in test
		Level level = strategy.getLogLevel(
			new IOException(""),
			new MethodInfo(getClass(), getClass().getMethod("getLogLevel_whenForcedError_thenError")));
		assertThat(level).isEqualTo(Level.ERROR);
	}

	@LoggableLevel(level = Level.INFO)
	static class AnnotatedInfoException extends RuntimeException {
	}

	static class AnnotatedChildInfoException extends AnnotatedInfoException {
	}

	@Test
	public void getLogLevel_whenAnnotatedInfoException_thenInfo() throws Exception { // NOSONAR - public required used in test
		Level level = strategy.getLogLevel(
			new AnnotatedInfoException(),
			new MethodInfo(getClass(), getClass().getMethod("getLogLevel_whenAnnotatedInfoException_thenInfo")));
		assertThat(level).isEqualTo(Level.INFO);
	}

	@Test
	public void getLogLevel_whenAnnotatedChildInfoException_thenInfo() throws Exception { // NOSONAR - public required used in test
		Level level = strategy.getLogLevel(
			new AnnotatedChildInfoException(),
			new MethodInfo(getClass(), getClass().getMethod("getLogLevel_whenAnnotatedChildInfoException_thenInfo")));
		assertThat(level).isEqualTo(Level.INFO);
	}

	@Test
	public void getLogLevel_whenHttpMediaTypeNotSupportedException_thenInfo() throws Exception { // NOSONAR - public required used in test
		Level level = strategy.getLogLevel(
			new HttpMediaTypeNotSupportedException(""),
			new MethodInfo(getClass(), getClass().getMethod("getLogLevel_whenHttpMediaTypeNotSupportedException_thenInfo")));
		assertThat(level).isEqualTo(Level.INFO);
	}

	@Test
	public void getLogLevel_whenHttpMessageNotReadableException_thenInfo() throws Exception { // NOSONAR - public required used in test
		Level level = strategy.getLogLevel(
			new HttpMessageNotReadableException(""),
			new MethodInfo(getClass(), getClass().getMethod("getLogLevel_whenHttpMessageNotReadableException_thenInfo")));
		assertThat(level).isEqualTo(Level.INFO);
	}

	@Test
	public void getLogLevel_whenMethodArgumentNotValidException_thenInfo() throws Exception { // NOSONAR - public required used in test
		BeanPropertyBindingResult result = new BeanPropertyBindingResult(this, "test");
		Level level = strategy.getLogLevel(
			new MethodArgumentNotValidException(null, result),
			new MethodInfo(getClass(), getClass().getMethod("getLogLevel_whenMethodArgumentNotValidException_thenInfo")));
		assertThat(level).isEqualTo(Level.INFO);
	}
}