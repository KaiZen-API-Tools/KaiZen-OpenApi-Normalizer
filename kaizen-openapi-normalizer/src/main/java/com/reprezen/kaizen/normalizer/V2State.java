package com.reprezen.kaizen.normalizer;

import static com.reprezen.kaizen.normalizer.ComponentUtil.makeNameRegex;

import java.net.URL;
import java.util.regex.Pattern;

public enum V2State implements Component {
	MODEL, // root of model specification
	PATH, // anywhere a path object is allowed
	SCHEMA, // anywhere else a schema object is allowed
	SCHEMA_DEF, // schema in `/definitions` object
	RESPONSE, RESPONSE_DEF, // similarly for other components
	PARAMETER, PARAMETER_DEF, // parameter in `/parameters` object
	OPERATION, // anywhere an operation object is allowed
	ANON, OFFROAD; // anonymous and off-road states all use these values

	// note this must be initialized before following static block executes
	private static ComponentUtil util = ComponentUtil.get();

	static {
		PATH.setConformingSite().setContainerPath().setMergeSemantics();
		SCHEMA.setConformingSite().setContainerPath("/definitions");
		RESPONSE.setConformingSite().setContainerPath();
		PARAMETER.setConformingSite().setContainerPath();

		PATH.setDefiningSite(PATH);
		SCHEMA_DEF.setDefiningSite(SCHEMA);
		RESPONSE_DEF.setDefiningSite(RESPONSE);
		PARAMETER_DEF.setDefiningSite(PARAMETER);
	}

	private boolean conformingSite = false;
	private boolean mergeSemantics = false;
	private boolean definingSite = false;
	private V2State definedComponent = null;
	private String containerPath = null;

	private V2State setConformingSite() {
		this.conformingSite = true;
		return this;
	}

	private V2State setMergeSemantics() {
		this.mergeSemantics = true;
		return this;
	}

	private V2State setDefiningSite(V2State definedComponent) {
		this.definingSite = true;
		this.definedComponent = definedComponent;
		return this;
	}

	private V2State setContainerPath(String containerPath) {
		this.containerPath = containerPath;
		return this;
	}

	private V2State setContainerPath() {
		return setContainerPath("/" + util.lowerCamelName(this) + "s");
	}

	@Override
	public boolean isConformingSite() {
		return conformingSite;
	}

	@Override
	public boolean hasMergeSemantics() {
		return mergeSemantics;
	}

	@Override
	public boolean isDefiningSite() {
		return definingSite;
	}

	@Override
	public Component getDefinedComponent() {
		return definedComponent;
	}

	@Override
	public String getContainerPath() {
		return containerPath;
	}

	private static Pattern schemaNameRegex = makeNameRegex("definitions", "schemas");
	private static Pattern responseNameRegex = makeNameRegex("responses", "responses");
	private static Pattern parameterNameRegex = makeNameRegex("parameters", "parameters");

	@Override
	public String getPreferredName(String path, URL url) {
		String urlPath = url.getPath();
		String fileName = util.fixName(util.getFileName(urlPath));
		switch (this) {
		case PATH: {
			String pathString = path.substring(path.lastIndexOf('/') + 1);
			return pathString.replaceAll("~1", "/").replaceAll("~0", "~");
		}
		case SCHEMA:
			return util.getDefaultName(path, schemaNameRegex, fileName, SCHEMA);
		case RESPONSE:
			return util.getDefaultName(path, responseNameRegex, fileName, RESPONSE);
		case PARAMETER:
			return util.getDefaultName(path, parameterNameRegex, fileName, PARAMETER);
		default:
			return null;
		}
	}

	@Override
	public String toString() {
		return name();
	}
}