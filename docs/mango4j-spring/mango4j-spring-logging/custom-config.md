# mango4j-spring-logging - Custom Configuration

What happens now?

mango4j-spring-logging allows for custom configuration.

The base configuration allows for the following beans to be overriden or provided:

## LoggerAdapter

In order to customize the looger adapter used, you can create a `@Configuration` class and provide a custom logger adapter to be used.

```java language=java
@Configuration
public class MangoLoggingConfig {

	@Bean
	public LoggerAdapter loggerAdapter() {
		return new CustomLoggerAdapter();
	}

}
```

Where `CustomLoggerAdapter` is a custom implementation of the `LoggerAdapter` interface.

By default, the `MapLoggerAdapter` is used, and a default "string" `DefaultLoggerAdapter` is also available within the library.