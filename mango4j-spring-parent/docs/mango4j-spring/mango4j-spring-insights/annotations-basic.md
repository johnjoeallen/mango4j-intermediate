# mango4j-spring-insights - Basic Annotating

WARNING: This framework relies on Spring AOP, therefore:
Any method calls need to cross service boundaries or be called via a WithSpringAOP<>proxy() in order to be intercepted by Spring AOP.

### Annotate insights

<!--name=admonition;type=info;title=NOTE;body=
This works on any type of methods. An insight will be emitted in the logs when this method returns

Typically used for controller entry points

@Insight(InsightsType.API, "funMethod")

Typically used for background tasks

@Insight(InsightsType.TASK, "funMethod")

Should be used for auditing information

@Insight(InsightsType.AUDIT, "funMethod")

The insight publisher can push each insight type to different places if necessary
-->


```java language=java
@Insight(InsightsType.API, "funMethod")
public void funMethod() {
}
```

<!--name=admonition;type=info;title=NOTE;body=
Insight parameters into methods
-->

```java language=java
public int funMethodWithParametersAndResponse(
    @InsightVariable(name = "hiParam") // this will come out as a value in the insight
    @InsightTag(name = "hiParam") // this will be available in the publisher as a set of tags
        String serviceParam) {
}
```


## Customisation & Configuration
