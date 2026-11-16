package ie.bitstep.mango.spring.logging;

import ie.bitstep.mango.spring.logging.adapter.LoggerAdapter;
import ie.bitstep.mango.spring.logging.adapter.MapLoggerAdapter;
import ie.bitstep.mango.spring.logging.web.LoggingWebInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class LoggingConfigurationTest {

	@Mock
	private InterceptorRegistry interceptorRegistry;
	@Mock
	private LoggingWebInterceptor loggingWebInterceptor;

	@InjectMocks
	private LoggingConfiguration loggingConfiguration;

	@Test
	void baseLoggerAdapterTest() {
		LoggerAdapter loggerAdapter = loggingConfiguration.baseLoggerAdapter();

		assertThat(loggerAdapter).isNotNull().isInstanceOf(MapLoggerAdapter.class);
	}

	@Test
	void webMvcConfigBeanTest() {
		WebMvcConfigurer actualWebMvcConfigurer = loggingConfiguration.loggingWebInterceptorConfigurer(loggingWebInterceptor);
		assertThat(actualWebMvcConfigurer).isNotNull();
		actualWebMvcConfigurer.addInterceptors(interceptorRegistry);
		verify(interceptorRegistry, times(1)).addInterceptor(loggingWebInterceptor);
		verifyNoMoreInteractions(interceptorRegistry);
		verifyNoInteractions(loggingWebInterceptor);
	}


}