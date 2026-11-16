# mango4j-spring-logging - Web Context Populators

mango4j-spring-logging allows for custom web context populators.
These are of two types: `WebContextPrePopulator` and `WebContextPostPopulator`.

There are default Pre and Post populators available set to Order 0. These populators will always run in web servlet enabled applications.

To add custom pre or post populators, all you have to do is create a bean that implements the interface `WebContextPrePopulator`, `WebContextPostPopulator` respectively.

Each Pre and Post populators adhere to the spring `@Order` precedence.

## Custom sample Pre populator

```java language=java
@Component
@Order(value = Integer.MIN_VALUE)
public class HighestOrderPrePopulator implements WebContextPrePopulator {

	@Override
	public void populate(HttpServletRequest httpServletRequest) {
		System.out.println("Pre-populator - HIGHEST precedence [order value: "+Integer.MIN_VALUE+"]");
		LogContext.putValue("customValue", "Lowest Pre Populator");
	}
}
```

## Custom sample Post populator

```java language=java
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class LowestOrderPostPopulator implements WebContextPostPopulator {

	@Override
	public void populate(HttpServletRequest req, HttpServletResponse res) {
		System.out.println("Post-populator - lowest precedence [order value: "+Ordered.LOWEST_PRECEDENCE+"]");
		LogContext.putValue("customValue", "Lowest Post Populator");
	}

}
```