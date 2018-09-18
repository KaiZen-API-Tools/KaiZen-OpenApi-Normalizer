package com.reprezen.kaizen.normalizer;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.reprezen.kaizen.normalizer.ReferenceScanner.ScanOp;

public class Content {
	private Reference ref;
	private JsonNode tree;
	private V2State scanState;
	private boolean valid;
	private String invalidReason = null;
	private ContentManager contentManager;
	private Options options;

	Content(Reference ref, JsonNode tree, V2State scanState, ContentManager contentManager, Options options) {
		this.ref = ref;
		this.tree = tree;
		this.scanState = scanState;
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
		this.tree = new ReferenceScanner(tree, ref, scanOp, contentManager, options).scan(scanState);
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