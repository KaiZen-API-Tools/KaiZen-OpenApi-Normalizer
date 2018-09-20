package com.reprezen.kaizen.normalizer.util;

import static com.reprezen.kaizen.normalizer.util.JsonStateWalker.Disposition.Action.DESCEND;
import static com.reprezen.kaizen.normalizer.util.JsonStateWalker.Disposition.Action.DONE;
import static com.reprezen.kaizen.normalizer.util.JsonStateWalker.Disposition.Action.REVISIT;

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
 * as a visitn method in the form of either an {@link AdvancedVisitnMethod} or a
 * {@link SimpleVisitMethod}. As the tree is walked (in a depth-first fashion),
 * the visit method is invoked for each visited node.
 * 
 * When visiting the children of an object node, the tracker moves to each
 * property name before visiting the property value, and then backs up
 * immediately afterward. When visiting the children of an array node, the
 * tracker moves to each element index before visiting the element, and then
 * backs up immediately afterward.
 * 
 * An advanced visit method is capable of:
 * <ul>
 * <li>providing a replacement JsonNode value for the node being visited;</li>
 * <li>controlling descent into the visited node's children; and</li>
 * <li>providing a callback method (void, no-arg) to be invoked just before
 * completing this visit.</li>
 * <ul>
 * A simple visit method, in contrast, can do none of these; descents always
 * happen, and no replacements or callbacks occur as a result of the visit.
 * 
 * The walker can optionally suppress invocation of the visit method whenever
 * the current state reported by the tracker is anonymous, or when the tracker
 * is off-road, or both.
 * 
 * @author Andy Lowry
 *
 * @param <E>
 */
public class JsonStateWalker<E extends Enum<E>> {

	private Tracker<E> tracker;
	private AdvancedVisitMethod<E> visitMethod;
	private boolean visitAnonymousStates;
	private boolean walkOffRoad;

	/**
	 * Create a walker with a simple walk method
	 * 
	 * @param tracker
	 *            the tracker to use during the walk
	 * @param visitMethod
	 *            the visit method to invoke
	 */
	public JsonStateWalker(Tracker<E> tracker, SimpleVisitMethod<E> visitMethod) {
		this(tracker, visitMethod, false, false);
	}

	/**
	 * Create a walker with a simple visit method
	 * 
	 * @param tracker
	 *            the tracker to use during the walk
	 * @param visitMethod
	 *            the visit method to invoke
	 * @param visitAnonymousStates
	 *            whether to visit nodes corresponding to anonymous machine states
	 * @param walkOffRoad
	 *            whether to visit nodes when the tracker is off-road
	 */
	public JsonStateWalker(Tracker<E> tracker, SimpleVisitMethod<E> visitMethod, boolean visitAnonymousStates,
			boolean walkOffRoad) {
		this(tracker, visitMethod.asAdvancedVisitMethod(), visitAnonymousStates, walkOffRoad);
	}

	/**
	 * Create a walker with an advanced visit method
	 * 
	 * @param tracker
	 *            the tracker to use during the walk
	 * @param visitMethod
	 *            the visit method to invoke
	 */
	public JsonStateWalker(Tracker<E> tracker, AdvancedVisitMethod<E> visitMethod) {
		this(tracker, visitMethod, false, false);
	}

	/**
	 * Create a walker with an advanced visit method
	 * 
	 * @param tracker
	 *            the tracker to use during the walk
	 * @param visitMethod
	 *            the visit method to invoke
	 * @param visitAnonymousStates
	 *            whether to visit nodes corresponding to anonymous machine states
	 * @param walkOffRoad
	 *            whether to visit nodes when the tracker is off-road
	 */
	public JsonStateWalker(Tracker<E> tracker, AdvancedVisitMethod<E> visitMethod, boolean visitAnonymousStates,
			boolean walkOffRoad) {
		this.tracker = tracker;
		this.visitMethod = visitMethod;
		this.visitAnonymousStates = visitAnonymousStates;
		this.walkOffRoad = walkOffRoad;
	}

	/**
	 * Perform the walk
	 * 
	 * @param node
	 *            root of the JsonNode tree to walk
	 * @return optional replacement node. This will be present if the visit method
	 *         specified a replacement for the provided top-level node. Replacements
	 *         of interior nodes are done in-place and will therefore be reflected
	 *         in the provided node whether or not that node is replaced.
	 */
	public Optional<JsonNode> walk(JsonNode node) {
		return visit(node);
	}

	private Optional<JsonNode> visit(JsonNode node) {
		State<E> state = tracker.getCurrentState();
		boolean replaced = false;
		boolean descend = true;
		boolean keepVisiting = state != null ? (state.isAnonymous() ? visitAnonymousStates : true) : walkOffRoad;
		// set up default disposition if we're not visiting this state:
		// - descend if because it's anonymous (we may hit named states during descent)
		// - done if because we're off-road (we'll be off-road throughout descent)
		Disposition disp = state != null || walkOffRoad ? Disposition.descend() : Disposition.done();
		while (keepVisiting) {
			State<E> currentState = tracker.getCurrentState();
			disp = visitMethod.visit(node, currentState,
					currentState != null ? currentState.getValue() : tracker.getOffRoadValue(), tracker.getPath(),
					getPointer(tracker));
			JsonNode replacement = disp.getReplacement();
			boolean replacedThisTime = false;
			// don't do a revisit unless it specifies a replacement node that is different
			// (as in ==, not Object#equals) from the the current node
			if (replacement != null && replacement != node) {
				node = replacement;
				replacedThisTime = replaced = true;
			}
			switch (disp.getAction()) {
			case DESCEND:
				keepVisiting = false;
				break;
			case REVISIT:
				if (!replacedThisTime) {
					keepVisiting = false;
				}
				break;
			case DONE:
				keepVisiting = false;
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
		if (disp.getCallback() != null) {
			disp.getCallback().call();
		}
		return replaced ? Optional.of(node) : Optional.empty();
	}

	private void walkObject(ObjectNode node) {
		for (Iterator<String> iter = node.fieldNames(); iter.hasNext();) {
			String name = iter.next();
			tracker.move(name);
			Optional<JsonNode> replacement = visit(node.get(name));
			if (replacement.isPresent()) {
				node.set(name, replacement.get());
			}
			tracker.backup();
		}
	}

	private void walkArray(ArrayNode node) {
		for (int i = 0; i < node.size(); i++) {
			tracker.move(i);
			Optional<JsonNode> replacement = visit(node.get(i));
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
	public interface AdvancedVisitMethod<E extends Enum<E>> {
		/**
		 * Process a JsonNode value during a state visit
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
		Disposition visit(JsonNode node, State<E> state, E stateValue, List<Object> path, JsonPointer pointer);
	}

	@FunctionalInterface
	public interface SimpleVisitMethod<E extends Enum<E>> {
		/**
		 * Like {@link AdvancedVisitMethod} but with no return value; normal disposition
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
		void visit(JsonNode node, State<E> state, E stateValue, List<Object> path, JsonPointer pointer);

		/**
		 * Method to deliver an advanced visit method that is equivalent to this simple
		 * visit method
		 * 
		 * @return equivalent {@link AdvancedVisitMethod}
		 */
		default AdvancedVisitMethod<E> asAdvancedVisitMethod() {
			return new AdvancedVisitMethod<E>() {
				@Override
				public Disposition visit(JsonNode node, State<E> state, E stateValue, List<Object> path,
						JsonPointer pointer) {
					SimpleVisitMethod.this.visit(node, state, stateValue, path, pointer);
					return Disposition.normal();
				}
			};
		}
	}

	/**
	 * Specify the what to do after visiting a node.
	 *
	 * The disposition can:
	 * <ul>
	 * <li>Provide a replacement value for the visited node</li>
	 * <li>Specify one of three behaviors wrt descending into child nodes</li>
	 * <dl>
	 * <dt>DESCEND</dt>
	 * <dd>Visit child nodes of the visited node (not of the replacment, if
	 * any)</dd>
	 * <dt>REWALK</dt>
	 * <dd>Visit the replacement node without descending into its children, and then
	 * proceed according to the resulting disposition</dd>
	 * <dt>DONE</dt>
	 * <dd>Do not visit the child nodes of the visited node</dd>
	 * </dl>
	 * 
	 * Note that <b>REWALK</b> is effective ONLY when a replacement node is
	 * provided, and that replacement is not the same as the visited node.
	 * Re-visiting these conditions are not met is guaranteed to result in an
	 * infinite loop, assuming a stateless visit method. Absent these conditions,
	 * <b>REWALK</b> is treated exactly like <b>DESCEND</b>.
	 * 
	 * @author Andy Lowry
	 *
	 */
	public static class Disposition {
		public static enum Action {
			DESCEND, REVISIT, DONE
		}

		private Action action;
		private JsonNode replacement;
		private CallbackMethod callback;

		public Disposition(Action action) {
			this.action = action;
		}

		/**
		 * Alias for {@link #descend()}, to emphasize that default behavior is being
		 * speicifed.
		 * 
		 * @return
		 */
		public static Disposition normal() {
			return descend();
		}

		public static Disposition descend() {
			return new Disposition(DESCEND);
		}

		public static Disposition revisit() {
			return new Disposition(REVISIT);
		}

		public static Disposition done() {
			return new Disposition(DONE);
		}

		public Disposition withReplacement(JsonNode replacement) {
			this.replacement = replacement;
			return this;
		}

		public Disposition withCallback(CallbackMethod callback) {
			this.callback = callback;
			return this;
		}

		public Action getAction() {
			return action;
		}

		public JsonNode getReplacement() {
			return replacement;
		}

		public CallbackMethod getCallback() {
			return callback;
		}
	}

	@FunctionalInterface
	public interface CallbackMethod {
		void call();
	}
}
