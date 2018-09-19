package com.reprezen.kaizen.normalizer.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to help detecting cycles during processing.
 * <p>
 * Detection means "visiting" a value object when an equal value is already
 * being visited.
 * <p>
 * In a typical recursive scenarios, values are visited while descending deeper
 * into the recursion, and unvisited during the unwinding of the recursion. In
 * tree processing, the current path from root would be reflected in the
 * collection of values being visited, and that collection will grow and shrink
 * as processing moves down and up the tree.
 * <p>
 * Value equality can be based on the equals implemented for the value type, or
 * it can be based on Object equality, depending on a constructor choice.
 * <p>
 * Two usage patterns are supported (they may be mixed):
 * <dl>
 * <dt>Visit/Unvisit</dt>
 * <dd>Code uses {@link #visit(Object)} and {@link #unvisit(Object)} to add and
 * remove values to/from the collection of currently visited values.</dd>
 * <dt>Visitation Resource</dt>
 * <dd>Allocate a {@link Visitation} object in a try-resource block; the visit
 * will be terminated automatically at block exit.</dd>
 * </dl>
 * 
 * If an attempt is made to visit a value that's currently being visited, that
 * attempt fails. There are two ways for this to manifest, depending on a
 * constructor option:
 * <dl>
 * <dt>Boolean result</dt>
 * <dd>The {@link #visit(Object)} method returns false, or
 * {@link Visitation#isVisited()} return false.</dd>
 * <dt>Exception</dt>
 * <dd>The visit method throws a {@link CycleDetectedException} (a checked
 * exception), from which the revisited value can be obtained.
 * </dl>
 * 
 * @author Andy Lowry
 *
 * @param <T>
 *            value object type
 */
public class CycleDetector<T> {

	private Set<T> visited = new HashSet<>();
	private Map<T, T> idVisited = new IdentityHashMap<>();
	private boolean useIdentity;
	private boolean throwOnRevisit;

	/**
	 * Create a cycle detector that uses value equality and does not throw
	 * exceptions on repeat visits.
	 */
	public CycleDetector() {
		this(false, false);
	}

	/**
	 * Crate a cycle detector.
	 * 
	 * @param useIdentity
	 *            true means use Object identity, rather than value identity, to
	 *            detect repeat visits
	 * @param throwOnRevisit
	 *            true means throw an exception on repeat visits, false means rely
	 *            on boolean results
	 */
	public CycleDetector(boolean useIdentity, boolean throwOnRevisit) {
		this.useIdentity = useIdentity;
		this.throwOnRevisit = throwOnRevisit;
	}

	/**
	 * Visit a value.
	 * 
	 * @param value
	 *            value to be visited
	 * @return false if this value was already being visited, else false
	 * @throws CycleDetectedException
	 *             if visit fails and this detector is configured to throw
	 *             exceptions
	 */
	public boolean visit(T value) throws CycleDetectedException {
		if (useIdentity) {
			if (idVisited.containsKey(value)) {
				return revisit(value);
			} else {
				idVisited.put(value, value);
				return true;
			}
		} else {
			if (visited.contains(value)) {
				return revisit(value);
			} else {
				visited.add(value);
				return true;
			}
		}
	}

	/**
	 * Stop visiting a value.
	 * <p>
	 * It is not an error to unvisit a value that is not being visited.
	 * 
	 * @param value
	 *            value to be unvisited
	 */
	public void unvisit(T value) {
		if (useIdentity) {
			idVisited.remove(value);
		} else {
			visited.remove(value);
		}
	}

	/**
	 * Create a new visitation object for the given value.
	 * <p>
	 * This causes the value to be visited, and if the visitation is obtained in a
	 * try-resource statement, it will be automatically terminated when the block
	 * exits.
	 * <p>
	 * Unless exceptions are configured for this detector instance, the code must
	 * check whether visitation succeeded using the {@link Visitation#isVisited()}
	 * method.
	 * 
	 * @param value
	 *            value to be visited
	 * @return visitation object
	 * @throws CycleDetectedException
	 *             if visit fails and exceptions are configured for this detector
	 *             instance.
	 */
	public Visitation getVistitation(T value) throws CycleDetectedException {
		return new Visitation(value);
	}

	/**
	 * Return the list of values currently being visited.
	 * 
	 * Ordering is not correlated with visit order. This is a list instead of a set
	 * so that in the case of a detector with <code>useIdentity = true</code>,
	 * multiple distinct but "equals" values will show up as expected.
	 * 
	 * @return values currently being visited
	 */
	public List<T> getActiveVisits() {
		return new ArrayList<>(useIdentity ? idVisited.keySet() : visited);
	}

	private boolean revisit(T value) throws CycleDetectedException {
		if (throwOnRevisit) {
			throw new CycleDetectedException("Attempt to revisit value", value);
		} else {
			return false;
		}
	}

	/**
	 * An active visitation, designed for use in try-resource statements.
	 * 
	 * @author Andy Lowry
	 *
	 */
	public class Visitation implements AutoCloseable {

		private T value;
		private boolean visited;

		/**
		 * Create a new visitation for the given value
		 * <p>
		 * An attempt to visit the value will be made as a side-effect.
		 * <p>
		 * The value will be unvisited when the visitation object is closed, and since
		 * the class implements {@link AutoCloseable}, this works well in a try-resource
		 * statement.
		 * 
		 * @param value
		 * @throws CycleDetectedException
		 */
		public Visitation(T value) throws CycleDetectedException {
			this.value = value;
			this.visited = visit(value);
		}

		/**
		 * Tells whether the visit attempt made in the constructor succeeded.
		 * <p>
		 * If exceptions are not enabled for this detector, this is the only way for the
		 * using code to detect whether the visit succeeded.
		 * 
		 * @return
		 */
		public boolean isVisited() {
			return visited;
		}

		@Override
		public void close() {
			unvisit(value);
		}

	}

	public static class CycleDetectedException extends Exception {
		private static final long serialVersionUID = 2049002878757520271L;

		public CycleDetectedException(String message, Object value) {
			super(message);
			this.value = value;
		}

		private Object value;

		public Object getValue() {
			return value;
		}
	}

}
