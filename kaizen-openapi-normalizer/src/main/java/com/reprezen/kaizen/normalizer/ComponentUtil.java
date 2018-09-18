package com.reprezen.kaizen.normalizer;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * These are methods that are shared by the {@link V2State} and {@link V3State}
 * enums.
 * <p>
 * The only other way to share them is to add them as public methods of the
 * {@link Component} interface with default implementations, but that seems like
 * a pretty sucky design. Hence this class. Both of the enum classes obtain a
 * static instance during initialization.
 * 
 * @author Andy Lowry
 *
 */
public class ComponentUtil {

	private ComponentUtil() {
	};

	private static ComponentUtil instance = new ComponentUtil();

	public static ComponentUtil get() {
		return instance;
	}

	/* package */ String lowerCamelName(Component comp) {
		final Pattern newWord = Pattern.compile("_[a-z]");
		Matcher m = newWord.matcher(comp.name().toLowerCase());
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, m.group().substring(1).toUpperCase());
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/* pacakge */static Pattern makeNameRegex(String v2Container, String v3Container) {
		String prefixes = Arrays.asList( //
				v2Container != null ? "/" + v2Container : null, //
				v3Container != null ? "/components/" + v3Container : null) //
				.stream().filter(s -> s != null).collect(Collectors.joining("|"));
		return Pattern.compile(String.format("(%s)/([^/]+)", prefixes));
	}

	/* package */String getFileName(String path) {
		if (path.contains("/")) {
			String last = path.substring(path.lastIndexOf('/'));
			return last.isEmpty() ? null //
					: last.contains(".") ? last.substring(0, last.indexOf('.')) : last;
		} else {
			return null;
		}
	}

	/* package */ String getDefaultName(String path, Pattern regex, String fileName, Component component) {
		Matcher m = regex.matcher(path);
		return m.matches() ? fixName(m.group(2))
				: fileName != null ? fileName + "_" + component.name() : component.name();

	}

	/* package */ String fixName(String name) {
		return name.replaceAll("[^A-Za-z0-9._-]", "_");
	}
}
