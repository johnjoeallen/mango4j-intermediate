package com.mastercard.commons.mango.insights;

import com.mastercard.commons.mango.insights.internal.AppInsight;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AppInsightTest {

	private AppInsight appInsight;

	@BeforeEach
	void setUp() {
		appInsight = new AppInsight();
	}

	@Test
	void setException() {
		// when
		appInsight.setException(new RuntimeException("test"));

		// then
		assertThat(appInsight.getException()).isNotEmpty();
		assertThat(appInsight.getException().get(0).getName()).isEqualTo("RuntimeException");
		assertThat(appInsight.getException().get(0).getMessage()).isEqualTo("test");
	}
}