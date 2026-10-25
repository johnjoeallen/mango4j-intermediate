# mango4j-crypto-caas-delegate
## Extension Plugin to easily support CaaS encryption in Mastercard applications

### Steps to use

#### Preconditions
Make sure you've followed the steps [documented here](https://confluence.mastercard.int/spaces/MANGO/pages/1181005587/mango4j-crypto+-+Documentation) to onboard your application into the mango4j-crypto framework.

#### Step 1
Add this dependency to your application POM file
~~~xml
    <dependency>
        <groupId>com.mastercard.commons.mango</groupId>
        <artifactId>mango4j-spring-crypto-caas-delegate</artifactId>
        <version>2.4.0</version>
    </dependency>
~~~

#### Step 2
To enable encryption using Mango4j with CaaS, configure a CaasDelegateConfig with values appropriate for your environment. These are typically sourced from your application properties.
    **Required**: caas.endpoint.base
    **Optional**: Other properties have sensible defaults but can be overridden if needed.

Depending on your use case, Mango4j supports two types of encryption delegates:
    **CaasBasicEncryptionServiceDelegate** — for basic CaaS
    **CaasAdvancedEncryptionServiceDelegate** — for advanced CaaS


**Basic CaaS Configuration**
```Java
@Configuration
public class MangoConfig {

	@Value("${caas.endpoint.base}")
	private String caasEndpointBase;

	@Bean
	public CaasBasicEncryptionServiceDelegate caasBasicEncryptionServiceDelegate() {
		return new CaasBasicEncryptionServiceDelegate(caasDelegateConfig);
	}

	@Bean
	public CaasDelegateConfig caasDelegateConfig() {
		CaasDelegateConfig caasDelegateConfig = new CaasDelegateConfig();
		caasDelegateConfig.setCaasEndpointBase(caasEndpointBase);
		return caasDelegateConfig;
	}

	@Bean
	public CryptoKeyProvider mango4jCryptoKeyProvider() {
		// Your implementation of the CryptoKeyProvider interface as documented in the official mango4j-crypto documentation
		return new MyApplicationCryptoKeyProvider();
	}

	@Bean
	public CryptoShield cryptoShield(CaasBasicEncryptionServiceDelegate caasBasicEncryptionServiceDelegate, CryptoKeyProvider mango4jCryptoKeyProvider) {
        // Include your configured caasBasicEncryptionServiceDelegate in CryptoShield's list of EncryptionServiceDelegates 
		return new CryptoShield(List.of(MyHighlyConfidentialEntity.class),
				mango4jCryptoKeyProvider,
				List.of(caasBasicEncryptionServiceDelegate));
	}
}
```

**Advanced CaaS Configuration**
```Java
@Configuration
public class MangoConfig {

	@Value("${caas.endpoint.base}")
	private String caasEndpointBase;

	@Bean
	public CaasAdvancedEncryptionServiceDelegate caasAdvancedEncryptionServiceDelegate(CaasDelegateConfig caasDelegateConfig) {
		return new CaasAdvancedEncryptionServiceDelegate(caasDelegateConfig);
	}

	@Bean
	public CaasDelegateConfig caasDelegateConfig() {
		CaasDelegateConfig caasDelegateConfig = new CaasDelegateConfig();
		caasDelegateConfig.setCaasEndpointBase(caasEndpointBase);
		return caasDelegateConfig;
	}

	@Bean
	public CryptoKeyProvider mango4jCryptoKeyProvider() {
		// Your implementation of the CryptoKeyProvider interface as documented in the official mango4j-crypto documentation
		return new MyApplicationCryptoKeyProvider();
	}

	@Bean
	public CryptoShield cryptoShield(CaasAdvancedEncryptionServiceDelegate caasAdvancedEncryptionServiceDelegate, CryptoKeyProvider mango4jCryptoKeyProvider) {
        // Include your configured caasAdvancedEncryptionServiceDelegate in CryptoShield's list of EncryptionServiceDelegates 
		return new CryptoShield(List.of(MyHighlyConfidentialEntity.class),
				mango4jCryptoKeyProvider,
				List.of(caasAdvancedEncryptionServiceDelegate));
	}
}
```

Now CryptoShield is configured to work seamlessly with CaaS (in addition to any other encryption service delegates you might already use). No other application code changes are necessary.

#### Step 3
Create whatever CryptoKey Definitions in your application that you need to work with this delegate. You need to define caas CryptoKeys in your application with the following values (we recommend you store your CryptoKey information in your database or similar runtime dynamic platform for seamless key rotation):

```json
{
  "id" : "<SOME_UNIQUE_UUID>",
  "type" : "CAAS_BASIC", // CAAS_ADVANCED
  "usage" : "ENCRYPTION",
  "createdDate" : "<DATE_THIS_CRYPTO_KEY_WAS_CREATED_IN_YOUR_APPLICATION>",
  "config" : {
    "keyAlias" : "<YOUR_CAAS_KEY_ALIAS>"
  }
}
```
(And for HMAC key follow a similar definition but with "usage" as "HMAC")

Upcoming feature support to include:
* Support for single batch call for encryption of entity which has both encrypted data and HMAC, rather than 2 separate calls to CaaS as happens currently.
* Remove spring-web dependency and move this module from the current mango4j-spring repository to the mango4j repository to avoid potential Spring version clashes with applications.
* Remove httpclient5 dependency and explore using native JDK HTTP API to reduce runtime dependencies to 0.