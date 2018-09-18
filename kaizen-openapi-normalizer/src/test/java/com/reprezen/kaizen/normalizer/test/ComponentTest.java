package com.reprezen.kaizen.normalizer.test;

import org.junit.Assert;
import org.junit.Test;

import com.reprezen.kaizen.normalizer.V2State;
import com.reprezen.kaizen.normalizer.V3State;

/**
 * We do some tricky stuff configuring state enums, so this just makes sure
 * things are working as expected.
 * 
 * @author Andy Lowry
 *
 */
public class ComponentTest extends Assert {

	@Test
	public void testV2StateConfig() {
		assertFalse(V2State.MODEL.isConformingSite());
		assertTrue(V2State.PATH.isConformingSite());
		assertNull(V2State.MODEL.getContainerPath());
		assertEquals("/paths", V2State.PATH.getContainerPath());
		assertEquals("/responses", V2State.RESPONSE.getContainerPath());
	}

	@Test
	public void TestV3StateConfig() {
		assertFalse(V3State.MODEL.isConformingSite());
		assertTrue(V3State.PATH.isConformingSite());
		assertNull(V3State.MODEL.getContainerPath());
		assertEquals("/paths", V3State.PATH.getContainerPath());
		assertEquals("/components/responses", V3State.RESPONSE.getContainerPath());
		assertEquals("/components/securitySchemes", V3State.SECURITY_SCHEME.getContainerPath());
	}
}
