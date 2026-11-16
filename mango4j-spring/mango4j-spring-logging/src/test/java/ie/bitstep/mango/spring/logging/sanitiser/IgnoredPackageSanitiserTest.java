package ie.bitstep.mango.spring.logging.sanitiser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class IgnoredPackageSanitiserTest {

	private IgnoredPackageSanitiser sanitiser = new IgnoredPackageSanitiser();

	@BeforeAll
	public static void before() {
		IgnoredPackageSanitiser.ignorePackage("org.junit.");
		IgnoredPackageSanitiser.ignorePackage("org.mockito.");
		IgnoredPackageSanitiser.ignorePackage("com.intellij.");
		IgnoredPackageSanitiser.ignorePackage("org.apache.maven.surefire");

		IgnoredPackageSanitiser.ignorePackage("com.example.app.utils.Timer.");
		IgnoredPackageSanitiser.ignorePackage("com.example.app.utils.Profiler.");
		IgnoredPackageSanitiser.ignorePackage("com.example.app.interceptor.XSSFilter.");
		IgnoredPackageSanitiser.ignorePackage("com.example.app.webcommon.filter.");
	}

	@Test
	void sanitiseStackTrace() {
		// given
		List<String> st = Arrays.asList(
			"org.springframework.web.client.OperationFailed: Failed to perform the operation",
			"at org.springframework.web.client.RestTemplate.postForObject(RestTemplate.java:437)",
			"at com.example.app.service.notification.TenantPushSender.sendPush_aroundBody0(TenantPushSender.java:101)",
			"at com.example.app.service.notification.TenantPushSender$AjcClosure1.run(TenantPushSender.java:1)",
			"at org.aspectj.runtime.reflect.JoinPointImpl.proceed(JoinPointImpl.java:149)",
			"at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.interceptLoggableMethod(AspectLogger.java:148)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.intercept(AspectLogger.java:148)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.ajc$inlineAccessMethod$ie_bitstep_mango_spring_logging_AspectLogger$ie_bitstep_mango_spring_logging_AspectLogger$intercept(AspectLogger.java:1)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.interceptLoggableMethod(AspectLogger.java:35)",
			"at com.example.app.service.notification.TenantPushSender.sendPush(TenantPushSender.java:74)",
			"at com.example.app.service.notification.AbstractPushExecutor$SendJob.run(AbstractPushExecutor.java:142)",
			"at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)",
			"at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)",
			"at java.lang.Thread.run(Thread.java:748)");

		// when
		List<String> result = sanitiser.sanitiseStackTrace(null, st);

		// then
		assertThat(result).contains(
			"org.springframework.web.client.OperationFailed: Failed to perform the operation",
			"at org.springframework.web.client.RestTemplate.postForObject(RestTemplate.java:437)",
			"at com.example.app.service.notification.TenantPushSender.sendPush_aroundBody0(TenantPushSender.java:101)",
			"at com.example.app.service.notification.TenantPushSender.sendPush(TenantPushSender.java:74)");
	}

	@Test
	void sanitiseStackTrace_whenThrownFromIgnoredPackage() {
		// given
		List<String> st = Arrays.asList(
			"java.lang.reflect.FailureException: Oops",
			"at java.lang.reflect.Handler.handleError(DefaultResponseErrorHandler.java:97)",
			"at org.springframework.web.client.DefaultResponseErrorHandler.handleError(DefaultResponseErrorHandler.java:79)",
			"at java.lang.reflect.Template.handleResponse(RestTemplate.java:777)",
			"at org.springframework.web.client.RestTemplate.postForObject(RestTemplate.java:437)");

		// when
		List<String> result = sanitiser.sanitiseStackTrace(null, st);

		// then
		assertThat(result).contains(
			"java.lang.reflect.FailureException: Oops",
			"at java.lang.reflect.Handler.handleError(DefaultResponseErrorHandler.java:97)",
			"at org.springframework.web.client.DefaultResponseErrorHandler.handleError(DefaultResponseErrorHandler.java:79)",
			"at org.springframework.web.client.RestTemplate.postForObject(RestTemplate.java:437)");
	}

	@Test
	void sanitiseStackTrace_withCause() {
		// given
		List<String> st = Arrays.asList(
			"org.springframework.web.client.OperationFailed: Failed to perform the operation",
			"at org.springframework.web.client.RestTemplate.postForObject(RestTemplate.java:437)",
			"at com.example.app.service.notification.TenantPushSender.sendPush_aroundBody0(TenantPushSender.java:101)",
			"at com.example.app.service.notification.TenantPushSender$AjcClosure1.run(TenantPushSender.java:1)",
			"at org.aspectj.runtime.reflect.JoinPointImpl.proceed(JoinPointImpl.java:149)",
			"at ie.bitstep.mango.spring.logging.internal.AspectLoggerDelegate.interceptLoggableMethod(AspectLogger.java:148)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.intercept(AspectLogger.java:148)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.ajc$inlineAccessMethod$ie_bitstep_mango_spring_logging_AspectLogger$ie_bitstep_mango_spring_logging_AspectLogger$intercept(AspectLogger.java:1)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.interceptLoggableMethod(AspectLogger.java:35)",
			"at com.example.app.service.notification.TenantPushSender.sendPush(TenantPushSender.java:74)",
			"at com.example.app.service.notification.AbstractPushExecutor$SendJob.run(AbstractPushExecutor.java:142)",
			"at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)",
			"at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)",
			"at java.lang.Thread.run(Thread.java:748)",
			"Caused by: java.lang.reflect.FailureException: Oops",
			"at java.lang.reflect.Handler.handleError(DefaultResponseErrorHandler.java:97)",
			"at org.springframework.web.client.DefaultResponseErrorHandler.handleError(DefaultResponseErrorHandler.java:79)",
			"at java.lang.reflect.Template.handleResponse(RestTemplate.java:777)",
			"at org.springframework.web.client.RestTemplate.postForObject(RestTemplate.java:437)");

		// when
		List<String> result = sanitiser.sanitiseStackTrace(null, st);

		// then
		assertThat(result).contains(
			"org.springframework.web.client.OperationFailed: Failed to perform the operation",
			"at org.springframework.web.client.RestTemplate.postForObject(RestTemplate.java:437)",
			"at com.example.app.service.notification.TenantPushSender.sendPush_aroundBody0(TenantPushSender.java:101)",
			"at com.example.app.service.notification.TenantPushSender.sendPush(TenantPushSender.java:74)",
			"Caused by: java.lang.reflect.FailureException: Oops",
			"at java.lang.reflect.Handler.handleError(DefaultResponseErrorHandler.java:97)",
			"at org.springframework.web.client.DefaultResponseErrorHandler.handleError(DefaultResponseErrorHandler.java:79)",
			"at org.springframework.web.client.RestTemplate.postForObject(RestTemplate.java:437)");
	}

	@Test
	void sanitiseStackTrace_withSampleTrace() {
		// given
		List<String> st = Arrays.asList(
			"com.example.app.exception.MISException: SERVER_BUSY: busy",
			"at com.example.app.service.notification.NotificationService.sendVerifyDeviceNotification(NotificationService.java:93)",
			"at com.example.app.service.AddDeviceSdkService.addDevice(AddDeviceSdkService.java:163)",
			"at org.aspectj.runtime.reflect.JoinPointImpl.proceed(JoinPointImpl.java:149)",
			"at com.example.app.utils.Profiler.profile(Profiler.java:123)",
			"at com.example.app.utils.Profiler.ajc$inlineAccessMethod$com_example_app_utils_Profiler$com_example_app_utils_Profiler$profile(Profiler.java:1)",
			"at com.example.app.utils.Profiler.profileService(Profiler.java:92)",
			"at com.example.app.service.AddDeviceSdkService$AjcClosure3.run(AddDeviceSdkService.java:1)",
			"at org.springframework.transaction.aspectj.AbstractTransactionAspect.ajc$around$org_springframework_transaction_aspectj_AbstractTransactionAspect$1$2a73e96cproceed(AbstractTransactionAspect.aj:66)",
			"at org.springframework.transaction.aspectj.AbstractTransactionAspect$AbstractTransactionAspect$1.proceedWithInvocation(AbstractTransactionAspect.aj:72)",
			"at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:294)",
			"at org.springframework.transaction.aspectj.AbstractTransactionAspect.ajc$around$org_springframework_transaction_aspectj_AbstractTransactionAspect$1$2a73e96c(AbstractTransactionAspect.aj:70)",
			"at com.example.app.service.AddDeviceSdkService$AjcClosure5.run(AddDeviceSdkService.java:1)",
			"at org.aspectj.runtime.reflect.JoinPointImpl.proceed(JoinPointImpl.java:149)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.intercept(AspectLogger.java:153)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.ajc$inlineAccessMethod$com_example_app_logger_AspectLogger$com_example_app_logger_AspectLogger$intercept(AspectLogger.java:1)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.interceptLoggableMethod(AspectLogger.java:36)",
			"at com.example.app.service.AddDeviceSdkService$AjcClosure1.run(AddDeviceSdkService.java:1)",
			"at com.example.app.controller.SDKController.addDevice(SDKController.java:89)",
			"at com.example.app.controller.SDKController$AjcClosure1.run(SDKController.java:1)",
			"at org.aspectj.runtime.reflect.JoinPointImpl.proceed(JoinPointImpl.java:149)",
			"at com.example.app.utils.Profiler.profile(Profiler.java:123)",
			"at com.example.app.utils.Profiler.ajc$inlineAccessMethod$com_example_app_utils_Profiler$com_example_app_utils_Profiler$profile(Profiler.java:1)",
			"at com.example.app.utils.Profiler.profileController(Profiler.java:97)",
			"at com.example.app.controller.SDKController$AjcClosure3.run(SDKController.java:1)",
			"at org.aspectj.runtime.reflect.JoinPointImpl.proceed(JoinPointImpl.java:149)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.intercept(AspectLogger.java:153)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.ajc$inlineAccessMethod$com_example_app_logger_AspectLogger$com_example_app_logger_AspectLogger$intercept(AspectLogger.java:1)",
			"at ie.bitstep.mango.spring.logging.aspect.AspectLogger.interceptLoggableMethod(AspectLogger.java:36)",
			"at com.example.app.controller.SDKController$AjcClosure3.run(SDKController.java:1)",
			"at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)",
			"at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)",
			"at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)",
			"at java.lang.reflect.Method.invoke(Method.java:498)",
			"at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:209)",
			"at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:136)",
			"at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:102)",
			"at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:870)",
			"at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:776)",
			"at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87)",
			"at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:991)",
			"at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:925)",
			"at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:978)",
			"at org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:881)",
			"at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:648)",
			"at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:855)",
			"at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:729)",
			"at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:292)",
			"at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:207)",
			"at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:52)",
			"at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:240)",
			"at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:207)",
			"at com.example.app.webcommon.filter.ContentEncodingFilter.doFilter(ContentEncodingFilter.java:30)",
			"at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:240)",
			"at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:207)",
			"at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:200)",
			"at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:107)",
			"at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:240)",
			"at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:207)",
			"at org.apache.logging.log4j.web.Log4jServletFilter.doFilter(Log4jServletFilter.java:71)",
			"at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:240)",
			"at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:207)",
			"at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:213)",
			"at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:106)",
			"at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:141)",
			"at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:79)",
			"at org.apache.catalina.valves.AbstractAccessLogValve.invoke(AbstractAccessLogValve.java:616)",
			"at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:88)",
			"at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:522)",
			"at org.apache.coyote.http11.AbstractHttp11Processor.process(AbstractHttp11Processor.java:1095)",
			"at org.apache.coyote.AbstractProtocol$AbstractConnectionHandler.process(AbstractProtocol.java:672)",
			"at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1520)",
			"at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.run(NioEndpoint.java:1476)",
			"at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)",
			"at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)",
			"at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61)",
			"at java.lang.Thread.run(Thread.java:748)");

		// when
		List<String> result = sanitiser.sanitiseStackTrace(null, st);

		assertThat(result).contains(
			"com.example.app.exception.MISException: SERVER_BUSY: busy",
			"at com.example.app.service.notification.NotificationService.sendVerifyDeviceNotification(NotificationService.java:93)",
			"at com.example.app.service.AddDeviceSdkService.addDevice(AddDeviceSdkService.java:163)",
			"at com.example.app.controller.SDKController.addDevice(SDKController.java:89)");
	}

}