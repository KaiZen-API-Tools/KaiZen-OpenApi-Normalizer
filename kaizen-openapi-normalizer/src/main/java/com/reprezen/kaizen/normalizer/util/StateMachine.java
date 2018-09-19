package com.reprezen.kaizen.normalizer.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
 * Labels are strings. There are three wildcard labels:
 * 
 * <ul>
 * <li><code>"*"</code> - matches any string
 * <li><code>"re: ..." - Java regular expression; whatever follows <code>re:</code>
 * is trimmed and then parsed as a Java regular expression. The label will match
 * strings that are matched by the regex.
 * <li><code>"#"</code> - matches any integer (i.e. a move using an integer
 * value, not a string that looks like an integer)
 * </ul>
 * Note: in the unlikely event that you need any of the above to be treated as a
 * literal value instead of a wildcard, precede your desired value with a colon;
 * if you want a label to start with a colon, start with two colons.
 * <p>
 * Once the transits ave been defined, a Tracker can be created and used to move
 * through the state machine.
 * <p>
 * The tracker is instantiated with an enum identifying the start state.
 * Thereafter, the tracker can be "moved" by providing a string or integer
 * value. The first matching edge originating in the current state, if any,
 * determines the new current state. Edge ordering is determined by the order of
 * the {@link #transit()} and/or {@link #copyOutEdges(Enum, Enum)} operations
 * performed when defining the machine.
 * <p>
 * The tracker records the "path" - the sequence of move values - and the
 * corresponding history of states. When a move value does not match any edges,
 * the tracker records the value in the path and adds a null value to the state
 * history. From that point forward, the machine is "off-road", and all moves
 * will result in null states being added to the history.
 * <p>
 * The tracker can also back up, removing items from the tails of the path and
 * the state history. Backing up when off-road removes the nulls caused by
 * non-matching moves. The tracker can also be reset to a given state, which
 * clears the path and history as a side-effect
 * 
 * @author Andy Lowry
 *
 * @param <E>
 *            enum whose values are used for named states
 */
public class StateMachine<E extends Enum<E>> {

	private Map<E, State<E>> namedStates = new HashMap<>();
	private Map<State<E>, List<Edge<E>>> graph = new IdentityHashMap<>();
	private Map<State<E>, Map<String, State<E>>> graphCache = new IdentityHashMap<>();
	private Map<State<E>, State<E>> graphIntCache = new IdentityHashMap<>();
	private E anonymousValue = null;
	private E offRoadValue = null;
	private Class<E> stateClass;

	/**
	 * Create a new state machine instance, with no special values for anonymous and
	 * off-road states.
	 * 
	 * @param stateClass
	 *            class of the state enum type
	 */
	public StateMachine(Class<E> stateClass) {
		this(stateClass, null, null);
	}

	/**
	 * Create a new state machine with special enum values for anonymous and
	 * off-road states.
	 * 
	 * @param stateClass
	 *            class of the state enum type
	 * @param anonymousValue
	 *            value to be used for anonymous states, or null for none
	 * @param offRoadValue
	 *            value to be used for off-road states, or null for none
	 */
	public StateMachine(Class<E> stateClass, E anonymousValue, E offRoadValue) {
		this.stateClass = stateClass;
		this.anonymousValue = anonymousValue;
		this.offRoadValue = offRoadValue;
	}

	/**
	 * Determine the state that the given string move value should move to, by
	 * considering the given state's out edges
	 * 
	 * @param start
	 *            start state for the presumed move
	 * @param value
	 *            value for the move
	 * @return end state of the presumed move
	 */
	public State<E> getMoveTarget(State<E> start, String value) {
		if (graphCache.containsKey(start)) {
			if (graphCache.get(start).containsKey(value)) {
				return graphCache.get(start).get(value);
			}
		}
		for (Edge<E> edge : getOutEdges(start)) {
			if (edge.matches(value)) {
				cacheMove(start, value, edge.getTarget());
				return edge.getTarget();
			}
		}
		return null;
	}

	/**
	 * Determine the state that the given integer move value should move to, by
	 * considering the given state's out edges
	 * 
	 * @param start
	 *            start state for the presumed move
	 * @param value
	 *            value for the move
	 * @return end state of the presumed move
	 */
	public State<E> getMoveTarget(State<E> start, int value) {
		if (!graphIntCache.containsKey(start)) {
			for (Edge<E> edge : getOutEdges(start)) {
				if (edge.matches(value)) {
					graphIntCache.put(start, edge.getTarget());
				}
			}
		}
		return graphIntCache.get(start);
	}

	private void cacheMove(State<E> from, String value, State<E> to) {
		if (!graphCache.containsKey(from)) {
			graphCache.put(from, new HashMap<String, State<E>>());
		}
		graphCache.get(from).put(value, to);
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
		List<String> moves = transit.getMoves();
		int size = moves.size();
		for (int i = 0; i < size - 1; i++) {
			current = installEdge(current, moves.get(i));
		}
		installEdge(current, moves.get(size - 1), transit.getEndState());
	}

	private State<E> installEdge(State<E> start, String label) {
		return installEdge(start, label, null);
	}

	private State<E> installEdge(State<E> start, String label, State<E> end) {
		List<Edge<E>> existingEdges = getOutEdges(start);
		for (Edge<E> existingEdge : existingEdges) {
			if (existingEdge.getLabel().equals(label)) {
				State<E> target = existingEdge.getTarget();
				if (end == null || end == target) {
					return target;
				} else {
					throw new IllegalArgumentException("Multiple edges with label '" + label + "' from same state");
				}
			}
		}
		// no existing edge with identical label... create new edge, either to provided
		// end state or to a new anonymous state
		State<E> target = end != null ? end : new State<E>(anonymousValue, true);
		existingEdges.add(new Edge<E>(label, target));
		return target;
	}

	private List<Edge<E>> getOutEdges(State<E> state) {
		if (!graph.containsKey(state)) {
			graph.put(state, new ArrayList<Edge<E>>());
		}
		return graph.get(state);
	}

	/**
	 * Copy the outgoing edges from one named state to another.
	 * 
	 * Existing edges for the to-state are not affected, but precede the copied
	 * edges when it comes to matching priority.
	 * 
	 * @param from
	 *            state whose edges are to be copied
	 * @param to
	 *            state that will receive the copies
	 */
	public void copyOutEdges(E from, E to) {
		State<E> fromState = getState(from);
		State<E> toState = getState(to);
		List<Edge<E>> edges = graph.get(fromState);
		if (edges != null) {
			for (Edge<E> edge : edges) {
				installEdge(toState, edge.getLabel(), edge.getTarget());
			}
		}
	}

	/**
	 * Return the named state for the given name value.
	 * <p>
	 * This method always returns the same state object for the same name value.
	 * 
	 * @param name
	 *            one of the values of the enum used for name states
	 * @return unique state value for the given name value
	 */
	public State<E> getState(E name) {
		if (!namedStates.containsKey(name)) {
			State<E> state = new State<E>(name, false);
			namedStates.put(name, state);
		}
		return namedStates.get(name);
	}

	/**
	 * Return the named state for the given enum name value
	 * <p>
	 * This is just like {@link #getState(Enum)}, but the enum value is specified by
	 * its name.
	 * 
	 * @param name
	 * @return
	 */
	public State<E> getState(String name) {
		return getState(Enum.valueOf(stateClass, name));
	}

	/**
	 * An edge, characterized by a label and a target state.
	 * 
	 * @author Andy Lowry
	 *
	 * @param <E>
	 */
	public static class Edge<E extends Enum<E>> {
		private String label;
		private State<E> target;
		private EdgeType type;
		private Pattern pattern = null;
		private String value = null;

		/**
		 * Create a new edge.
		 * <p>
		 * Label values are interpreted as follows:
		 * <dl>
		 * <dt><code>*</code></dt>
		 * <dd>Wildcard - equivalent to <code>"re: .*"</code></dd>
		 * <dt><code>re: <i>regex</i></code></dt>
		 * <dd>Regex - everything following <code>":re"</code> is interpreted as a Java
		 * regular expression.</dd>
		 * <dt><code>#</code></dt>
		 * <dd>Any integer value (not a string that looks like an integer)</dd>
		 * <dt><code>:<i>anything</i></code></dt>
		 * <dd>A fixed-string label defined by the string following the initial
		 * colon</dd>
		 * <dt><code><i>anything-else</i></code></dt>
		 * <dd>A fixed-string label</dd>
		 * </ul>
		 * <p>
		 * 
		 * @param label
		 *            the label string
		 * @param target
		 *            the target state
		 */
		public Edge(String label, State<E> target) {
			this.label = label;
			this.target = target;
			if (label.equals("#")) {
				this.type = EdgeType.INTEGER;
			} else if (label.equals("*")) {
				this.pattern = Pattern.compile(".*");
				this.type = EdgeType.REGEX;
			} else if (label.startsWith("re:")) {
				this.pattern = Pattern.compile(label.substring(3).trim());
				this.type = EdgeType.REGEX;
			} else {
				this.value = label.startsWith(":") ? label.substring(1) : label;
				this.type = EdgeType.FIXED_STRING;
			}
		}

		/**
		 * Return the label used to define this edge (not any of its interpretations)
		 * 
		 * @return
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * Return the type of this edge: one of <code>FIXED_STRING</code>,
		 * <code>REGEX</code>, and <code>INTEGER</code>.
		 * 
		 * @return
		 */
		public EdgeType getType() {
			return type;
		}

		/**
		 * Return the fixed value determined from the label, if the edge type is
		 * <code>FIXED_VALUE</code>
		 * 
		 * @return the fixed value, or null if this is not a fixed value edge
		 */
		public String getFixedValue() {
			return value;
		}

		/**
		 * Return the pattern determined from the label, if the edge type is
		 * <code>REGEX</code>.
		 * 
		 * @return the regex pattern, or null if this is not a regex edge
		 */
		public Pattern getRegex() {
			return pattern;
		}

		/**
		 * Determine whether this edge matches a given string value
		 * 
		 * @param s
		 *            the string value
		 * @return true if this edge matches
		 */
		/**
		 * @param s
		 * @return
		 */
		public boolean matches(String s) {
			switch (type) {
			case FIXED_STRING:
				return value.equals(s);
			case REGEX:
				return pattern.matcher(s).matches();
			case INTEGER:
				return false;
			default:
				return false;
			}
		}

		/**
		 * Determine whether this edge matches a given integer value.
		 * 
		 * @param i
		 *            the integer value
		 * @return true if this edge matches (i.e. its any integer-type edge)
		 */
		public boolean matches(int i) {
			return type == EdgeType.INTEGER;
		}

		/**
		 * Get the target state of this edge.
		 * 
		 * @return
		 */
		public State<E> getTarget() {
			return target;
		}

		public enum EdgeType {
			FIXED_STRING, REGEX, INTEGER
		};
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
		List<String> moves = new ArrayList<>();
		State<E> endState = null;

		/**
		 * Identify a named state for the beginning of the transit.
		 * 
		 * @param start
		 * @return
		 */
		public TransitDef from(E start) {
			if (startState != null || !moves.isEmpty() || endState != null) {
				throw new IllegalStateException();
			}
			this.startState = getState(start);
			return this;
		}

		/**
		 * Provide labels for the edges that will be used/created for the transit
		 * 
		 * @param moves
		 * @return
		 */
		public TransitDef via(String... moves) {
			if (startState == null || endState != null) {
				throw new IllegalStateException();
			}
			this.moves.addAll(Arrays.asList(moves));
			return this;
		}

		/**
		 * Provide a named state for the end of the transit.
		 * 
		 * @param end
		 */
		public void to(E end) {
			if (startState == null || moves.isEmpty() || endState != null) {
				throw new IllegalStateException();
			}
			this.endState = getState(end);
			installTransit(this);
		}

		public State<E> getStartState() {
			return startState;
		}

		public List<String> getMoves() {
			return moves;
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
	public static class Tracker<E extends Enum<E>> {
		private StateMachine<E> machine;
		private State<E> currentState;
		private List<State<E>> crumbs = new ArrayList<>();
		private List<Object> path = new ArrayList<>();
		private E offRoadValue = null;
		private State<E> initialStartState;

		/**
		 * Create a tracker for a given state machine with a given start state
		 * 
		 * @param machine
		 *            the state machine
		 * @param start
		 *            the start state enum value
		 */
		private Tracker(StateMachine<E> machine, E start) {
			this(machine, machine.getState(start));
		}

		/**
		 * Create a tracker for a given state machine with a given start state
		 * 
		 * @param machine
		 *            the state machine
		 * @param start
		 *            the start state value
		 */
		private Tracker(StateMachine<E> machine, State<E> start) {
			this.machine = machine;
			this.currentState = start;
			this.offRoadValue = machine.getOffRoadValue();
			this.initialStartState = start;
		}

		/**
		 * Get the tracker's current state
		 * 
		 * If the tracker is off-road, null will be returned unless an off-road enum
		 * value has been set for the machine. In that case, a new State instance will
		 * be created and returned, using that value.
		 * 
		 * @return the current state, or null-or-offroad-state if the current path
		 *         includes a unmatched move.
		 */
		public State<E> getCurrentState() {
			return currentState;
		}

		/**
		 * Move to a new state based on a string value
		 * 
		 * @param value
		 *            value to match against available edges
		 * @return new current state, or null if current state was already null, or if
		 *         there was no matching edge (or the anonymous or off-road state
		 *         instead of null, if they have been provided for the machine)
		 */
		public State<E> move(String value) {
			return moveTo(peek(value), value);
		}

		/**
		 * Move to a new state based on an integer value
		 * 
		 * @param value
		 *            integer value. Only an edge labeled "#" can match. N.B. An edge
		 *            that matches <code>Integer.toString(value)</code> will NOT match.
		 * @return new current state, or null if current state was already null, or if
		 *         there was no matching edge (or the anonymous or off-road state
		 *         instead of null, if they have been provided for the machine)
		 */
		public State<E> move(int value) {
			return moveTo(peek(value), value);
		}

		private State<E> moveTo(State<E> newState, Object edgeValue) {
			if (newState == null && offRoadValue != null) {
				newState = new State<E>(offRoadValue, false);
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
		 * @param value
		 * 
		 * @return the state that would become current, or null if the current state is
		 *         already null or if there is no matching edge (or anonymous or
		 *         off-road state instead of null, if they have been provided for the
		 *         machine)
		 */
		public State<E> peek(String value) {
			return machine.getMoveTarget(currentState, value);
		}

		/**
		 * Determine the state that would be current after a move to the given value,
		 * but don't actually perform the move.
		 * 
		 * @param value
		 * @return the state that would become current, or null if the current state is
		 *         already null or if there is no matching edge (or anonymous or
		 *         off-road state instead of null, if they have been provided for the
		 *         machine)
		 */
		public State<E> peek(int value) {
			return machine.getMoveTarget(currentState, value);
		}

		/**
		 * Back up one move.
		 * 
		 * The final entries on the current path and the state history are removed.
		 * 
		 * @return the new current state - may be null if the tracker is still off-road
		 *         or the new state is anonymous - or the anonymous or off-road state
		 *         instead of null, if they have been provided for the machine)
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
		 *         or if the new state is an anonymous state - or the anonymous or
		 *         off-road value instead of null if they have been provided for the
		 *         machine)
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

		/**
		 * Reset the tracker to the given state.
		 * <p>
		 * This clears the path and state history.
		 * 
		 * @param state
		 *            the state to become the new current state
		 */
		public void reset(State<E> state) {
			this.currentState = state;
			this.crumbs.clear();
			this.path.clear();
		}

		/**
		 * Reset the tracker to the given state.
		 * <p>
		 * This clears the path and state history.
		 * 
		 * @param state
		 *            the enum value for the state to become the new current state
		 */
		public void reset(E stateValue) {
			reset(machine.getState(stateValue));
		}

		/**
		 * Reset the tracker to the start state that was specified at its creation.
		 */
		public void reset() {
			reset(this.initialStartState);
		}

		/**
		 * Get the list of values that led from the start state to the current state.
		 * 
		 * @return list of string and integer values used in moves
		 */
		public List<Object> getPath() {
			return new ArrayList<>(path);
		}

		/**
		 * Return the value used for off-road states, if one has been provided for the
		 * machine
		 * 
		 * @return off-road value, or null if none has been provided
		 */
		public E getOffRoadValue() {
			return offRoadValue;
		}
	}

	/**
	 * A state in a state machine.
	 * <p>
	 * Encompasses a value from the underlying enum type.
	 * <p>
	 * The encompassed value may be null in the case of an anonymous or off-road
	 * state, if the state machine has not been configured with enum values to use
	 * for such states.
	 * 
	 * @author Andy Lowry
	 *
	 * @param <E>
	 *            the underlying enum type
	 */
	public static class State<E extends Enum<E>> {
		private E value = null;
		private boolean anonymous;

		/**
		 * Create a new state for the given enum value
		 * 
		 * @param value
		 *            enum value, or null for an off-road state; may also be null for an
		 *            anonymous state
		 * @param anonymous
		 *            whether this is an anonymous state
		 */
		private State(E value, boolean anonymous) {
			this.value = value;
			this.anonymous = anonymous;
		}

		/**
		 * Get this state's enum value
		 * 
		 * @return the enum naming this state. This may be null for an anonymous or
		 *         off-road state.
		 */
		public E getValue() {
			return value;
		}

		public boolean isAnonymous() {
			return anonymous;
		}

		@Override
		public String toString() {
			return String.format("State[%s]", getValue());
		}
	}
}
