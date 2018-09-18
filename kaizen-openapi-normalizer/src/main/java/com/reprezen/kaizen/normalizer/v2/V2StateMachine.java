package com.reprezen.kaizen.normalizer.v2;

import static com.reprezen.kaizen.normalizer.v2.V2State.ANON;
import static com.reprezen.kaizen.normalizer.v2.V2State.MODEL;
import static com.reprezen.kaizen.normalizer.v2.V2State.OFFROAD;
import static com.reprezen.kaizen.normalizer.v2.V2State.OPERATION;
import static com.reprezen.kaizen.normalizer.v2.V2State.PARAMETER;
import static com.reprezen.kaizen.normalizer.v2.V2State.PARAMETER_DEF;
import static com.reprezen.kaizen.normalizer.v2.V2State.PATH;
import static com.reprezen.kaizen.normalizer.v2.V2State.RESPONSE;
import static com.reprezen.kaizen.normalizer.v2.V2State.RESPONSE_DEF;
import static com.reprezen.kaizen.normalizer.v2.V2State.SCHEMA;
import static com.reprezen.kaizen.normalizer.v2.V2State.SCHEMA_DEF;

import com.reprezen.kaizen.normalizer.util.StateMachine;

public class V2StateMachine extends StateMachine<V2State> {

	public V2StateMachine() {
		// use a special dummy node for anonymous and off-road states, so we won't need
		// null checks
		super(V2State.class, ANON, OFFROAD);

		// Set up all state transitions to use while traversing a Swagger model spec

		// ways to get to object definitions
		transit().from(MODEL).via("paths", "re: /.*").to(PATH);
		transit().from(MODEL).via("definitions", "re: (?!x-)[a-zA-Z0-9._-]+").to(SCHEMA_DEF);
		transit().from(MODEL).via("responses", "re: (?!x-)[a-zA-Z0-9._-]+").to(RESPONSE_DEF);
		transit().from(MODEL).via("parameters", "re: (?!x-)[a-zA-Z0-9._-]+").to(PARAMETER_DEF);

		// ways to get to a schema object
		transit().from(PARAMETER).via("schema").to(SCHEMA); // valid if `in == "body"`
		transit().from(RESPONSE).via("schema").to(SCHEMA);
		transit().from(SCHEMA).via("items").to(SCHEMA);
		transit().from(SCHEMA).via("allOf", "#").to(SCHEMA);
		transit().from(SCHEMA).via("properties", "*").to(SCHEMA);
		transit().from(SCHEMA).via("additionalProperties").to(SCHEMA);

		// ways to get to an operation object
		transit().from(PATH).via("get").to(OPERATION);
		transit().from(PATH).via("put").to(OPERATION);
		transit().from(PATH).via("post").to(OPERATION);
		transit().from(PATH).via("delete").to(OPERATION);
		transit().from(PATH).via("options").to(OPERATION);
		transit().from(PATH).via("head").to(OPERATION);
		transit().from(PATH).via("patch").to(OPERATION);

		// ways to get to a response object
		transit().from(OPERATION).via("responses", "*").to(RESPONSE);

		// ways to get to a parameter object
		transit().from(PATH).via("parameters", "#").to(PARAMETER);
		transit().from(OPERATION).via("parameters", "#").to(PARAMETER);

		// definition sites can't have references, so we have separate state values for
		// them. But outgoing edges are
		// identical to those of other sites for the same object type
		copyOutEdges(SCHEMA, SCHEMA_DEF);
		copyOutEdges(RESPONSE, RESPONSE_DEF);
		copyOutEdges(PARAMETER, PARAMETER_DEF);
	}
}
