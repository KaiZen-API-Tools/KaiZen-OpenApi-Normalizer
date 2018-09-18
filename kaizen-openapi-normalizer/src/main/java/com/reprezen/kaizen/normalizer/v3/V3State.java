package com.reprezen.kaizen.normalizer.v3;

import java.net.URL;

import com.reprezen.kaizen.normalizer.Component;
import com.reprezen.kaizen.normalizer.ComponentUtil;

public enum V3State implements Component {
	MODEL, // root of model specificaiton
	PATH, // anywhere a path object is allowed
	SCHEMA, // anywhere else a schema object is allowed
	SCHEMA_DEF, // schema in `/definitions` object
	RESPONSE, RESPONSE_DEF, // similarly for other compoents..
	PARAMETER, PARAMETER_DEF, //
	EXAMPLE, EXAMPLE_DEF, //
	REQUEST_BODY, REQUEST_BODY_DEF, //
	HEADER, HEADER_DEF, //
	SECURITY_SCHEME, SECURITY_SCHEME_DEF, //
	LINK, LINK_DEF, //
	CALLBACK, CALLBACK_DEF, //
	OPERATION, //
	MEDIA_TYPE, //
	ENCODING, //
	ANON, OFFROAD; // anonymous and off-road states all use these values

	// note this must be initialized prior to executing the following static block
	private static ComponentUtil util = ComponentUtil.get();

	static {
		PATH.setConformingSite().setContainerPath("/paths").setMergeSemantics();
		SCHEMA.setConformingSite().setContainerPath();
		RESPONSE.setConformingSite().setContainerPath();
		PARAMETER.setConformingSite().setContainerPath();
		EXAMPLE.setConformingSite().setContainerPath();
		REQUEST_BODY.setConformingSite().setContainerPath("/components/requestBodies");
		HEADER.setConformingSite().setContainerPath();
		SECURITY_SCHEME.setConformingSite().setContainerPath();
		LINK.setConformingSite().setContainerPath();
		CALLBACK.setConformingSite().setContainerPath();

		PATH.setDefiningSite(PATH);
		SCHEMA_DEF.setDefiningSite(SCHEMA);
		RESPONSE_DEF.setDefiningSite(RESPONSE);
		PARAMETER_DEF.setDefiningSite(PARAMETER);
		EXAMPLE_DEF.setDefiningSite(EXAMPLE);
		REQUEST_BODY_DEF.setDefiningSite(REQUEST_BODY);
		HEADER_DEF.setDefiningSite(HEADER);
		SECURITY_SCHEME_DEF.setDefiningSite(SECURITY_SCHEME);
		LINK_DEF.setDefiningSite(LINK);
		CALLBACK_DEF.setDefiningSite(CALLBACK);
	}

	private boolean conformingSite = false;
	private boolean mergeSemnatics;
	private boolean definingSite = false;
	private V3State definedComponent = null;
	private String containerPath = null;

	private V3State setConformingSite() {
		this.conformingSite = true;
		return this;
	}

	private V3State setMergeSemantics() {
		this.mergeSemnatics = true;
		return this;
	}

	private V3State setDefiningSite(V3State definedComponent) {
		this.definingSite = true;
		this.definedComponent = definedComponent;
		return this;
	}

	private V3State setContainerPath(String containerPath) {
		this.containerPath = containerPath;
		return this;
	}

	private V3State setContainerPath() {
		// use enum name when it yields correct value
		return setContainerPath("/components/" + util.lowerCamelName(this) + "s");
	}

	@Override
	public boolean isConformingSite() {
		return conformingSite;
	}

	@Override
	public boolean hasMergeSemantics() {
		return mergeSemnatics;
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

	@Override
	public String getPreferredName(String path, URL url) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return name();
	}
}