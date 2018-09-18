package com.reprezen.kaizen.normalizer.v3;

import static com.reprezen.kaizen.normalizer.v3.V3State.CALLBACK;
import static com.reprezen.kaizen.normalizer.v3.V3State.CALLBACK_DEF;
import static com.reprezen.kaizen.normalizer.v3.V3State.ENCODING;
import static com.reprezen.kaizen.normalizer.v3.V3State.EXAMPLE;
import static com.reprezen.kaizen.normalizer.v3.V3State.EXAMPLE_DEF;
import static com.reprezen.kaizen.normalizer.v3.V3State.HEADER;
import static com.reprezen.kaizen.normalizer.v3.V3State.HEADER_DEF;
import static com.reprezen.kaizen.normalizer.v3.V3State.LINK;
import static com.reprezen.kaizen.normalizer.v3.V3State.LINK_DEF;
import static com.reprezen.kaizen.normalizer.v3.V3State.MEDIA_TYPE;
import static com.reprezen.kaizen.normalizer.v3.V3State.MODEL;
import static com.reprezen.kaizen.normalizer.v3.V3State.OPERATION;
import static com.reprezen.kaizen.normalizer.v3.V3State.PARAMETER;
import static com.reprezen.kaizen.normalizer.v3.V3State.PARAMETER_DEF;
import static com.reprezen.kaizen.normalizer.v3.V3State.PATH;
import static com.reprezen.kaizen.normalizer.v3.V3State.REQUEST_BODY;
import static com.reprezen.kaizen.normalizer.v3.V3State.REQUEST_BODY_DEF;
import static com.reprezen.kaizen.normalizer.v3.V3State.RESPONSE;
import static com.reprezen.kaizen.normalizer.v3.V3State.RESPONSE_DEF;
import static com.reprezen.kaizen.normalizer.v3.V3State.SCHEMA;
import static com.reprezen.kaizen.normalizer.v3.V3State.SCHEMA_DEF;
import static com.reprezen.kaizen.normalizer.v3.V3State.SECURITY_SCHEME;
import static com.reprezen.kaizen.normalizer.v3.V3State.SECURITY_SCHEME_DEF;

import com.reprezen.kaizen.normalizer.util.StateMachine;

public class V3StateMachine extends StateMachine<V3State> {

	public V3StateMachine() {
		// use a special dummy node for anonymous and off-road states, so we won't need
		// null checks
		super(V3State.class, V3State.ANON, V3State.OFFROAD);

		// Set up all state transitions to use while traversing OpenAPI v3 model spec

		// ways to get to component definitions
		transit().from(MODEL).via("paths", "re: /.*").to(PATH);
		transit().from(MODEL).via("components", "schemas", "re: (?!x-)[A-Za-z0-9._-]+").to(SCHEMA_DEF);
		transit().from(MODEL).via("components", "responses", "re: (?!x-)[A-Za-z0-9._-]+").to(RESPONSE_DEF);
		transit().from(MODEL).via("components", "parameters", "re: (?!x-)[A-Za-z0-9._-]+").to(PARAMETER_DEF);
		transit().from(MODEL).via("components", "examples", "re: (?!x-)[A-Za-z0-9._-]+").to(EXAMPLE_DEF);
		transit().from(MODEL).via("components", "requestBodies", "re: (?!x-)[A-Za-z0-9._-]+").to(REQUEST_BODY_DEF);
		transit().from(MODEL).via("components", "headers", "re: (?!x-)[A-Za-z0-9._-]+").to(HEADER_DEF);
		transit().from(MODEL).via("components", "securitySchemes", "re: (?!x-)[A-Za-z0-9._-]+").to(SECURITY_SCHEME_DEF);
		transit().from(MODEL).via("components", "links", "re: (?!x-)[A-Za-z0-9._-]+").to(LINK_DEF);
		transit().from(MODEL).via("components", "callbacks", "re: (?!x-)[A-Za-z0-9._-]+").to(CALLBACK_DEF);

		// ways to get to a schema object
		transit().from(PARAMETER).via("schema").to(SCHEMA);
		transit().from(MEDIA_TYPE).via("schema").to(SCHEMA);
		transit().from(SCHEMA).via("allOf", "#").to(SCHEMA);
		transit().from(SCHEMA).via("oneOf", "#").to(SCHEMA);
		transit().from(SCHEMA).via("anyOf", "#").to(SCHEMA);
		transit().from(SCHEMA).via("not").to(SCHEMA);
		transit().from(SCHEMA).via("items").to(V3State.SCHEMA);
		transit().from(SCHEMA).via("properties", "*").to(SCHEMA);
		transit().from(SCHEMA).via("additionalProperties").to(SCHEMA);

		// ways to get to a response object
		transit().from(OPERATION).via("responses", "*").to(RESPONSE);

		// ways to get to a parameter object
		transit().from(PATH).via("parameters", "#").to(PARAMETER);
		transit().from(OPERATION).via("parameters", "#").to(PARAMETER);

		// ways to get to an example object
		transit().from(PARAMETER).via("examples", "*").to(EXAMPLE);
		transit().from(MEDIA_TYPE).via("examples", "*").to(EXAMPLE);

		// ways to get to a request body object
		transit().from(OPERATION).via("requestBody").to(REQUEST_BODY);

		// ways to get to a header object
		transit().from(ENCODING).via("headers", "*").to(HEADER);
		transit().from(RESPONSE).via("headers", "*").to(HEADER);

		// ways to get to a security scheme object
		// none - they're referenced by name within the model

		// ways to get to a link object
		transit().from(RESPONSE).via("links", "*").to(LINK);

		// ways to get to a callback object
		transit().from(OPERATION).via("callbacks", "*").to(CALLBACK);

		// ways to get to an operation object
		transit().from(PATH).via("get").to(OPERATION);
		transit().from(PATH).via("put").to(OPERATION);
		transit().from(PATH).via("post").to(OPERATION);
		transit().from(PATH).via("delete").to(OPERATION);
		transit().from(PATH).via("options").to(OPERATION);
		transit().from(PATH).via("head").to(OPERATION);
		transit().from(PATH).via("patch").to(OPERATION);
		transit().from(PATH).via("trace").to(OPERATION);

		// ways to get to a media type object
		transit().from(PARAMETER).via("content", "*").to(MEDIA_TYPE);
		transit().from(REQUEST_BODY).via("content", "*").to(MEDIA_TYPE);
		transit().from(RESPONSE).via("content", "*").to(MEDIA_TYPE);

		// ways to get to an encoding object
		transit().from(MEDIA_TYPE).via("encoding", "*").to(ENCODING);

		// definition sites can't have references, so we have separate state values for
		// them. But outgoing edges are identical to those of other sites for the same
		// object type
		copyOutEdges(SCHEMA, SCHEMA_DEF);
		copyOutEdges(RESPONSE, RESPONSE_DEF);
		copyOutEdges(PARAMETER, PARAMETER_DEF);
		copyOutEdges(EXAMPLE, EXAMPLE_DEF);
		copyOutEdges(REQUEST_BODY, REQUEST_BODY_DEF);
		copyOutEdges(HEADER, HEADER_DEF);
		copyOutEdges(SECURITY_SCHEME, SECURITY_SCHEME_DEF);
		copyOutEdges(LINK, LINK_DEF);
		copyOutEdges(CALLBACK, CALLBACK_DEF);

	}
}
