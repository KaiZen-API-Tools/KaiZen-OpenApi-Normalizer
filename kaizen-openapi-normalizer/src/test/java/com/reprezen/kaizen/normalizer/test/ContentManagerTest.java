package com.reprezen.kaizen.normalizer.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.kaizen.normalizer.Component;
import com.reprezen.kaizen.normalizer.Content;
import com.reprezen.kaizen.normalizer.ContentManager;
import com.reprezen.kaizen.normalizer.Localizer.LocalizedContent;
import com.reprezen.kaizen.normalizer.Option;
import com.reprezen.kaizen.normalizer.Options;
import com.reprezen.kaizen.normalizer.Reference;
import com.reprezen.kaizen.normalizer.ReferenceScanner.ScanOp;
import com.reprezen.kaizen.normalizer.V2State;

public class ContentManagerTest extends NormalizerTestBase {

	private ContentManager cm;
	private Content doc;

	@Before
	public void setup() {
		this.cm = new ContentManager(new Options());
		Reference ref = new Reference(getYamlFileUrl("StateWalkV2"));
		this.doc = cm.load(ref, V2State.MODEL);
	}

	@Test
	public void testLoadSuccessful() {
		assertTrue(doc.isValid());
		Reference equivRef = new Reference(getYamlFileUrl("./StateWalkV2"));
		assertEquals(doc.getRef(), equivRef);
		assertTrue(doc == cm.load(equivRef, V2State.MODEL));
		assertEquals("2.0", doc.at("/swagger").asText());
		assertEquals("Longitude component of end location.",
				doc.at("/paths/~1estimates~1price/get/parameters/3/description").asText());
	}

	@Test
	public void testLoadSubTree() {
		Content activities = cm.load(new Reference("#/definitions/Activities", doc.getRef(), V2State.SCHEMA_DEF));
		assertTrue(activities.isValid());
		assert (activities.getTree() == doc.at("/definitions/Activities"));
	}

	@Test
	public void testNotFound() {
		Content notFound = cm.load(new Reference("./xxx.yaml", doc.getRef(), V2State.MODEL));
		assertFalse(notFound.isValid());
		assertTrue(notFound.getInvalidReason().toLowerCase().contains("no such file"));
	}

	@Test
	public void testCantParse() {
		Content cantParse = cm.load(new Reference("./shopping-list.txt", doc.getRef(), V2State.MODEL));
		assertFalse(cantParse.isValid());
		assertTrue(cantParse.getInvalidReason().toLowerCase().contains("does not contain valid yaml"));
	}

	@Test
	public void testSubtreeNotFound() {
		// lower-case schema name is incorrect
		Content subtree = cm.load(new Reference("#/definitions/activities", doc.getRef()), V2State.SCHEMA);
		assertFalse(subtree.isValid());
		assertTrue(subtree.getInvalidReason().toLowerCase().contains("no json value at specified pointer"));
	}

	@Test
	public void loadScanTest() {
		Content model = cm.load(new Reference("LoadScanTest.yaml", doc.getRef(), V2State.MODEL), V2State.MODEL);
		model.scan(ScanOp.LOAD);

		assertTrue(model.at("/info/title").isTextual());
		assertEquals("Load Scan Test", model.at("/info/title").asText());
		Content titleContent = cm.load(new Reference("#/info/title", model.getRef()), null);
		assertTrue(titleContent.isValid());
		assertEquals(model.at("/info/title"), titleContent.getTree());

		JsonNode contactNode = model.at("/info/contact");
		assertTrue(Reference.isRefNode(contactNode));
		Reference ref = Reference.of(contactNode);
		assertFalse(ref.isValid());

		JsonNode param0Node = model.at("/paths/~1foo/get/parameters/0");
		assertTrue(Reference.isRefNode(param0Node));
		ref = Reference.of(param0Node);
		assertTrue(ref.isValid());

		JsonNode schemaNode = model.at("/paths/~1foo/get/responses/200/schema");
		assertTrue(Reference.isRefNode(schemaNode));
		ref = Reference.of(schemaNode);
		assertTrue(ref.isValid());
	}

	@Test
	public void componentScanTest() {
		Content model = cm.load(new Reference("uber.yaml", doc.getRef(), V2State.MODEL));
		model.scan(ScanOp.LOAD);
		model.scan(ScanOp.COMPONENTS);
		checkDefinitions(V2State.PATH, "/products", "/estimates/price", "/estimates/time", "/me", "/history");
		checkDefinitions(V2State.SCHEMA, "Product", "PriceEstimate", "Profile", "Activity", "Activities", "Error");
		checkDefinitions(V2State.PARAMETER);
		checkDefinitions(V2State.RESPONSE);
	}

	@Test
	public void testPolicyPhase_inline() {
		cm = new ContentManager(Options.of(Option.INLINE_ALL));
		Content model = cm.load(new Reference("multifile-uber.yaml", doc.getRef(), V2State.MODEL), V2State.MODEL);
		model.scan(ScanOp.LOAD);
		model.scan(ScanOp.COMPONENTS);
		model.scan(ScanOp.POLICY);
		checkDefinitions(V2State.PATH, "/products", "/estimates/price", "/estimates/time", "/me", "/history");
		checkDefinitions(V2State.SCHEMA);
		checkDefinitions(V2State.PARAMETER);
		checkDefinitions(V2State.RESPONSE);
	}

	@Test
	public void testPolicyPhase_localize() {
		cm = new ContentManager(Options.of(Option.INLINE_NONE));
		Content model = cm.load(new Reference("multifile-uber.yaml", doc.getRef(), V2State.MODEL), V2State.MODEL);
		model.scan(ScanOp.LOAD);
		model.scan(ScanOp.COMPONENTS);
		model.scan(ScanOp.POLICY);
		checkDefinitions(V2State.PATH, "/products", "/estimates/price", "/estimates/time", "/me", "/history");
		checkDefinitions(V2State.SCHEMA, "PriceEstimate", "Profile", "Activity", "Activities", "Product", "Error");
		checkDefinitions(V2State.PARAMETER, "latitude", "longitude", "start_latitude", "start_longitude",
				"end_latitude", "end_longitude", "customer_uuid", "product_id", "offset", "limit");
		checkDefinitions(V2State.RESPONSE, "ErrorResponse");
	}

	private void checkDefinitions(Component component, String... names) {
		Set<String> expected = new HashSet<>(Arrays.asList(names));
		Iterable<LocalizedContent> localized = cm.getLocalizedContent(component);
		for (LocalizedContent item : localized) {
			if (expected.contains(item.getName())) {
				expected.remove(item.getName());
			} else {
				fail(String.format("Unexpected localized content for %s named %s", component.name(), item.getName()));
			}
		}
		for (String name : expected) {
			fail(String.format("Missing localized content for %s named %s", component.name(), name));
		}
	}
}
