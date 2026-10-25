package ie.bitstep.mango.spring.logging.internal;

import ie.bitstep.mango.spring.aop.MethodInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test to verify that DiagnosticContextItem properly clears circular references
 * to prevent memory leaks.
 */
class DiagnosticContextItemMemoryLeakTest {

	@Test
	void testClearReferencesBreaksCircularReferences() {
		// Create a tree structure with parent-child relationships
		MethodInfo mockMethodInfo = mock(MethodInfo.class);

		DiagnosticContextItem root = new DiagnosticContextItem(mockMethodInfo, null);
		DiagnosticContextItem child1 = new DiagnosticContextItem(mockMethodInfo, root);
		DiagnosticContextItem child2 = new DiagnosticContextItem(mockMethodInfo, root);
		DiagnosticContextItem grandchild = new DiagnosticContextItem(mockMethodInfo, child1);

		root.addChild(child1);
		root.addChild(child2);
		child1.addChild(grandchild);

		// Add some data to the items
		root.putValue("key1", "value1");
		child1.putValue("key2", "value2");
		grandchild.putValue("key3", "value3");
		grandchild.putResultValue("result", "success");

		// Verify the tree structure is set up correctly
		assertEquals(2, root.getChildren().size());
		assertEquals(1, child1.getChildren().size());
		assertEquals(root, child1.getParent());
		assertEquals(child1, grandchild.getParent());
		assertEquals(1, root.getValues().size());
		assertEquals(1, child1.getValues().size());
		assertEquals(1, grandchild.getValues().size());
		assertEquals(1, grandchild.getResultValues().size());

		// Clear references
		root.clearReferences();

		// Verify children lists are cleared
		assertTrue(root.getChildren().isEmpty(), "Root children should be cleared");
		assertTrue(child1.getChildren().isEmpty(), "Child1 children should be cleared");
		assertTrue(child2.getChildren().isEmpty(), "Child2 children should be cleared");
		assertTrue(grandchild.getChildren().isEmpty(), "Grandchild children should be cleared");

		// Verify data collections are cleared
		assertTrue(root.getValues().isEmpty(), "Root values should be cleared");
		assertTrue(child1.getValues().isEmpty(), "Child1 values should be cleared");
		assertTrue(grandchild.getValues().isEmpty(), "Grandchild values should be cleared");
		assertTrue(grandchild.getResultValues().isEmpty(), "Grandchild result values should be cleared");

		/*
		 * Note: Parent references remain (they are final), but with children cleared,
		 * the circular reference chain is broken and GC can collect the objects
		 */
		assertNotNull(child1.getParent(), "Parent reference remains (final field)");
		assertNotNull(grandchild.getParent(), "Parent reference remains (final field)");
	}

	@Test
	void testClearReferencesWithThrowable() {
		MethodInfo mockMethodInfo = mock(MethodInfo.class);
		DiagnosticContextItem item = new DiagnosticContextItem(mockMethodInfo, null);

		// Add a throwable
		Exception testException = new RuntimeException("Test exception");
		item.putThrowable(testException);

		assertNotNull(item.getThrowable(), "Throwable should be set");

		// Clear references
		item.clearReferences();

		// Verify throwable is cleared
		assertNull(item.getThrowable(), "Throwable should be cleared");
	}

	@Test
	void testClearReferencesOnEmptyTree() {
		MethodInfo mockMethodInfo = mock(MethodInfo.class);
		DiagnosticContextItem item = new DiagnosticContextItem(mockMethodInfo, null);

		// Clear references on an item with no children or data
		assertDoesNotThrow(() -> item.clearReferences(),
			"clearReferences should not throw on empty item");

		assertTrue(item.getChildren().isEmpty());
		assertTrue(item.getValues().isEmpty());
		assertTrue(item.getResultValues().isEmpty());
		assertNull(item.getThrowable());
	}

	@Test
	void testClearReferencesDeepTree() {
		// Create a deep tree to test recursive clearing
		MethodInfo mockMethodInfo = mock(MethodInfo.class);
		DiagnosticContextItem root = new DiagnosticContextItem(mockMethodInfo, null);
		DiagnosticContextItem current = root;

		// Create a 100-level deep tree
		for (int i = 0; i < 100; i++) {
			DiagnosticContextItem child = new DiagnosticContextItem(mockMethodInfo, current);
			child.putValue("level", i);
			current.addChild(child);
			current = child;
		}

		// Clear the entire tree
		assertDoesNotThrow(() -> root.clearReferences(),
			"clearReferences should handle deep trees without stack overflow");

		// Verify root is cleared
		assertTrue(root.getChildren().isEmpty());
		assertTrue(root.getValues().isEmpty());
	}

	@Test
	void testClearReferencesWithCurrentItemInMiddleOfTree() {
		/*
		 * This test simulates the scenario where currentItem points to a node
		 * in the middle of the tree, not at the root.
		 *
		 * In DiagnosticContext, currentItem tracks the current position in the call stack.
		 * When clear() is called, both rootItem and currentItem must be properly cleaned
		 * to ensure no references to the tree remain. Without clearing currentItem's
		 * subtree, we could leak the entire portion of the tree it references.
		 */
		MethodInfo mockMethodInfo = mock(MethodInfo.class);

		DiagnosticContextItem root = new DiagnosticContextItem(mockMethodInfo, null);
		DiagnosticContextItem middle = new DiagnosticContextItem(mockMethodInfo, root);
		DiagnosticContextItem leaf = new DiagnosticContextItem(mockMethodInfo, middle);

		root.addChild(middle);
		middle.addChild(leaf);

		// Add data to verify cleanup
		root.putValue("root", "data");
		middle.putValue("middle", "data");
		leaf.putValue("leaf", "data");

		// Verify tree structure
		assertEquals(1, root.getChildren().size());
		assertEquals(1, middle.getChildren().size());
		assertEquals(0, leaf.getChildren().size());

		// Clear from root - should clear entire tree including middle and leaf
		root.clearReferences();

		// Verify entire tree is cleared
		assertTrue(root.getChildren().isEmpty());
		assertTrue(root.getValues().isEmpty());
		assertTrue(middle.getChildren().isEmpty());
		assertTrue(middle.getValues().isEmpty());
		assertTrue(leaf.getChildren().isEmpty());
		assertTrue(leaf.getValues().isEmpty());

		/*
		 * Even if we had a reference to middle or leaf, they should be cleared.
		 * This simulates what happens when DiagnosticContext.clear() is called
		 * with currentItem pointing to 'middle' or 'leaf'.
		 */
	}
}

