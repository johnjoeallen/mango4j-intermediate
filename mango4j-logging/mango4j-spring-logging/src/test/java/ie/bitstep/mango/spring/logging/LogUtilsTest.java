package ie.bitstep.mango.spring.logging;

import ie.bitstep.mango.logging.LogUtils;

import ie.bitstep.mango.logging.TestClassForExceptions;

import ie.bitstep.mango.logging.test.ListBuilder;
import ie.bitstep.mango.logging.sanitiser.IgnoredPackageSanitiser;
import ie.bitstep.mango.spring.logging.web.WebMDCField;
import ie.bitstep.mango.logging.test.MDCTestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.MethodParameter;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.CORRELATION_ID;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.OPERATION;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.REQUEST_ID_ATTRIBUTE;
import static ie.bitstep.mango.spring.logging.web.populators.base.PopulatorConstants.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LogUtilsTest { // WARNING! Some tests here rely on line numbers so changes to the LogUtils and the test itself might inflict failures
	public static final String TEST_USER = "TEST_USER";

	@Mock
	private HttpServletRequest mockRequest;

	private List<String> removeLineNumbers(List<String> stackTrace) {
		List<String> noNumbers = new ArrayList<>();

		for (String s : stackTrace) {
			noNumbers.add(s.replaceAll(":[0-9]+\\)", ")"));
		}

		return noNumbers;
	}

	@BeforeEach
	void setup() {
		MDCTestUtils.init();
		IgnoredPackageSanitiser.ignorePackage("org.junit.");
		IgnoredPackageSanitiser.ignorePackage("org.mockito.");
		IgnoredPackageSanitiser.ignorePackage("com.intellij.");
		IgnoredPackageSanitiser.ignorePackage("org.apache.maven.surefire");

		IgnoredPackageSanitiser.ignorePackage("com.example.app.utils.Timer.");
		IgnoredPackageSanitiser.ignorePackage("com.example.app.utils.Profiler.");
		IgnoredPackageSanitiser.ignorePackage("com.example.app.interceptor.XSSFilter.");
		IgnoredPackageSanitiser.ignorePackage("com.example.app.webcommon.filter.");
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void privateConstructorTest() throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
		Constructor<LogUtils> constructor = LogUtils.class.getDeclaredConstructor();
		assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
		constructor.setAccessible(true);
		LogUtils logUtils = constructor.newInstance();
		assertThat(logUtils).isNotNull(); // best we can do
	}

	@Test
	void testIsValidCause() {
		assertThat(LogUtils.isValidCause(new RuntimeException("Some error", null))).isFalse();
		assertThat(LogUtils.isValidCause(new RuntimeException("Some error", new RuntimeException("Another error")))).isTrue();
		assertThat(LogUtils.isValidCause(new RuntimeException("Some error", new RuntimeException("Some error")))).isTrue();
	}

	@Test
	void exception2Message() {
		assertThat(LogUtils.exception2Message(null)).isBlank();

		assertThat(LogUtils.exception2Message(new RuntimeException("Some error")))
			.isEqualTo("Some error");

		assertThat(LogUtils.exception2Message(new RuntimeException("Some error", new RuntimeException("First error"))))
			.isEqualTo("Some error; Cause: First error");

		assertThat(LogUtils.exception2Message(new IllegalStateException(new RuntimeException("First error"))))
			.isEqualTo("java.lang.IllegalStateException; Cause: First error");

		assertThat(LogUtils.exception2Message(new RuntimeException("Some error", new IllegalStateException())))
			.isEqualTo("Some error; Cause: java.lang.IllegalStateException");

	}

	@Test
	void exception2Message_withoutCauses() {
		assertThat(LogUtils.exception2Message(null, false))
			.isBlank();

		assertThat(LogUtils.exception2Message(new RuntimeException("Some error"), false))
			.isEqualTo("Some error");

		assertThat(LogUtils.exception2Message(new RuntimeException("Some error", new RuntimeException("First error")), false))
			.isEqualTo("Some error");

		assertThat(LogUtils.exception2Message(new IllegalStateException(new RuntimeException("First error")), false))
			.isEqualTo("java.lang.RuntimeException: First error");

		assertThat(LogUtils.exception2Message(new RuntimeException("Some error", new IllegalStateException()), false))
			.isEqualTo("Some error");
	}

	@Test
	void exception2Message_whenMethodArgumentNotValidException() throws Exception {
		MapBindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), "TEST_OBJECT_NAME");
		bindingResult.addError(new FieldError("TEST_OBJECT_NAME", "TEST_FIELD", "TEST_MESSAGE"));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(buildMethodParameter(), bindingResult);

		assertThat(LogUtils.exception2Message(ex)).isEqualTo("Field error in object 'TEST_OBJECT_NAME' on field 'TEST_FIELD': TEST_MESSAGE");
	}

	@Test
	void maxLevel() {
		assertThat(LogUtils.maxLevel(Level.INFO, Level.ERROR)).isEqualTo(Level.ERROR);
		assertThat(LogUtils.maxLevel(Level.INFO, Level.WARN)).isEqualTo(Level.WARN);
		assertThat(LogUtils.maxLevel(Level.ERROR, Level.INFO)).isEqualTo(Level.ERROR);
		assertThat(LogUtils.maxLevel(Level.INFO, Level.INFO)).isEqualTo(Level.INFO);
	}

	@Test
	void toLogSafeValue() {
		assertThat(LogUtils.toLogSafeValue(null)).isNull();
		assertThat(LogUtils.toLogSafeValue("")).isBlank();
		assertThat(LogUtils.toLogSafeValue("   ")).isBlank();

		assertThat(LogUtils.toLogSafeValue("safe value")).isEqualTo("safe value");

		assertThat(LogUtils.toLogSafeValue("new\n\tlines")).isEqualTo("new\\n\\tlines");

		assertThat(LogUtils.toLogSafeValue("val\u044C\u1111 <>end")).isEqualTo("val\\u044c\\u1111 <>end");
	}


	@Test
	void getStackTrace_whenSimpleException() {
		// given
		List<String> expected = ListBuilder.<String>list()
			.add("java.lang.RuntimeException: Oops")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do2(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do1(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doException(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.spring.logging.LogUtilsTest.getStackTrace_whenSimpleException(LogUtilsTest.java)")
			.build();

		Exception exception = null;
		try {
			new TestClassForExceptions().doException();
		} catch (Exception ex) {
			exception = ex;
		}

		// when
		List<String> result = LogUtils.getStackTrace(exception);

		// then
		assertThat(removeLineNumbers(result)).isEqualTo(removeLineNumbers(expected));
	}

	@Test
	void getStackTrace_whenExceptionWithCause() {
		// given
		List<String> expected = ListBuilder.<String>list()
			.add("java.lang.RuntimeException: Failed")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithCause2(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithCause1(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doExceptionWithCause(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.spring.logging.LogUtilsTest.getStackTrace_whenExceptionWithCause(LogUtilsTest.java)")
			.add("Caused by: java.lang.RuntimeException: Oops")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do2(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do1(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithCause2(TestClassForExceptions.java)")
			.build();

		Exception exception = null;
		try {
			new TestClassForExceptions().doExceptionWithCause();
		} catch (Exception ex) {
			exception = ex;
		}

		// when
		List<String> result = LogUtils.getStackTrace(exception);

		// then
		assertThat(removeLineNumbers(result)).isEqualTo(removeLineNumbers(expected));
	}

	@Test
	void getStackTrace_whenExceptionWithSuppressed() {
		// given
		List<String> expected = ListBuilder.<String>list()
			.add("java.lang.RuntimeException: Failed")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithSuppressed2(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithSuppressed1(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doExceptionWithSuppressed(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.spring.logging.LogUtilsTest.getStackTrace_whenExceptionWithSuppressed(LogUtilsTest.java)")
			.add("Suppressed: java.lang.RuntimeException: Oops")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do2(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do1(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithSuppressed2(TestClassForExceptions.java)")
			.build();

		Exception exception = null;
		try {
			new TestClassForExceptions().doExceptionWithSuppressed();
		} catch (Exception ex) {
			exception = ex;
		}

		// when
		List<String> result = LogUtils.getStackTrace(exception);

		// then
		assertThat(removeLineNumbers(result)).isEqualTo(removeLineNumbers(expected));
	}

	@Test
	void getStackTrace_whenMethodArgumentNotValidException() throws Exception {
		// given
		List<String> expected = ListBuilder.<String>list()
			.add("org.springframework.web.bind.MethodArgumentNotValidException: Field error in object 'TEST_OBJECT_NAME' on field 'TEST_FIELD': TEST_MESSAGE")
			.add("at ie.bitstep.mango.spring.logging.LogUtilsTest.getStackTrace_whenMethodArgumentNotValidException(LogUtilsTest.java)")
			.build();
		MapBindingResult bindingResult = new MapBindingResult(Collections.emptyMap(), "TEST_OBJECT_NAME");
		bindingResult.addError(new FieldError("TEST_OBJECT_NAME", "TEST_FIELD", "TEST_MESSAGE"));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(buildMethodParameter(), bindingResult);

		// when
		List<String> result = LogUtils.getStackTrace(ex);

		assertThat(removeLineNumbers(result)).isEqualTo(removeLineNumbers(expected));
	}

	// ---- getStackTraceWithoutMessages: like getStackTrace but never includes exception messages -------------

	@Test
	void getStackTraceWithoutMessages_whenSimpleException_headerIsClassNameOnly() {
		// given
		List<String> expected = ListBuilder.<String>list()
			.add("java.lang.RuntimeException")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do2(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do1(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doException(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.spring.logging.LogUtilsTest.getStackTraceWithoutMessages_whenSimpleException_headerIsClassNameOnly(LogUtilsTest.java)")
			.build();

		Exception exception = null;
		try {
			new TestClassForExceptions().doException();
		} catch (Exception ex) {
			exception = ex;
		}

		// when
		List<String> result = LogUtils.getStackTraceWithoutMessages(exception);

		// then
		assertThat(removeLineNumbers(result)).isEqualTo(removeLineNumbers(expected));
	}

	@Test
	void getStackTraceWithoutMessages_whenExceptionWithCause_causeHeaderHasNoMessage() {
		// given
		List<String> expected = ListBuilder.<String>list()
			.add("java.lang.RuntimeException")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithCause2(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithCause1(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doExceptionWithCause(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.spring.logging.LogUtilsTest.getStackTraceWithoutMessages_whenExceptionWithCause_causeHeaderHasNoMessage(LogUtilsTest.java)")
			.add("Caused by: java.lang.RuntimeException")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do2(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do1(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithCause2(TestClassForExceptions.java)")
			.build();

		Exception exception = null;
		try {
			new TestClassForExceptions().doExceptionWithCause();
		} catch (Exception ex) {
			exception = ex;
		}

		// when
		List<String> result = LogUtils.getStackTraceWithoutMessages(exception);

		// then: same shape as getStackTrace (frames shared with the enclosing trace are omitted), just no messages
		assertThat(removeLineNumbers(result)).isEqualTo(removeLineNumbers(expected));
	}

	@Test
	void getStackTraceWithoutMessages_whenExceptionWithSuppressed_suppressedHeaderHasNoMessage() {
		// given
		List<String> expected = ListBuilder.<String>list()
			.add("java.lang.RuntimeException")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithSuppressed2(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithSuppressed1(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doExceptionWithSuppressed(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.spring.logging.LogUtilsTest.getStackTraceWithoutMessages_whenExceptionWithSuppressed_suppressedHeaderHasNoMessage(LogUtilsTest.java)")
			.add("Suppressed: java.lang.RuntimeException")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do2(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.do1(TestClassForExceptions.java)")
			.add("at ie.bitstep.mango.logging.TestClassForExceptions.doWithSuppressed2(TestClassForExceptions.java)")
			.build();

		Exception exception = null;
		try {
			new TestClassForExceptions().doExceptionWithSuppressed();
		} catch (Exception ex) {
			exception = ex;
		}

		// when
		List<String> result = LogUtils.getStackTraceWithoutMessages(exception);

		// then
		assertThat(removeLineNumbers(result)).isEqualTo(removeLineNumbers(expected));
	}

	@Test
	void getStackTraceWithoutMessages_secretMessagesNeverAppear() {
		RuntimeException ex = new RuntimeException("top-SECRETA", new IllegalStateException("cause-SECRETB"));
		ex.addSuppressed(new IllegalArgumentException("suppressed-SECRETC"));

		List<String> result = LogUtils.getStackTraceWithoutMessages(ex);

		assertThat(String.join("\n", result)).doesNotContain("SECRET");
		assertThat(result).contains("java.lang.RuntimeException", "Suppressed: java.lang.IllegalArgumentException", "Caused by: java.lang.IllegalStateException");
		// and the default does leak them (the reason this method exists)
		assertThat(String.join("\n", LogUtils.getStackTrace(ex))).contains("top-SECRETA");
	}

	@Test
	void getStackTraceWithoutMessages_nullMessage_sameAsAnyOther() {
		List<String> result = LogUtils.getStackTraceWithoutMessages(new IllegalStateException());

		assertThat(result.get(0)).isEqualTo("java.lang.IllegalStateException");
	}

	@Test
	void getStackTraceWithoutMessages_circularCause_terminates() {
		RuntimeException a = new RuntimeException("a");
		RuntimeException b = new RuntimeException("b", a);
		a.initCause(b);

		List<String> result = LogUtils.getStackTraceWithoutMessages(a);

		assertThat(result).contains("java.lang.RuntimeException", "Caused by: java.lang.RuntimeException",
			"Caused by: [CIRCULAR REFERENCE: java.lang.RuntimeException]");
	}

	@Test
	void getStackTraceWithoutMessages_appliesStackTraceSanitisers() {
		List<String> result = LogUtils.getStackTraceWithoutMessages(new RuntimeException("x"));

		// org.junit. frames are ignored by the sanitiser registered in setup()
		assertThat(result).noneMatch(line -> line.startsWith("at org.junit."));
	}

	// ---- getStackTrace(throwable, withMessages, withCauses) ------------------------------------------------

	private Exception thrownWithCauseAndSuppressed() {
		RuntimeException ex = new RuntimeException("top-MSGA", new IllegalStateException("cause-MSGB"));
		ex.addSuppressed(new IllegalArgumentException("suppressed-MSGC"));
		return ex;
	}

	@Test
	void getStackTraceFlags_messagesAndCauses_sameAsDefaultGetStackTrace() throws Exception {
		for (Runnable r : List.<Runnable>of(
			() -> new TestClassForExceptions().doException(),
			() -> new TestClassForExceptions().doExceptionWithCause(),
			() -> new TestClassForExceptions().doExceptionWithSuppressed())) {
			Exception exception = null;
			try {
				r.run();
			} catch (Exception ex) {
				exception = ex;
			}

			assertThat(removeLineNumbers(LogUtils.getStackTrace(exception, true, true)))
				.isEqualTo(removeLineNumbers(LogUtils.getStackTrace(exception)));
		}
	}

	@Test
	void getStackTraceFlags_messagesWithoutCauses_headerHasMessageAndNoCauseOrSuppressedLines() {
		List<String> trace = LogUtils.getStackTrace(thrownWithCauseAndSuppressed(), true, false);

		assertThat(trace.get(0)).isEqualTo("java.lang.RuntimeException: top-MSGA");
		assertThat(trace).noneMatch(l -> l.startsWith("Caused by") || l.startsWith("Suppressed"));
		assertThat(String.join("\n", trace)).doesNotContain("MSGB").doesNotContain("MSGC");
	}

	@Test
	void getStackTraceFlags_causesWithoutMessages_classNamesOnlyButCausesAndSuppressedPresent() {
		List<String> trace = LogUtils.getStackTrace(thrownWithCauseAndSuppressed(), false, true);

		assertThat(trace).contains("java.lang.RuntimeException", "Caused by: java.lang.IllegalStateException",
			"Suppressed: java.lang.IllegalArgumentException");
		assertThat(String.join("\n", trace)).doesNotContain("MSG");
	}

	@Test
	void getStackTraceFlags_neitherMessagesNorCauses_exceptionClassAndFramesOnly() {
		List<String> trace = LogUtils.getStackTrace(thrownWithCauseAndSuppressed(), false, false);

		assertThat(trace.get(0)).isEqualTo("java.lang.RuntimeException");
		assertThat(trace.subList(1, trace.size())).allMatch(l -> l.startsWith("at "));
		assertThat(String.join("\n", trace)).doesNotContain("MSG").doesNotContain("IllegalStateException");
	}

	@Test
	void getStackTraceFlags_wrapperWhoseMessageIsTheCause_causeTextNotLeakedWithoutCauses() {
		RuntimeException wrapper = new RuntimeException(new IllegalStateException("cause-MSGB"));
		assertThat(wrapper.getMessage()).contains("MSGB");    // the JDK derives the wrapper's message from the cause

		List<String> withoutCauses = LogUtils.getStackTrace(wrapper, true, false);
		List<String> withCauses = LogUtils.getStackTrace(wrapper, true, true);

		assertThat(withoutCauses.get(0)).isEqualTo("java.lang.RuntimeException");
		assertThat(String.join("\n", withoutCauses)).doesNotContain("MSGB");
		assertThat(String.join("\n", withCauses)).contains("Caused by: java.lang.IllegalStateException: cause-MSGB");
	}

	@Test
	void testPropagateToMDCAllFields() {
		// when
		when(mockRequest.getAttribute(REQUEST_ID_ATTRIBUTE)).thenReturn("X");
		when(mockRequest.getAttribute(CORRELATION_ID)).thenReturn("X");
		when(mockRequest.getAttribute(TYPE)).thenReturn("X");
		when(mockRequest.getAttribute(OPERATION)).thenReturn("X");
		when(mockRequest.getRemotePort()).thenReturn(8088);
		when(mockRequest.getRemoteUser()).thenReturn("User");
		when(mockRequest.getMethod()).thenReturn("GET");
		when(mockRequest.getRequestURI()).thenReturn("/hello");
		when(mockRequest.getQueryString()).thenReturn("?q=1");
		when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");

		SpringLogUtils.propagateToMDC(mockRequest, Optional.empty());

		// then
		Map<String, String> context = MDC.getCopyOfContextMap();
		assertThat(context).containsKey(WebMDCField.REQUEST_ID.getFieldName())
			.containsKey(WebMDCField.CORRELATION_ID.getFieldName())
			.containsKey(WebMDCField.TYPE.getFieldName())
			.containsKey(WebMDCField.OPERATION.getFieldName())
			.containsKey(WebMDCField.USER_ID.getFieldName())
			.containsKey(WebMDCField.HTTP_METHOD.getFieldName())
			.containsKey(WebMDCField.URL.getFieldName())
			.containsKey(WebMDCField.REQUEST_PARAMS.getFieldName())
			.containsKey(WebMDCField.REMOTE_IP.getFieldName())
			.containsKey(WebMDCField.PORT.getFieldName());
	}

	@Test
	void testPropagateToMDC1() {
		given(mockRequest.getRemoteUser()).willReturn(TEST_USER);
		SpringLogUtils.propagateToMDC(mockRequest, Optional.empty());

		assertThat(MDC.get(WebMDCField.USER_ID.getFieldName())).isEqualTo(TEST_USER);
	}

	@Test
	void testPropagateToMDC2() {
		given(mockRequest.getRemoteUser()).willReturn(TEST_USER);
		Set<String> includes = new HashSet<>();
		includes.add(WebMDCField.USER_ID.getFieldName());
		SpringLogUtils.propagateToMDC(mockRequest, Optional.of(includes));

		assertThat(MDC.get(WebMDCField.USER_ID.getFieldName())).isEqualTo(TEST_USER);
	}

	MethodParameter buildMethodParameter() throws Exception {
		return new MethodParameter(getClass().getDeclaredMethod("buildMethodParameter"), -1);
	}
}
