package com.reprezen.kaizen.normalizer.v3;

import com.reprezen.kaizen.normalizer.util.StateMachine;

public class V3StateMachine extends StateMachine<V3State> {

	public V3StateMachine() {
		// use a special dummy node for anonymous and off-road states, so we won't need
		// null checks
		super(V3State.class, V3State.ANON, V3State.OFFROAD);

		// Set up all state transitions to use while traversing OpenAPI v3 model spec
		// TODO
	}
}
