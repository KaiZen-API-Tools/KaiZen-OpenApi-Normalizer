package com.reprezen.kaizen.normalizer.test;

import static com.reprezen.kaizen.normalizer.test.StateMachineTest.S.A;
import static com.reprezen.kaizen.normalizer.test.StateMachineTest.S.ANON;
import static com.reprezen.kaizen.normalizer.test.StateMachineTest.S.B;
import static com.reprezen.kaizen.normalizer.test.StateMachineTest.S.C;
import static com.reprezen.kaizen.normalizer.test.StateMachineTest.S.OFF_ROAD;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.reprezen.kaizen.normalizer.util.StateMachine;
import com.reprezen.kaizen.normalizer.util.StateMachine.Tracker;

public class StateMachineTest extends Assert {

	private StateMachine<S> machine;

	@Before
	public void createMachine() {
		machine = new StateMachine<S>(S.class);
		defineTransits();
	}

	private void defineTransits() {
		//         ⇗ "shortcut"⇒ ⇒ ⇒ ⇒ ⇘
		// Ⓐ ⇒ "x*" ⇒ "*" ⇒ Ⓑ ⇒ "done" ⇒ Ⓒ
		//   ⇘ ⇒  "y*" ⇒ ⇗  ⇙ ⇖
		//                  int
		//
		machine.transit().from(A).via("re: x.*", "shortcut").to(C);
		machine.transit().from(B).via("#").to(B);
		machine.transit().from(B).via("done").to(C);
		machine.transit().from(A).via("re: x.*", "*", "y").to(B);
		machine.transit().from(A).via("re: y.*").to(B);
	}

	@Test
	public void testSimpleMoves() {
		performSimpleMoves();
	}

	private Tracker<S> performSimpleMoves() {
		Tracker<S> tracker = machine.tracker(A);
		checkState(tracker, A);
		checkMove(tracker, "yellow", B);
		checkPath(tracker, "yellow");
		checkReset(tracker, A);
		checkMove(tracker, "x", null);
		checkPath(tracker, "x");
		checkMove(tracker, "hello", null);
		checkPath(tracker, "x", "hello");
		checkMove(tracker, "y", B);
		checkPath(tracker, "x", "hello", "y");
		checkMove(tracker, 10, B);
		checkPath(tracker, "x", "hello", "y", 10);
		checkMove(tracker, 20, B);
		checkPath(tracker, "x", "hello", "y", 10, 20);
		checkMove(tracker, "done", C);
		checkPath(tracker, "x", "hello", "y", 10, 20, "done");
		return tracker;
	}

	@Test
	public void testPaths() {
		performSimpleMoves();
	}

	@Test
	public void testBackup() {
		Tracker<S> tracker = performSimpleMoves();
		checkBackup(tracker, 1, B);
		checkPath(tracker, "x", "hello", "y", 10, 20);
		checkBackup(tracker, 2, B);
		checkPath(tracker, "x", "hello", "y");
		checkBackup(tracker, 1, null);
		checkPath(tracker, "x", "hello");
		checkBackup(tracker, 2, A);
		checkPath(tracker);
		checkMove(tracker, "x", null);
		checkMove(tracker, "huh", null);
		checkPath(tracker, "x", "huh");
		checkBadBackup(tracker, 3);
	}

	@Test
	public void testSharedPath() {
		Tracker<S> tracker = machine.tracker(A);
		checkMove(tracker, "x", null);
		checkMove(tracker, "shortcut", C);
		checkPath(tracker, "x", "shortcut");
		checkBackup(tracker, 1, null);
		checkMove(tracker, "xyzzy", null);
		checkMove(tracker, "y", B);
	}

	@Test
	public void testOffRoad() {
		Tracker<S> tracker = performSimpleMoves();
		checkOffRoadMove(tracker, "uh oh");
		checkOffRoadMove(tracker, 1);
		checkPath(tracker, "x", "hello", "y", 10, 20, "done", "uh oh", 1);
		checkBackup(tracker, 2, C);
		checkPath(tracker, "x", "hello", "y", 10, 20, "done");
	}

	@Test
	public void testSpecialValues() {
		machine = new StateMachine<S>(S.class, ANON, OFF_ROAD);
		defineTransits();
		Tracker<S> tracker = machine.tracker(A);
		checkMove(tracker, "x", ANON);
		checkMove(tracker, "blah", ANON);
		checkMove(tracker, "y", B);
		checkMove(tracker, "oops", OFF_ROAD);
		checkBackup(tracker, 1, B);
		checkBackup(tracker, 2, ANON);
		checkBackup(tracker, 1, A);
	}

	private void checkState(Tracker<S> tracker, S expected) {
		assertEquals(expected, tracker.getCurrentState().getValue());
	}

	private void checkMove(Tracker<S> tracker, String value, S expected) {
		tracker.move(value);
		checkState(tracker, expected);
	}

	private void checkMove(Tracker<S> tracker, int value, S expected) {
		tracker.move(value);
		checkState(tracker, expected);
	}

	private void checkOffRoadMove(Tracker<S> tracker, String value) {
		tracker.move(value);
		assertNull(tracker.getCurrentState());
	}

	private void checkOffRoadMove(Tracker<S> tracker, int value) {
		tracker.move(value);
		assertNull(tracker.getCurrentState());
	}

	private void checkBackup(Tracker<S> tracker, int n, S expected) {
		if (n == 1) {
			tracker.backup();
		} else {
			tracker.backup(n);
		}
		checkState(tracker, expected);
	}

	public void checkBadBackup(Tracker<S> tracker, int n) {
		try {
			tracker.backup(n);
			fail("Backing up too far should have thrown exception");
		} catch (IllegalArgumentException e) {
		}
	}

	private void checkPath(Tracker<S> tracker, Object... expectedPath) {
		assertEquals(Arrays.asList(expectedPath), tracker.getPath());
	}

	private void checkReset(Tracker<S> tracker, S value) {
		tracker.reset(value);
		checkState(tracker, value);
		checkPath(tracker);
	}

	public static enum S {
		A, B, C, ANON, OFF_ROAD;
	}
}
