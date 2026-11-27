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

## 2.21.0

 * Feature/2.21.0 upgrade



## 2.19.0

 * Dependency refactor
 * Update to mango core libs 2.19.0



## 2.18.0

 * Memory leak fix



## 2.17.0

 * Bugfix/circular ref memory leak fix



## 2.14.0

 * Replace fresh() with copy*()
 * Documentation to markdown



## 2.11.0

 * Feature/add equals hashcode offset limit pageable



## 2.10.0

 * Added new mango4j-spring-data module
 * Spring version upgrade to 3.5.5



## 2.9.0

 * Feature/aop updates



## 2.8.0

 * Updated to remove DiagnosticContext from populators



## 2.7.0

 * Update dependencies



## 2.6.0

 * More methods moved to LogContext



## 2.5.0

 * Fix LogContext.put() to match DiagnosticContext.put()



## 2.4.0

 * Fixed module name



## 2.3.0

 * Updated to latest mango4j-crypto-core release
 * Added the concept of the Root Diagnostic context



## 2.2.0

 * New LogProjectionMapper, maps data from internal pojo to pojo implementing LogProjection<>



## 2.1.0

 * Fix typo getFeldName() 0> getFieldName()
 * Fix Spring Boot 3 Auto-Configuration Compatibility and Add Log Exposure Configuration Support
 * Internal code and quality improvements
 * Removed unneeded dependency on mango4j-validation



## 2.0.0

 * Updated mango4j deps to 2.0.0 :)
 * Latest mango4j
 * Java17 Spring 3
 * Java17/Spring3 upgrade



## 1.4.0

 * BB links in docs has been updated



## 1.3.0

 * masking null values
 * consumers
 * More documentation



## 1.2.0

 * Don't LoggerMapper.toLoggable(arg) args before putting in the parameters map


## 1.1.0

 * Fix correlationid generation
 * Fix masked logging generic map


## 1.0.0

* Initial aop
* Initial insights
* Initial logging
* Backport all old style mango libraries