package com.mastercard.commons.mango.logging.dc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercard.commons.mango.aop.MethodInfo;
import com.mastercard.commons.mango.logging.LogContext;
import com.mastercard.commons.mango.logging.LogUtils;
import com.mastercard.commons.mango.logging.RootLogContext;
import com.mastercard.commons.mango.logging.config.DiagnosticLifecycleLogger;
import com.mastercard.commons.mango.reflection.utils.test.MDCTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mastercard.commons.mango.logging.test.TestUtils.fakeLoggable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class DiagnosticContextTest {
	private final Clock fixedClock = Clock.fixed(Instant.ofEpochMilli(1529000000000L), ZoneId.of("UTC"));

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setup() {
		ReflectionTestUtils.setField(DiagnosticContext.class, "clock", fixedClock);
		MDCTestUtils.init();
		DiagnosticContextThreadLocal.reset();
		LogContext.put(DiagnosticContext.LOG_KEY_CORRELATION_ID, "TID55");
	}

	@AfterEach
	void destroy() {
		MDCTestUtils.cleanup();
	}

	@Test
	void testDiagnosticContextUp() {
		DiagnosticContext dc = new DiagnosticContext();
		dc.putStatValue("responseTime", 100);
		dc.up();
		assertThat(dc.getStatValues()).hasSize(1);
	}

	@Test
	void testClearWithRootItem() throws Exception {
		// Test that clearReferences() is called on rootItem and actually clears internal data
		DiagnosticContext dc = new DiagnosticContext();
		dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));
		dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenFirstChild")));
		dc.putValue("key1", "value1");

		// Verify item has data and children before clear
		assertThat(dc.getRootItem()).isNotNull();
		assertThat(dc.getCurrentItem()).isNotNull();
		assertThat(dc.getRootItem().getChildren()).hasSize(1);

		// Keep reference to verify clearReferences() was called
		var rootItemRef = dc.getRootItem();
		var childItemRef = dc.getCurrentItem();

		// Call up twice to trigger clear()
		dc.up();
		dc.up();

		// Verify clear was called on DiagnosticContext
		assertThat(dc.getRootItem()).isNull();
		assertThat(dc.getCurrentItem()).isNull();

		// Verify clearReferences() actually cleared the item's internal data
		// If clearReferences() wasn't called, these would still have data
		assertThat(rootItemRef.getChildren()).isEmpty();
		assertThat(rootItemRef.getValues()).isEmpty();
		assertThat(childItemRef.getValues()).isEmpty();
	}

	@Test
	void testClearWithCurrentItemDifferentFromRoot() throws Exception {
		/*
		 * Test that clearReferences() is called on currentItem when it's different from rootItem.
		 * This tests the condition: if (currentItem != null && currentItem != rootItem)
		 *
		 * This kills the mutation: "negated conditional" on line 60
		 */
		DiagnosticContext dc = new DiagnosticContext();

		// Create a detached item that is NOT part of the root tree
		var detachedItem = new com.mastercard.commons.mango.logging.internal.DiagnosticContextItem(
			new MethodInfo(getClass(), getClass().getMethod("push_whenSecondChild")),
			null
		);
		detachedItem.putValue("detachedKey", "detachedValue");

		// Create a normal root item
		dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));
		dc.getRootItem().putValue("rootKey", "rootValue");

		var rootRef = dc.getRootItem();

		// Use reflection to set currentItem to the detached item (simulates edge case)
		ReflectionTestUtils.setField(dc, "currentItem", detachedItem);

		// Verify setup: currentItem is different from rootItem
		assertThat(dc.getCurrentItem()).isEqualTo(detachedItem);
		assertThat(dc.getCurrentItem()).isNotEqualTo(dc.getRootItem());
		assertThat(detachedItem.getValues()).containsKey("detachedKey");
		assertThat(detachedItem.getValues()).hasSize(1);

		// Manually invoke clear() via reflection to test the defensive condition
		ReflectionTestUtils.invokeMethod(dc, "clear");

		// Verify clear was called on both rootItem and currentItem
		assertThat(dc.getRootItem()).isNull();
		assertThat(dc.getCurrentItem()).isNull();

		// Critical: Verify that detachedItem's clearReferences() was called
		// If the condition was negated (mutation), this would fail
		assertThat(detachedItem.getValues()).isEmpty();
		assertThat(rootRef.getValues()).isEmpty();
	}

	@Test
	void testClearWithCurrentItemSameAsRoot() throws Exception {
		DiagnosticContext dc = new DiagnosticContext();
		dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));

		var rootRef = dc.getRootItem();
		rootRef.putValue("key", "value");

		// Verify currentItem is the same as rootItem (only one level)
		assertThat(dc.getCurrentItem()).isEqualTo(dc.getRootItem());
		assertThat(rootRef.getValues()).hasSize(1);

		// Call up to trigger clear
		dc.up();

		// Verify clear was successful
		assertThat(dc.getRootItem()).isNull();
		assertThat(dc.getCurrentItem()).isNull();
		assertThat(rootRef.getValues()).isEmpty(); // clearReferences was called
	}

	@Test
	void testClearRemovesMDCKeys() throws Exception {
		// Test that forEach on TOP_LEVEL_KEYS_TO_REMOVE_FROM_MDC is executed
		List<String> topLevelKeysToRemoveFromMDC = (List<String>) ReflectionTestUtils.getField(
			DiagnosticContext.class, "TOP_LEVEL_KEYS_TO_REMOVE_FROM_MDC");

		// Set MDC keys
		topLevelKeysToRemoveFromMDC.forEach(key -> MDC.put(key, "testValue"));

		// Verify keys are set
		topLevelKeysToRemoveFromMDC.forEach(key ->
			assertThat(MDC.get(key)).isEqualTo("testValue")
		);

		// Create context and trigger clear
		DiagnosticContext dc = new DiagnosticContext();
		dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));
		dc.up(); // This triggers clear()

		// Verify MDC keys were removed by forEach
		Map<String, String> contextMap = MDC.getCopyOfContextMap();
		if (contextMap != null) {
			topLevelKeysToRemoveFromMDC.forEach(key ->
				assertThat(contextMap).doesNotContainKey(key)
			);
		}
	}

	@Test
	void testConstructor() {
		// given
		List<String> topLevelKeysToRemoveFromMDC = (List<String>) ReflectionTestUtils.getField(DiagnosticContext.class, "TOP_LEVEL_KEYS_TO_REMOVE_FROM_MDC");
		topLevelKeysToRemoveFromMDC.forEach(key ->
			MDC.put(key, "value")
		);

		// when
		new DiagnosticContext();

		// then
		Map<String, String> contextMap = MDC.getCopyOfContextMap();

		topLevelKeysToRemoveFromMDC.forEach(key ->
			assertThat(contextMap).doesNotContainKey(key)
		);
	}

	@Test
	void testContainsTrue() {
		fakeLoggable();
		LogContext.put("name", "batman");
		assertThat(LogContext.contains("name")).isTrue();
	}

	@Test
	void testContainsFalse() {
		fakeLoggable();
		LogContext.put("title", "mr");
		assertThat(LogContext.contains("name")).isFalse();
	}

	@Test
	void testDiagnosticContextAddValueNoCurrentContextItem() {
		DiagnosticContext dc = new DiagnosticContext();
		dc.addValue("name", "batman");
		assertThat(dc.getValue("name")).isNull();
		assertThat(dc.containsKey("name")).isFalse();
	}

	@Test
	void testDiagnosticContextAddValuesNoCurrentContextItem() {
		DiagnosticContext dc = new DiagnosticContext();
		dc.addValues("name", "batman", "robin");
		assertThat(dc.getValue("name")).isNull();
	}

	@Test
	void testDiagnosticContextAddValuesWithCurrentContextItem() throws Exception {
		// Test that addValues actually calls the private addValues method
		fakeLoggable();
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();

		// Add multiple values
		dc.addValues("heroes", "batman", "superman", "wonderwoman");

		// Verify the values were added (should be in a list)
		Object result = dc.getCurrentItem().getValue("heroes");
		assertThat(result).isNotNull().isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<Object> heroes = (List<Object>) result;
		assertThat(heroes).hasSize(3);
		assertThat(heroes.get(0)).isEqualTo("batman");
		assertThat(heroes.get(1)).isEqualTo("superman");
		assertThat(heroes.get(2)).isEqualTo("wonderwoman");
	}

	@Test
	void testDiagnosticContextMapValue() {
		fakeLoggable();
		LinkedHashMap expected = new LinkedHashMap();
		expected.put("hero", "batman");
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();
		dc.mapValue("names", "hero", "batman");
		LinkedHashMap result = (LinkedHashMap) dc.getValue("names");
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void testDiagnosticContextMapValueNoCurrentContextItem() {
		DiagnosticContext dc = new DiagnosticContext();
		dc.mapValue("names", "hero", "batman");
		assertThat(dc.getValue("names")).isNull();
	}

	@Test
	void testDiagnosticContextMapValuesNoCurrentContextItem() {
		DiagnosticContext dc = new DiagnosticContext();
		LogContext.map("names", "hero", "batman");
		assertThat(dc.getValue("names")).isNull();
	}

	@Test
	void testGetTopLevelValue() {
		DiagnosticContext dc = new DiagnosticContext();
		dc.putValue(DiagnosticContext.LOG_KEY_CORRELATION_ID, "batman");
		assertThat(dc.getValue(DiagnosticContext.LOG_KEY_CORRELATION_ID)).isEqualTo("batman");
	}

	@Test
	void testContainsTopLevelValue() {
		DiagnosticContext dc = new DiagnosticContext();
		dc.putValue(DiagnosticContext.LOG_KEY_CORRELATION_ID, "batman");
		assertThat(dc.containsKey(DiagnosticContext.LOG_KEY_CORRELATION_ID)).isTrue();
	}

	@Test
	void testGetValue() {
		fakeLoggable();
		LogContext.put("name", "batman");
		assertThat(LogContext.get("name")).isEqualTo("batman");
	}

	@Test
	void testGetThrowable() {
		fakeLoggable();
		RuntimeException ex = new RuntimeException();
		LogContext.putThrowable(ex);
		assertThat(LogContext.getThrowable()).isEqualTo(ex);
	}

	@Test
	void testGetValueNoCurrentContextItem() {
		DiagnosticContext dc = new DiagnosticContext();
		dc.putValue("name", "batman");
		assertThat(LogContext.get("name")).isNull();
	}

	@Test
	void testPopTillEmpty() {
		DiagnosticContext diagnosticContext1 = DiagnosticContextThreadLocal.get();
		DiagnosticContextThreadLocal.pop();
		DiagnosticContext diagnosticContext2 = DiagnosticContextThreadLocal.get();

		assertThat(diagnosticContext1).isNotEqualTo(diagnosticContext2);
	}

	@Test
	public void push_whenRoot() throws Exception { // NOSONAR needs public for test
		// given
		DiagnosticContext context = new DiagnosticContext();

		// when
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));

		// then
		assertThat(context.getRootItem().getName()).isEqualTo("DiagnosticContextTest.push_whenRoot");
		assertThat(context.getRootItem().isFinalMethodInfo()).isTrue();
		assertThat(context.getRootItem().getChildren()).isEmpty();
		assertThat(context.getCurrentItem().getName()).isEqualTo("DiagnosticContextTest.push_whenRoot");
	}

	@Test
	public void push_whenNotFinalMethodInfo() throws Exception { // NOSONAR needs public for test
		// given
		DiagnosticContext context = new DiagnosticContext();
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")))
			.setFinalMethodInfo(false);

		// when
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenNotFinalMethodInfo")));

		// then
		assertThat(context.getRootItem().getName()).isEqualTo("DiagnosticContextTest.push_whenNotFinalMethodInfo");
		assertThat(context.getRootItem().isFinalMethodInfo()).isTrue();
		assertThat(context.getRootItem().getChildren()).isEmpty();
		assertThat(context.getCurrentItem().getName()).isEqualTo("DiagnosticContextTest.push_whenNotFinalMethodInfo");
	}

	@Test
	void up_whenRoot() throws Exception {
		// given
		DiagnosticContext context = new DiagnosticContext();
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));

		// when
		context.up();

		// then
		assertThat(context.getRootItem()).isNull();
		assertThat(context.getCurrentItem()).isNull();
	}

	@Test
	public void push_whenFirstChild() throws Exception { // NOSONAR needs public for test
		// given
		DiagnosticContext context = new DiagnosticContext();
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));

		// when
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenFirstChild")));

		// then
		assertThat(context.getRootItem().getName()).isEqualTo("DiagnosticContextTest.push_whenRoot");
		assertThat(context.getRootItem().getChildren()).hasSize(1);
		assertThat(context.getRootItem().getChildren().get(0).getName()).isEqualTo("DiagnosticContextTest.push_whenFirstChild");
		assertThat(context.getCurrentItem().getName()).isEqualTo("DiagnosticContextTest.push_whenFirstChild");
	}

	@Test
	void up_whenChild() throws Exception {
		// given
		DiagnosticContext context = new DiagnosticContext();
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenFirstChild")));

		// when
		context.up();

		// then
		assertThat(context.getRootItem().getName()).isEqualTo("DiagnosticContextTest.push_whenRoot");
		assertThat(context.getRootItem().getChildren()).hasSize(1);
		assertThat(context.getRootItem().getChildren().get(0).getName()).isEqualTo("DiagnosticContextTest.push_whenFirstChild");
		assertThat(context.getCurrentItem().getName()).isEqualTo("DiagnosticContextTest.push_whenRoot");
	}

	@Test
	public void push_whenSecondChild() throws Exception { // NOSONAR needs public for test
		// given
		DiagnosticContext context = new DiagnosticContext();
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenFirstChild")));
		context.up();

		// when
		context.push(new MethodInfo(getClass(), getClass().getMethod("push_whenSecondChild")));

		// then
		assertThat(context.getRootItem().getName()).isEqualTo("DiagnosticContextTest.push_whenRoot");
		assertThat(context.getRootItem().getChildren()).hasSize(2);
		assertThat(context.getRootItem().getChildren().get(0).getName()).isEqualTo("DiagnosticContextTest.push_whenFirstChild");
		assertThat(context.getRootItem().getChildren().get(1).getName()).isEqualTo("DiagnosticContextTest.push_whenSecondChild");
		assertThat(context.getCurrentItem().getName()).isEqualTo("DiagnosticContextTest.push_whenSecondChild");
	}

	@Test
	void put() throws Exception {
		// given
		fakeLoggable();

		// when
		LogContext.put("key1", "val1");

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\"," +
				"\"method\":\"TestUtils.fakeLoggable\"," +
				"\"key1\":\"val1\"}");
	}

	@Test
	void mixedCurrentAndRoot() throws Exception {
		// given
		fakeLoggable();
		fakeLoggable();

		// when
		LogContext.put("currentKey", "currentVal");

		RootLogContext.put("rootKey", "rootVal");
		RootLogContext.add("rootCars", "BMW");
		RootLogContext.add("rootCars", "Mercedes", "Porsche");
		LogContext.add("cars", "Mercedes", "Porsche");

		RootLogContext.addMap("rootColours", "Red", "100", "Yellow", "50");
		LogContext.addMap("colours", "Red", "100", "Yellow", "50");

		LogContext.map("manufacturers", "BMW", "520d", "Mercedes", "e220d");
		RootLogContext.map("rootManufacturers", "BMW", "520d", "Mercedes", "e220d");

		LogContext.map("currentX", "y", "z");
		RootLogContext.map("rootX", "y", "z");

		assertThat(RootLogContext.get("rootKey")).isEqualTo("rootVal");
		assertThat(RootLogContext.get("currentKey")).isNull();

		assertThat(LogContext.get("currentKey")).isEqualTo("currentVal");
		assertThat(LogContext.get("rootKey")).isNull();

		assertThat(RootLogContext.contains("rootKey")).isTrue();
		assertThat(RootLogContext.contains("currentKey")).isFalse();

		// then
		Map<String, Object> actual = LogContext.toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\"," +
				"\"method\":\"TestUtils.fakeLoggable\"," +
				"\"rootKey\":\"rootVal\",\"rootCars\":[\"BMW\",\"Mercedes\",\"Porsche\"]," +
				"\"rootColours\":[{\"Red\":\"100\",\"Yellow\":\"50\"}]," +
				"\"rootManufacturers\":{\"BMW\":\"520d\",\"Mercedes\":\"e220d\"}," +
				"\"rootX\":{\"y\":\"z\"}," +
				"\"called\":[{\"method\":\"TestUtils.fakeLoggable\"," +
				"\"currentKey\":\"currentVal\"," +
				"\"cars\":[\"Mercedes\",\"Porsche\"]," +
				"\"colours\":[{\"Red\":\"100\",\"Yellow\":\"50\"}]," +
				"\"manufacturers\":{\"BMW\":\"520d\",\"Mercedes\":\"e220d\"}," +
				"\"currentX\":{\"y\":\"z\"}}]}"
		);
	}

	@Test
	void map1() throws Exception {
		// given
		fakeLoggable();

		// when
		String mapName = "mapName";
		LogContext.map(mapName, "key1", "val1");
		LogContext.map(mapName, "key2", "val2");

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\"," +
				"\"method\":\"TestUtils.fakeLoggable\"," +
				"\"mapName\":{\"key1\":\"val1\",\"key2\":\"val2\"}}");
	}

	@Test
	void map2() throws Exception {
		// given
		fakeLoggable();

		// when
		String mapName = "mapName";
		LogContext.map(mapName, "key1", "val1", "key2", "val2");

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\"," +
				"\"method\":\"TestUtils.fakeLoggable\"," +
				"\"mapName\":{\"key1\":\"val1\",\"key2\":\"val2\"}}");
	}

	@Test
	void add() throws Exception {
		// given
		fakeLoggable();

		// when
		String listName = "listName";
		LogContext.add(listName, "val1");
		LogContext.add(listName, "val2");

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\"," +
				"\"method\":\"TestUtils.fakeLoggable\"," +
				"\"listName\":[\"val1\",\"val2\"]}");
	}

	@Test
	void addMultiple() throws Exception {
		// given
		fakeLoggable();

		// when
		String listName = "listName";
		LogContext.add(listName, "A", "B", "C", "D");
		LogContext.add(listName, "W", "X", "Y", "Z");

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\",\"method\":\"TestUtils.fakeLoggable\"," +
				"\"listName\":[\"A\",\"B\",\"C\",\"D\",\"W\",\"X\",\"Y\",\"Z\"]}");
	}

	@Test
	void map() throws Exception {
		// given
		fakeLoggable();

		// when
		LogContext.map("heroes", "batman", "yes");

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\"," +
				"\"method\":\"TestUtils.fakeLoggable\"," +
				"\"heroes\":{\"batman\":\"yes\"}}");
	}

	@Test
	void testStatValues() {
		// given
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();

		// when
		dc.putStatValue("tps", 200);
		dc.putValue("tpsa", 201);
		assertThat(dc.getStatValues()).isNotEmpty();
		Map<String, Object> topValuesPre = (Map<String, Object>) ReflectionTestUtils.getField(dc, "topValues");
		assertThat(topValuesPre).isNotEmpty();

		dc.clear();

		// then
		assertThat(dc.getStatValues()).isEmpty();
		Map<String, Object> topValuesPostClear = (Map<String, Object>) ReflectionTestUtils.getField(dc, "topValues");
		assertThat(topValuesPostClear).isEmpty();
	}

	@Test
	void topLevelKeysTest() {
		List<String> preUpdateTopLevelKeys = DiagnosticContext.getTopLevelKeys(); // saving existing keys
		DiagnosticContext.setTopLevelKeys(new ArrayList<>()); // resetting

		DiagnosticContext.setTopLevelKeys(Arrays.asList("Hello", "world"));
		List<String> postUpdateTopLevelKeys = DiagnosticContext.getTopLevelKeys();

		assertThat(postUpdateTopLevelKeys).hasSize(2).containsExactly("Hello", "world");

		DiagnosticContext.setTopLevelKeys(preUpdateTopLevelKeys);
	}

	@Test
	void testThrown() {
		// given
		fakeLoggable();
		Throwable t = new RuntimeException();
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();

		// when
		LogContext.putThrowable(t);
		assertThat(dc.getThrown()).isNotEmpty();
		dc.clear();

		// then
		assertThat(dc.getThrown()).isEmpty();
	}

	@Test
	void put_whenTopValue() throws Exception {
		// given
		fakeLoggable();

		// when
		LogContext.put(DiagnosticContext.LOG_KEY_CORRELATION_ID, "TID22");
		LogContext.put(DiagnosticContext.LOG_KEY_URL, "URL33");
		LogContext.put(DiagnosticContext.KEY_REQUESTOR_ID, "requestor3");

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\"," +
				"\"correlationid\":\"TID22\"," +           // logged before the method
				"\"url\":\"URL33\"," +
				"\"requestorid\":\"requestor3\"," +
				"\"method\":\"TestUtils.fakeLoggable\"}");
	}

	@Test
	void put_whenNoRootItem() throws Exception {
		// given
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();

		// when
		LogContext.put(DiagnosticContext.LOG_KEY_CORRELATION_ID, "TID22");
		LogContext.put(DiagnosticContext.LOG_KEY_URL, "URL33");
		LogContext.put(DiagnosticContext.KEY_REQUESTOR_ID, "requestor3");

		// then
		Map<String, Object> actual = dc.toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID22\",\"url\":\"URL33\",\"requestorid\":\"requestor3\"}"
		);
	}

	@Test
	void putStat() throws Exception {
		// given
		fakeLoggable();

		// when
		LogContext.putStat("splunkTimeMs", 45);

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\"," +
				"\"method\":\"TestUtils.fakeLoggable\"," +
				"\"splunkTimeMs\":45}");   // logged at the end
	}

	@Test
	void get() {
		// given
		fakeLoggable();
		LogContext.put("key1", "val1");

		// when
		Object actual = LogContext.get("key1");

		// then
		assertThat(actual).isEqualTo("val1");
	}

	@Test
	void putThrowable() throws Exception {
		// given
		MethodInfo rootMethodInfo = new MethodInfo(getClass(), getClass().getMethod("push_whenRoot"));
		MethodInfo childMethodInfo = new MethodInfo(getClass(), getClass().getMethod("push_whenFirstChild"));
		DiagnosticContextThreadLocal.get().push(rootMethodInfo);
		DiagnosticContextThreadLocal.get().push(childMethodInfo);
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();

		Exception ex = new RuntimeException("failed5");

		// when
		LogContext.putThrowable(ex);

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		String jsonStackTrace = objectMapper.writeValueAsString(LogUtils.getStackTrace(ex));
		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\"," +
				"\"method\":\"DiagnosticContextTest.push_whenRoot\"," +
				"\"called\":[" +
				"{\"method\":\"DiagnosticContextTest.push_whenFirstChild\"," +
				"\"reason\":\"failed5\"," +     // exception goes into the current method
				"\"stacktrace\":" + jsonStackTrace + "}]}");

		assertThat(dc.getThrown()).hasSize(1);
		assertThat(dc.getThrown().get(0).getThrowable()).isEqualTo(ex);
		assertThat(dc.getThrown().get(0).getMethodInfo()).isEqualTo(childMethodInfo);
	}

	@Test
	void putThrowable_whenAlreadyThere() {
		// given
		fakeLoggable();

		Exception ex = new RuntimeException("failed5");

		// when
		LogContext.putThrowable(ex);
		LogContext.putThrowable(ex);     // very same exception added again

		// then
		assertThat(DiagnosticContextThreadLocal.get().getThrown()).hasSize(1);
	}

	@Test
	void putThrowable_whenContextItemIsNull() {
		// given
		DiagnosticContextThreadLocal.reset();

		Exception ex = new RuntimeException("failed5");

		// when
		LogContext.putThrowable(ex);

		// then
		assertThat(DiagnosticContextThreadLocal.get().getThrown()).isEmpty();
	}

	@Test
	void putLastCallThrowable() throws Exception {
		// given
		MethodInfo rootMethodInfo = new MethodInfo(getClass(), getClass().getMethod("push_whenRoot"));
		MethodInfo childMethodInfo = new MethodInfo(getClass(), getClass().getMethod("push_whenFirstChild"));
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();
		dc.push(rootMethodInfo);
		dc.push(childMethodInfo);
		dc.up();

		Exception ex = new RuntimeException("failed5");

		// when
		LogContext.putPreviousCallThrowable(ex);

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		String jsonStackTrace = objectMapper.writeValueAsString(LogUtils.getStackTrace(ex));
		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\"," +
				"\"method\":\"DiagnosticContextTest.push_whenRoot\"," +
				"\"called\":[" +
				"{\"method\":\"DiagnosticContextTest.push_whenFirstChild\"," +
				"\"reason\":\"failed5\"," +     // exception goes into the current method
				"\"stacktrace\":" + jsonStackTrace + "}]}");

		assertThat(dc.getThrown()).hasSize(1);
		assertThat(dc.getThrown().get(0).getThrowable()).isEqualTo(ex);
		assertThat(dc.getThrown().get(0).getMethodInfo()).isEqualTo(childMethodInfo);
	}

	@Test
	void putLastCallThrowableNoChildren() throws Exception {
		// given
		DiagnosticContextThreadLocal.reset();
		fakeLoggable();

		Exception ex = new RuntimeException("failed5");

		// when
		LogContext.putPreviousCallThrowable(ex);
		DiagnosticContext dc = DiagnosticContextThreadLocal.get();

		// then
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"method\":\"TestUtils.fakeLoggable\"}");

		assertThat(dc.getThrown()).isEmpty();
	}

	@Test
	void putLastCallThrowable_whenContextItemIsNull() {
		// given
		DiagnosticContextThreadLocal.reset();

		Exception ex = new RuntimeException("failed5");

		// when
		LogContext.putPreviousCallThrowable(ex);

		// then
		assertThat(DiagnosticContextThreadLocal.get().getThrown()).isEmpty();
	}

	@Test
	void setWithTimeStamp_whenEnabled() throws Exception {
		// given
		fakeLoggable();

		// when
		Map<String, Object> actual = DiagnosticContextThreadLocal.get().toLoggable();

		assertThat(objectMapper.writeValueAsString(actual)).isEqualTo(
			"{\"ts\":\"2018-06-14T18:13:20Z\",\"correlationid\":\"TID55\"," +
				"\"method\":\"TestUtils.fakeLoggable\"}");
	}


	@Test
	public void testIsolate() throws Exception { // NOSONAR needs public for test
		fakeLoggable();
		LogContext.put("Message", "Elephant");
		LogContext.putStat("Message", "Elephant");
		LogContext.putThrowable(new RuntimeException("Arrrghhhh!!!"));

		// Create an isolated context
		DiagnosticContextThreadLocal.push();
		DiagnosticContextThreadLocal.get().push(new MethodInfo(getClass(), getClass().getMethod("testIsolate")));

		assertThat(LogContext.getStat("Message")).isEqualTo("Elephant");

		LogContext.put("Message", "Tiger");
		LogContext.put("Colour", "Yellow");

		LogContext.putStat("Message", "Tiger");
		LogContext.putStat("Colour", "Yellow");

		assertThat(DiagnosticContextThreadLocal.get().getThrown()).isEmpty();

		assertThat(LogContext.get("Message")).isEqualTo("Tiger");
		assertThat(LogContext.get("Colour")).isEqualTo("Yellow");

		assertThat(LogContext.getStat("Message")).isEqualTo("Tiger");
		assertThat(LogContext.getStat("Colour")).isEqualTo("Yellow");

		DiagnosticContextThreadLocal.pop();

		// Check values in outer context
		assertThat(DiagnosticContextThreadLocal.get().getThrown()).hasSize(1);
		assertThat(LogContext.get("Message")).isEqualTo("Elephant");
		assertThat(LogContext.get("Colour")).isNull();
		assertThat(LogContext.getStat("Message")).isEqualTo("Elephant");
		assertThat(LogContext.getStat("Colour")).isNull();
	}

	// Tests with static mocking to verify DiagnosticLifecycleLogger calls

	@Test
	void testConstructor_verifiesLoggingCall() {
		try (MockedStatic<DiagnosticLifecycleLogger> mockedLogger = mockStatic(DiagnosticLifecycleLogger.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			// Enable logging
			DiagnosticLifecycleLogger.setEnabled(true);

			// Create new context
			DiagnosticContext dc = new DiagnosticContext();

			// Verify logContextCreated was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextCreated(any(Logger.class), eq(dc)),
				times(1)
			);
		}
	}

	@Test
	void testClear_verifiesLoggingCalls() throws Exception {
		try (MockedStatic<DiagnosticLifecycleLogger> mockedLogger = mockStatic(DiagnosticLifecycleLogger.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			// Enable logging
			DiagnosticLifecycleLogger.setEnabled(true);

			// Create context with root and current items
			DiagnosticContext dc = new DiagnosticContext();
			dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));
			var rootItem = dc.getRootItem();

			// Clear the mocked calls from setup
			mockedLogger.clearInvocations();

			// Manually invoke clear
			ReflectionTestUtils.invokeMethod(dc, "clear");

			// Verify logContextClearStart was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextClearStart(any(Logger.class), eq(dc), any(), any()),
				times(1)
			);

			// Verify logContextClearingRootItem was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextClearingRootItem(any(Logger.class), eq(rootItem)),
				times(1)
			);

			// Verify logContextClearComplete was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextClearComplete(any(Logger.class), eq(dc)),
				times(1)
			);
		}
	}

	@Test
	void testClear_withDetachedItem_verifiesLoggingCalls() throws Exception {
		try (MockedStatic<DiagnosticLifecycleLogger> mockedLogger = mockStatic(DiagnosticLifecycleLogger.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			// Enable logging
			DiagnosticLifecycleLogger.setEnabled(true);

			// Create context with root
			DiagnosticContext dc = new DiagnosticContext();
			dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));

			// Create detached item
			var detachedItem = new com.mastercard.commons.mango.logging.internal.DiagnosticContextItem(
				new MethodInfo(getClass(), getClass().getMethod("push_whenSecondChild")),
				null
			);
			ReflectionTestUtils.setField(dc, "currentItem", detachedItem);

			// Clear the mocked calls from setup
			mockedLogger.clearInvocations();

			// Manually invoke clear
			ReflectionTestUtils.invokeMethod(dc, "clear");

			// Verify logContextClearingDetachedItem was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextClearingDetachedItem(any(Logger.class), eq(detachedItem)),
				times(1)
			);
		}
	}

	@Test
	void testPush_withMethodInfo_verifiesLoggingCalls() throws Exception {
		try (MockedStatic<DiagnosticLifecycleLogger> mockedLogger = mockStatic(DiagnosticLifecycleLogger.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			// Enable logging
			DiagnosticLifecycleLogger.setEnabled(true);

			// Create context and push
			DiagnosticContext dc = new DiagnosticContext();

			// Clear setup calls
			mockedLogger.clearInvocations();

			// Push method info
			MethodInfo mi = new MethodInfo(getClass(), getClass().getMethod("push_whenRoot"));
			dc.push(mi);

			// Verify logContextPushMethodInfo was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextPushMethodInfo(
						any(Logger.class),
						eq(dc),
						any(),
						any(),
						any()
					),
				times(1)
			);

			// Verify logContextPushItem was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextPushItem(
						any(Logger.class),
						any(),
						any(),
						any(),
						any()
					),
				times(1)
			);

			// Verify logContextSetRoot was called (since rootItem was null)
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextSetRoot(any(Logger.class), any(), any()),
				times(1)
			);

			// Verify logContextSetCurrent was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextSetCurrent(any(Logger.class), any(), any()),
				times(1)
			);
		}
	}

	@Test
	void testPush_reusingExistingItem_verifiesLoggingCalls() throws Exception {
		try (MockedStatic<DiagnosticLifecycleLogger> mockedLogger = mockStatic(DiagnosticLifecycleLogger.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			// Enable logging
			DiagnosticLifecycleLogger.setEnabled(true);

			// Create context and push with non-final item
			DiagnosticContext dc = new DiagnosticContext();
			dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")))
				.setFinalMethodInfo(false);

			var currentItem = dc.getCurrentItem();

			// Clear setup calls
			mockedLogger.clearInvocations();

			// Push again - should reuse existing item
			dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenFirstChild")));

			// Verify logContextPushReusingItem was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextPushReusingItem(any(Logger.class), eq(currentItem)),
				times(1)
			);
		}
	}

	@Test
	void testPush_creatingNewItem_verifiesLoggingCalls() throws Exception {
		try (MockedStatic<DiagnosticLifecycleLogger> mockedLogger = mockStatic(DiagnosticLifecycleLogger.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			// Enable logging
			DiagnosticLifecycleLogger.setEnabled(true);

			// Create context and push
			DiagnosticContext dc = new DiagnosticContext();
			dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));

			// Clear setup calls
			mockedLogger.clearInvocations();

			// Push another - should create new item
			dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenFirstChild")));

			// Verify logContextPushCreatingNewItem was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextPushCreatingNewItem(any(Logger.class), any()),
				times(1)
			);

			// Verify logContextAddChild was called (since rootItem exists)
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextAddChild(any(Logger.class), any(), any()),
				times(1)
			);
		}
	}

	@Test
	void testUp_verifiesLoggingCalls() throws Exception {
		try (MockedStatic<DiagnosticLifecycleLogger> mockedLogger = mockStatic(DiagnosticLifecycleLogger.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			// Enable logging
			DiagnosticLifecycleLogger.setEnabled(true);

			// Create context with child
			DiagnosticContext dc = new DiagnosticContext();
			dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));
			dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenFirstChild")));

			var leftItem = dc.getCurrentItem();

			// Clear setup calls
			mockedLogger.clearInvocations();

			// Call up
			dc.up();

			// Verify logContextUpBefore was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextUpBefore(any(Logger.class), eq(dc), any(), any()),
				times(1)
			);

			// Verify logContextUpAfter was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextUpAfter(any(Logger.class), eq(dc), eq(leftItem), any()),
				times(1)
			);
		}
	}

	@Test
	void testUp_reachingRoot_verifiesLoggingCalls() throws Exception {
		try (MockedStatic<DiagnosticLifecycleLogger> mockedLogger = mockStatic(DiagnosticLifecycleLogger.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			// Enable logging
			DiagnosticLifecycleLogger.setEnabled(true);

			// Create context with only root
			DiagnosticContext dc = new DiagnosticContext();
			dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));

			// Clear setup calls
			mockedLogger.clearInvocations();

			// Call up - should reach root and trigger clear
			dc.up();

			// Verify logContextUpReachedRoot was called
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextUpReachedRoot(any(Logger.class), eq(dc)),
				times(1)
			);

			// Verify logContextClearStart was called (because clear() is invoked)
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextClearStart(any(Logger.class), eq(dc), any(), any()),
				times(1)
			);
		}
	}

	@Test
	void testLogging_whenDisabled_doesNotLog() throws Exception {
		try (MockedStatic<DiagnosticLifecycleLogger> mockedLogger = mockStatic(DiagnosticLifecycleLogger.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
			// Disable logging
			DiagnosticLifecycleLogger.setEnabled(false);

			// Create context and perform operations
			DiagnosticContext dc = new DiagnosticContext();
			dc.push(new MethodInfo(getClass(), getClass().getMethod("push_whenRoot")));
			dc.up();

			// Verify the logging methods WERE called (they check enabled flag internally)
			// But they should NOT have actually logged anything
			// The methods were invoked but the conditional inside prevented logging
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextCreated(any(Logger.class), any()),
				times(1)
			);
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextPushMethodInfo(any(Logger.class), any(), any(), any(), any()),
				times(1)
			);
			mockedLogger.verify(() ->
					DiagnosticLifecycleLogger.logContextUpBefore(any(Logger.class), any(), any(), any()),
				times(1)
			);
		}
	}
}
