package com.reprezen.kaizen.normalizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.scanner.ScannerException;
import com.reprezen.kaizen.normalizer.Localizer.LocalizedContent;
import com.reprezen.kaizen.normalizer.util.StateMachine;
import com.reprezen.kaizen.normalizer.util.StateMachine.State;

public class ContentManager<E extends Enum<E> & Component> {

	private Map<Reference, Content<E>> contentCache = new HashMap<>();
	private Localizer localizer = new Localizer();
	private Options options;
	private StateMachine<E> machine;

	public ContentManager(Options options, StateMachine<E> machine) {
		this.options = options;
		this.machine = machine;
	}

	public Content<E> load(String refString, Reference base, State<E> scanState) {
		return load(new Reference(refString, base), scanState);
	}

	public Content<E> load(Reference ref, String scanState) {
		return load(ref, machine.getState(scanState));
	}

	public Content<E> load(Reference ref, State<E> scanState) {
		if (contentCache.containsKey(ref)) {
			return contentCache.get(ref);
		} else if (!ref.isValid()) {
			return createInvalidContent(ref, ref.getInvalidReason());
		}
		Content<E> doc = loadDoc(ref, ref.equals(ref.getUrlRef()) ? scanState : null);
		if (contentCache.containsKey(ref)) {
			return contentCache.get(ref);
		}
		if (doc.isValid()) {
			JsonPointer pointer = ref.getPointer();
			if (pointer.matches()) {
				return doc;
			} else {
				JsonNode node = doc.at(pointer);
				if (node.isMissingNode()) {
					ref.markInvalid("No JSON value at specified pointer location in retrieved document");
					return createInvalidContent(ref);
				} else {
					return createContent(ref, node, scanState);
				}
			}
		} else {
			ref.markInvalid(doc.getInvalidReason());
			return createInvalidContent(ref, doc.getInvalidReason());
		}
	}

	public LocalizedContent localize(JsonNode node, Component component, JsonPointer pointer, Reference base) {
		return localizer.localize(node, component, pointer, base);
	}

	public LocalizedContent mergeLocalize(JsonNode node, Component component, JsonPointer pointer, Reference base) {
		return localizer.mergeLocalize(node, component, pointer, base);
	}

	public Iterable<LocalizedContent> getLocalizedContent(Component component) {
		return new Iterable<LocalizedContent>() {
			@Override
			public Iterator<LocalizedContent> iterator() {
				return localizer.getLocalizedContent(component).iterator();
			}
		};
	}

	public LocalizedContent getLocalizedContent(Reference ref, Reference base) {
		return localizer.getLocalizedContent(ref, base);
	}

	public LocalizedContent getLocalizedContent(Reference ref) {
		return localizer.getLocalizedContent(ref);
	}

	public Content<E> createContent(Reference ref, JsonNode tree, State<E> scanState) {
		if (contentCache.containsKey(ref)) {
			throw duplicateContent(ref);
		} else {
			contentCache.put(ref, new Content<E>(ref, tree, scanState, this, options));
		}
		return contentCache.get(ref);
	}

	public Content<E> createInvalidContent(Reference ref) {
		return createInvalidContent(ref, ref.getInvalidReason());
	}

	public Content<E> createInvalidContent(Reference ref, String invalidReason) {
		if (contentCache.containsKey(ref)) {
			throw duplicateContent(ref);
		} else {
			contentCache.put(ref, new Content<E>(ref, invalidReason));
		}
		return contentCache.get(ref);
	}

	private Content<E> loadDoc(Reference ref, State<E> scanState) {
		String text;
		Reference rootRef = ref.getUrlRef();
		if (contentCache.containsKey(rootRef)) {
			return contentCache.get(rootRef);
		}
		try {
			text = readFromUrl(ref.getUrl());
			JsonNode tree = loadTree(text);
			return createContent(rootRef, tree, scanState);
		} catch (Exception e) {
			// The YAML scanner produces exceptions that are very clumsy: toString is
			// overridden and provides a multi-line explanation of the parse error. Not good
			// for summaries, and out of alignment with default exception behavior. So we
			// provide our own summary in that case.
			rootRef.markInvalid(
					e instanceof ScannerException ? "File does not contain valid YAML content" : e.toString(), e);
			return createInvalidContent(rootRef);
		}
	}

	private String readFromUrl(URL url) throws IOException {
		String content = "";
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
			content = reader.lines().collect(Collectors.joining("\n"));
		}
		return content;
	}

	private static ObjectMapper jsonMapper = new ObjectMapper();
	private static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

	private static JsonNode loadTree(String text) throws IOException {
		if (text.trim().startsWith("{")) {
			return jsonMapper.readTree(text);
		} else {
			return yamlMapper.readTree(text);
		}
	}

	private IllegalStateException duplicateContent(Reference ref) {
		return new IllegalStateException("Duplicate content item for reference " + ref);
	}
}
