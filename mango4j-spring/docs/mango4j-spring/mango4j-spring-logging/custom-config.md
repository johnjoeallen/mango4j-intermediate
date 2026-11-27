# mango4j-spring-logging - Custom Configuration

What happens now?

mango4j-spring-logging allows for custom configuration.

The base configuration allows for the following beans to be overriden or provided:

## LoggerAdapter

In order to customize the looger adapter used, you can create a `@Configuration` class and provide a custom logger adapter to be used.

```java
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

## ExceptionLogLevelStrategy

The log level used for an exception is chosen by an `ExceptionLogLevelStrategy`. The default,
`DefaultExceptionLogLevelStrategy`, uses `ERROR` for `RuntimeException`s and `WARN` for other exceptions, `INFO` for
common request validation failures, honours `@LoggableLevel` on the exception class, and uses `ERROR` when
`forceErrorOnException` is set on the method. Provide your own bean to change that:

```java
@Configuration
public class MangoLoggingConfig {

	@Bean
	@Primary
	public ExceptionLogLevelStrategy exceptionLogLevelStrategy() {
		return (throwable, methodInfo) -> throwable instanceof NotFoundException ? Level.INFO : Level.ERROR;
	}

}
```

!!! note "@Primary"
    `DefaultExceptionLogLevelStrategy` is always registered, so a bean of your own needs `@Primary` to be the one used.
    To keep most of the defaults, extend `DefaultExceptionLogLevelStrategy` and override `getLogLevel(Throwable)`.

## ExceptionDetailStrategy

What is logged about an exception (the reason, the stack trace, the cause chain, and so on) can be set for the
whole application with an `ExceptionDetailStrategy` bean. There is none by default, so each method's
`logException` applies.

```java
@Configuration
public class MangoLoggingConfig {

	@Bean
	public ExceptionDetailStrategy exceptionDetailStrategy() {
		return ExceptionDetailRules.builder()
			.on(NotFoundException.class)                          // log nothing
			.on(ValidationException.class, REASON, CLASS_NAME)
			.build();
	}

}
```

See [exception logging](exception-logging.md#global-rules) for how the rules combine with `@OnException` and
`logException`.
