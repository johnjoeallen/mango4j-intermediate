# Diagnostic Lifecycle Logging - Complete Guide

**Component:** mango4j-spring-logging  
**Version:** 2.17.0-SNAPSHOT  
**Date:** December 11, 2025

---

## 📚 Table of Contents

1. [Quick Reference](#quick-reference)
2. [Overview](#overview)
3. [Spring Boot Auto-Configuration](#spring-boot-auto-configuration)
4. [Configuration Guide](#configuration-guide)
5. [What Gets Logged](#what-gets-logged)
6. [Usage Examples](#usage-examples)
7. [Production Considerations](#production-considerations)
8. [Troubleshooting](#troubleshooting)
9. [API Reference](#api-reference)
10. [Best Practices](#best-practices)

---

# Quick Reference

## Enable/Disable Logging

```java
import ie.bitstep.mango.spring.logging.config.DiagnosticLifecycleLogger;

// Enable general lifecycle logging
DiagnosticLifecycleLogger.setEnabled(true);

// Enable thread-local logging
DiagnosticLifecycleLogger.setThreadlocalEnabled(true);

// Set max children limit (default: 100)
DiagnosticLifecycleLogger.setMaxChildren(50);

// Disable all diagnostic logging
DiagnosticLifecycleLogger.setEnabled(false);
DiagnosticLifecycleLogger.setThreadlocalEnabled(false);

// Check status
boolean enabled = DiagnosticLifecycleLogger.isEnabled();
boolean tlEnabled = DiagnosticLifecycleLogger.isThreadlocalEnabled();
int maxChildren = DiagnosticLifecycleLogger.getMaxChildren();
```

## Common Use Cases

### Debug Memory Leak
```java
// Enable everything
DiagnosticLifecycleLogger.setEnabled(true);
DiagnosticLifecycleLogger.setThreadlocalEnabled(true);

// Run your code
myService.processRequest();

// Look for:
// - CREATED without matching CLEAR (complete)
// - "Acquiring threadlocal" without "Releasing threadlocal"
```

### Trace Request Flow
```java
// Enable lifecycle logging only
DiagnosticLifecycleLogger.setEnabled(true);

// Run request
myController.handleRequest(request);

// Observe PUSH/UP/CLEAR sequence
```

### Detect Excessive Branching
```java
// Set low limit to catch methods calling children in loops
DiagnosticLifecycleLogger.setMaxChildren(10);

// Run suspected code - typically a loop calling @LoggableMethod
myService.processLargeList(items);  // Calls processItem() in a loop

// Check logs for ERROR about max children exceeded
// Example: If list has 100 items, and each processItem() is a child,
// you'll get 90 warnings (children 11-100)
```

### Production Troubleshooting
```java
// Endpoint to enable temporarily
@PostMapping("/admin/diagnostic/enable")
public String enable() {
    DiagnosticLifecycleLogger.setEnabled(true);
    return "Enabled";
}

@PostMapping("/admin/diagnostic/disable")
public String disable() {
    DiagnosticLifecycleLogger.setEnabled(false);
    return "Disabled";
}
```

## Performance Impact

| State | Log Lines/Request | Overhead |
|-------|-------------------|----------|
| Disabled (default) | 0 | None (zero) |
| Enabled | 15-25 | Moderate I/O |
| Both Enabled | 20-30 | High I/O |

## What Gets Logged

| Event | Example Log |
|-------|-------------|
| Context Created | `DiagnosticContext CREATED: @123456789` |
| Method Push | `PUSH (MethodInfo): Method: MyService.process` |
| Context Up | `UP (before): Current: @987654 │ Parent: @123456` |
| Context Clear | `CLEAR (complete): @123456789` |
| ThreadLocal Acquire | `Acquiring threadlocal for thread main` |
| ThreadLocal Release | `Releasing threadlocal for thread main` |
| Max Children Exceeded | `DiagnosticContextItem max children limit exceeded: @123456789 │ Current: 101 │ Max: 100` |

---

# Overview

The mango4j-spring-logging component includes **configurable diagnostic lifecycle logging** that helps developers debug and troubleshoot diagnostic context lifecycle issues, memory leaks, and thread-local management problems.

## Key Features

- ✅ **Zero overhead by default** - Disabled when not needed
- ✅ **Runtime configurable** - No restart required
- ✅ **Separate controls** - General and thread-local logging
- ✅ **Comprehensive tracking** - All lifecycle events logged
- ✅ **Object identity tracking** - Memory leak debugging
- ✅ **Spring Boot integration** - Auto-configuration support
- ✅ **Production ready** - Designed for safe production use

## What Problems Does It Solve?

### 1. **Memory Leak Detection**
```
Problem: DiagnosticContext instances not being cleaned up
Solution: Log shows CREATED without matching CLEAR (complete)
```

### 2. **Thread-Local Leaks**
```
Problem: ThreadLocal not released in thread pools
Solution: Log shows "Acquiring" without "Releasing"
```

### 3. **Excessive Method Branching**
```
Problem: Single method spawning too many child method calls
Solution: ERROR logged when DiagnosticContextItem exceeds max children limit
```

### 4. **Request Flow Understanding**
```
Problem: Complex nested method calls hard to trace
Solution: Log shows complete PUSH/UP/CLEAR sequence
```

### 5. **Debugging Test Failures**
```
Problem: Tests fail with "context already exists"
Solution: Log shows exact lifecycle and state
```

---

# Spring Boot Auto-Configuration

## ✅ Automatic Configuration

This module provides **automatic Spring Boot configuration** for diagnostic lifecycle logging. No manual configuration class needed!

## Quick Start

### 1. Add to `application.yml`

```yaml
mango:
  logging:
    diagnostic:
      enabled: false                  # General lifecycle logging
      threadlocal-enabled: false      # Thread-local specific logging
```

### 2. That's It!

The configuration is **automatically applied** when your Spring Boot application starts.

## What's Included

### Auto-Configuration Classes

1. **`DiagnosticLoggingAutoConfiguration`**
   - Spring Boot `@Configuration` class
   - Automatically detected and loaded
   - Initializes logging on application startup

2. **`DiagnosticLoggingProperties`**
   - `@ConfigurationProperties` for type-safe configuration
   - Binds to `mango.logging.diagnostic.*` properties
   - Validates configuration values
   - Provides defaults (both disabled)

3. **`DiagnosticLoggingInitializer`**
   - Implements `ApplicationRunner`
   - Runs after application context is loaded
   - Applies configuration to `DiagnosticLifecycleLogger`

## Configuration Options

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `mango.logging.diagnostic.enabled` | boolean | `false` | Enable general diagnostic lifecycle logging |
| `mango.logging.diagnostic.threadlocal-enabled` | boolean | `false` | Enable thread-local specific logging |
| `mango.logging.diagnostic.max-children` | int | `100` | Maximum direct children per DiagnosticContextItem before warning |

## Configuration Examples

### Development (Verbose Logging)
```yaml
mango:
  logging:
    diagnostic:
      enabled: true
      threadlocal-enabled: true
      max-children: 50  # Lower limit to catch issues earlier
```

### Production (Default - Disabled)
```yaml
mango:
  logging:
    diagnostic:
      enabled: false
      threadlocal-enabled: false
      max-children: 100  # Still monitors for excessive method branching
```

### Troubleshooting (Enable Temporarily)
```yaml
mango:
  logging:
    diagnostic:
      enabled: true           # Enable general logging
      threadlocal-enabled: false  # Keep thread-local disabled
```

## Environment Variables

You can also configure via **environment variables** (useful for production):

```bash
# Enable diagnostic logging
export MANGO_LOGGING_DIAGNOSTIC_ENABLED=true

# Enable thread-local logging
export MANGO_LOGGING_DIAGNOSTIC_THREADLOCAL_ENABLED=true
```

**Spring Boot automatically converts:**
- `mango.logging.diagnostic.enabled` → `MANGO_LOGGING_DIAGNOSTIC_ENABLED`
- Dots (`.`) → Underscores (`_`)
- Lowercase → Uppercase

## How It Works

### Startup Sequence

1. **Spring Boot starts** your application
2. **Auto-configuration detected** via `@Configuration`
3. **Properties loaded** from `application.yml` or environment
4. **Properties bound** to `DiagnosticLoggingProperties`
5. **ApplicationRunner executes** after context initialization
6. **DiagnosticLifecycleLogger configured** with properties
7. **Logging enabled** (if configured)

### Initialization Log

When your application starts, you'll see:

```
INFO  c.m.c.m.l.c.DiagnosticLoggingAutoConfiguration - 
  Diagnostic lifecycle logging initialized - enabled: true, threadlocal-enabled: false
```

## Runtime Configuration Changes

While the auto-configuration runs at startup, you can still change settings at runtime:

```java
@RestController
@RequestMapping("/admin/diagnostic")
public class DiagnosticController {
    
    @PostMapping("/enable")
    public String enable() {
        DiagnosticLifecycleLogger.setEnabled(true);
        return "Enabled";
    }
    
    @PostMapping("/disable")
    public String disable() {
        DiagnosticLifecycleLogger.setEnabled(false);
        return "Disabled";
    }
    
    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of(
            "enabled", DiagnosticLifecycleLogger.isEnabled(),
            "threadlocalEnabled", DiagnosticLifecycleLogger.isThreadlocalEnabled()
        );
    }
}
```

---

# Configuration Guide

## Configuration Options

### 1. General Lifecycle Logging

Controls logging for all diagnostic context lifecycle events.

**Default:** `false` (disabled)

**Enable:**
```java
DiagnosticLifecycleLogger.setEnabled(true);
```

**What it logs:**
- DiagnosticContext creation
- Context push/pop operations
- Context up/down navigation
- Context clearing and cleanup
- DiagnosticContextItem lifecycle
- Method entry/exit tracking

**Example output:**
```
INFO DiagnosticContext - DiagnosticContext CREATED: @123456789
INFO DiagnosticContext - DiagnosticContext PUSH (MethodInfo): @123456789 | Method: MyService.processRequest | Current: null | isFinal: N/A
INFO DiagnosticContext - DiagnosticContext UP (before): @123456789 | Current: @987654321 | Parent: null
INFO DiagnosticContext - DiagnosticContext CLEAR (complete): @123456789
```

### 2. Thread-Local Specific Logging

Controls logging specifically for thread-local acquisition and release.

**Default:** `false` (disabled)

**Enable:**
```java
DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
```

**What it logs:**
- Thread-local acquisition (first access per thread)
- Thread-local release (cleanup)
- Thread identification

**Example output:**
```
INFO DiagnosticContextThreadLocal - Acquiring threadlocal for thread main
INFO DiagnosticContextThreadLocal - Releasing threadlocal for thread main
INFO DiagnosticContextThreadLocal - Acquiring threadlocal for thread pool-1-thread-1
```

### 3. Max Children Monitoring

Controls the maximum number of **direct children** allowed per DiagnosticContextItem before logging a warning.

**Default:** `100`

**Configure:**
```java
DiagnosticLifecycleLogger.setMaxChildren(50);
```

**What it monitors:**
- Number of **direct children** added to each DiagnosticContextItem
- Detects when a single method calls too many child methods
- Warns about excessive method branching/fan-out
- Identifies potential performance bottlenecks

**Common Scenario:** This typically occurs when a parent method calls a child method **inside a loop**:
```java
@LoggableMethod
public void processItems(List<Item> items) {
    for (Item item : items) {
        processItem(item);  // Each call creates a new child
    }
    // If items.size() > maxChildren, warning is logged
}
```

**When limit exceeded:**
```
ERROR DiagnosticContextItem - DiagnosticContextItem max children limit exceeded: @123456789 | Current: 101 | Max: 100 | Parent: @987654321 | Method: MyService.processItems
```

**Note:** This is a **soft limit** - children are still added even when the limit is exceeded, but an ERROR is logged to alert you to the issue.

**Important:** This counts **direct children only**, not nesting depth. Each iteration of a loop calling a @LoggableMethod creates a new child. Deep nesting without many siblings won't trigger this warning.

**Use cases:**
- **Development:** Set lower limit (e.g., 50) to catch unusual branching patterns
- **Production:** Use default (100) or higher for real-world scenarios  
- **Performance analysis:** Identify methods with excessive fan-out

### 4. Combined Configuration

For comprehensive debugging, enable all features:

```java
// Enable everything for maximum visibility
DiagnosticLifecycleLogger.setEnabled(true);
DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
DiagnosticLifecycleLogger.setMaxChildren(50); // Lower limit for development
```

---

# What Gets Logged

## DiagnosticContext Lifecycle

When `enabled = true`:

| Event | Log Message | Object Info |
|-------|-------------|-------------|
| Creation | `DiagnosticContext CREATED: @{hash}` | Context identity |
| Push (MethodInfo) | `PUSH (MethodInfo): @{context} │ Method: {class}.{method}` | Context, method, current state |
| Push (Item) | `PUSH (Item): @{context} │ Item: @{item} │ Root: @{root}` | All object identities |
| Pop | `POP: @{context} │ Item: @{item}` | Context and item being popped |
| Up | `UP (before/after): @{context} │ Current: @{current} │ Parent: @{parent}` | Navigation state |
| Clear | `CLEAR (start/complete): @{context} │ Root: @{root} │ Current: @{current}` | Full cleanup state |

## DiagnosticContextItem Lifecycle

When `enabled = true`:

| Event | Log Message | Info |
|-------|-------------|------|
| Creation | `DiagnosticContextItem CREATED: @{hash}` | Item identity, parent, method |
| Clearing | `DiagnosticContextItem CLEARING: @{hash}` | Parent, children count, method |
| Cleared | `DiagnosticContextItem CLEARED: @{hash}` | Confirmation |

## Thread-Local Management

When `threadlocalEnabled = true`:

| Event | Log Message | Info |
|-------|-------------|------|
| Acquisition | `Acquiring threadlocal for thread {name}` | Thread name |
| Release | `Releasing threadlocal for thread {name}` | Thread name |

---

# Usage Examples

## Example 1: Debug Memory Leak

**Scenario:** Suspected memory leak with DiagnosticContext not being cleaned up.

**Configuration:**
```java
// Enable full logging
DiagnosticLifecycleLogger.setEnabled(true);
DiagnosticLifecycleLogger.setThreadlocalEnabled(true);

// Run your application
myService.processRequest();

// Analyze logs for:
// - Contexts created but not cleared
// - Circular references (same @hash appearing multiple times)
// - Thread-locals not released
```

**What to look for:**
```
✅ Good: Every CREATED has a matching CLEAR (complete)
❌ Bad: CREATED without CLEAR (complete) = memory leak

✅ Good: Every "Acquiring threadlocal" has "Releasing threadlocal"
❌ Bad: Acquiring without Releasing = thread-local leak
```

## Example 2: Trace Request Flow

**Scenario:** Understanding the diagnostic context hierarchy for a complex request.

**Configuration:**
```java
DiagnosticLifecycleLogger.setEnabled(true);

// Your request processing
myController.handleRequest(request);
```

**Expected log pattern:**
```
INFO - DiagnosticContext CREATED: @12345
INFO - DiagnosticContext PUSH: Method: Controller.handleRequest
INFO - DiagnosticContext PUSH: Method: Service.processData
INFO - DiagnosticContext PUSH: Method: Repository.saveData
INFO - DiagnosticContext UP: Left: @67890 | Current: @54321
INFO - DiagnosticContext UP: Left: @54321 | Current: @12345
INFO - DiagnosticContext UP reached root, calling clear()
INFO - DiagnosticContext CLEAR (complete): @12345
```

## Example 3: Production Troubleshooting

**Scenario:** Intermittent issue in production that needs investigation.

**Step 1: Enable logging temporarily**
```java
// In your diagnostic endpoint or JMX bean
@PostMapping("/admin/diagnostic/enable")
public String enableDiagnosticLogging() {
    DiagnosticLifecycleLogger.setEnabled(true);
    return "Diagnostic logging enabled";
}

@PostMapping("/admin/diagnostic/disable")
public String disableDiagnosticLogging() {
    DiagnosticLifecycleLogger.setEnabled(false);
    return "Diagnostic logging disabled";
}
```

**Step 2: Monitor logs**
```bash
# Watch for lifecycle events
tail -f application.log | grep "DiagnosticContext"
```

**Step 3: Disable when done**
```bash
curl -X POST http://localhost:8080/admin/diagnostic/disable
```

## Example 4: Unit Test Debugging

**Scenario:** Test failures related to diagnostic context state.

**Configuration:**
```java
@BeforeEach
void setUp() {
    // Enable for this test
    DiagnosticLifecycleLogger.setEnabled(true);
    DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
}

@AfterEach
void tearDown() {
    // Disable after test
    DiagnosticLifecycleLogger.setEnabled(false);
    DiagnosticLifecycleLogger.setThreadlocalEnabled(false);
}

@Test
void testComplexFlow() {
    // Your test - logs will show exact lifecycle
    myService.complexOperation();
}
```

## Example 5: Detecting Loop-Based Child Creation

**Scenario:** A parent method calls a child method in a loop, creating many children.

**Code:**
```java
@Service
public class OrderService {
    
    @LoggableMethod
    public void processOrders(List<Order> orders) {
        for (Order order : orders) {
            processOrder(order);  // Each iteration creates a new child
        }
    }
    
    @LoggableMethod
    private void processOrder(Order order) {
        // Process single order
    }
}
```

**Configuration:**
```java
// Set a reasonable limit for development
DiagnosticLifecycleLogger.setMaxChildren(50);

// Test with large list
List<Order> orders = createOrders(100);  // 100 orders
orderService.processOrders(orders);
```

**Expected logs:**
```
INFO  DiagnosticContext - DiagnosticContext PUSH: Method: OrderService.processOrders
INFO  DiagnosticContext - DiagnosticContext PUSH: Method: OrderService.processOrder  # 1st child
INFO  DiagnosticContext - DiagnosticContext UP
INFO  DiagnosticContext - DiagnosticContext PUSH: Method: OrderService.processOrder  # 2nd child
INFO  DiagnosticContext - DiagnosticContext UP
...
INFO  DiagnosticContext - DiagnosticContext PUSH: Method: OrderService.processOrder  # 50th child
INFO  DiagnosticContext - DiagnosticContext UP
ERROR DiagnosticContextItem - DiagnosticContextItem max children limit exceeded: @12345 | Current: 51 | Max: 50 | Parent: null | Method: OrderService.processOrders
INFO  DiagnosticContext - DiagnosticContext PUSH: Method: OrderService.processOrder  # 51st child
...
ERROR DiagnosticContextItem - DiagnosticContextItem max children limit exceeded: @12345 | Current: 100 | Max: 50 | Parent: null | Method: OrderService.processOrders
```

**Analysis:**
- 100 `processOrder()` calls = 100 direct children of `processOrders()`
- First 50 children added without warning
- Children 51-100 each trigger an ERROR log
- Total: 50 ERROR messages
- All children still processed (soft limit)

**Solutions:**
1. **Increase limit** if 100+ items is normal:
   ```java
   DiagnosticLifecycleLogger.setMaxChildren(200);
   ```

2. **Use batch processing** to reduce children:
   ```java
   @LoggableMethod
   public void processOrders(List<Order> orders) {
       // Process in batches - only creates children for batch calls, not individual orders
       Lists.partition(orders, 50).forEach(this::processBatch);
   }
   
   @LoggableMethod
   private void processBatch(List<Order> batch) {
       // No @LoggableMethod on processOrder = no children created
       batch.forEach(this::processOrder);
   }
   
   private void processOrder(Order order) {  // Not @LoggableMethod
       // Process order
   }
   ```

3. **Remove @LoggableMethod from inner call** if detailed logging not needed:
   ```java
   @LoggableMethod
   public void processOrders(List<Order> orders) {
       for (Order order : orders) {
           processOrder(order);  // No longer creates children
       }
   }
   
   private void processOrder(Order order) {  // @LoggableMethod removed
       // Process order
   }
   ```

---

# Production Considerations

## Performance Impact

### When Disabled (default):
- ✅ **Zero overhead** - Single boolean check, no object creation
- ✅ **No logger calls** - Conditional executes before logger invocation
- ✅ **No string concatenation** - Parameters not evaluated if disabled

### When Enabled:
- ⚠️ **Additional I/O** - Log messages written to configured appenders
- ⚠️ **String operations** - Object identity hashes calculated
- ⚠️ **Log volume** - Can generate significant log data (see below)

## Log Volume Estimates

For a typical REST API request with 3 nested service calls:

| Logging Level | Events Logged | Approx. Log Lines |
|---------------|---------------|-------------------|
| Disabled (default) | None | 0 |
| `enabled = true` | Context lifecycle only | 15-25 per request |
| Both enabled | Context + ThreadLocal | 20-30 per request |

**Calculation:**
- 1 request = ~10 DiagnosticContext events
- Each nested call = ~5 additional events
- Thread-local events = 2 per thread (acquire + release)

**For 1000 requests/second:**
- Disabled: 0 extra log lines
- Enabled: ~20,000 log lines/second
- Both enabled: ~30,000 log lines/second

## Recommendations

### Development Environment
```java
// Enable everything
DiagnosticLifecycleLogger.setEnabled(true);
DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
```

### Testing/Staging Environment
```java
// Enable based on need
DiagnosticLifecycleLogger.setEnabled(true);  // For integration testing
DiagnosticLifecycleLogger.setThreadlocalEnabled(false);  // Usually not needed
```

### Production Environment
```java
// Disabled by default
DiagnosticLifecycleLogger.setEnabled(false);
DiagnosticLifecycleLogger.setThreadlocalEnabled(false);

// Enable temporarily for troubleshooting via JMX/admin endpoint
```

## Log Configuration

### Logback (logback.xml)
```xml
<!-- Separate appender for diagnostic logs -->
<appender name="DIAGNOSTIC" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/diagnostic.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/diagnostic.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>7</maxHistory>
    </rollingPolicy>
    <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<!-- Route diagnostic logs to separate file -->
<logger name="ie.bitstep.mango.spring.logging.dc" level="INFO" additivity="false">
    <appender-ref ref="DIAGNOSTIC"/>
</logger>

<logger name="ie.bitstep.mango.spring.logging.internal.DiagnosticContextItem" level="INFO" additivity="false">
    <appender-ref ref="DIAGNOSTIC"/>
</logger>
```

### Log4j2 (log4j2.xml)
```xml
<Appenders>
    <RollingFile name="DiagnosticLog" fileName="logs/diagnostic.log"
                 filePattern="logs/diagnostic-%d{yyyy-MM-dd}.log">
        <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
        <Policies>
            <TimeBasedTriggeringPolicy/>
        </Policies>
        <DefaultRolloverStrategy max="7"/>
    </RollingFile>
</Appenders>

<Loggers>
    <Logger name="ie.bitstep.mango.spring.logging.dc" level="info" additivity="false">
        <AppenderRef ref="DiagnosticLog"/>
    </Logger>
    
    <Logger name="ie.bitstep.mango.spring.logging.internal.DiagnosticContextItem" level="info" additivity="false">
        <AppenderRef ref="DiagnosticLog"/>
    </Logger>
</Loggers>
```

---

# Troubleshooting

## Problem: Logs not appearing

### Check 1: Is logging enabled?
```java
System.out.println("Enabled: " + DiagnosticLifecycleLogger.isEnabled());
System.out.println("ThreadLocal Enabled: " + DiagnosticLifecycleLogger.isThreadlocalEnabled());
```

### Check 2: Is log level correct?
```xml
<!-- Ensure INFO level is enabled -->
<logger name="ie.bitstep.mango.spring.logging.dc" level="INFO"/>
```

### Check 3: Are you looking at the right logger?
- DiagnosticContext logs → `ie.bitstep.mango.spring.logging.dc.DiagnosticContext`
- ThreadLocal logs → `ie.bitstep.mango.spring.logging.dc.DiagnosticContextThreadLocal`
- DiagnosticContextItem logs → `ie.bitstep.mango.spring.logging.internal.DiagnosticContextItem`

## Problem: Too many logs

### Solution 1: Disable thread-local logging
```java
DiagnosticLifecycleLogger.setEnabled(true);
DiagnosticLifecycleLogger.setThreadlocalEnabled(false);  // Reduces volume
```

### Solution 2: Filter by logger
```xml
<!-- Only log DiagnosticContext, not DiagnosticContextItem -->
<logger name="ie.bitstep.mango.spring.logging.dc.DiagnosticContext" level="INFO"/>
<logger name="ie.bitstep.mango.spring.logging.internal.DiagnosticContextItem" level="WARN"/>
```

### Solution 3: Sample logging
```java
// Custom wrapper for sampled logging
public class SampledDiagnosticLogger {
    private static final AtomicLong counter = new AtomicLong(0);
    private static final int SAMPLE_RATE = 100; // Log 1 in 100
    
    public static void enableIfSampled() {
        if (counter.incrementAndGet() % SAMPLE_RATE == 0) {
            DiagnosticLifecycleLogger.setEnabled(true);
        }
    }
}
```

## Problem: Memory leak detected

### Steps to investigate:

1. **Enable full logging:**
   ```java
   DiagnosticLifecycleLogger.setEnabled(true);
   DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
   ```

2. **Look for orphaned contexts:**
   ```bash
   # Find contexts that were created but never cleared
   grep "CREATED:" diagnostic.log > created.txt
   grep "CLEAR (complete):" diagnostic.log > cleared.txt
   # Compare the @hash values
   ```

3. **Look for orphaned thread-locals:**
   ```bash
   # Find thread-locals that were acquired but never released
   grep "Acquiring threadlocal" diagnostic.log > acquired.txt
   grep "Releasing threadlocal" diagnostic.log > released.txt
   # Compare thread names
   ```

4. **Check for circular references:**
   ```bash
   # Look for same @hash appearing in parent/child relationships
   grep "PUSH.*Parent:.*@12345" diagnostic.log
   grep "CLEARING.*Parent:.*@12345" diagnostic.log
   ```

## Problem: Configuration not applied (Spring Boot)

### Verify application.yml:
```yaml
# Ensure correct indentation and property names
mango:
  logging:
    diagnostic:
      enabled: true  # Not 'enable' or 'diagnostics'
```

### Verify environment variables:
```bash
# Check variable names are correct
echo $MANGO_LOGGING_DIAGNOSTIC_ENABLED
echo $MANGO_LOGGING_DIAGNOSTIC_THREADLOCAL_ENABLED
```

### Check initialization log:
```
# Should see this on startup if auto-configuration is working
INFO DiagnosticLoggingAutoConfiguration - Diagnostic lifecycle logging initialized - enabled: true
```

---

# API Reference

## DiagnosticLifecycleLogger

**Package:** `ie.bitstep.mango.spring.logging.config`

### Methods

#### `setEnabled(boolean enabled)`
Enable or disable general diagnostic lifecycle logging.

**Parameters:**
- `enabled` - `true` to enable, `false` to disable

**Example:**
```java
DiagnosticLifecycleLogger.setEnabled(true);
```

#### `isEnabled()`
Check if general diagnostic lifecycle logging is enabled.

**Returns:** `boolean` - `true` if enabled

**Example:**
```java
if (DiagnosticLifecycleLogger.isEnabled()) {
    System.out.println("Diagnostic logging is active");
}
```

#### `setThreadlocalEnabled(boolean enabled)`
Enable or disable thread-local specific logging.

**Parameters:**
- `enabled` - `true` to enable, `false` to disable

**Example:**
```java
DiagnosticLifecycleLogger.setThreadlocalEnabled(true);
```

#### `isThreadlocalEnabled()`
Check if thread-local diagnostic logging is enabled.

**Returns:** `boolean` - `true` if enabled

**Example:**
```java
if (DiagnosticLifecycleLogger.isThreadlocalEnabled()) {
    System.out.println("ThreadLocal logging is active");
}
```

#### `setMaxChildren(int max)`
Set the maximum number of children allowed per DiagnosticContextItem before logging a warning.

**Parameters:**
- `max` - Maximum number of children (default: 100)

**Example:**
```java
// Lower limit for early detection in development
DiagnosticLifecycleLogger.setMaxChildren(50);
```

#### `getMaxChildren()`
Get the current maximum children limit.

**Returns:** `int` - Current maximum children limit

**Example:**
```java
int limit = DiagnosticLifecycleLogger.getMaxChildren();
System.out.println("Max children limit: " + limit);
```

### Internal Methods

The following methods are used internally by the framework:

- `logInfo(Logger logger, String message)` - Log with no arguments
- `logInfo(Logger logger, String message, Object arg)` - Log with 1 argument
- `logInfo(Logger logger, String message, Object arg1, Object arg2)` - Log with 2 arguments
- `logInfo(Logger logger, String message, Object arg1, Object arg2, Object arg3)` - Log with 3 arguments
- `logInfo(Logger logger, String message, Object arg1, Object arg2, Object arg3, Object arg4)` - Log with 4 arguments
- `logInfo(Logger logger, String message, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5)` - Log with 5 arguments

**Note:** Application code should use the setEnabled/isEnabled methods, not the internal logging methods.

---

# Best Practices

## ✅ DO

- **Enable temporarily** - Turn on for troubleshooting, then disable
- **Use separate log files** - Configure diagnostic logs to separate file
- **Monitor log volume** - Watch disk space when enabled in production
- **Document when enabled** - Log when you enable/disable diagnostic logging
- **Combine with thread dumps** - Use with thread dumps for complete picture
- **Test before production** - Verify log volume in staging environment
- **Use admin endpoints** - Enable/disable without restart
- **Set up log rotation** - Configure rolling file appenders
- **Use environment variables** - For production configuration flexibility
- **Monitor performance** - Check impact when enabled

## ❌ DON'T

- **Don't enable by default in production** - Performance impact
- **Don't ignore log rotation** - Diagnostic logs can grow quickly
- **Don't enable permanently** - Only for debugging
- **Don't rely on for business logic** - This is diagnostic only
- **Don't log sensitive data** - Logs contain object hashes, not data
- **Don't mix with application logs** - Use separate appenders
- **Don't forget to disable** - Remember to turn off after troubleshooting
- **Don't skip testing** - Test log volume before production

## Configuration Checklist

```
□ Disabled by default (application.yml: enabled: false)
□ Admin endpoints configured for runtime control
□ Separate log file configured
□ Log rotation configured (max history: 7 days)
□ Log level set to INFO for diagnostic packages
□ Performance impact tested in staging
□ Documentation updated with endpoints
□ Monitoring configured for log volume
```

## Defaults Summary

| Setting | Default | Production | Development |
|---------|---------|------------|-------------|
| General Logging | Disabled | Enable temporarily | Enabled |
| ThreadLocal Logging | Disabled | Rarely needed | Enabled |
| Performance Impact | Zero | Moderate when enabled | Acceptable |
| Log Volume | None | High when enabled | High |
| Use Case | N/A | Troubleshooting | Debugging |

---

# Summary

## Key Features

✅ **Zero-configuration Spring Boot integration** - Automatic setup  
✅ **Type-safe properties** - IDE auto-completion  
✅ **Environment variable support** - Production flexibility  
✅ **Default disabled** - Zero overhead  
✅ **Runtime control** - Enable/disable without restart  
✅ **Fully tested** - Test coverage included  
✅ **Production ready** - Best practices built-in  

## Quick Commands

```java
// Enable for debugging
DiagnosticLifecycleLogger.setEnabled(true);

// Check status
boolean enabled = DiagnosticLifecycleLogger.isEnabled();

// Disable when done
DiagnosticLifecycleLogger.setEnabled(false);
```

## Spring Boot Configuration

```yaml
mango:
  logging:
    diagnostic:
      enabled: false                  # General lifecycle logging
      threadlocal-enabled: false      # Thread-local logging
```

## When to Use

| Scenario | Enable | ThreadLocal |
|----------|--------|-------------|
| Memory leak debugging | ✅ Yes | ✅ Yes |
| Request flow tracing | ✅ Yes | ❌ No |
| Production troubleshooting | ⏰ Temporarily | ❌ Rarely |
| Unit test debugging | ✅ Yes | ✅ Yes |
| Normal operation | ❌ No | ❌ No |

---

**Document Version:** 1.0  
**Last Updated:** December 11, 2025  
**Component:** mango4j-spring-logging  
**Minimum Version:** 2.17.0-SNAPSHOT

---

**For support:** Refer to this document or contact the mango4j-spring team.

