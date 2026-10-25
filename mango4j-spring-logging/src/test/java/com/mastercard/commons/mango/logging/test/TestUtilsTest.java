package com.mastercard.commons.mango.logging.test;

import com.mastercard.commons.mango.logging.dc.DiagnosticContext;
import com.mastercard.commons.mango.logging.dc.DiagnosticContextThreadLocal;
import com.mastercard.commons.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TestUtilsTest {
	class TestObject {
		public void helloWorld() {
			// do nothing
		}
	}

	@BeforeEach
	public void setup() throws Throwable {
		MDCTestUtils.init();
		DiagnosticContextThreadLocal.unset();
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void fakeLoggableNoMethodTest() {
		NoSuchMethodException thrown = assertThrows(NoSuchMethodException.class, () -> {
			TestUtils.fakeLoggable(new Object(), "method");
		});

		assertThat(thrown.getMessage()).isEqualTo("java.lang.Object.method()");
	}

	@Test
	void fakeLoggableTest() throws NoSuchMethodException {
		try (MockedConstruction<DiagnosticContext> mockedConstruction =
				 Mockito.mockConstruction(DiagnosticContext.class)) {
			TestUtils.fakeLoggable(new TestObject(), "helloWorld");
			assertThat(mockedConstruction.constructed()).hasSize(1);
		}
	}

	@Test
	void fakeLoggable() {
		Method doesNotExist = TestUtils.getFakeLoggableMethod("doesNotExist");

		assertThat(doesNotExist).isNull();
	}
}