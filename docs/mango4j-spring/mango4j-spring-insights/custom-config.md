# mango4j-spring-insights - Custom Configuration

What happens now?

mango4j-spring-insights allows for custom configuration.

The base configuration allows for the following beans to be overridden or provided:

## ContextInitialisers

To enable the default log file publisher you need to set the following property in the application.yml
```yaml language=yaml
mango:
 insights:
  publishers:
   log:
    enabled:
```

Applications can initialise a context by providing context initialisers, this will be called by the insights framework when an new insight is created.
```java language=java
@Component
@Order(10) // optional order annoatation to control the order initialisers are executed in
public class CustomInsightsContextInitialiser implements InsightsContextInitialiser {

	public static final String TECH_ASSET_ID = "bc7c480d-1bca-32fb-9b85-b40db4cfec4b";

	@Override
	public void initialise(AppInsight insight) {
		insight.setServiceId(TECH_ASSET_ID);
		insight.setUser(MDC.get(USER_IDENTITY_ID_MDC_KEY));
	}
}
```

Where `CustomInsightsContextInitialiser` is a custom implementation of the `InsightsContextInitialiser` interface.

A default context initialiser is provided by the framework and has the lowest execution order (i.e. runs first).