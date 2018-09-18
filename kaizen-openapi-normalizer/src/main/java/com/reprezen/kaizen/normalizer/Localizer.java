package com.reprezen.kaizen.normalizer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Localizer {

	Map<Reference, LocalizedContent> contentByRef = new HashMap<>();
	Map<Component, Map<String, LocalizedContent>> localizedContent = new HashMap<>();

	public LocalizedContent localize(JsonNode node, Component component, JsonPointer pointer, Reference base) {
		Reference ref = new Reference("#" + pointer, base, component.getDefinedComponent());
		if (!contentByRef.containsKey(ref)) {
			if (!localizedContent.containsKey(component)) {
				localizedContent.put(component, new LinkedHashMap<>());
			}
			Map<String, LocalizedContent> componentMap = localizedContent.get(component);
			String preferredName = component.getPreferredName(pointer.toString(), ref.getUrl());
			String name = preferredName;
			int i = 1;
			while (componentMap.containsKey(name)) {
				name = preferredName + "_" + i++;
			}
			LocalizedContent localized = new LocalizedContent(component, name, node, ref.getUrlRef());
			componentMap.put(name, localized);
			contentByRef.put(ref, localized);
		}
		return contentByRef.get(ref);
	}

	public LocalizedContent mergeLocalize(JsonNode node, Component component, JsonPointer pointer, Reference base) {
		Reference ref = new Reference("#" + pointer, base, component.getDefinedComponent());
		if (contentByRef.containsKey(ref)) {
			ObjectNode current = (ObjectNode) contentByRef.get(ref).getNode();
			for (Iterator<String> iter = node.fieldNames(); iter.hasNext();) {
				String name = iter.next();
				current.set(name, node.get(name));
			}
			return contentByRef.get(ref);
		} else {
			throw new IllegalStateException(
					"Merging a path reference for without having localized its non-ref content: "
							+ ref.getCanonicalString());
		}
	}

	public Collection<LocalizedContent> getLocalizedContent(Component component) {
		if (!localizedContent.containsKey(component)) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableCollection(localizedContent.get(component).values());
		}
	}
	
	public LocalizedContent getLocalizedContent(Reference ref, Reference base) {
		return getLocalizedContent(new Reference(ref.getRefString(), base));
	}

	public LocalizedContent getLocalizedContent(Reference ref) {
		return contentByRef.get(ref);
	}

	public static class LocalizedContent {
		private Component component;
		private String name;
		private JsonNode node;
		private Reference base;
		private JsonPointer containerPointer;

		public LocalizedContent(Component component, String name, JsonNode node, Reference base) {
			super();
			this.component = component;
			this.name = name;
			this.node = node;
			this.base = base;
			this.containerPointer = JsonPointer.compile(component.getContainerPath());
		}

		public Component getComponent() {
			return component;
		}

		public String getName() {
			return name;
		}

		public JsonNode getNode() {
			return node;
		}

		public Reference getBase() {
			return base;
		}

		public Reference getLocalizedRef(Reference base) {
			String refString = String.format("#%s/%s", component.getContainerPath(), name);
			return new Reference(refString, base, component);
		}

		public void addDefinition(JsonNode model) {
			addMissingObjectNode((ObjectNode) model, containerPointer);
		}

		private void addMissingObjectNode(ObjectNode node, JsonPointer pointer) {
			if (!pointer.matches()) {
				String head = pointer.getMatchingProperty();
				if (!node.has(head)) {
					ObjectNode headNode = node.putObject(head);
					addMissingObjectNode(headNode, pointer.tail());
				}
			}
		}
	}
}
