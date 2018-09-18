package com.reprezen.kaizen.normalizer;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.kaizen.normalizer.ReferenceScanner.ScanOp;
import com.reprezen.kaizen.normalizer.util.StateMachine.State;

public class Content<E extends Enum<E> & Component> {
	private Reference ref;
	private JsonNode tree;
	private State<E> scanState;
	private boolean valid;
	private String invalidReason = null;
	private ContentManager<E> contentManager;
	private Options options;

	Content(Reference ref, JsonNode tree, State<E> scanState2, ContentManager<E> contentManager, Options options) {
		this.ref = ref;
		this.tree = tree;
		this.scanState = scanState2;
		this.valid = true;
		this.contentManager = contentManager;
		this.options = options;
	}

	Content(Reference ref, String invalidReason) {
		this.ref = ref;
		this.valid = false;
		this.invalidReason = invalidReason;
	}

	public void scan(ScanOp scanOp) {
		this.tree = new ReferenceScanner<E>(tree, ref, scanOp, contentManager, options).scan(scanState);
	}

	public Reference getRef() {
		return ref;
	}

	public JsonNode getTree() {
		return tree;
	}

	public JsonNode copyTree() {
		return tree.deepCopy();
	}

	public JsonNode at(String pointer) {
		return at(JsonPointer.compile(pointer));
	}

	public JsonNode at(JsonPointer pointer) {
		return tree.at(pointer);
	}

	public boolean isValid() {
		return valid;
	}

	public String getInvalidReason() {
		return invalidReason;
	}
}