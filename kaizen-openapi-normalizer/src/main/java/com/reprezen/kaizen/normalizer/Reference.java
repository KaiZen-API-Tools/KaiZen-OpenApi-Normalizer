package com.reprezen.kaizen.normalizer;

import static com.reprezen.kaizen.normalizer.Reference.ReferenceTreatment.ERROR;
import static com.reprezen.kaizen.normalizer.Reference.ReferenceTreatment.INLINE_CONFORMING;
import static com.reprezen.kaizen.normalizer.Reference.ReferenceTreatment.INLINE_NONCONFORMING;
import static com.reprezen.kaizen.normalizer.Reference.ReferenceTreatment.LOCALIZE;
import static com.reprezen.kaizen.normalizer.Reference.ReferenceTreatment.MERGE;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Reference {

	public static boolean isRefNode(JsonNode node) {
		return getRefString(node).isPresent();
	}

	public static Optional<String> getRefString(JsonNode node) {
		if (node instanceof ObjectNode) {
			ObjectNode objNode = (ObjectNode) node;
			if (objNode.has("$ref") && objNode.get("$ref").isTextual()) {
				return Optional.of(objNode.get("$ref").asText());
			}
		}
		return Optional.empty();
	}

	private String refString;
	private URL url;
	private String urlString;
	private String fragment;
	private JsonPointer pointer;
	private Component component = null;
	private boolean valid = true;
	private Exception invalidException = null;
	private String invalidReason;

	public Reference(String refString) {
		this(refString, null);
	}

	public Reference(URL url) {
		this(url.toString());
	}

	public Reference(String refString, Reference base) {
		this(refString, base, null);
	}

	public Reference(URL url, Reference base) {
		this(url.toString(), base, null);
	}

	public Reference(URL url, Reference base, Component component) {
		this(url.toString(), base, component);
	}

	public Reference(String refString, Reference baseRef, Component component) {
		this.refString = refString;
		String[] parts = refString.split("#", 2);
		this.urlString = parts[0];
		try {
			URL base = baseRef != null ? baseRef.url : null;
			this.url = normalizeUrl(base, urlString);
			this.urlString = url.toString();
		} catch (MalformedURLException | URISyntaxException e) {
			markInvalid(e);
		}
		if (parts.length > 1 && !parts[1].isEmpty()) {
			fragment = parts[1];
		}
		try {
			this.pointer = JsonPointer.compile(fragment != null ? fragment : "");
		} catch (Exception e) {
			markInvalid(e);
		}
		this.component = component;
	}

	public static Reference invalidRef(String refString, String invalidReason) {
		Reference ref = new Reference(refString);
		ref.markInvalid(invalidReason);
		return ref;
	}

	public String getRefString() {
		return refString;
	}

	public URL getUrl() {
		return url;
	}

	public Reference getUrlRef() {
		return new Reference(url);
	}

	public String getUrlString() {
		return urlString;
	}

	public String getCanonicalString() {
		return urlString + (fragment != null ? "#" + fragment : "");
	}

	public String getFragment() {
		return fragment;
	}

	public JsonPointer getPointer() {
		return pointer;
	}

	public Component getComponent() {
		return component;
	}

	public boolean isValid() {
		return valid;
	}

	public String getInvalidReason() {
		return invalidReason;
	}

	public Exception getInvalidException() {
		return invalidException;
	}

	public JsonNode getRefNode() {
		return getRefNode(true);
	}

	public static final String ADORNMENT_PROPERTY = "_info";

	public JsonNode getRefNode(boolean adorned) {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("$ref", refString);
		if (adorned) {
			ObjectNode adornment = node.putObject(ADORNMENT_PROPERTY);
			adornment.put("valid", valid);
			adornment.put("url", urlString);
			adornment.put("fragment", fragment);
			adornment.set("component", getComponentInfo());
			adornment.put("invalidReason", invalidReason);
			adornment.set("invalidException", getExceptionInfo());
		}
		return node;
	}

	public static Reference of(JsonNode refNode) {
		if (isRefNode(refNode)) {
			Reference ref = new Reference(refNode.get("$ref").asText());
			adorn(ref, refNode);
			return ref;
		} else {
			throw new IllegalArgumentException();
		}
	}

	private static void adorn(Reference ref, JsonNode refNode) {
		JsonNode info = refNode.path(ADORNMENT_PROPERTY);
		if (!info.isMissingNode()) {
			ref.valid = info.path("valid").asBoolean();
			ref.urlString = info.path("url").asText();
			try {
				ref.url = new URL(ref.urlString);
			} catch (MalformedURLException e) {
			}
			ref.fragment = info.path("fragment").asText();
			ref.component = reconstituteComponent(info.path("component"));
			ref.invalidReason = info.path("invalidReason").asText();
			ref.invalidException = reconstituteException(info.path("invalidException"));
		}
	}

	private JsonNode getComponentInfo() {
		if (component != null) {
			ObjectNode info = JsonNodeFactory.instance.objectNode();
			info.put("class", component.getClass().getName());
			info.put("name", component.name());
			return info;
		} else {
			return MissingNode.getInstance();
		}
	}

	private static Component reconstituteComponent(JsonNode comp) {
		if (comp.isMissingNode()) {
			return null;
		} else {
			try {
				Class<?> cls = Class.forName(comp.path("class").asText());
				Method nameMeth = cls.getMethod("valueOf", String.class);
				return (Component) nameMeth.invoke(null, comp.path("name").asText());
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private JsonNode getExceptionInfo() {
		if (invalidException != null) {
			ObjectNode info = JsonNodeFactory.instance.objectNode();
			info.put("class", invalidException.getClass().getName());
			info.put("message", invalidException.getMessage());
			return info;
		} else {
			return MissingNode.getInstance();
		}
	}

	private static Exception reconstituteException(JsonNode exc) {
		if (exc.isMissingNode()) {
			return null;
		} else {
			Class<?> cls;
			try {
				cls = Class.forName(exc.path("class").asText());
				Constructor<?> cons = cls.getConstructor(String.class);
				return (Exception) cons.newInstance(exc.path("message").textValue());
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
					| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	public void markInvalid(String invalidReason) {
		markInvalid(invalidReason, null);
	}

	public void markInvalid(Exception e) {
		markInvalid(e.toString(), e);
	}

	public void markInvalid(String invalidReason, Exception e) {
		this.valid = false;
		this.invalidReason = invalidReason;
		this.invalidException = e;
	}

	public ReferenceTreatment getTreatment(Options options) {
		ReferenceTreatment treatment;
		if (!valid) {
			treatment = ERROR;
		} else if (!component.isConformingSite()) {
			treatment = INLINE_NONCONFORMING;
		} else if (component.hasMergeSemantics()) {
			treatment = MERGE;
		} else if (options.isInlined(component)) {
			treatment = INLINE_CONFORMING;
		} else {
			treatment = LOCALIZE;
		}
		return treatment;
	}

	// simple ref starts with alpha or underscore, ends with alphanum or underscore,
	// and has alphanums, underscores and
	// hyphens within.
	private static Pattern SIMPLE_REF_PAT = Pattern.compile("[_A-Za-z]([-A-Za-z0-9_]*[_A-Za-z0-9])?");

	public boolean isSimpleRef() {
		return fragment != null && SIMPLE_REF_PAT.matcher(fragment).matches();
	}

	public void rewriteSimpleRef() {
		if (isSimpleRef()) {
			String containerPath = component != null ? component.getContainerPath() : null;
			if (containerPath != null) {
				this.fragment = String.format("%s/%s", component.getContainerPath(), fragment);
			} else {
				throw new IllegalStateException();
			}
		}
		// if we're invalid because of a bad fragment, we should be valid now
		if (invalidReason != null && invalidReason.toLowerCase().contains("json pointer expression")) {
			try {
				this.pointer = JsonPointer.compile(fragment);
				this.valid = true;
			} catch (Exception e) {
				this.invalidReason = e.toString();
			}
		}
	}

	private URL normalizeUrl(URL base, String urlString) throws MalformedURLException, URISyntaxException {
		URL url = new URL(base, urlString);
		url = url.toURI().normalize().toURL();
		return url;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fragment == null) ? 0 : fragment.hashCode());
		result = prime * result + ((urlString == null) ? 0 : urlString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Reference other = (Reference) obj;
		if (fragment == null) {
			if (other.fragment != null)
				return false;
		} else if (!fragment.equals(other.fragment))
			return false;
		if (urlString == null) {
			if (other.urlString != null)
				return false;
		} else if (!urlString.equals(other.urlString))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.format("Ref[%s; valid=%s; comp=%s; canon=%s%s", refString, valid ? "yes" : "no: " + invalidReason,
				component, urlString, fragment != null ? "#" + fragment : "");
	}

	public enum ReferenceTreatment {
		RETAIN, INLINE_CONFORMING, INLINE_NONCONFORMING, LOCALIZE, ERROR, //
		// special treatment just for paths - just someone decided paths just HAVE to be
		// totally screwy! Grr...
		MERGE
	}
}
