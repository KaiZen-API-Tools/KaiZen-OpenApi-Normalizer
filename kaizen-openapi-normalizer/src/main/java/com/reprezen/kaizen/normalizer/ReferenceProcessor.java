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
import com.reprezen.kaizen.normalizer.v2.V2State;

public class ReferenceProcessor<E extends Enum<E> & Component> {
	private Options options;
	private ContentManager<E> contentManager;
	private StateMachine<E> machine;
	private State<E> modelState;

	private ReferenceProcessor(StateMachine<E> machine, Option... options) {
		this(machine, Options.of(options));
	}

	private ReferenceProcessor(StateMachine<E> machine, Options options) {
		this.options = options;
		this.contentManager = new ContentManager<E>(options, machine);
		this.machine = machine;
		this.modelState = machine.getState("MODEL");
	}

	public JsonNode process(URL sourceModel) {
		List<Content<E>> models = new ArrayList<>();
		models.add(contentManager.load(new Reference(sourceModel), modelState));
		for (URL additionalFile : options.getAdditionalFileUrls()) {
			models.add(contentManager.load(new Reference(additionalFile), modelState));
		}
		inlineNonConformingRefs(models);
		localizeComponents(models);
		applyPolicy(models);
		return buildNormalizedModel(models.get(0));
	}

	private void inlineNonConformingRefs(List<Content<E>> models) {
		for (Content<E> model : models) {
			model.scan(ScanOp.LOAD);
		}
	}

	private void localizeComponents(List<Content<E>> models) {
		for (Content<E> model : models) {
			model.scan(ScanOp.COMPONENTS);
		}
	}

	private void applyPolicy(List<Content<E>> models) {
		for (Content<E> model : models) {
			model.scan(ScanOp.POLICY);
		}
	}

	private JsonNode buildNormalizedModel(Content<E> topModel) {
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		copyOtherElements(topModel.getTree(), result);
		return result;
	}

	private void copyOtherElements(JsonNode model, ObjectNode result) {
		Tracker<E> tracker = machine.tracker(modelState);
		OtherElementWalkMethod<E> walkMethod = new OtherElementWalkMethod<E>(result);
		new JsonStateWalker<E>(tracker, walkMethod, true, true).walk(model);
	}

	private static class OtherElementWalkMethod<E extends Enum<E> & Component> implements AdvancedWalkMethod<E> {
		private JsonNode target;

		public OtherElementWalkMethod(JsonNode copyTo) {
			this.target = copyTo;
		}

		@Override
		public Disposition walk(JsonNode node, State<E> state, E stateValue, List<Object> path, JsonPointer pointer) {
			if (stateValue == V2State.OFFROAD) {
				JsonCopier.copy(node, target, path);
				return Disposition.done();
			}
			return Disposition.normal();
		}

	}
}
