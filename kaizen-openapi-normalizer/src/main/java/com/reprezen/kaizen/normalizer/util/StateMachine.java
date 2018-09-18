package com.reprezen.kaizen.normalizer.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class for defining and moving through a state machine.
 * <p>
 * States come in two varieties:
 * <ul>
 * <li>Named states are named with values from the enum class provided for the
 * generic type parameter. Because enums can have fields and methods, these can
 * actually serve as much more than names, including whatever information or
 * functionality may be required by the application.</li>
 * 
 * <li>Unnamed states are created as needed for multi-step transitions between
 * named states.</li>
 * </ul>
 * 
 * The state machine is defined by creating a collection of <i>transits</i>.
 * Each transit defines a path from a start state to an end state along one or
 * more labeled edges.
 * <p>
 * Labels are strings. There are two wildcard labels:
 * 
 * <ul>
 * <li><code>"*"</code> - matches any string
 * <li><code>"#"</code> - matches any integer
 * </ul>
 * 
 * Once the transits ave been defined, a Tracker can be created and used to move
 * through the state machine.
 * <p>
 * The tracker is instantiated with an enum identifying the start state.
 * Thereafter, the tracker can be "moved" by providing a string or integer
 * value. If a matching edge exists from the current state, its destination
 * becomes the new current state. If a string value does not correspond to an
 * edge, a "*" edge, if present, will be used instead. If an int value is
 * provided, a "#" edge will be followed, if present.
 * <p>
 * The tracker records the "path" - the sequence of move values - and the
 * corresponding history of states. When a move value does not match any edges,
 * the tracker records the value in the path and adds a null value to the state
 * history. From that point forward, the machine is "off-road", and all moves
 * will result in null states being added to the history.
 * <p>
 * The tracker can also back up, removing items from the tails of the path and
 * the state history. Backing up when off-road removes the nulls caused by
 * non-matching moves.
 * 
 * @author Andy Lowry
 *
 * @param <E>
 *            enum whose values are used for named states
 */
public class StateMachine<E extends Enum<E>> {

	private Map<E, State<E>> namedStates = new HashMap<>();
	private Map<State<E>, Map<String, State<E>>> graph = new IdentityHashMap<>();
	private E anonymousValue = null;
	private E offRoadValue = null;

	/**
	 * Create a new state machine instance, with no special values for anonymous and
	 * off-road states.
	 */
	public StateMachine() {
	}

	/**
	 * Create a new state machine with special enum values for anonymous and
	 * off-road states.
	 * 
	 * @param anonymousValue
	 *            value to be used for anonymous states, or null for none
	 * @param offRoadValue
	 *            value to be used for off-road states, or null for none
	 */
	public StateMachine(E anonymousValue, E offRoadValue) {
		this.anonymousValue = anonymousValue;
		this.offRoadValue = offRoadValue;
	}

	/**
	 * Define a new transit for the machine, consisting of a named start stated, a
	 * sequence of edge labels, and a named end state.
	 * 
	 * Existing edges are used if they exist. Otherwise new edges are created as
	 * needed. For all but the final edge, these will lead to new anonymous states.
	 * The final edge will lead to the provided end state.
	 * 
	 * If the entire transit is already present in the machine but it ends in a
	 * state other than the provided end state, the transit definition fails.
	 * 
	 * @return
	 */
	public TransitDef transit() {
		return new TransitDef();
	}

	/**
	 * Create a new tracker for moving through the machine
	 * 
	 * @param start
	 *            identify named state to start from
	 * @return the new tracker
	 */
	public Tracker<E> tracker(E start) {
		return new Tracker<E>(this, start);
	}

	/**
	 * Create a new tracker for moving throug the machine.
	 * 
	 * @param start
	 *            the state to start from (may be anonymous, or null to start
	 *            off-road; a tracker that starts off-road can never get back on)
	 * @return the new tracker
	 */
	public Tracker<E> tracker(State<E> start) {
		return new Tracker<E>(this, start);
	}

	/**
	 * Get the value used for anonymous states
	 * 
	 * @return anonymous state value, or null if one has not been set
	 */
	public E getAnonymousValue() {
		return anonymousValue;
	}

	/**
	 * Get the value used for off-road states
	 * 
	 * @return off-road state value, or null if none has been set
	 */
	public E getOffRoadValue() {
		return offRoadValue;
	}

	private void installTransit(TransitDef transit) {
		State<E> current = transit.getStartState();
		List<String> edges = transit.getEdges();
		int size = edges.size();
		for (int i = 0; i < size - 1; i++) {
			current = installEdge(current, edges.get(i));
		}
		installEdge(current, edges.get(size - 1), transit.getEndState());
	}

	/**
	 * Copy the outgoing edges from one named state to another.
	 * 
	 * Existing edges with the same labels are overwritten.
	 * 
	 * @param from
	 * @param to
	 */
	public void copyOutEdges(E from, E to) {
		State<E> fromState = getState(from);
		State<E> toState = getState(to);
		Map<String, State<E>> edges = graph.get(fromState);
		if (edges != null) {
			for (Entry<String, State<E>> edge : edges.entrySet()) {
				installEdge(toState, edge.getKey(), edge.getValue());
			}
		}
	}

	private State<E> installEdge(State<E> from, String edge) {
		return installEdge(from, edge, null);
	}

	private State<E> installEdge(State<E> from, String edge, State<E> to) {
		if (!graph.containsKey(from)) {
			graph.put(from, new HashMap<>());
		}
		State<E> target = graph.get(from).get(edge);
		if (target != null) {
			// edge already exists - must match to-state if provided
			if (to != null && to != target) {
				throw new IllegalArgumentException();
			}
		} else {
			// edge not in graph - use given to-state if provided
			target = to;
			if (target == null) {
				// else allocate a new state as target
				target = new State<E>(anonymousValue);
			}
			// install this as a new edge in the graph
			graph.get(from).put(edge, target);
		}
		return target;
	}

	public State<E> getState(E name) {
		if (!namedStates.containsKey(name)) {
			State<E> state = new State<E>(name);
			namedStates.put(name, state);
		}
		return namedStates.get(name);
	}

	private Map<String, State<E>> getOutEdges(State<E> state) {
		return graph.get(state);
	}

	/**
	 * Class that defines a transit to be created in the event machine
	 * 
	 * A typical design is to create class XStateMachine with an inner enum class
	 * called XState, and have XStateMachine extend StateMachine<XState>. Then
	 * transits can be defined in the constructor of XStateMachine, so a new
	 * instance of XStateMachine is ready for moves.
	 * 
	 * @author Andy Lowry
	 */
	public class TransitDef {
		State<E> startState = null;
		List<String> edges = new ArrayList<>();
		State<E> endState = null;

		/**
		 * Identify a named state for the beginning of the transit.
		 * 
		 * @param start
		 * @return
		 */
		public TransitDef from(E start) {
			if (startState != null || !edges.isEmpty() || endState != null) {
				throw new IllegalStateException();
			}
			this.startState = getState(start);
			return this;
		}

		/**
		 * Provide labels for the edges that will be used/created for the transit
		 * 
		 * @param edges
		 * @return
		 */
		public TransitDef via(String... edges) {
			if (startState == null || endState != null) {
				throw new IllegalStateException();
			}
			this.edges.addAll(Arrays.asList(edges));
			return this;
		}

		/**
		 * Provide a named state for the end of the transit.
		 * 
		 * @param end
		 */
		public void to(E end) {
			if (startState == null || edges.isEmpty() || endState != null) {
				throw new IllegalStateException();
			}
			this.endState = getState(end);
			installTransit(this);
		}

		public State<E> getStartState() {
			return startState;
		}

		public List<String> getEdges() {
			return edges;
		}

		public State<E> getEndState() {
			return endState;
		}

	}

	/**
	 * Class for exercising an state machine.
	 * 
	 * @author Andy Lowry
	 *
	 */
	/**
	 * @author Andy Lowry
	 *
	 */
	public static class Tracker<E extends Enum<E>> {
		private StateMachine<E> machine;
		private State<E> currentState;
		private List<State<E>> crumbs = new ArrayList<>();
		private List<Object> path = new ArrayList<>();
		private E offRoadValue = null;

		private Tracker(StateMachine<E> machine, E start) {
			this(machine, machine.getState(start));
		}

		private Tracker(StateMachine<E> machine, State<E> start) {
			this.machine = machine;
			this.currentState = start;
			this.offRoadValue = machine.getOffRoadValue();
		}

		/**
		 * Get the machine's current state
		 * 
		 * If the tracker is off-road, null will be returned unless an off-road enum
		 * value has been set for the machine. In that case, a new State instance will
		 * be created and returned, using that value.
		 * 
		 * @return the current state, or null if the current path includes a unmatched
		 *         move.
		 */
		public State<E> getCurrentState() {
			return currentState;
		}

		/**
		 * Move to a new state based on a string value
		 * 
		 * @param edgeValue
		 *            value to match against available edges. An edge labeled "*" will
		 *            be considered if no non-wild edge matches.
		 * @return new current state, or null if current state was already null, or if
		 *         there was no matching edge
		 */
		public State<E> move(String edgeValue) {
			return moveTo(peek(edgeValue, true), edgeValue);
		}

		/**
		 * Move to a new state based on an int value
		 * 
		 * @param edgeValue
		 *            integer value. Only an edge labeled "#" can match. N.B. An edge
		 *            labeled with Integer.toString(edgeValue) will NOT match.
		 * @return new current state, or null if current state was already null, or if
		 *         there was no matching edge
		 */
		public State<E> move(int edgeValue) {
			return moveTo(peek("#", false), edgeValue);
		}

		private State<E> moveTo(State<E> newState, Object edgeValue) {
			if (newState == null && offRoadValue != null) {
				newState = new State<E>(offRoadValue);
			}
			crumbs.add(currentState);
			path.add(edgeValue);
			currentState = newState;
			return currentState;
		}

		/**
		 * Determine the state that would be current after a move to the given value,
		 * but don't actually perform the move.
		 * 
		 * @param edgeValue
		 * @param wildOk
		 *            whether to consider an edge labeled "*"
		 * @return the state that would become current, or null if the current state is
		 *         already null or if there is no matching edge
		 */
		public State<E> peek(String edgeValue, boolean wildOk) {
			Map<String, State<E>> edges = machine.getOutEdges(currentState);
			State<E> result = edges != null ? edges.get(edgeValue) : null;
			if (result == null && wildOk) {
				result = edges != null ? edges.get("*") : null;
			}
			return result;
		}

		/**
		 * Determine the state that would be current after a mvoe to the given value,
		 * but don't actually perform the move.
		 * 
		 * @param edgeValue
		 * @return the state that would become current, or null if the current state is
		 *         already null or if there is no matching edge
		 */
		public State<E> peek(int edgeValue) {
			return peek("#", false);
		}

		/**
		 * Back up one move.
		 * 
		 * The final entries on the current path and the state history are removed.
		 * 
		 * @return the new current state - may be null if the machine is still off-road
		 */
		public State<E> backup() {
			return backup(1);
		}

		/**
		 * Back up a given number of moves
		 * 
		 * @param n
		 *            number of moves to back out of
		 * @return the new current state - may be null if the machine is still off-road
		 */
		public State<E> backup(int n) {
			while (n-- > 0) {
				if (crumbs.isEmpty()) {
					throw new IllegalArgumentException("Can't back up past initial state");
				} else {
					currentState = crumbs.remove(crumbs.size() - 1);
					path.remove(path.size() - 1);
				}
			}
			return currentState;
		}

		public List<Object> getPath() {
			return new ArrayList<>(path);
		}
	}

	public static class State<E extends Enum<E>> {
		private E value = null;

		private State(E name) {
			this.value = name;
		}

		/**
		 * Get this state's name
		 * 
		 * @return the enum naming this state, or null if this is an anonymous state
		 */
		public E getValue() {
			return value;
		}

		@Override
		public String toString() {
			return String.format("State[%s]", getValue());
		}
	}

}
