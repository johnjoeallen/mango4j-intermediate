# mango4j core

The plain-Java modules. None of them depend on Spring; the build enforces that. Add only the
artifacts you need, all under group ID `ie.bitstep.mango`.

```xml
<dependency>
    <groupId>ie.bitstep.mango</groupId>
    <artifactId>mango4j-collections</artifactId>
    <version>4.0.0-SNAPSHOT</version>
</dependency>
```

## mango4j-collections

Package `ie.bitstep.mango.collections`.

- `MapBuilder` and `ListBuilder`: fluent builders for maps and lists.
- `MapUtils`: helpers for working with maps.
- `CollectionReconciler`: compares a *current* collection (for example the state in a database)
  with a *desired* one, matching elements by a key you extract from each, so you can work out
  what to add, remove or keep.

## mango4j-masking

Package `ie.bitstep.mango.utils.masking`.

- `Masker`: functional interface that masks a sensitive value, for example before logging it.
- Implementations: `PanMasker`, `AccountIdMasker`, `FullMasker`, `NoMasker`, and the
  comma-separated list maskers `CommaSeparatedPanListMasker` and `CommaSeparatedIdListMasker`.
- `MaskerFactory`: looks up maskers.
- `@Mask`: marks a field or parameter for masking, with a custom definition if needed.

`mango4j-spring-logging` uses this module.

## mango4j-reflection

Package `ie.bitstep.mango.reflection`.

- `PropertyAccessor`, `PropertyGetter`, `PropertySetter`: read and write properties by name.
- `ReflectionUtils`, `ClassInfo`, `MethodInfo`, `StringUtils`.

## mango4j-utils

Package `ie.bitstep.mango.utils`.

- `date`: `DateUtils`, `CalendarUtils`, `Proximity`.
- `entity`: `EntityToStringBuilder`, a `toString` builder for Hibernate entities. It only prints
  primitives, strings, numbers and dates, so it never forces lazy children to load.
- `thread`: `NamedScheduledExecutorBuilder`, a builder for a `ScheduledExecutorService` with named
  threads and common tunables.
- `mapping`: `MappingUtils`.
- `proxy`: `ProxyResolver` and `IdentityProxyResolver`.
- `conformance`: `Conformer`, with the `@Tolerate` annotation and its mutators.

Depends on `mango4j-reflection`.

## mango4j-validation

Package `ie.bitstep.mango.validation`.

- `ValidationUtils` and `ValidationUtilsException`.
- Constraints: `@Type4UUID`, `@StrictType4UUID`, `@IsValidKebabCase`, `@IsValidDottedCase`.
- `IdentifierValidator`.

Depends on `mango4j-utils`.

## mango4j-hibernate-proxy-resolver

Package `ie.bitstep.mango.proxy`. `HibernateProxyResolver` unwraps Hibernate proxies to the real
entity. It implements `ProxyResolver` from `mango4j-utils`, which it depends on.

## Dependencies between modules

```
mango4j-collections        (none)
mango4j-masking            (none)
mango4j-reflection         (none)
mango4j-utils              -> mango4j-reflection
mango4j-validation         -> mango4j-utils
mango4j-hibernate-proxy-resolver -> mango4j-utils
```
