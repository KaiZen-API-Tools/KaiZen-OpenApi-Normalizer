package com.reprezen.kaizen.normalizer.test;

import static com.reprezen.kaizen.normalizer.Reference.ADORNMENT_PROPERTY;
import static com.reprezen.kaizen.normalizer.test.ReferenceTest.RefTester.ref;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.kaizen.normalizer.Component;
import com.reprezen.kaizen.normalizer.Reference;
import com.reprezen.kaizen.normalizer.v2.V2State;
import com.reprezen.kaizen.normalizer.v3.V3State;

public class ReferenceTest extends NormalizerTestBase {

	@Test
	public void testValidReferences() throws MalformedURLException, URISyntaxException {
		ref("http://example.com") //
				.hasUrl("http://example.com") //
				.hasCanonicalString("http://example.com") //
				.hasFragment(null) //
				.hasPointer("") //
				.isForComponent(null) //
				.isValid();
		Reference base = new Reference("http://base.com");
		new RefTester(base).isValid();
		ref("http://example.com", base) //
				.hasUrl("http://example.com") //
				.hasCanonicalString("http://example.com") //
				.hasFragment(null) //
				.hasPointer("") //
				.isForComponent(null) //
				.isValid();
		ref("/foo/bar", base) //
				.hasUrl("http://base.com/foo/bar") //
				.hasCanonicalString("http://base.com/foo/bar") //
				.hasFragment(null) //
				.hasPointer("") //
				.isForComponent(null) //
				.isValid();
		ref("/foo/bar?x=1#/a/b/c", base) //
				.hasUrl("http://base.com/foo/bar?x=1")//
				.hasFragment("/a/b/c") //
				.hasPointer("/a/b/c") //
				.isValid();
		base = new Reference("http://base.com/blah/blatch?a=1#/x");
		new RefTester(base) //
				.hasUrl("http://base.com/blah/blatch?a=1") //
				.hasFragment("/x") //
				.isValid();
		ref("/foo/bar", base) //
				.hasUrl("http://base.com/foo/bar") //
				.hasFragment(null) //
				.isValid();
		ref("./foo/bar", base) //
				.hasUrl("http://base.com/blah/foo/bar") //
				.hasFragment(null) //
				.isValid();
		ref("../foo/bar", base) //
				.hasUrl("http://base.com/foo/bar") //
				.hasFragment(null) //
				.isValid();
		ref("../foo/./../xxx/bar", base) //
				.hasUrl("http://base.com/xxx/bar") //
				.hasFragment(null) //
				.isValid();
		ref("#/x/y/z", base) //
				.hasUrl("http://base.com/blah/blatch?a=1") //
				.hasFragment("/x/y/z") //
				.isValid();
		ref("#", base) //
				.hasUrl("http://base.com/blah/blatch?a=1") //
				.hasFragment(null) //
				.isValid();
		ref("?b=1", base) //
				.hasUrl("http://base.com/blah/?b=1") //
				.hasFragment(null) //
				.isValid();
		ref("?b=1#/x", base) //
				.hasUrl("http://base.com/blah/?b=1") //
				.hasFragment("/x") //
				.isValid();
		ref("http://example.com", null, V2State.SCHEMA) //
				.hasUrl("http://example.com") //
				.isForComponent(V2State.SCHEMA) //
				.isValid();
		ref("/foo/bar", base, V3State.CALLBACK) //
				.hasUrl("http://base.com/foo/bar") //
				.isForComponent(V3State.CALLBACK) //
				.isValid();
	}

	@Test
	public void testInvalidRefs() {
		// the Java URL class catches very few URL syntax errors. It appears to only
		// notice a missing or invalid protocol. Some problems aren't checked until an
		// attempt is made to open the URL. So unless we work in a more robust syntactic
		// check, we can't test much here.
		ref("/foo/bar") //
				.isntValid("no protocol");
		ref("foo://x.y") //
				.isntValid("unknown protocol");
		ref("http://example.com#a.b.c")//
				.hasPointer(null) //
				.isntValid("json pointer expression");
	}

	@Test
	public void testSimpleRefs() {
		ref("http://example.com/#Pet", null, V2State.SCHEMA) //
				.isSimpleRef() //
				.isntValid("json pointer expression") //
				.rewriteSimpleRef() //
				.isntSimpleRef() //
				.isValid();
	}

	@Test
	public void testAdornedRefs() throws MalformedURLException {
		Reference ref = new Reference("http://example.com#/a/b/c", null, V2State.MODEL);
		JsonNode refNode = ref.getRefNode();
		checkStringField(refNode.path("$ref"), "http://example.com#/a/b/c");
		checkBooleanField(refNode.path(ADORNMENT_PROPERTY).path("valid"), true);
		checkStringField(refNode.path(ADORNMENT_PROPERTY).path("url"), "http://example.com");
		checkStringField(refNode.path(ADORNMENT_PROPERTY).path("fragment"), "/a/b/c");
		checkComponent(refNode.path(ADORNMENT_PROPERTY).path("component"), V2State.MODEL);
		checkStringField(refNode.path(ADORNMENT_PROPERTY).path("invalidReason"), null);
		checkException(refNode.path(ADORNMENT_PROPERTY).path("invalidException"), null);
		ref(refNode) //
				.hasUrl("http://example.com") //
				.hasFragment("/a/b/c") //
				.isForComponent(V2State.MODEL) //
				.isValid();
		refNode = ref.getRefNode(false);
		checkStringField(refNode.path("$ref"), "http://example.com#/a/b/c");
		checkMissing(refNode.path(ADORNMENT_PROPERTY));
		ref(refNode) //
				.hasUrl("http://example.com") //
				.hasFragment("/a/b/c") //
				.isForComponent(null);
		ref = new Reference("http://example.com#abc");
		refNode = ref.getRefNode();
		checkStringField(refNode.path("$ref"), "http://example.com#abc");
		checkException(refNode.path(ADORNMENT_PROPERTY).path("invalidException"),
				new IllegalArgumentException("Invalid input: JSON Pointer expression must start with '/': \"abc\""));
	}

	private void checkStringField(JsonNode node, String value) {
		if (value != null) {
			assertEquals(value, node.asText());
		} else {
			assertTrue(node.isNull() || node.isMissingNode());
		}
	}

	private void checkBooleanField(JsonNode node, Boolean value) {
		if (value != null) {
			assertEquals(value, node.asBoolean());
		} else {
			assertTrue(node.isNull() || node.isMissingNode());
		}
	}

	private void checkMissing(JsonNode node) {
		assertTrue(node.isMissingNode());
	}

	private void checkComponent(JsonNode node, Component comp) {
		if (comp != null) {
			checkStringField(node.path("class"), comp.getClass().getName());
			checkStringField(node.path("name"), comp.name());
		} else {
			assertTrue(node.isNull() || node.isMissingNode());
		}
	}

	private void checkException(JsonNode node, Exception e) {
		if (e != null) {
			checkStringField(node.path("class"), e.getClass().getName());
			checkStringField(node.path("message"), e.getMessage());
		} else {
			assertTrue(node.isNull() || node.isMissingNode());
		}
	}

	@Test
	public void testRefEquality() {
		// Reference ref = new Reference("http://example.com/foo/bar")
	}

	public static class RefTester {
		private Reference ref;

		private RefTester(Reference ref, String refString) {
			this.ref = ref;
			assertEquals(refString, ref.getRefString());
		}

		private RefTester(Reference ref) {
			this.ref = ref;
		}

		private RefTester(JsonNode refNode) {
			this.ref = Reference.of(refNode);
		}

		public static RefTester ref(String refString) {
			Reference ref = new Reference(refString);
			return new RefTester(ref, refString);
		}

		public static RefTester ref(JsonNode refNode) {
			Reference ref = Reference.of(refNode);
			return new RefTester(ref, ref.getRefString());
		}

		public static RefTester ref(String refString, Reference base) {
			Reference ref = new Reference(refString, base);
			return new RefTester(ref, refString);
		}

		public static RefTester ref(String refString, Reference base, Component comp) {
			Reference ref = new Reference(refString, base, comp);
			return new RefTester(ref, refString);
		}

		public RefTester hasFragment(String fragment) {
			assertEquals(fragment, ref.getFragment());
			return this;
		}

		public RefTester hasPointer(String ptrString) {
			JsonPointer ptr = ptrString != null ? JsonPointer.compile(ptrString) : null;
			assertEquals(ptr, ref.getPointer());
			return this;
		}

		public RefTester hasUrl(String urlString) throws MalformedURLException {
			assertEquals(new URL(urlString), ref.getUrl());
			assertEquals(urlString, ref.getUrlString());
			assertEquals(new Reference(urlString), ref.getUrlRef());
			return this;
		}

		public RefTester hasCanonicalString(String canonical) {
			assertEquals(canonical, ref.getCanonicalString());
			return this;
		}

		public RefTester isForComponent(Component comp) {
			assertEquals(comp, ref.getComponent());
			return this;
		}

		public RefTester isSimpleRef() {
			assertTrue(ref.isSimpleRef());
			return this;
		}

		public RefTester isntSimpleRef() {
			assertFalse(ref.isSimpleRef());
			return this;
		}

		public RefTester rewriteSimpleRef() {
			ref.rewriteSimpleRef();
			return this;
		}

		public RefTester isValid() {
			assertTrue("Expected valid ref but '" + ref.getInvalidReason() + "'", ref.isValid());
			return this;
		}

		public RefTester isntValid() {
			assertFalse(ref.isValid());
			return this;
		}

		public RefTester isntValid(String reason) {
			assertTrue("Expected invalidReason to contain '" + reason + "'",
					ref.getInvalidReason().toLowerCase().contains(reason.toLowerCase()));
			return this;
		}
	}

}
