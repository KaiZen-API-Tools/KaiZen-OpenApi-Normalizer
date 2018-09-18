package com.reprezen.kaizen.normalizer.util;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonCopier {

	/**
	 * Copy a JsonNode value to a particular position in another JsonNode structure.
	 * <p>
	 * If the required container to receive the copied value does not exist in the
	 * target structure, that structure is augmented as needed. This may entail
	 * creation of object and array nodes, and extension of existing array nodes.
	 * <p>
	 * N.B. This method assumes that every component in the provided JsonPointer
	 * that looks like a positive integer does, in fact, represent the index of an
	 * array node. That's not necessarily correct, since that same component could
	 * be interpreted as an object property name. Method
	 * {@link #copy(JsonNode, JsonNode, List)} does the same job but without this
	 * ambiguity that is inherent in the JSONPointer design.
	 * 
	 * @param from
	 *            value to be set
	 * @param to
	 *            structure in which the value will be set
	 * @param pointer
	 *            JSON pointer that should address the set value within the "to"
	 *            structure, once the operation is complete.
	 * @throws IllegalArgumentException
	 *             if the pointer matches root - i.e. addresses the entire target
	 *             structure, not value within a container
	 * @throws IllegalStateException
	 *             if existing values within the target structure do not provide the
	 *             required container type. E.g. if a pointer component is an
	 *             integer, and the target structure includes a JSON string or
	 *             object at the position identified by preceding pointer
	 *             components, this exception will be thrown.
	 */
	public static void copy(JsonNode from, JsonNode to, JsonPointer pointer) {
		List<Object> path = new ArrayList<>();
		while (!pointer.matches()) {
			if (pointer.getMatchingIndex() >= 0) {
				path.add(pointer.getMatchingIndex());
			} else {
				path.add(pointer.getMatchingProperty());
			}
			pointer = pointer.tail();
		}
		copy(from, to, path);
	}

	/**
	 * Copy a JsonNode value to a particular position in another JsonNode structure.
	 * <p>
	 * If the required container to receive the copied value does not exist in the
	 * target structure, that structure is augmented as needed. This may entail
	 * creation of object and array nodes, and extension of existing array nodes.
	 * <p>
	 * The position is given as a list of strings and integers which, with proper
	 * escaping, could be the components of a JsonPointer that would address the
	 * point in the target structure where the copied value is to appear. However,
	 * unlike JsonPointer, array indexes and property names are unambiguously
	 * differentiated.
	 * <p>
	 * A {@link JsonStateWalker} walk can conveniently make use of this method,
	 * since the path parameter is in the form provided by the walk traker.
	 * 
	 * @param from
	 *            value to be set
	 * @param to
	 *            structure in which the value will be set
	 * @param pointer
	 *            JSON pointer that should address the set value within the "to"
	 *            structure, once the operation is complete.
	 * @throws IllegalArgumentException
	 *             if the pointer matches root - i.e. addresses the entire target
	 *             structure, not value within a container
	 * @throws IllegalStateException
	 *             if existing values within the target structure do not provide the
	 *             required container type. E.g. if a pointer component is an
	 *             integer, and the target structure includes a JSON string or
	 *             object at the position identified by preceding pointer
	 *             components, this exception will be thrown.
	 */
	public static void copy(JsonNode from, JsonNode to, List<Object> path) {
		if (path.isEmpty()) {
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < path.size() - 1; i++) {
			Object step = path.get(i);
			if (step instanceof String && to.isObject()) {
				String s = (String) step;
				ObjectNode obj = (ObjectNode) to;
				if (!obj.has(s)) {
					obj.set(s, containerFor(path.get(i + 1)));
				}
				to = obj.get(s);
			} else if (step instanceof Integer && to.isArray()) {
				int pos = (int) step;
				ArrayNode arr = (ArrayNode) to;
				while (arr.size() < pos + 1) {
					arr.add(NullNode.instance);
				}
				arr.set(pos, containerFor(path.get(i + 1)));
				to = arr.get(pos);
			} else {
				throw new IllegalArgumentException();
			}
		}
		Object step = path.get(path.size() - 1);
		if (step instanceof String && to.isObject()) {
			((ObjectNode) to).set((String) step, from.deepCopy());
		} else if (step instanceof Integer && to.isArray()) {
			int pos = (int) step;
			ArrayNode arr = (ArrayNode) to;
			while (arr.size() < pos + 1) {
				arr.add(NullNode.instance);
			}
			arr.set(pos, from);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private static JsonNode containerFor(Object step) {
		if (step instanceof String) {
			return JsonNodeFactory.instance.objectNode();
		} else if (step instanceof Integer) {
			ArrayNode arr = JsonNodeFactory.instance.arrayNode();
			for (int i = 0; i < (int) step; i++) {
				arr.add(NullNode.instance);
			}
			return arr;
		} else {
			throw new IllegalArgumentException();
		}
	}
}
