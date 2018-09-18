package com.reprezen.kaizen.normalizer.test;

import static com.reprezen.kaizen.normalizer.test.StateMachineTest.TestState.A;
import static com.reprezen.kaizen.normalizer.test.StateMachineTest.TestState.ANON;
import static com.reprezen.kaizen.normalizer.test.StateMachineTest.TestState.B;
import static com.reprezen.kaizen.normalizer.test.StateMachineTest.TestState.C;
import static com.reprezen.kaizen.normalizer.test.StateMachineTest.TestState.OFF_ROAD;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.reprezen.kaizen.normalizer.util.StateMachine;
import com.reprezen.kaizen.normalizer.util.StateMachine.Tracker;

public class StateMachineTest extends Assert {

	private StateMachine<TestState> machine;

	@Before
	public void createMachine() {
		machine = new StateMachine<TestState>();
		defineTransits();
	}

	private void defineTransits() {
		//         ⇗ "shortcut"⇒ ⇒ ⇒ ⇒ ⇘
		// Ⓐ ⇒ "x" ⇒ "*" ⇒ Ⓑ ⇒ "done" ⇒ Ⓒ
		//                 ⇙ ⇖
		//                 int
		//
		machine.transit().from(A).via("x", "*", "y").to(B);
		machine.transit().from(A).via("x", "shortcut").to(C);
		machine.transit().from(B).via("#").to(B);
		machine.transit().from(B).via("done").to(C);
	}

	@Test
	public void testSimpleMoves() {
		performSimpleMoves();
	}

	private Tracker<TestState> performSimpleMoves() {
		Tracker<TestState> tracker = machine.tracker(A);
		checkState(tracker, A);
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
		Tracker<TestState> tracker = performSimpleMoves();
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
		Tracker<TestState> tracker = machine.tracker(A);
		checkMove(tracker, "x", null);
		checkMove(tracker, "shortcut", C);
		checkPath(tracker, "x", "shortcut");
		checkBackup(tracker, 1, null);
		checkMove(tracker, "xyzzy", null);
		checkMove(tracker, "y", B);
	}

	@Test
	public void testOffRoad() {
		Tracker<TestState> tracker = performSimpleMoves();
		checkOffRoadMove(tracker, "uh oh");
		checkOffRoadMove(tracker, 1);
		checkPath(tracker, "x", "hello", "y", 10, 20, "done", "uh oh", 1);
		checkBackup(tracker, 2, C);
		checkPath(tracker, "x", "hello", "y", 10, 20, "done");
	}

	@Test
	public void testSpecialValues() {
		machine = new StateMachine<TestState>(ANON, OFF_ROAD);
		defineTransits();
		Tracker<TestState> tracker = machine.tracker(A);
		checkMove(tracker, "x", ANON);
		checkMove(tracker, "blah", ANON);
		checkMove(tracker, "y", B);
		checkMove(tracker, "oops", OFF_ROAD);
		checkBackup(tracker, 1, B);
		checkBackup(tracker, 2, ANON);
		checkBackup(tracker, 1, A);
	}

	private void checkState(Tracker<TestState> tracker, TestState expected) {
		assertEquals(expected, tracker.getCurrentState().getValue());
	}

	private void checkMove(Tracker<TestState> tracker, String value, TestState expected) {
		tracker.move(value);
		checkState(tracker, expected);
	}

	private void checkMove(Tracker<TestState> tracker, int value, TestState expected) {
		tracker.move(value);
		checkState(tracker, expected);
	}

	private void checkOffRoadMove(Tracker<TestState> tracker, String value) {
		tracker.move(value);
		assertNull(tracker.getCurrentState());
	}

	private void checkOffRoadMove(Tracker<TestState> tracker, int value) {
		tracker.move(value);
		assertNull(tracker.getCurrentState());
	}

	private void checkBackup(Tracker<TestState> tracker, int n, TestState expected) {
		if (n == 1) {
			tracker.backup();
		} else {
			tracker.backup(n);
		}
		checkState(tracker, expected);
	}

	public void checkBadBackup(Tracker<TestState> tracker, int n) {
		try {
			tracker.backup(n);
			fail("Backing up too far should have thrown exception");
		} catch (IllegalArgumentException e) {
		}
	}

	private void checkPath(Tracker<TestState> tracker, Object... expectedPath) {
		assertEquals(Arrays.asList(expectedPath), tracker.getPath());
	}

	public static enum TestState {
		A, B, C, ANON, OFF_ROAD;
	}
}
