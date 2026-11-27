# mango4j-spring - Change log

## 4.0.0

 * Java 25, Spring Boot 4.1, Jackson 3
 * Moved into the `mango4j` repository and released with the core modules at one version
 * Exception logging is configurable: `@LoggableMethod.logException` is now an array of `ExceptionDetail`
   (`REASON`, `CLASS_NAME`, `MESSAGE`, `CAUSE`, `EXCEPTION`, `STACK_TRACE`), with the new `@OnException` per
   exception type and a global `ExceptionDetailStrategy` bean. See [exception logging](mango4j-spring-logging/exception-logging.md)
 * An exception is logged once, at the method that threw it; a parent that only propagates it does not log it again
   unless it has a matching `@OnException`
 * **Breaking:** `logException = true|false` is replaced by the array (`false` is `{}`); `stacktrace` is now an object
   (`exception`, `message`, `frames`, `cause`) instead of a list of lines; the fields of a `@LoggableClass` exception are
   under `exception` instead of `reason`; exceptions are recorded on the method that threw them, not on the root method
