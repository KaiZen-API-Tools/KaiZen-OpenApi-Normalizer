package com.reprezen.kaizen.normalizer;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.reprezen.kaizen.normalizer.ReferenceScanner.ScanOp;
import com.reprezen.kaizen.normalizer.util.JsonCopier;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker.AdvancedWalkMethod;
import com.reprezen.kaizen.normalizer.util.JsonStateWalker.Disposition;
import com.reprezen.kaizen.normalizer.util.StateMachine;
import com.reprezen.kaizen.normalizer.util.StateMachine.State;
import com.reprezen.kaizen.normalizer.util.StateMachine.Tracker;

public class ReferenceProcessor {
	private Options options;
	private ContentManager contentManager;

	private ReferenceProcessor(Option... options) {
		this(Options.of(options));
	}

	private ReferenceProcessor(Options options) {
		this.options = options;
		this.contentManager = new ContentManager(options);
	}

	public JsonNode process(URL sourceModel) {
		List<Content> models = new ArrayList<>();
		models.add(contentManager.load(new Reference(sourceModel)));
		for (URL additionalFile : options.getAdditionalFileUrls()) {
			models.add(contentManager.load(new Reference(additionalFile)));
		}
		inlineNonConformingRefs(models);
		localizeComponents(models);
		applyPolicy(models);
		return buildNormalizedModel(models.get(0));
	}

	private void inlineNonConformingRefs(List<Content> models) {
		for (Content model : models) {
			model.scan(ScanOp.LOAD);
		}
	}

	private void localizeComponents(List<Content> models) {
		for (Content model : models) {
			model.scan(ScanOp.COMPONENTS);
		}
	}

	private void applyPolicy(List<Content> models) {
		for (Content model : models) {
			model.scan(ScanOp.POLICY);
		}
	}

	private JsonNode buildNormalizedModel(Content topModel) {
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		copyOtherElements(topModel.getTree(), result);
		return result;
	}

	private void copyOtherElements(JsonNode model, ObjectNode result) {
		StateMachine<V2State> machine = new StateMachine<V2State>();
		Tracker<V2State> tracker = machine.tracker(V2State.MODEL);
		OtherElementWalkMethod walkMethod = new OtherElementWalkMethod(result, tracker);
		new JsonStateWalker<>(tracker, walkMethod, true, true).walk(model);
	}

	private static class OtherElementWalkMethod implements AdvancedWalkMethod<V2State> {
		private JsonNode target;

		public OtherElementWalkMethod(JsonNode copyTo, Tracker<V2State> tracker) {
			this.target = copyTo;
		}

		@Override
		public Disposition walk(JsonNode node, State<V2State> state, V2State stateValue, List<Object> path,
				JsonPointer pointer) {
			if (stateValue == V2State.OFFROAD) {
				JsonCopier.copy(node, target, path);
				return Disposition.done();
			}
			return Disposition.normal();
		}

	}
}
