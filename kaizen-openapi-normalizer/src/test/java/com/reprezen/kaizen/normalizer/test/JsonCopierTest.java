package com.reprezen.kaizen.normalizer.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.reprezen.kaizen.normalizer.util.JsonCopier;

public class JsonCopierTest extends NormalizerTestBase {

	private JsonNode target;

	@Before
	public void setup() throws JsonProcessingException, IOException {
		target = loadYaml("copyTest");
	}

	@Test
	public void addToExistingObj() {
		checkCopy("top", "obj", "d");
	}

	public void replaceInExsitingObj() {
		checkCopy("top", "obj", "b");
	}

	@Test
	public void addToExistingArray() {
		checkCopy("top", "obj", "b", 4);
	}

	@Test
	public void replaceInExistingArray() {
		checkCopy("top", "obj", "b", 2);
	}

	@Test
	public void extendExisingArray() {
		checkCopy("top", "obj", "b", 10);
	}

	@Test
	public void createNewObject() {
		checkCopy("top", "newObj", "a");
	}

	@Test
	public void createNewArray() {
		checkCopy("top", "newArr", 3);
	}

	@Test
	public void createDeepStructure() {
		checkCopy("x", "a", 1, "b", "c", 2, 0, "d");
	}

	@Test
	public void checkBadPath() {
		echeckCopy(IllegalArgumentException.class);
	}

	@Test
	public void checkBadExistingContainers() {
		echeckCopy(IllegalArgumentException.class, "top", 3);
		echeckCopy(IllegalArgumentException.class, "top", "obj", "c", "x", "y");
		echeckCopy(IllegalArgumentException.class, "top", "obj", "c", 0, "y");
		// following only throws exception when using the List<Object> path, rather than
		// JsonPointer, because the array index can be interpreted as a property name
		echeckCopy(IllegalArgumentException.class, target, Arrays.asList("top", "obj", 0));
	}

	private void checkCopy(Object... path) {
		checkCopy(target.deepCopy(), Arrays.asList(path));
		checkCopy(target, getPointer(Arrays.asList(path)));
	}

	private void echeckCopy(Class<?> eClass, Object... path) {
		echeckCopy(eClass, target.deepCopy(), Arrays.asList(path));
		echeckCopy(eClass, target, getPointer(Arrays.asList(path)));
	}

	private void echeckCopy(Class<?> eClass, JsonNode target, Object locator) {
		try {
			if (locator instanceof JsonPointer) {
				checkCopy(target, locator);
			} else {
				checkCopy(target, locator);
			}
			fail("Copy operation should have thrown " + eClass.getSimpleName());
		} catch (Exception e) {
			assertEquals(eClass, e.getClass());
		}
	}

	private void checkCopy(JsonNode target, Object locator) {
		@SuppressWarnings("unchecked")
		JsonPointer ptr = locator instanceof JsonPointer ? (JsonPointer) locator : getPointer((List<Object>) locator);
		JsonNode waldo = TextNode.valueOf("Waldo");
		assertNotEquals(waldo, target.at(ptr));
		if (locator instanceof JsonPointer) {
			JsonCopier.copy(waldo, target, ptr);
		} else {
			@SuppressWarnings("unchecked")
			List<Object> path = (List<Object>) locator;
			JsonCopier.copy(waldo, target, path);
		}
		assertEquals(waldo, target.at(ptr));
	}

	private JsonPointer getPointer(List<Object> path) {
		return JsonPointer.compile(path.stream().map(s -> "/" + s.toString()).collect(Collectors.joining()));
	}
}
