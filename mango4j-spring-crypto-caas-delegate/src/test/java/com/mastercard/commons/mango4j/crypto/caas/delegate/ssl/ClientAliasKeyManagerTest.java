package com.mastercard.commons.mango4j.crypto.caas.delegate.ssl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.TRACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ClientAliasKeyManagerTest {

	private static final String TEST_CLIENT_ALIAS = "TEST_CLIENT_ALIAS";
	private static final String TEST_SERVER_ALIAS = "TEST_SERVER_ALIAS";
	private static final String[] TEST_ALIASES = new String[]{"ALIAS0", "ALIAS1"};
	private static final String TEST_KEY_TYPE = "TEST_KEY_TYPE";
	private static final String[] TEST_KEY_TYPES = new String[]{TEST_KEY_TYPE};
	private static final Principal[] TEST_PRINCIPALS = new Principal[0];

	private ClientAliasKeyManager keyManager;

	@Mock
	private X509KeyManager mockDelegate;

	@Mock
	private Socket mockSocket;

	@Mock
	private X509Certificate mockCert;

	@Mock
	private PrivateKey mockPrivateKey;

	@Mock
	private System.Logger mockLogger;

	@BeforeEach
	void setUp() {
		keyManager = new ClientAliasKeyManager(mockDelegate, TEST_CLIENT_ALIAS);
		ReflectionTestUtils.setField(keyManager, "logger", mockLogger);
	}

	@Test
	void getClientAliases() {
		given(mockDelegate.getClientAliases(TEST_KEY_TYPE, TEST_PRINCIPALS)).willReturn(TEST_ALIASES);

		String[] actual = keyManager.getClientAliases(TEST_KEY_TYPE, TEST_PRINCIPALS);

		assertThat(actual).isEqualTo(TEST_ALIASES);
	}

	@Test
	void chooseClientAliasExists() {
		String actual = keyManager.chooseClientAlias(TEST_KEY_TYPES, TEST_PRINCIPALS, mockSocket);

		assertThat(actual).isEqualTo(TEST_CLIENT_ALIAS);
	}

	@Test
	void chooseClientAliasDoesNotExist() {
		keyManager = new ClientAliasKeyManager(mockDelegate, null);

		String actual = keyManager.chooseClientAlias(TEST_KEY_TYPES, TEST_PRINCIPALS, mockSocket);

		assertThat(actual).isNull();
	}

	@Test
	void getServerAliases() {
		given(mockDelegate.getServerAliases(TEST_KEY_TYPE, TEST_PRINCIPALS)).willReturn(TEST_ALIASES);

		String[] actual = keyManager.getServerAliases(TEST_KEY_TYPE, TEST_PRINCIPALS);

		assertThat(actual).isEqualTo(TEST_ALIASES);
	}

	@Test
	void chooseServerAlias() {
		given(mockDelegate.chooseServerAlias(TEST_KEY_TYPE, TEST_PRINCIPALS, mockSocket)).willReturn(TEST_SERVER_ALIAS);

		String actual = keyManager.chooseServerAlias(TEST_KEY_TYPE, TEST_PRINCIPALS, mockSocket);

		assertThat(actual).isEqualTo(TEST_SERVER_ALIAS);
	}

	@Test
	void getCertificateChain() {
		X509Certificate[] testCerts = new X509Certificate[]{mockCert};
		given(mockDelegate.getCertificateChain(TEST_SERVER_ALIAS)).willReturn(testCerts);

		X509Certificate[] actual = keyManager.getCertificateChain(TEST_SERVER_ALIAS);

		assertThat(actual).isEqualTo(testCerts);
		then(mockLogger).should().log(DEBUG, "Getting certificate chain for alias={0}", TEST_SERVER_ALIAS);
		then(mockLogger).should().log(TRACE, "issuerDN : {}, serialNumber : {}, subjectDN : {}, x509Certificate : {}",
				mockCert.getIssuerX500Principal(),
				mockCert.getSerialNumber(),
				mockCert.getSubjectX500Principal(),
				mockCert);
		then(mockLogger).shouldHaveNoMoreInteractions();
	}

	@Test
	void getPrivateKey() {
		given(mockDelegate.getPrivateKey(TEST_CLIENT_ALIAS)).willReturn(mockPrivateKey);

		PrivateKey actual = keyManager.getPrivateKey(TEST_CLIENT_ALIAS);

		assertThat(actual).isEqualTo(mockPrivateKey);
	}
}