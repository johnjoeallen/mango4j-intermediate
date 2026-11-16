package ie.bitstep.mango.spring.insights;

import ie.bitstep.mango.spring.insights.internal.InsightWebInterceptor;
import ie.bitstep.mango.spring.insights.internal.InsightsProvider;
import ie.bitstep.mango.spring.insights.publisher.InsightsPublisher;
import ie.bitstep.mango.spring.insights.publisher.InsightsPublisherLogFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static ie.bitstep.mango.spring.insights.InsightsConfiguration.PRECEDENCE_ORDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightsConfigurationTest {

	@InjectMocks
	private InsightsConfiguration insightsConfiguration;

	@Mock
	private InsightsProvider insightsProvider;
	@Mock
	private InterceptorRegistry interceptorRegistry;
	@Mock
	private InterceptorRegistration interceptorRegistration;
	@Captor
	private ArgumentCaptor<HandlerInterceptor> handlerInterceptorArgumentCaptor;

	@Test
	void logPublisherBeanTest() {
		InsightsPublisher publisher = insightsConfiguration.logPublisher();

		assertThat(publisher).isNotNull().isInstanceOf(InsightsPublisherLogFile.class);
	}

	@Test
	void insightsWebInterceptorConfigurerTest() {
		when(interceptorRegistry.addInterceptor(any(HandlerInterceptor.class))).thenReturn(interceptorRegistration);

		WebMvcConfigurer webMvcConfigurer = insightsConfiguration.insightsWebInterceptorConfigurer(insightsProvider);

		assertThat(webMvcConfigurer).isNotNull();

		webMvcConfigurer.addInterceptors(interceptorRegistry);

		verify(interceptorRegistry, times(1)).addInterceptor(handlerInterceptorArgumentCaptor.capture());
		verify(interceptorRegistration, times(1)).order(PRECEDENCE_ORDER);
		verifyNoMoreInteractions(interceptorRegistration);
		verifyNoMoreInteractions(interceptorRegistry);
		assertThat(handlerInterceptorArgumentCaptor.getAllValues()).hasSize(1);
		assertThat(handlerInterceptorArgumentCaptor.getValue()).isInstanceOf(InsightWebInterceptor.class);
		InsightWebInterceptor insightWebInterceptor = (InsightWebInterceptor) handlerInterceptorArgumentCaptor.getValue();
		Object actualInsightsProvider = ReflectionTestUtils.getField(insightWebInterceptor, "insightsProvider");
		assertThat(actualInsightsProvider).isNotNull().isInstanceOf(InsightsProvider.class).isEqualTo(insightsProvider);

	}

}