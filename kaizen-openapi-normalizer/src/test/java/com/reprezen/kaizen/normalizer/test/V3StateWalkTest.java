package com.reprezen.kaizen.normalizer.test;

import static com.reprezen.kaizen.normalizer.v3.V3State.PARAMETER;
import static com.reprezen.kaizen.normalizer.v3.V3State.PATH;
import static com.reprezen.kaizen.normalizer.v3.V3State.RESPONSE;
import static com.reprezen.kaizen.normalizer.v3.V3State.SCHEMA;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker;
import com.reprezen.kaizen.normalizer.util.StateMachine.State;
import com.reprezen.kaizen.normalizer.util.StateMachine.Tracker;
import com.reprezen.kaizen.normalizer.v3.V3State;
import com.reprezen.kaizen.normalizer.v3.V3StateMachine;

public class V3StateWalkTest extends NormalizerTestBase {

	private JsonNode model;
	private Tracker<V3State> tracker;
	private HashMap<JsonPointer, V3State> refableResults;

	@Before
	public void setup() throws JsonProcessingException, IOException {
		this.model = loadYaml("StateWalkV3");
		this.tracker = new V3StateMachine().tracker(V3State.MODEL);
		this.refableResults = new HashMap<JsonPointer, V3State>() {
			private static final long serialVersionUID = 1L;
			{
				add("/paths/~1products", PATH);
				add("/paths/~1products/get/parameters/0", PARAMETER);
				add("/paths/~1products/get/parameters/0/schema", SCHEMA);
				add("/paths/~1products/get/parameters/1", PARAMETER);
				add("/paths/~1products/get/parameters/1/schema", SCHEMA);
				add("/paths/~1products/get/responses/200", RESPONSE);
				add("/paths/~1products/get/responses/200/content/application~1json/schema", SCHEMA);
				add("/paths/~1products/get/responses/default", RESPONSE);
				add("/paths/~1products/get/responses/default/content/application~1json/schema", SCHEMA);
				add("/paths/~1estimates~1price", PATH);
				add("/paths/~1estimates~1price/get/parameters/0", PARAMETER);
				add("/paths/~1estimates~1price/get/parameters/0/schema", SCHEMA);
				add("/paths/~1estimates~1price/get/parameters/1", PARAMETER);
				add("/paths/~1estimates~1price/get/parameters/1/schema", SCHEMA);
				add("/paths/~1estimates~1price/get/parameters/2", PARAMETER);
				add("/paths/~1estimates~1price/get/parameters/2/schema", SCHEMA);
				add("/paths/~1estimates~1price/get/parameters/3", PARAMETER);
				add("/paths/~1estimates~1price/get/parameters/3/schema", SCHEMA);
				add("/paths/~1estimates~1price/get/responses/200", RESPONSE);
				add("/paths/~1estimates~1price/get/responses/200/content/application~1json/schema", SCHEMA);
				add("/paths/~1estimates~1price/get/responses/200/content/application~1json/schema/items", SCHEMA);
				add("/paths/~1estimates~1price/get/responses/default", RESPONSE);
				add("/paths/~1estimates~1price/get/responses/default/content/application~1json/schema", SCHEMA);
				add("/paths/~1estimates~1time", PATH);
				add("/paths/~1estimates~1time/get/parameters/0", PARAMETER);
				add("/paths/~1estimates~1time/get/parameters/0/schema", SCHEMA);
				add("/paths/~1estimates~1time/get/parameters/1", PARAMETER);
				add("/paths/~1estimates~1time/get/parameters/1/schema", SCHEMA);
				add("/paths/~1estimates~1time/get/parameters/2", PARAMETER);
				add("/paths/~1estimates~1time/get/parameters/2/schema", SCHEMA);
				add("/paths/~1estimates~1time/get/parameters/3", PARAMETER);
				add("/paths/~1estimates~1time/get/parameters/3/schema", SCHEMA);
				add("/paths/~1estimates~1time/get/responses/200", RESPONSE);
				add("/paths/~1estimates~1time/get/responses/200/content/application~1json/schema", SCHEMA);
				add("/paths/~1estimates~1time/get/responses/200/content/application~1json/schema/items", SCHEMA);
				add("/paths/~1estimates~1time/get/responses/default", RESPONSE);
				add("/paths/~1estimates~1time/get/responses/default/content/application~1json/schema", SCHEMA);
				add("/paths/~1me", PATH);
				add("/paths/~1me/get/responses/200", RESPONSE);
				add("/paths/~1me/get/responses/200/content/application~1json/schema", SCHEMA);
				add("/paths/~1me/get/responses/default", RESPONSE);
				add("/paths/~1me/get/responses/default/content/application~1json/schema", SCHEMA);
				add("/paths/~1history", PATH);
				add("/paths/~1history/get/parameters/0", PARAMETER);
				add("/paths/~1history/get/parameters/0/schema", SCHEMA);
				add("/paths/~1history/get/parameters/1", PARAMETER);
				add("/paths/~1history/get/parameters/1/schema", SCHEMA);
				add("/paths/~1history/get/responses/200", RESPONSE);
				add("/paths/~1history/get/responses/200/content/application~1json/schema", SCHEMA);
				add("/paths/~1history/get/responses/default", RESPONSE);
				add("/paths/~1history/get/responses/default/content/application~1json/schema", SCHEMA);
				add("/components/schemas/Product/properties/product_id", SCHEMA);
				add("/components/schemas/Product/properties/description", SCHEMA);
				add("/components/schemas/Product/properties/display_name", SCHEMA);
				add("/components/schemas/Product/properties/capacity", SCHEMA);
				add("/components/schemas/Product/properties/image", SCHEMA);
				add("/components/schemas/ProductList/properties/products", SCHEMA);
				add("/components/schemas/ProductList/properties/products/items", SCHEMA);
				add("/components/schemas/PriceEstimate/properties/product_id", SCHEMA);
				add("/components/schemas/PriceEstimate/properties/currency_code", SCHEMA);
				add("/components/schemas/PriceEstimate/properties/display_name", SCHEMA);
				add("/components/schemas/PriceEstimate/properties/estimate", SCHEMA);
				add("/components/schemas/PriceEstimate/properties/low_estimate", SCHEMA);
				add("/components/schemas/PriceEstimate/properties/high_estimate", SCHEMA);
				add("/components/schemas/PriceEstimate/properties/surge_multiplier", SCHEMA);
				add("/components/schemas/Profile/properties/first_name", SCHEMA);
				add("/components/schemas/Profile/properties/last_name", SCHEMA);
				add("/components/schemas/Profile/properties/email", SCHEMA);
				add("/components/schemas/Profile/properties/picture", SCHEMA);
				add("/components/schemas/Profile/properties/promo_code", SCHEMA);
				add("/components/schemas/Activity/properties/uuid", SCHEMA);
				add("/components/schemas/Activities/properties/offset", SCHEMA);
				add("/components/schemas/Activities/properties/limit", SCHEMA);
				add("/components/schemas/Activities/properties/count", SCHEMA);
				add("/components/schemas/Activities/properties/history", SCHEMA);
				add("/components/schemas/Activities/properties/history/items", SCHEMA);
				add("/components/schemas/Error/properties/code", SCHEMA);
				add("/components/schemas/Error/properties/message", SCHEMA);
				add("/components/schemas/Error/properties/fields", SCHEMA);
			}

			private void add(String path, V3State state) {
				JsonPointer ptr = JsonPointer.compile(path);
				if (!containsKey(ptr)) {
					put(ptr, state);
				} else {
					throw new RuntimeException("Duplicate key in expected results map: " + ptr);
				}
			}
		};
	}

	@Test
	public void testWalk() {
		JsonStateWalker<V3State> walker = new JsonStateWalker<V3State>(tracker, this::walk, true, true);
		walker.walk(model);
		for (Entry<JsonPointer, V3State> expected : refableResults.entrySet()) {
			fail(String.format("Expected refable state %s at %s was not encountered", expected.getValue(),
					expected.getKey()));

		}
	}

	private void walk(JsonNode node, State<V3State> state, V3State value, List<Object> path, JsonPointer pointer) {
		if (value.isConformingSite()) {
			assertNotNull("Path reported as refable but not in expected results: " + pointer,
					refableResults.get(pointer));
			assertEquals("Incorrect state for " + pointer, refableResults.get(pointer), value);
			refableResults.remove(pointer);
		}
	}
}
