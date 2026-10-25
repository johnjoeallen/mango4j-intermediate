package com.mastercard.commons.mango4j.crypto.caas.delegate.ssl;

import javax.net.ssl.X509KeyManager;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.TRACE;

public class ClientAliasKeyManager implements X509KeyManager {

	private System.Logger logger = System.getLogger(ClientAliasKeyManager.class.getName());

	private final X509KeyManager delegate;
	private final String clientAlias;

	public ClientAliasKeyManager(X509KeyManager delegate, String clientAlias) {
		this.delegate = delegate;
		this.clientAlias = clientAlias;
	}

	@Override
	public String[] getClientAliases(String keyType, Principal[] principals) {
		return delegate.getClientAliases(keyType, principals);
	}

	@Override
	public String chooseClientAlias(String[] keyTypes, Principal[] principals, Socket socket) {
		return clientAlias != null ? clientAlias : delegate.chooseClientAlias(keyTypes, principals, socket);
	}

	@Override
	public String[] getServerAliases(String keyType, Principal[] principals) {
		return delegate.getServerAliases(keyType, principals);
	}

	@Override
	public String chooseServerAlias(String keyType, Principal[] principals, Socket socket) {
		return delegate.chooseServerAlias(keyType, principals, socket);
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		logger.log(DEBUG, "Getting certificate chain for alias={0}", alias);
		X509Certificate[] certificates = delegate.getCertificateChain(alias);
		for (X509Certificate x509Certificate : certificates) {
			logger.log(TRACE, "issuerDN : {}, serialNumber : {}, subjectDN : {}, x509Certificate : {}",
					x509Certificate.getIssuerX500Principal(),
					x509Certificate.getSerialNumber(),
					x509Certificate.getSubjectX500Principal(),
					x509Certificate);
		}
		return certificates;
	}

	@Override
	public PrivateKey getPrivateKey(String alias) {
		return delegate.getPrivateKey(alias);
	}
}
