package com.mastercard.commons.mango.logging.internal;

import com.mastercard.commons.mango.logging.LogProjection;
import com.mastercard.commons.mango.logging.test.data.SecretData;
import com.mastercard.commons.mango.logging.test.data.SecretDataLogProjection;
import com.mastercard.commons.mango.logging.test.data.SecretDataLogProjectionNoAnnotation;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@RequiredArgsConstructor
class LogProjectionRegistryTest {

	@Mock
	private ApplicationContext applicationContext;

	@Test
	void getLogProjectionSuccess() {
		when(applicationContext.getBeansOfType(LogProjection.class)).thenReturn(
			Map.of(SecretData.class.getSimpleName(), new SecretDataLogProjection())
		);

		LogProjectionRegistry logProjectionRegistry = new LogProjectionRegistry(applicationContext);
		LogProjection<?> projection = LogProjectionRegistry.getLogProjection(SecretData.class);

		assertThat(projection).isInstanceOf(SecretDataLogProjection.class);
	}

	@Test
	void getLogProjectionFailure() {
		when(applicationContext.getBeansOfType(LogProjection.class)).thenReturn(
			Map.of()
		);

		LogProjectionRegistry logProjectionRegistry = new LogProjectionRegistry(applicationContext);
		LogProjection<?> projection = LogProjectionRegistry.getLogProjection(SecretData.class);

		assertNull(projection);
	}

	@Test
	void getLogProjectionNotAnnotatedClass() {
		when(applicationContext.getBeansOfType(LogProjection.class)).thenReturn(
			Map.of(SecretData.class.getSimpleName(), new SecretDataLogProjectionNoAnnotation())
		);

		LogProjectionRegistry logProjectionRegistry = new LogProjectionRegistry(applicationContext);
		LogProjection<?> projection = LogProjectionRegistry.getLogProjection(SecretData.class);

		assertNull(projection);
	}
}