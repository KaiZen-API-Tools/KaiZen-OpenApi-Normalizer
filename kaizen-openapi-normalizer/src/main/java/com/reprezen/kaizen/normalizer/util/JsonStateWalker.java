package com.reprezen.kaizen.normalizer.util;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.reprezen.kaizen.normalizer.util.StateMachine.State;
import com.reprezen.kaizen.normalizer.util.StateMachine.Tracker;

/**
 * Class to walk a JsonNode tree while navigating a {@link StateMachine} by
 * making moves with a supplied tracker whenever descending into child nodes.
 * 
 * The walker is intialized with a {@link Tracker}, as indicated above, as well
 * as a walk method in the form of either an {@link AdvancedWalkMethod} or a
 * {@link SimpleWalkMethod}. As the tree is walked (in a depth-first fashion),
 * the walk method is invoked for each visited node.
 * 
 * When walking the children of an object node, the tracker moves to each
 * property name before walking the property value, and then backs up
 * immediately afterward. When walking the children of an array node, the
 * tracker moves to each element index before walking the element, and then
 * backs up immediately afterward.
 * 
 * An advanced walk method is capable of providing a replacement JsonNode value
 * for the node being walked, and it can also control descent into the walked
 * node's children. A simple walk method, in contrast, can do neither of these;
 * descents always happen, and no replacements occur as a result fo the walk.
 * 
 * The walker can optionally suppress invocation of the walk method whenever the
 * current state reported by the tracker is anonymous, or when the tracker is
 * off-road, or both.
 * 
 * @author Andy Lowry
 *
 * @param <E>
 */
public class JsonStateWalker<E extends Enum<E>> {

	private Tracker<E> tracker;
	private AdvancedWalkMethod<E> walkMethod;
	private boolean walkAnonymousStates;
	private boolean walkOffRoad;

	/**
	 * Create a walker with a simple walk method
	 * 
	 * @param tracker
	 *            the tracker to use during the walk
	 * @param walkMethod
	 *            the walk method to invoke
	 */
	public JsonStateWalker(Tracker<E> tracker, SimpleWalkMethod<E> walkMethod) {
		this(tracker, walkMethod, false, false);
	}

	/**
	 * Create a walker with a simple walk method
	 * 
	 * @param tracker
	 *            the tracker to use during the walk
	 * @param walkMethod
	 *            the walk method to invoke
	 * @param walkAnonymousStates
	 *            whether to walk nodes corresponding to anonymous machine states
	 * @param walkOffRoad
	 *            whether to walk nodes when the tracker is off-road
	 */
	public JsonStateWalker(Tracker<E> tracker, SimpleWalkMethod<E> walkMethod, boolean walkAnonymousStates,
			boolean walkOffRoad) {
		this(tracker, walkMethod.asAdvancedWalkMethod(), walkAnonymousStates, walkOffRoad);
	}

	/**
	 * Create a walker with an advanced walk method
	 * 
	 * @param tracker
	 *            the tracker to use during the walk
	 * @param walkMethod
	 *            the walk method to invoke
	 */
	public JsonStateWalker(Tracker<E> tracker, AdvancedWalkMethod<E> walkMethod) {
		this(tracker, walkMethod, false, false);
	}

	/**
	 * Create a walker with an advanced walk method
	 * 
	 * @param tracker
	 *            the tracker to use during the walk
	 * @param walkMethod
	 *            the walk method to invoke
	 * @param walkAnonymousStates
	 *            whether to walk nodes corresponding to anonymous machine states
	 * @param walkOffRoad
	 *            whether to walk nodes when the tracker is off-road
	 */
	public JsonStateWalker(Tracker<E> tracker, AdvancedWalkMethod<E> walkMethod, boolean walkAnonymousStates,
			boolean walkOffRoad) {
		this.tracker = tracker;
		this.walkMethod = walkMethod;
		this.walkAnonymousStates = walkAnonymousStates;
		this.walkOffRoad = walkOffRoad;
	}

	/**
	 * Perform the walk
	 * 
	 * @param node
	 *            JsonNode value to walk
	 * @return optional replacement node. This will be present if the walk method
	 *         specified a replacement for the provided top-level node. Replacements
	 *         of interior nodes are done in-place and will therefore be reflected
	 *         in the provided node whether or not that node is replaced.
	 */
	public Optional<JsonNode> walk(JsonNode node) {
		State<E> state = tracker.getCurrentState();
		boolean replaced = false;
		boolean descend = true;
		boolean keepWalking = state != null ? state.getValue() != null ? true : walkAnonymousStates : walkOffRoad;
		while (keepWalking) {
			State<E> currentState = tracker.getCurrentState();
			Disposition disp = walkMethod.walk(node, currentState,
					currentState != null ? currentState.getValue() : null, tracker.getPath(), getPointer(tracker));
			JsonNode replacement = disp.getReplacement();
			boolean replacedThisTime = false;
			// don't do a rewalk unless it specifies a replacement node that is different
			// (as in ==, not Object#equals) from the the current node
			if (replacement != null && replacement != node) {
				node = replacement;
				replacedThisTime = replaced = true;
			}
			switch (disp.getAction()) {
			case Disposition.DESCEND:
				keepWalking = false;
				break;
			case Disposition.REWALK:
				if (!replacedThisTime) {
					keepWalking = false;
				}
				break;
			case Disposition.DONE:
				keepWalking = false;
				descend = false;
				break;
			}
		}
		if (descend) {
			if (node.isObject()) {
				walkObject((ObjectNode) node);
			} else if (node.isArray()) {
				walkArray((ArrayNode) node);
			}
		}
		return replaced ? Optional.of(node) : Optional.empty();
	}

	private void walkObject(ObjectNode node) {
		for (Iterator<String> iter = node.fieldNames(); iter.hasNext();) {
			String name = iter.next();
			tracker.move(name);
			Optional<JsonNode> replacement = walk(node.get(name));
			if (replacement.isPresent()) {
				node.set(name, replacement.get());
			}
			tracker.backup();
		}
	}

	private void walkArray(ArrayNode node) {
		for (int i = 0; i < node.size(); i++) {
			tracker.move(i);
			Optional<JsonNode> replacement = walk(node.get(i));
			if (replacement.isPresent()) {
				node.set(i, replacement.get());
			}
			tracker.backup();
		}
	}

	private JsonPointer getPointer(Tracker<E> tracker) {
		List<Object> path = tracker.getPath();
		String ptrString = path.isEmpty() ? ""
				: "/" + path.stream().map(x -> x.toString().replaceAll("~", "~0").replaceAll("/", "~1"))
						.collect(Collectors.joining("/"));
		return JsonPointer.compile(ptrString);
	}

	@FunctionalInterface
	public interface AdvancedWalkMethod<E extends Enum<E>> {
		/**
		 * Process a JsonNode value during a state walk
		 * 
		 * @param node
		 *            The node to process
		 * @param state
		 *            Current state machine state; may be null if off-road
		 * @param stateValue
		 *            State value associated with state; may be null if anonymous or
		 *            off-road
		 * @param path
		 *            the current tracker path, indicating the location fo the node in
		 *            the overall walked tree
		 * @param pointer
		 *            JsonPointer specifying location of node in overall walked tree
		 *            (constructed from tracker path)
		 * @return disposition information, including how to proceed and an optional
		 *         replacement for this node
		 */
		Disposition walk(JsonNode node, State<E> state, E stateValue, List<Object> path, JsonPointer pointer);
	}

	@FunctionalInterface
	public interface SimpleWalkMethod<E extends Enum<E>> {
		/**
		 * Like {@link AdvancedWalkMethod} but with no return value; normal disposition
		 * is always used.
		 * 
		 * @param node
		 *            The node to process
		 * @param state
		 *            Current state machine state; may be null if off-road
		 * @param stateValue
		 *            State value associated with state; may be null if anonymous or
		 *            off-road
		 * @param path
		 *            the current tracker path, indicating the location fo the node in
		 *            the overall walked tree
		 * @param pointer
		 *            JsonPointer specifying location of node in overall walked tree
		 *            (constructed from tracker path)
		 */
		void walk(JsonNode node, State<E> state, E stateValue, List<Object> path, JsonPointer pointer);

		/**
		 * Method to deliver an advanced walk method that is equivalent to this simple
		 * walk method
		 * 
		 * @return equivalent {@link AdvancedWalkMethod}
		 */
		default AdvancedWalkMethod<E> asAdvancedWalkMethod() {
			return new AdvancedWalkMethod<E>() {
				@Override
				public Disposition walk(JsonNode node, State<E> state, E stateValue, List<Object> path,
						JsonPointer pointer) {
					SimpleWalkMethod.this.walk(node, state, stateValue, path, pointer);
					return Disposition.normal();
				}
			};
		}
	}

	/**
	 * Specify the what to do after walking a node.
	 *
	 * The disposition can:
	 * <ul>
	 * <li>Provide a replacement value for the walked node</li>
	 * <li>Specify one of three behaviors wrt descending into child nodes</li>
	 * <dl>
	 * <dt>DESCEND</dt>
	 * <dd>Walk child nodes of the walked node (not of the replacment, if any)</dd>
	 * <dt>REWALK</dt>
	 * <dd>Walk the replacement node without descending into its children, and then
	 * proceed according to the resulting disposition</dd>
	 * <dt>DONE</dt>
	 * <dd>Do not walk the child nodes of the walked node</dd>
	 * </dl>
	 * 
	 * Note that <b>REWALK</b> is effective ONLY when a replacement node is
	 * provided, and that replacement is not the same as the walked node. Re-walking
	 * these conditions are not met is guaranteed to result in an infinite loop,
	 * assuming a stateless walk method. Absent these conditions, <b>REWALK</b> is
	 * treated exactly like <b>DESCEND</b>.
	 * 
	 * @author Andy Lowry
	 *
	 */
	public static class Disposition {
		public static final String DESCEND = "descend";
		public static final String REWALK = "rewalk";
		public static final String DONE = "done";

		private static final Disposition descendDisposition = descend(null);
		private static final Disposition rewalkDisposition = rewalk(null);
		private static final Disposition doneDisposition = done(null);
		private static final Disposition normalDisposition = descendDisposition;

		private String action;
		private JsonNode replacement;

		private Disposition(String action, JsonNode replacement) {
			this.action = action;
			this.replacement = replacement;
		}

		/**
		 * Alias for {@link #descend()}, to emphasize that default behavior is being
		 * speicifed.
		 * 
		 * @return
		 */
		public static Disposition normal() {
			return normalDisposition;
		}

		public static Disposition descend() {
			return descendDisposition;
		}

		public static Disposition descend(JsonNode replacement) {
			return new Disposition(DESCEND, replacement);
		}

		public static Disposition rewalk() {
			return rewalkDisposition;
		}

		public static Disposition rewalk(JsonNode replacement) {
			return new Disposition(REWALK, replacement);
		}

		public static Disposition done() {
			return doneDisposition;
		}

		public static Disposition done(JsonNode replacement) {
			return new Disposition(DONE, replacement);
		}

		public String getAction() {
			return action;
		}

		public JsonNode getReplacement() {
			return replacement;
		}
	}
}
