package com.reprezen.kaizen.normalizer.test;

import static com.reprezen.kaizen.normalizer.test.JsonStateWalkerTest.VisitResult.visitResult;
import static com.reprezen.kaizen.normalizer.util.JsonStateWalker.Disposition.Action.DESCEND;
import static com.reprezen.kaizen.normalizer.util.JsonStateWalker.Disposition.Action.DONE;
import static com.reprezen.kaizen.normalizer.util.JsonStateWalker.Disposition.Action.REVISIT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker.AdvancedVisitMethod;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker.Disposition;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker.Disposition.Action;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker.SimpleVisitMethod;
import com.reprezen.kaizen.normalizer.util.StateMachine;
import com.reprezen.kaizen.normalizer.util.StateMachine.State;
import com.reprezen.kaizen.normalizer.util.StateMachine.Tracker;

public class JsonStateWalkerTest extends NormalizerTestBase {

	private JsonNode tree;
	private StateMachine<S> machine;

	@Before
	public void setup() throws JsonProcessingException, IOException {
		this.tree = loadYaml("walkTest");
		this.machine = new StateMachine<S>(S.class, S.ANON, S.OFF);
		machine.transit().from(S.TOP).via("a").to(S.A);
		machine.transit().from(S.TOP).via("b").to(S.B);
		machine.transit().from(S.TOP).via("c").to(S.C);
		machine.transit().from(S.A).via("a").to(S.A);
		machine.transit().from(S.A).via("b").to(S.B);
		machine.transit().from(S.B).via("#").to(S.C);
	}

	@Test
	public void simpleTest() {
		SimpleVisitMethod<S> visitMehod = (n, s, v, path, ptr) -> {
		};
		verifyWalk(tree, machine.tracker(S.TOP), visitMehod, //
				visitResult(S.TOP, ""), //
				visitResult(S.A, "/a"), //
				visitResult(S.A, "/a/a"), //
				visitResult(S.B, "/a/a/b"), //
				visitResult(S.C, "/a/a/b/0"), //
				visitResult(S.C, "/a/a/b/1"), //
				visitResult(S.C, "/a/a/b/2"), //
				visitResult(S.C, "/a/a/b/3"), //
				visitResult(S.OFF, "/a/a/b/3/0"), //
				visitResult(S.OFF, "/a/a/b/3/1"), //
				visitResult(S.OFF, "/a/a/b/3/2"), //
				visitResult(S.B, "/a/b"), //
				visitResult(S.C, "/a/b/0"), //
				visitResult(S.C, "/c"));
	}

	@Test
	public void pruneTest() {
		AdvancedVisitMethod<S> visitMethod = (n, s, v, path, ptr) -> {
			return v == S.C ? Disposition.done() : Disposition.normal();
		};
		verifyWalk(tree, machine.tracker(S.TOP), visitMethod, //
				visitResult(S.TOP, ""), //
				visitResult(S.A, "/a"), //
				visitResult(S.A, "/a/a"), //
				visitResult(S.B, "/a/a/b"), //
				visitResult(S.C, "/a/a/b/0", DONE), //
				visitResult(S.C, "/a/a/b/1", DONE), //
				visitResult(S.C, "/a/a/b/2", DONE), //
				visitResult(S.C, "/a/a/b/3", DONE), //
				visitResult(S.B, "/a/b"), //
				visitResult(S.C, "/a/b/0", DONE), //
				visitResult(S.C, "/c", DONE));
	}

	@Test
	public void replaceTest() {
		AdvancedVisitMethod<S> walk = (n, s, v, path, ptr) -> {
			return v == S.C ? Disposition.done().withReplacement(TextNode.valueOf("replaced")) : Disposition.normal();
		};
		JsonNode newTree = verifyWalk(tree, machine.tracker(S.TOP), walk, //
				visitResult(S.TOP, ""), //
				visitResult(S.A, "/a"), //
				visitResult(S.A, "/a/a"), //
				visitResult(S.B, "/a/a/b"), //
				visitResult(S.C, "/a/a/b/0", DONE), //
				visitResult(S.C, "/a/a/b/1", DONE), //
				visitResult(S.C, "/a/a/b/2", DONE), //
				visitResult(S.C, "/a/a/b/3", DONE), //
				visitResult(S.B, "/a/b"), //
				visitResult(S.C, "/a/b/0", DONE), //
				visitResult(S.C, "/c", DONE));
		SimpleVisitMethod<S> checkReplacements = (n, s, v, path, ptr) -> {
			if (v == S.C) {
				assertEquals("Replacement failed", TextNode.valueOf("replaced"), n);
			}
		};
		new JsonStateWalker<S>(machine.tracker(S.TOP), checkReplacements).walk(newTree);
	}

	@Test
	public void revisitTest() {
		AdvancedVisitMethod<S> visit = (n, s, v, path, ptr) -> {
			if (v == S.B) {
				JsonNode replacement = arrayNode(TextNode.valueOf("x"));
				return Disposition.revisit().withReplacement(n.equals(replacement) ? null : replacement);
			}
			return Disposition.normal();
		};
		JsonNode newTree = verifyWalk(tree, machine.tracker(S.TOP), visit, //
				visitResult(S.TOP, ""), //
				visitResult(S.A, "/a"), //
				visitResult(S.A, "/a/a"), //
				visitResult(S.B, "/a/a/b", REVISIT), //
				visitResult(S.B, "/a/a/b", REVISIT), //
				visitResult(S.C, "/a/a/b/0"), //
				visitResult(S.B, "/a/b", REVISIT), //
				visitResult(S.B, "/a/b", REVISIT), //
				visitResult(S.C, "/a/b/0"), //
				visitResult(S.C, "/c"));
		SimpleVisitMethod<S> checkReplacements = (n, s, v, path, ptr) -> {
			if (v == S.B) {
				assertEquals("Replacement failed at " + ptr, arrayNode(TextNode.valueOf("x")), n);
			}
		};
		new JsonStateWalker<S>(machine.tracker(S.TOP), checkReplacements).walk(newTree);

	}

	private ArrayNode arrayNode(JsonNode... elements) {
		ArrayNode array = JsonNodeFactory.instance.arrayNode();
		Stream.of(elements).forEach(e -> array.add(e));
		return array;
	}

	private <E extends Enum<E>> void verifyWalk(JsonNode tree, Tracker<E> tracker, SimpleVisitMethod<E> walkMethod,
			VisitResult<?>... expected) {
		verifyWalk(tree, tracker, walkMethod.asAdvancedVisitMethod(), expected);
	}

	private <E extends Enum<E>> JsonNode verifyWalk(JsonNode tree, Tracker<E> tracker,
			AdvancedVisitMethod<E> visitMethod, VisitResult<?>... expected) {
		List<VisitResult<E>> results = new ArrayList<>();
		AdvancedVisitMethod<E> wrappedWalkMethod = new AdvancedVisitMethod<E>() {
			@Override
			public Disposition visit(JsonNode node, State<E> state, E stateValue, List<Object> path,
					JsonPointer pointer) {
				Disposition disp = visitMethod.visit(node, state, stateValue, path, pointer);
				results.add(visitResult(stateValue, pointer.toString(), disp.getAction()));
				return disp;
			}
		};
		Optional<JsonNode> newTree = new JsonStateWalker<E>(tracker, wrappedWalkMethod).walk(tree);
		checkWalkResults(Arrays.asList(expected), results);
		return newTree.isPresent() ? newTree.get() : tree;
	}

	private <E extends Enum<E>> void checkWalkResults(List<VisitResult<?>> expected, List<VisitResult<E>> actual) {
		for (int i = 0; i < expected.size() && i < actual.size(); i++) {
			assertEquals("Walk result #" + i + " is incorrect", expected.get(i), actual.get(i));
		}
		for (int i = actual.size(); i < expected.size(); i++) {
			fail("Missing walk result at position " + i + ": " + expected.get(i));
		}
		for (int i = expected.size(); i < actual.size(); i++) {
			fail("Unexpected walk result at position " + i + ": " + actual.get(i));
		}
	}

	public static class VisitResult<E extends Enum<E>> {
		E stateValue;
		JsonPointer pointer;
		Action action;

		private VisitResult(E stateValue, JsonPointer pointer, Action action) {
			this.stateValue = stateValue;
			this.pointer = pointer;
			this.action = action;
		}

		public static <E extends Enum<E>> VisitResult<E> visitResult(E stateValue, String pointer, Action action) {
			return new VisitResult<E>(stateValue, JsonPointer.compile(pointer), action);
		}

		public static <E extends Enum<E>> VisitResult<E> visitResult(E stateValue, String pointer) {
			return visitResult(stateValue, pointer, DESCEND);
		}

		@Override
		public String toString() {
			return String.format("%s[%s]: %s", pointer, stateValue, action);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((action == null) ? 0 : action.hashCode());
			result = prime * result + ((pointer == null) ? 0 : pointer.hashCode());
			result = prime * result + ((stateValue == null) ? 0 : stateValue.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			VisitResult<?> other = (VisitResult<?>) obj;
			if (action == null) {
				if (other.action != null)
					return false;
			} else if (!action.equals(other.action))
				return false;
			if (pointer == null) {
				if (other.pointer != null)
					return false;
			} else if (!pointer.equals(other.pointer))
				return false;
			if (stateValue == null) {
				if (other.stateValue != null)
					return false;
			} else if (!stateValue.equals(other.stateValue))
				return false;
			return true;
		}
	}

	public enum S {
		TOP, A, B, C, ANON, OFF;
	}
}
