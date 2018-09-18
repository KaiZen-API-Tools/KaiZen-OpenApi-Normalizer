package com.reprezen.kaizen.normalizer;

import java.net.URL;

public interface Component {
	boolean isConformingSite();

	boolean isDefiningSite();

	boolean hasMergeSemantics();

	Component getDefinedComponent();

	String getContainerPath();

	String name();

	String getPreferredName(String path, URL url);
}
