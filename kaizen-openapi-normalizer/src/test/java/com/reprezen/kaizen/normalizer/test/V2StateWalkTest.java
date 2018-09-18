package com.reprezen.kaizen.normalizer.test;

import static com.reprezen.kaizen.normalizer.v2.V2State.PARAMETER;
import static com.reprezen.kaizen.normalizer.v2.V2State.PATH;
import static com.reprezen.kaizen.normalizer.v2.V2State.RESPONSE;
import static com.reprezen.kaizen.normalizer.v2.V2State.SCHEMA;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker;
import com.reprezen.kaizen.normalizer.util.StateMachine.State;
import com.reprezen.kaizen.normalizer.util.StateMachine.Tracker;
import com.reprezen.kaizen.normalizer.v2.V2State;
import com.reprezen.kaizen.normalizer.v2.V2StateMachine;

public class V2StateWalkTest extends NormalizerTestBase {

	private JsonNode model;
	private Tracker<V2State> tracker;
	private HashMap<JsonPointer, V2State> refableResults;

	@Before
	public void setup() throws JsonProcessingException, IOException {
		this.model = loadYaml("StateWalkV2");
		this.tracker = new V2StateMachine().tracker(V2State.MODEL);
		this.refableResults = new HashMap<JsonPointer, V2State>() {
			private static final long serialVersionUID = 1L;
			{
				add("/paths/~1products", PATH);
				add("/paths/~1products/get/parameters/0", PARAMETER);
				add("/paths/~1products/get/parameters/1", PARAMETER);
				add("/paths/~1products/get/responses/200", RESPONSE);
				add("/paths/~1products/get/responses/200/schema", SCHEMA);
				add("/paths/~1products/get/responses/200/schema/items", SCHEMA);
				add("/paths/~1products/get/responses/default", RESPONSE);
				add("/paths/~1products/get/responses/default/schema", SCHEMA);
				add("/paths/~1estimates~1price", PATH);
				add("/paths/~1estimates~1price/get/parameters/0", PARAMETER);
				add("/paths/~1estimates~1price/get/parameters/1", PARAMETER);
				add("/paths/~1estimates~1price/get/parameters/2", PARAMETER);
				add("/paths/~1estimates~1price/get/parameters/3", PARAMETER);
				add("/paths/~1estimates~1price/get/responses/200", RESPONSE);
				add("/paths/~1estimates~1price/get/responses/200/schema", SCHEMA);
				add("/paths/~1estimates~1price/get/responses/200/schema/items", SCHEMA);
				add("/paths/~1estimates~1price/get/responses/default", RESPONSE);
				add("/paths/~1estimates~1price/get/responses/default/schema", SCHEMA);
				add("/paths/~1estimates~1time", PATH);
				add("/paths/~1estimates~1time/get/parameters/0", PARAMETER);
				add("/paths/~1estimates~1time/get/parameters/1", PARAMETER);
				add("/paths/~1estimates~1time/get/parameters/2", PARAMETER);
				add("/paths/~1estimates~1time/get/parameters/3", PARAMETER);
				add("/paths/~1estimates~1time/get/responses/200", RESPONSE);
				add("/paths/~1estimates~1time/get/responses/200/schema", SCHEMA);
				add("/paths/~1estimates~1time/get/responses/200/schema/items", SCHEMA);
				add("/paths/~1estimates~1time/get/responses/default", RESPONSE);
				add("/paths/~1estimates~1time/get/responses/default/schema", SCHEMA);
				add("/paths/~1me", PATH);
				add("/paths/~1me/get/responses/200", RESPONSE);
				add("/paths/~1me/get/responses/200/schema", SCHEMA);
				add("/paths/~1me/get/responses/default", RESPONSE);
				add("/paths/~1me/get/responses/default/schema", SCHEMA);
				add("/paths/~1history", PATH);
				add("/paths/~1history/get/parameters/0", PARAMETER);
				add("/paths/~1history/get/parameters/1", PARAMETER);
				add("/paths/~1history/get/responses/200", RESPONSE);
				add("/paths/~1history/get/responses/200/schema", SCHEMA);
				add("/paths/~1history/get/responses/default", RESPONSE);
				add("/paths/~1history/get/responses/default/schema", SCHEMA);
				add("/definitions/Product/properties/product_id", SCHEMA);
				add("/definitions/Product/properties/description", SCHEMA);
				add("/definitions/Product/properties/display_name", SCHEMA);
				add("/definitions/Product/properties/capacity", SCHEMA);
				add("/definitions/Product/properties/image", SCHEMA);
				add("/definitions/PriceEstimate/properties/product_id", SCHEMA);
				add("/definitions/PriceEstimate/properties/currency_code", SCHEMA);
				add("/definitions/PriceEstimate/properties/display_name", SCHEMA);
				add("/definitions/PriceEstimate/properties/estimate", SCHEMA);
				add("/definitions/PriceEstimate/properties/low_estimate", SCHEMA);
				add("/definitions/PriceEstimate/properties/high_estimate", SCHEMA);
				add("/definitions/PriceEstimate/properties/surge_multiplier", SCHEMA);
				add("/definitions/Profile/properties/first_name", SCHEMA);
				add("/definitions/Profile/properties/last_name", SCHEMA);
				add("/definitions/Profile/properties/email", SCHEMA);
				add("/definitions/Profile/properties/picture", SCHEMA);
				add("/definitions/Profile/properties/promo_code", SCHEMA);
				add("/definitions/Activity/properties/uuid", SCHEMA);
				add("/definitions/Activities/properties/offset", SCHEMA);
				add("/definitions/Activities/properties/limit", SCHEMA);
				add("/definitions/Activities/properties/count", SCHEMA);
				add("/definitions/Activities/properties/history", SCHEMA);
				add("/definitions/Activities/properties/history/items", SCHEMA);
				add("/definitions/Error/properties/code", SCHEMA);
				add("/definitions/Error/properties/message", SCHEMA);
				add("/definitions/Error/properties/fields", SCHEMA);
			}

			private void add(String path, V2State state) {
				JsonPointer ptr = JsonPointer.compile(path);
				if (!containsKey(ptr)) {
					put(ptr, state);
				} else {
					throw new RuntimeException("Duplicate key in expected results map: " + ptr);
				}
			}
		};
	}

	private int refableCount;

	@Test
	public void testWalk() {
		refableCount = 0;
		JsonStateWalker<V2State> walker = new JsonStateWalker<V2State>(tracker, this::walk, true, true);
		walker.walk(model);
		assertEquals(refableResults.size(), refableCount);
	}

	private void walk(JsonNode node, State<V2State> state, V2State value, List<Object> path, JsonPointer pointer) {
		if (value.isConformingSite()) {
			assertNotNull("Path reported as refable but not in expected results: " + pointer,
					refableResults.get(pointer));
			assertEquals("Incorrect state for " + pointer, refableResults.get(pointer), value);
			refableCount += 1;
		}
	}
}
