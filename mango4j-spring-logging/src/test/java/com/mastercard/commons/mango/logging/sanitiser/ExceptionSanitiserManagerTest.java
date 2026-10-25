package com.mastercard.commons.mango.logging.sanitiser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ExceptionSanitiserManagerTest {

	@BeforeAll
	public static void before() {
		IgnoredPackageSanitiser.ignorePackage("org.junit.");
		IgnoredPackageSanitiser.ignorePackage("org.mockito.");
		IgnoredPackageSanitiser.ignorePackage("com.intellij.");
		IgnoredPackageSanitiser.ignorePackage("org.apache.maven.surefire");
	}

	@Test
	void privateConstructorTest() throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
		Constructor<ExceptionSanitiserManager> constructor = ExceptionSanitiserManager.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		ExceptionSanitiserManager esm = constructor.newInstance();
		assertThat(esm).isNotNull(); // best we can do
	}

	@Test
	void testAddSanitiser() {
		// given

		// when
		List<ExceptionSanitiser> SANITISERS = (List<ExceptionSanitiser>) ReflectionTestUtils.getField(ExceptionSanitiserManager.class, "SANITISERS");

		// then
		int size = SANITISERS.size();
		assertThat(SANITISERS.get(0)).isInstanceOf(IgnoredPackageSanitiser.class);
		assertThat(SANITISERS.get(1)).isInstanceOf(MethodArgumentNotValidExceptionSanitiser.class);

		ExceptionSanitiserManager.addSanitiser(new IgnoredPackageSanitiser());

		assertThat(SANITISERS).hasSize(size + 1);
		assertThat(SANITISERS.get(0)).isInstanceOf(IgnoredPackageSanitiser.class);
		assertThat(SANITISERS.get(1)).isInstanceOf(MethodArgumentNotValidExceptionSanitiser.class);
		assertThat(SANITISERS.get(0)).isInstanceOf(IgnoredPackageSanitiser.class);
	}

	@Test
	void getSanitisedMessage() {
		// given
		RuntimeException ex = new RuntimeException("Oops");

		// when
		String result = ExceptionSanitiserManager.getSanitisedMessage(ex);

		// then
		assertThat(result).isEqualTo("Oops");
	}

	@Test
	void getSanitisedMessage_whenMethodArgumentNotValidException() throws Exception {
		// given
		MapBindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), "TEST_OBJECT_NAME");
		bindingResult.addError(
			new FieldError("TEST_OBJECT_NAME", "TEST_FIELD", "TEST_MESSAGE"));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(buildMethodParameter(), bindingResult);

		// when
		String result = ExceptionSanitiserManager.getSanitisedMessage(ex);

		// then
		assertThat(result).isEqualTo("Field error in object 'TEST_OBJECT_NAME' on field 'TEST_FIELD': TEST_MESSAGE");
	}


	@Test
	void sanitiseStackTrace_withSampleTrace() {
		// given
		List<String> stackTrace = Arrays.asList(
			"com.mastercard.ess.mis.exception.MISException: SERVER_BUSY: busy",
			"at com.mastercard.ess.mis.service.notification.NotificationService.sendVerifyDeviceNotification(NotificationService.java:93)",
			"at com.mastercard.ess.mis.service.AddDeviceSdkService.addDevice(AddDeviceSdkService.java:163)",
			"at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:294)",
			"at com.mastercard.commons.mango.logging.aspect.AspectLogger.intercept(AspectLogger.java:153)",
			"at com.mastercard.commons.mango.logging.aspect.AspectLogger.interceptLoggableMethod(AspectLogger.java:36)",
			"at com.mastercard.ess.mis.controller.SDKController.addDevice(SDKController.java:89)",
			"at com.mastercard.commons.mango.logging.aspect.AspectLogger.intercept(AspectLogger.java:153)",
			"at com.mastercard.commons.mango.logging.aspect.AspectLogger.interceptLoggableMethod(AspectLogger.java:36)",
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
			"at com.mastercard.ess.mis.webcommon.filter.ContentEncodingFilter.doFilter(ContentEncodingFilter.java:30)",
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
		List<String> result = ExceptionSanitiserManager.sanitiseStackTrace(null, stackTrace);

		assertThat(result).contains(
			"com.mastercard.ess.mis.exception.MISException: SERVER_BUSY: busy",
			"at com.mastercard.ess.mis.service.notification.NotificationService.sendVerifyDeviceNotification(NotificationService.java:93)",
			"at com.mastercard.ess.mis.service.AddDeviceSdkService.addDevice(AddDeviceSdkService.java:163)",
			"at com.mastercard.ess.mis.controller.SDKController.addDevice(SDKController.java:89)");
	}

	MethodParameter buildMethodParameter() throws Exception {
		return new MethodParameter(getClass().getDeclaredMethod("buildMethodParameter"), -1);
	}
}