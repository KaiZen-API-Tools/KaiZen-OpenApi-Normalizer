package com.reprezen.kaizen.normalizer.test;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.reprezen.kaizen.normalizer.util.CycleDetector;
import com.reprezen.kaizen.normalizer.util.CycleDetector.CycleDetectedException;

public class CycleDetectorTest extends Assert {

	private CycleDetector<String> detector;
	private CycleDetector<String> idDetector;
	private CycleDetector<String> throwDetector;
	List<CycleDetector<String>> detectors;

	@Before
	public void setup() {
		this.detector = new CycleDetector<String>();
		this.idDetector = new CycleDetector<String>(true, false);
		this.throwDetector = new CycleDetector<String>(false, true);
		// no idThrowDetector because the features are orthogonal
		this.detectors = Arrays.asList(detector, idDetector, throwDetector);
	}

	@Test
	public void firstVisitsWork() throws CycleDetectedException {
		checkVisit("a", "a");
		checkVisit("b", "a", "b");
		checkVisit("c", "a", "b", "c");
		checkUnvisit("c", "a", "b");
		checkUnvisit("a", "b");
		checkVisit("a", "a", "b");
		checkVisit("d", "a", "b", "d");
	}

	@Test
	public void revisitsFail() throws CycleDetectedException {
		String a = "a";
		String b = "b";
		String c = "c";
		checkVisit(a, a);
		checkVisit(b, a, b);
		checkRevisit(a, a, b);
		checkVisit(c, a, b, c);
		checkUnvisit(a, b, c);
		checkVisit(a, a, b, c);
	}

	@Test
	public void testEqualityVariations() throws CycleDetectedException {
		String a = "a";
		String a2 = new String("a");
		checkVisit(a, a);
		checkRevisit(detector, a2, a);
		checkVisit(idDetector, a2, a, a2);
		checkRevisit(idDetector, a2, a, a2);
		checkUnvisit(idDetector, a, a2);
		checkVisit(idDetector, a, a, a2);
	}

	public void checkVisit(String value, String... expectedVisits) throws CycleDetectedException {
		for (CycleDetector<String> detector : detectors) {
			checkVisit(detector, value, expectedVisits);
		}
	}

	private void checkVisit(CycleDetector<String> detector, String value, String... expectedVisits)
			throws CycleDetectedException {
		assertTrue(detector.visit(value));
		checkActiveVisits(detector, expectedVisits);
	}

	public void checkUnvisit(String value, String... expectedVisits) throws CycleDetectedException {
		for (CycleDetector<String> detector : detectors) {
			checkUnvisit(detector, value, expectedVisits);
		}
	}

	private void checkUnvisit(CycleDetector<String> detector, String value, String... expectedVisits) {
		detector.unvisit(value);
		checkActiveVisits(detector, expectedVisits);
	}

	public void checkRevisit(String value, String... expectedVisits) {
		for (CycleDetector<String> detector : detectors) {
			checkRevisit(detector, value, expectedVisits);
		}
	}

	private void checkRevisit(CycleDetector<String> detector, String value, String... expectedVisits) {
		try {
			boolean ok = true;
			if (detector.visit(value)) {
				ok = false;
			}
			if (detector == throwDetector) {
				ok = false; // should be in catch clause for this detector
			}
			if (!ok) {
				fail("Repeat vistitation not detected for '" + value + "'");
			}
		} catch (CycleDetectedException e) {
			if (detector != throwDetector) {
				fail("Detector threw exception on repeat visit but was not configured to do so");
			}
		}
		checkActiveVisits(detector, expectedVisits);
	}

	private void checkActiveVisits(CycleDetector<String> detector, String... expected) {
		List<String> activeVisits = detector.getActiveVisits();
		List<String> expectedVisits = Arrays.asList(expected);
		for (String s : expectedVisits) {
			if (!contains(activeVisits, s, detector)) {
				fail("Missing active visit for '" + s + "'");
			}
		}
		for (String s : activeVisits) {
			if (!contains(expectedVisits, s, detector)) {
				fail("Unexpected active visit for '" + s + "'");
			}
		}
	}

	private boolean contains(List<String> list, String value, CycleDetector<String> detector) {
		if (detector == idDetector) {
			for (String listValue : list) {
				if (listValue == value) {
					return true;
				}
			}
			return false;
		} else {
			return list.contains(value);
		}
	}

}
